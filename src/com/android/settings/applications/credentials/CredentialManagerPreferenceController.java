/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.applications.credentials;

import static androidx.lifecycle.Lifecycle.Event.ON_CREATE;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.credentials.CredentialManager;
import android.credentials.CredentialProviderInfo;
import android.credentials.SetEnabledProvidersException;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.OutcomeReceiver;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.service.autofill.AutofillServiceInfo;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.content.PackageMonitor;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.utils.ThreadUtils;
import com.android.settingslib.widget.TwoTargetPreference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

/** Queries available credential manager providers and adds preferences for them. */
public class CredentialManagerPreferenceController extends BasePreferenceController
        implements LifecycleObserver {
    public static final String ADD_SERVICE_DEVICE_CONFIG = "credential_manager_service_search_uri";

    /**
     * In the settings logic we should hide the list of additional credman providers if there is no
     * provider selected at the top. The current logic relies on checking whether the autofill
     * provider is set which won't work for cred-man only providers. Therefore when a CM only
     * provider is set we will set the autofill setting to be this placeholder.
     */
    public static final String AUTOFILL_CREDMAN_ONLY_PROVIDER_PLACEHOLDER = "credential-provider";

    private static final String TAG = "CredentialManagerPreferenceController";
    private static final String ALTERNATE_INTENT = "android.settings.SYNC_SETTINGS";
    private static final String PRIMARY_INTENT = "android.settings.CREDENTIAL_PROVIDER";
    private static final int MAX_SELECTABLE_PROVIDERS = 5;
    private static final String SETTINGS_ACTIVITY_INTENT_ACTION = "android.intent.action.MAIN";
    private static final String SETTINGS_ACTIVITY_INTENT_CATEGORY =
            "android.intent.category.LAUNCHER";

    private final PackageManager mPm;
    private final List<CredentialProviderInfo> mServices;
    private final Set<String> mEnabledPackageNames;
    private final @Nullable CredentialManager mCredentialManager;
    private final Executor mExecutor;
    private final Map<String, CombiPreference> mPrefs = new HashMap<>(); // key is package name
    private final List<ServiceInfo> mPendingServiceInfos = new ArrayList<>();
    private final Handler mHandler = new Handler();
    private final SettingContentObserver mSettingsContentObserver;

    private @Nullable FragmentManager mFragmentManager = null;
    private @Nullable Delegate mDelegate = null;
    private @Nullable String mFlagOverrideForTest = null;
    private @Nullable PreferenceScreen mPreferenceScreen = null;

    private boolean mVisibility = false;
    private boolean mIsWorkProfile = false;

    public CredentialManagerPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mPm = context.getPackageManager();
        mServices = new ArrayList<>();
        mEnabledPackageNames = new HashSet<>();
        mExecutor = ContextCompat.getMainExecutor(mContext);
        mCredentialManager =
                getCredentialManager(context, preferenceKey.equals("credentials_test"));
        mSettingsContentObserver =
                new SettingContentObserver(mHandler, context.getContentResolver());
        mSettingsContentObserver.register();
        mSettingsPackageMonitor.register(context, context.getMainLooper(), false);
    }

    private @Nullable CredentialManager getCredentialManager(Context context, boolean isTest) {
        if (isTest) {
            return null;
        }

        Object service = context.getSystemService(Context.CREDENTIAL_SERVICE);

        if (service != null && CredentialManager.isServiceEnabled(context)) {
            return (CredentialManager) service;
        }

        return null;
    }

    @Override
    public int getAvailabilityStatus() {
        if (mCredentialManager == null) {
            return UNSUPPORTED_ON_DEVICE;
        }

        if (!mVisibility) {
            return CONDITIONALLY_UNAVAILABLE;
        }

        if (mServices.isEmpty()) {
            return CONDITIONALLY_UNAVAILABLE;
        }

        // If we are in work profile mode and there is no user then we
        // should hide for now. We use CONDITIONALLY_UNAVAILABLE
        // because it is possible for the user to be set later.
        if (mIsWorkProfile) {
            UserHandle workProfile = getWorkProfileUserHandle();
            if (workProfile == null) {
                return CONDITIONALLY_UNAVAILABLE;
            }
        }

        return AVAILABLE;
    }

    @VisibleForTesting
    public boolean isConnected() {
        return mCredentialManager != null;
    }

    /**
     * Initializes the controller with the parent fragment and adds the controller to observe its
     * lifecycle. Also stores the fragment manager which is used to open dialogs.
     *
     * @param fragment the fragment to use as the parent
     * @param fragmentManager the fragment manager to use
     * @param intent the intent used to start the activity
     * @param delegate the delegate to send results back to
     * @param isWorkProfile whether this controller is under a work profile user
     */
    public void init(
            DashboardFragment fragment,
            FragmentManager fragmentManager,
            @Nullable Intent launchIntent,
            @NonNull Delegate delegate,
            boolean isWorkProfile) {
        fragment.getSettingsLifecycle().addObserver(this);
        mFragmentManager = fragmentManager;
        mIsWorkProfile = isWorkProfile;

        setDelegate(delegate);
        verifyReceivedIntent(launchIntent);

        // Recreate the content observers because the user might have changed.
        mSettingsContentObserver.unregister();
        mSettingsContentObserver.register();

        // When we set the mIsWorkProfile above we should try and force a refresh
        // so we can get the correct data.
        delegate.forceDelegateRefresh();
    }

    /**
     * Parses and sets the package component name. Returns a boolean as to whether this was
     * successful.
     */
    @VisibleForTesting
    boolean verifyReceivedIntent(Intent launchIntent) {
        if (launchIntent == null || launchIntent.getAction() == null) {
            return false;
        }

        final String action = launchIntent.getAction();
        final boolean isCredProviderAction = TextUtils.equals(action, PRIMARY_INTENT);
        final boolean isExistingAction = TextUtils.equals(action, ALTERNATE_INTENT);
        final boolean isValid = isCredProviderAction || isExistingAction;

        if (!isValid) {
            return false;
        }

        // After this point we have received a set credential manager provider intent
        // so we should return a cancelled result if the data we got is no good.
        if (launchIntent.getData() == null) {
            setActivityResult(Activity.RESULT_CANCELED);
            return false;
        }

        String packageName = launchIntent.getData().getSchemeSpecificPart();
        if (packageName == null) {
            setActivityResult(Activity.RESULT_CANCELED);
            return false;
        }

        mPendingServiceInfos.clear();
        for (CredentialProviderInfo cpi : mServices) {
            final ServiceInfo serviceInfo = cpi.getServiceInfo();
            if (serviceInfo.packageName.equals(packageName)) {
                mPendingServiceInfos.add(serviceInfo);
            }
        }

        // Don't set the result as RESULT_OK here because we should wait for the user to
        // enable the provider.
        if (!mPendingServiceInfos.isEmpty()) {
            return true;
        }

        setActivityResult(Activity.RESULT_CANCELED);
        return false;
    }

    @VisibleForTesting
    void setDelegate(Delegate delegate) {
        mDelegate = delegate;
    }

    private void setActivityResult(int resultCode) {
        if (mDelegate == null) {
            Log.e(TAG, "Missing delegate");
            return;
        }
        mDelegate.setActivityResult(resultCode);
    }

    private void handleIntent() {
        List<ServiceInfo> pendingServiceInfos = new ArrayList<>(mPendingServiceInfos);
        mPendingServiceInfos.clear();
        if (pendingServiceInfos.isEmpty()) {
            return;
        }

        ServiceInfo serviceInfo = pendingServiceInfos.get(0);
        ApplicationInfo appInfo = serviceInfo.applicationInfo;
        CharSequence appName = "";
        if (appInfo.nonLocalizedLabel != null) {
            appName = appInfo.loadLabel(mPm);
        }

        // Stop if there is no name.
        if (TextUtils.isEmpty(appName)) {
            return;
        }

        NewProviderConfirmationDialogFragment fragment =
                newNewProviderConfirmationDialogFragment(
                        serviceInfo.packageName, appName, /* setActivityResult= */ true);
        if (fragment == null || mFragmentManager == null) {
            return;
        }

        fragment.show(mFragmentManager, NewProviderConfirmationDialogFragment.TAG);
    }

    @OnLifecycleEvent(ON_CREATE)
    void onCreate(LifecycleOwner lifecycleOwner) {
        update();
    }

    private void update() {
        if (mCredentialManager == null) {
            return;
        }

        setAvailableServices(
                mCredentialManager.getCredentialProviderServices(
                        getUser(), CredentialManager.PROVIDER_FILTER_USER_PROVIDERS_ONLY),
                null);
    }

    private Set<ComponentName> buildComponentNameSet(
            List<CredentialProviderInfo> providers, boolean removeNonPrimary) {
        Set<ComponentName> output = new HashSet<>();

        for (CredentialProviderInfo cpi : providers) {
            if (removeNonPrimary && !cpi.isPrimary()) {
                continue;
            }

            output.add(cpi.getComponentName());
        }

        return output;
    }

    private void updateFromExternal() {
        if (mCredentialManager == null) {
            return;
        }

        // Get the list of new providers and components.
        List<CredentialProviderInfo> newProviders =
                mCredentialManager.getCredentialProviderServices(
                        getUser(), CredentialManager.PROVIDER_FILTER_USER_PROVIDERS_ONLY);
        Set<ComponentName> newComponents = buildComponentNameSet(newProviders, false);
        Set<ComponentName> newPrimaryComponents = buildComponentNameSet(newProviders, true);

        // Get the list of old components
        Set<ComponentName> oldComponents = buildComponentNameSet(mServices, false);
        Set<ComponentName> oldPrimaryComponents = buildComponentNameSet(mServices, true);

        // If the sets are equal then don't update the UI.
        if (oldComponents.equals(newComponents)
                && oldPrimaryComponents.equals(newPrimaryComponents)) {
            return;
        }

        setAvailableServices(newProviders, null);

        if (mPreferenceScreen != null) {
            displayPreference(mPreferenceScreen);
        }

        if (mDelegate != null) {
            mDelegate.forceDelegateRefresh();
        }
    }

    private void setVisibility(boolean newVisibility) {
        if (newVisibility == mVisibility) {
            return;
        }

        mVisibility = newVisibility;
        if (mDelegate != null) {
            mDelegate.forceDelegateRefresh();
        }
    }

    @VisibleForTesting
    void setAvailableServices(
            List<CredentialProviderInfo> availableServices, String flagOverrideForTest) {
        mFlagOverrideForTest = flagOverrideForTest;
        mServices.clear();
        mServices.addAll(availableServices);

        // If there is a pending dialog then show it.
        handleIntent();

        mEnabledPackageNames.clear();
        for (CredentialProviderInfo cpi : availableServices) {
            if (cpi.isEnabled() && !cpi.isPrimary()) {
                mEnabledPackageNames.add(cpi.getServiceInfo().packageName);
            }
        }

        for (String packageName : mPrefs.keySet()) {
            mPrefs.get(packageName).setChecked(mEnabledPackageNames.contains(packageName));
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        // Since the UI is being cleared, clear any refs.
        mPrefs.clear();

        mPreferenceScreen = screen;
        PreferenceGroup group = screen.findPreference(getPreferenceKey());
        group.removeAll();

        Context context = screen.getContext();
        mPrefs.putAll(buildPreferenceList(context, group));
    }

    /**
     * Gets the preference that allows to add a new cred man service.
     *
     * @return the pref to be added
     */
    @VisibleForTesting
    public Preference newAddServicePreference(String searchUri, Context context) {
        final Intent addNewServiceIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(searchUri));
        final Preference preference = new Preference(context);
        preference.setOnPreferenceClickListener(
                p -> {
                    context.startActivityAsUser(addNewServiceIntent, UserHandle.of(getUser()));
                    return true;
                });
        preference.setTitle(R.string.print_menu_item_add_service);
        preference.setOrder(Integer.MAX_VALUE - 1);
        preference.setPersistent(false);

        // Try to set the icon this should fail in a test environment but work
        // in the actual app.
        try {
            preference.setIcon(R.drawable.ic_add_24dp);
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Failed to find icon for add services link", e);
        }
        return preference;
    }

    /** Aggregates the list of services and builds a list of UI prefs to show. */
    @VisibleForTesting
    public Map<String, CombiPreference> buildPreferenceList(
            Context context, PreferenceGroup group) {
        // Get the selected autofill provider. If it is the placeholder then replace it with an
        // empty string.
        String selectedAutofillProvider =
                DefaultCombinedPicker.getSelectedAutofillProvider(mContext, getUser());
        if (TextUtils.equals(
                selectedAutofillProvider, AUTOFILL_CREDMAN_ONLY_PROVIDER_PLACEHOLDER)) {
            selectedAutofillProvider = "";
        }

        // Get the list of combined providers.
        List<CombinedProviderInfo> providers =
                CombinedProviderInfo.buildMergedList(
                        AutofillServiceInfo.getAvailableServices(context, getUser()),
                        mServices,
                        selectedAutofillProvider);

        // Get the provider that is displayed at the top. If there is none then hide
        // everything.
        CombinedProviderInfo topProvider = CombinedProviderInfo.getTopProvider(providers);
        if (topProvider == null) {
            setVisibility(false);
            return new HashMap<>();
        }

        Map<String, CombiPreference> output = new HashMap<>();
        for (CombinedProviderInfo combinedInfo : providers) {
            final String packageName = combinedInfo.getApplicationInfo().packageName;

            // If this provider is displayed at the top then we should not show it.
            if (topProvider != null
                    && topProvider.getApplicationInfo().packageName.equals(packageName)) {
                continue;
            }

            // If this is an autofill provider then don't show it here.
            if (combinedInfo.getCredentialProviderInfos().isEmpty()) {
                continue;
            }

            // Get the settings activity.
            CharSequence settingsActivity =
                    combinedInfo.getCredentialProviderInfos().get(0).getSettingsActivity();

            Drawable icon = combinedInfo.getAppIcon(context, getUser());
            CharSequence title = combinedInfo.getAppName(context);

            // Build the pref and add it to the output & group.
            CombiPreference pref =
                    addProviderPreference(
                            context,
                            title == null ? "" : title,
                            icon,
                            packageName,
                            combinedInfo.getSettingsSubtitle(),
                            settingsActivity);
            output.put(packageName, pref);
            group.addPreference(pref);
        }

        // Set the visibility if we have services.
        setVisibility(!output.isEmpty());

        return output;
    }

    /** Creates a preference object based on the provider info. */
    @VisibleForTesting
    public CombiPreference createPreference(Context context, CredentialProviderInfo service) {
        CharSequence label = service.getLabel(context);
        return addProviderPreference(
                context,
                label == null ? "" : label,
                service.getServiceIcon(mContext),
                service.getServiceInfo().packageName,
                service.getSettingsSubtitle(),
                service.getSettingsActivity());
    }

    /**
     * Enables the package name as an enabled credential manager provider.
     *
     * @param packageName the package name to enable
     */
    @VisibleForTesting
    public boolean togglePackageNameEnabled(String packageName) {
        if (mEnabledPackageNames.size() >= MAX_SELECTABLE_PROVIDERS) {
            return false;
        } else {
            mEnabledPackageNames.add(packageName);
            commitEnabledPackages();
            return true;
        }
    }

    /**
     * Disables the package name as a credential manager provider.
     *
     * @param packageName the package name to disable
     */
    @VisibleForTesting
    public void togglePackageNameDisabled(String packageName) {
        mEnabledPackageNames.remove(packageName);
        commitEnabledPackages();
    }

    /** Returns the enabled credential manager provider package names. */
    @VisibleForTesting
    public Set<String> getEnabledProviders() {
        return mEnabledPackageNames;
    }

    /**
     * Returns the enabled credential manager provider flattened component names that can be stored
     * in the setting.
     */
    @VisibleForTesting
    public List<String> getEnabledSettings() {
        // Get all the component names that match the enabled package names.
        List<String> enabledServices = new ArrayList<>();
        for (CredentialProviderInfo service : mServices) {
            ComponentName cn = service.getServiceInfo().getComponentName();
            if (mEnabledPackageNames.contains(service.getServiceInfo().packageName)) {
                enabledServices.add(cn.flattenToString());
            }
        }

        return enabledServices;
    }

    private CombiPreference addProviderPreference(
            @NonNull Context prefContext,
            @NonNull CharSequence title,
            @Nullable Drawable icon,
            @NonNull String packageName,
            @Nullable CharSequence subtitle,
            @Nullable CharSequence settingsActivity) {
        final CombiPreference pref =
                new CombiPreference(prefContext, mEnabledPackageNames.contains(packageName));
        pref.setTitle(title);

        if (icon != null) {
            pref.setIcon(icon);
        }

        pref.setLayoutResource(R.layout.preference_icon_credman);

        if (subtitle != null) {
            pref.setSummary(subtitle);
        }

        pref.setPreferenceListener(
                new CombiPreference.OnCombiPreferenceClickListener() {
                    @Override
                    public void onCheckChanged(CombiPreference p, boolean isChecked) {
                        if (isChecked) {
                            if (mEnabledPackageNames.size() >= MAX_SELECTABLE_PROVIDERS) {
                                // Show the error if too many enabled.
                                pref.setChecked(false);
                                final DialogFragment fragment = newErrorDialogFragment();

                                if (fragment == null || mFragmentManager == null) {
                                    return;
                                }

                                fragment.show(mFragmentManager, ErrorDialogFragment.TAG);
                                return;
                            }

                            togglePackageNameEnabled(packageName);

                            // Enable all prefs.
                            if (mPrefs.containsKey(packageName)) {
                                mPrefs.get(packageName).setChecked(true);
                            }
                        } else {
                            togglePackageNameDisabled(packageName);
                        }
                    }

                    @Override
                    public void onLeftSideClicked() {
                        if (settingsActivity == null) {
                            Log.w(TAG, "settingsActivity was null");
                            return;
                        }

                        String settingsActivityStr = String.valueOf(settingsActivity);
                        ComponentName cn = ComponentName.unflattenFromString(settingsActivityStr);
                        if (cn == null) {
                            Log.w(
                                    TAG,
                                    "Failed to deserialize settingsActivity attribute, we got: "
                                            + settingsActivityStr);
                            return;
                        }

                        Intent intent = new Intent(SETTINGS_ACTIVITY_INTENT_ACTION);
                        intent.addCategory(SETTINGS_ACTIVITY_INTENT_CATEGORY);
                        intent.setComponent(cn);

                        Context context = mContext;
                        int currentUserId = getUser();
                        int contextUserId = context.getUser().getIdentifier();

                        if (currentUserId != contextUserId) {
                            Log.d(
                                    TAG,
                                    "onLeftSideClicked(): using context for current user ("
                                            + currentUserId
                                            + ") instead of user "
                                            + contextUserId
                                            + " on headless system user mode");
                            context =
                                    context.createContextAsUser(
                                            UserHandle.of(currentUserId), /* flags= */ 0);
                        }

                        context.startActivity(intent);
                    }
                });

        return pref;
    }

    private void commitEnabledPackages() {
        // Commit using the CredMan API.
        if (mCredentialManager == null) {
            return;
        }

        // Get the existing primary providers since we don't touch them in
        // this part of the UI we should just copy them over.
        Set<String> primaryServices = new HashSet<>();
        List<String> enabledServices = getEnabledSettings();
        for (CredentialProviderInfo service : mServices) {
            if (service.isPrimary()) {
                String flattened = service.getServiceInfo().getComponentName().flattenToString();
                primaryServices.add(flattened);
                enabledServices.add(flattened);
            }
        }

        mCredentialManager.setEnabledProviders(
                new ArrayList<>(primaryServices),
                enabledServices,
                getUser(),
                mExecutor,
                new OutcomeReceiver<Void, SetEnabledProvidersException>() {
                    @Override
                    public void onResult(Void result) {
                        Log.i(TAG, "setEnabledProviders success");
                        updateFromExternal();
                    }

                    @Override
                    public void onError(SetEnabledProvidersException e) {
                        Log.e(TAG, "setEnabledProviders error: " + e.toString());
                    }
                });
    }

    /** Create the new provider confirmation dialog. */
    private @Nullable NewProviderConfirmationDialogFragment
            newNewProviderConfirmationDialogFragment(
                    @NonNull String packageName,
                    @NonNull CharSequence appName,
                    boolean setActivityResult) {
        DialogHost host =
                new DialogHost() {
                    @Override
                    public void onDialogClick(int whichButton) {
                        completeEnableProviderDialogBox(
                                whichButton, packageName, setActivityResult);
                    }

                    @Override
                    public void onCancel() {}
                };

        return new NewProviderConfirmationDialogFragment(host, packageName, appName);
    }

    @VisibleForTesting
    void completeEnableProviderDialogBox(
            int whichButton, String packageName, boolean setActivityResult) {
        int activityResult = -1;
        if (whichButton == DialogInterface.BUTTON_POSITIVE) {
            if (togglePackageNameEnabled(packageName)) {
                // Enable all prefs.
                if (mPrefs.containsKey(packageName)) {
                    mPrefs.get(packageName).setChecked(true);
                }
                activityResult = Activity.RESULT_OK;
            } else {
                // There are too many providers so set the result as cancelled.
                activityResult = Activity.RESULT_CANCELED;

                // Show the error if too many enabled.
                final DialogFragment fragment = newErrorDialogFragment();

                if (fragment == null || mFragmentManager == null) {
                    return;
                }

                fragment.show(mFragmentManager, ErrorDialogFragment.TAG);
            }
        } else {
            // The user clicked the cancel button so send that result back.
            activityResult = Activity.RESULT_CANCELED;
        }

        // If the dialog is being shown because of the intent we should
        // return a result.
        if (activityResult == -1 || !setActivityResult) {
            setActivityResult(activityResult);
        }
    }

    private @Nullable ErrorDialogFragment newErrorDialogFragment() {
        DialogHost host =
                new DialogHost() {
                    @Override
                    public void onDialogClick(int whichButton) {}

                    @Override
                    public void onCancel() {}
                };

        return new ErrorDialogFragment(host);
    }

    protected int getUser() {
        if (mIsWorkProfile) {
            UserHandle workProfile = getWorkProfileUserHandle();
            if (workProfile != null) {
                return workProfile.getIdentifier();
            }
        }
        return UserHandle.myUserId();
    }

    private @Nullable UserHandle getWorkProfileUserHandle() {
        if (mIsWorkProfile) {
            return Utils.getManagedProfile(UserManager.get(mContext));
        }

        return null;
    }

    /** Called when the dialog button is clicked. */
    private static interface DialogHost {
        void onDialogClick(int whichButton);

        void onCancel();
    }

    /** Called to send messages back to the parent fragment. */
    public static interface Delegate {
        void setActivityResult(int resultCode);

        void forceDelegateRefresh();
    }

    /**
     * Monitor coming and going credman services and calls {@link #DefaultCombinedPicker} when
     * necessary
     */
    private final PackageMonitor mSettingsPackageMonitor =
            new PackageMonitor() {
                @Override
                public void onPackageAdded(String packageName, int uid) {
                    ThreadUtils.postOnMainThread(() -> updateFromExternal());
                }

                @Override
                public void onPackageModified(String packageName) {
                    ThreadUtils.postOnMainThread(() -> updateFromExternal());
                }

                @Override
                public void onPackageRemoved(String packageName, int uid) {
                    ThreadUtils.postOnMainThread(() -> updateFromExternal());
                }
            };

    /** Dialog fragment parent class. */
    private abstract static class CredentialManagerDialogFragment extends DialogFragment
            implements DialogInterface.OnClickListener {

        public static final String TAG = "CredentialManagerDialogFragment";
        public static final String PACKAGE_NAME_KEY = "package_name";
        public static final String APP_NAME_KEY = "app_name";

        private DialogHost mDialogHost;

        CredentialManagerDialogFragment(DialogHost dialogHost) {
            super();
            mDialogHost = dialogHost;
        }

        public DialogHost getDialogHost() {
            return mDialogHost;
        }

        @Override
        public void onCancel(@NonNull DialogInterface dialog) {
            getDialogHost().onCancel();
        }
    }

    /** Dialog showing error when too many providers are selected. */
    public static class ErrorDialogFragment extends CredentialManagerDialogFragment {

        ErrorDialogFragment(DialogHost dialogHost) {
            super(dialogHost);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(getContext().getString(R.string.credman_error_message_title))
                    .setMessage(getContext().getString(R.string.credman_error_message))
                    .setPositiveButton(android.R.string.ok, this)
                    .create();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {}
    }

    /**
     * Confirmation dialog fragment shows a dialog to the user to confirm that they would like to
     * enable the new provider.
     */
    public static class NewProviderConfirmationDialogFragment
            extends CredentialManagerDialogFragment {

        NewProviderConfirmationDialogFragment(
                DialogHost dialogHost, @NonNull String packageName, @NonNull CharSequence appName) {
            super(dialogHost);

            final Bundle argument = new Bundle();
            argument.putString(PACKAGE_NAME_KEY, packageName);
            argument.putCharSequence(APP_NAME_KEY, appName);
            setArguments(argument);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Bundle bundle = getArguments();
            final Context context = getContext();
            final CharSequence appName =
                    bundle.getCharSequence(CredentialManagerDialogFragment.APP_NAME_KEY);
            final String title =
                    context.getString(R.string.credman_enable_confirmation_message_title, appName);
            final String message =
                    context.getString(R.string.credman_enable_confirmation_message, appName);

            return new AlertDialog.Builder(getActivity())
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, this)
                    .setNegativeButton(android.R.string.cancel, this)
                    .create();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            getDialogHost().onDialogClick(which);
        }
    }

    /** Updates the list if setting content changes. */
    private final class SettingContentObserver extends ContentObserver {

        private final Uri mAutofillService =
                Settings.Secure.getUriFor(Settings.Secure.AUTOFILL_SERVICE);

        private final Uri mCredentialService =
                Settings.Secure.getUriFor(Settings.Secure.CREDENTIAL_SERVICE);

        private final Uri mCredentialPrimaryService =
                Settings.Secure.getUriFor(Settings.Secure.CREDENTIAL_SERVICE_PRIMARY);

        private ContentResolver mContentResolver;

        public SettingContentObserver(Handler handler, ContentResolver contentResolver) {
            super(handler);
            mContentResolver = contentResolver;
        }

        public void register() {
            mContentResolver.registerContentObserver(mAutofillService, false, this, getUser());
            mContentResolver.registerContentObserver(mCredentialService, false, this, getUser());
            mContentResolver.registerContentObserver(
                    mCredentialPrimaryService, false, this, getUser());
        }

        public void unregister() {
            mContentResolver.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            updateFromExternal();
        }
    }

    /** CombiPreference is a combination of TwoTargetPreference and SwitchPreference. */
    public static class CombiPreference extends TwoTargetPreference {

        private final Listener mListener = new Listener();

        private class Listener implements View.OnClickListener {
            @Override
            public void onClick(View buttonView) {
                // Forward the event.
                if (mSwitch != null) {
                    mOnClickListener.onCheckChanged(CombiPreference.this, mSwitch.isChecked());
                }
            }
        }

        // Stores a reference to the switch view.
        private @Nullable CompoundButton mSwitch;

        // Switch text for on and off states
        private @NonNull boolean mChecked = false;
        private @Nullable OnCombiPreferenceClickListener mOnClickListener = null;

        public interface OnCombiPreferenceClickListener {
            /** Called when the check is updated */
            void onCheckChanged(CombiPreference p, boolean isChecked);

            /** Called when the left side is clicked. */
            void onLeftSideClicked();
        }

        public CombiPreference(Context context, boolean initialValue) {
            super(context);
            mChecked = initialValue;
        }

        /** Set the new checked value */
        public void setChecked(boolean isChecked) {
            // Don't update if we don't need too.
            if (mChecked == isChecked) {
                return;
            }

            mChecked = isChecked;

            if (mSwitch != null) {
                mSwitch.setChecked(isChecked);
            }
        }

        public boolean isChecked() {
            return mChecked;
        }

        public void setPreferenceListener(OnCombiPreferenceClickListener onClickListener) {
            mOnClickListener = onClickListener;
        }

        @Override
        protected int getSecondTargetResId() {
            return com.android.settingslib.R.layout.preference_widget_primary_switch;
        }

        @Override
        public void onBindViewHolder(PreferenceViewHolder view) {
            super.onBindViewHolder(view);

            // Setup the switch.
            View checkableView =
                    view.itemView.findViewById(com.android.settingslib.R.id.switchWidget);
            if (checkableView instanceof CompoundButton switchView) {
                switchView.setChecked(mChecked);
                switchView.setOnClickListener(mListener);

                // Store this for later.
                mSwitch = switchView;
            }

            super.setOnPreferenceClickListener(
                    new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            if (mOnClickListener != null) {
                                mOnClickListener.onLeftSideClicked();
                            }

                            return true;
                        }
                    });
        }
    }
}
