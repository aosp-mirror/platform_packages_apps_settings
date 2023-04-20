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
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.text.TextUtils;
import com.android.settingslib.utils.ThreadUtils;
import com.android.internal.content.PackageMonitor;
import android.util.IconDrawableFactory;
import android.util.Log;

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
import androidx.preference.SwitchPreference;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.content.PackageMonitor;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.utils.ThreadUtils;

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
    private static final String TAG = "CredentialManagerPreferenceController";
    private static final String ALTERNATE_INTENT = "android.settings.SYNC_SETTINGS";
    private static final int MAX_SELECTABLE_PROVIDERS = 5;

    private final PackageManager mPm;
    private final IconDrawableFactory mIconFactory;
    private final List<CredentialProviderInfo> mServices;
    private final Set<ServiceInfo> mEnabledServiceInfos;
    private final @Nullable CredentialManager mCredentialManager;
    private final Executor mExecutor;
    private final Map<ServiceInfo, SwitchPreference> mPrefs = new HashMap<>();
    private final List<ServiceInfo> mPendingServiceInfos = new ArrayList<>();
    private final Handler mHandler = new Handler();

    private @Nullable FragmentManager mFragmentManager = null;
    private @Nullable Delegate mDelegate = null;
    private @Nullable String mFlagOverrideForTest = null;
    private @Nullable PreferenceScreen mPreferenceScreen = null;

    public CredentialManagerPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mPm = context.getPackageManager();
        mIconFactory = IconDrawableFactory.newInstance(mContext);
        mServices = new ArrayList<>();
        mEnabledServiceInfos = new HashSet<>();
        mExecutor = ContextCompat.getMainExecutor(mContext);
        mCredentialManager =
                getCredentialManager(context, preferenceKey.equals("credentials_test"));
        new SettingContentObserver(mHandler).register(context.getContentResolver());
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
     */
    public void init(
            DashboardFragment fragment,
            FragmentManager fragmentManager,
            @Nullable Intent launchIntent,
            @NonNull Delegate delegate) {
        fragment.getSettingsLifecycle().addObserver(this);
        mFragmentManager = fragmentManager;
        setDelegate(delegate);
        verifyReceivedIntent(launchIntent);
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
        final boolean isCredProviderAction =
                TextUtils.equals(action, Settings.ACTION_CREDENTIAL_PROVIDER);
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
        CharSequence serviceLabel = "";
        if (serviceInfo.nonLocalizedLabel != null) {
            serviceLabel = serviceInfo.loadLabel(mPm);
        }

        // Stop if there is no service label.
        if (TextUtils.isEmpty(serviceLabel)) {
            return;
        }

        NewProviderConfirmationDialogFragment fragment =
                newNewProviderConfirmationDialogFragment(serviceInfo, serviceLabel, /* setActivityResult= */ true);
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

    private void updateFromExternal() {
        update();

        if (mPreferenceScreen != null) {
            displayPreference(mPreferenceScreen);
        }
    }

    @VisibleForTesting
    void setAvailableServices(
            List<CredentialProviderInfo> availableServices,
            String flagOverrideForTest) {
        mFlagOverrideForTest = flagOverrideForTest;
        mServices.clear();
        mServices.addAll(availableServices);

        // If there is a pending dialog then show it.
        handleIntent();

        mEnabledServiceInfos.clear();
        for (CredentialProviderInfo cpi : availableServices) {
            if (cpi.isEnabled()) {
                mEnabledServiceInfos.add(cpi.getServiceInfo());
            }
        }

        for (ServiceInfo serviceInfo : mPrefs.keySet()) {
            mPrefs.get(serviceInfo).setChecked(mEnabledServiceInfos.contains(serviceInfo));
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return mServices.isEmpty() ? CONDITIONALLY_UNAVAILABLE : AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        // Since the UI is being cleared, clear any refs.
        mPrefs.clear();

        mPreferenceScreen = screen;
        PreferenceGroup group = screen.findPreference(getPreferenceKey());
        Context context = screen.getContext();
        mPrefs.putAll(buildPreferenceList(context, group));

        // Add the "add service" button only when there are no providers.
        if (mPrefs.isEmpty()) {
            String searchUri = getAddServiceUri(context);
            if (!TextUtils.isEmpty(searchUri)) {
                group.addPreference(newAddServicePreference(searchUri, context));
            }
        }
    }

    /**
     * Returns the "add service" URI to show the play store. It will first try and use the
     * credential manager specific search URI and if that is null it will fallback to the autofill
     * one.
     */
    public @NonNull String getAddServiceUri(@NonNull Context context) {
        // Check the credential manager gflag for a link.
        String searchUri =
                DeviceConfig.getString(
                        DeviceConfig.NAMESPACE_CREDENTIAL,
                        ADD_SERVICE_DEVICE_CONFIG,
                        mFlagOverrideForTest);
        if (!TextUtils.isEmpty(searchUri)) {
            return searchUri;
        }

        // If not fall back on autofill.
        return Settings.Secure.getStringForUser(
                context.getContentResolver(),
                Settings.Secure.AUTOFILL_SERVICE_SEARCH_URI,
                getUser());
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
    public Map<ServiceInfo, SwitchPreference> buildPreferenceList(
            Context context, PreferenceGroup group) {
        // Build the pref list.
        Map<ServiceInfo, SwitchPreference> output = new HashMap<>();
        for (CredentialProviderInfo service : mServices) {
            // Build the pref and add it to the output & group.
            SwitchPreference pref = createPreference(mContext, service);
            output.put(service.getServiceInfo(), pref);
            group.addPreference(pref);
        }

        return output;
    }

    /** Creates a preference object based on the provider info. */
    @VisibleForTesting
    public SwitchPreference createPreference(Context context, CredentialProviderInfo service) {
        final String packageName = service.getServiceInfo().packageName;
        CharSequence label = service.getLabel(context);

        // If there is no label then show the package name.
        if (label == null || TextUtils.isEmpty(label)) {
            label = packageName;
        }

        return addProviderPreference(
                context,
                label,
                service.getServiceIcon(mContext),
                packageName,
                service.getSettingsSubtitle(),
                service.getServiceInfo());
    }

    /**
     * Enables the service as an enabled credential manager provider.
     *
     * @param service the service to enable
     */
    @VisibleForTesting
    public boolean toggleServiceInfoEnabled(ServiceInfo service) {
        if (mEnabledServiceInfos.size() >= MAX_SELECTABLE_PROVIDERS) {
            return false;
        } else {
            mEnabledServiceInfos.add(service);
            commitEnabledServices();
            return true;
        }
    }

    /**
     * Disables the service as a credential manager provider.
     *
     * @param service the service to disable
     */
    @VisibleForTesting
    public void toggleServiceInfoDisabled(ServiceInfo service) {
        mEnabledServiceInfos.remove(service);
        commitEnabledServices();
    }

    /** Returns the enabled credential manager provider package names. */
    @VisibleForTesting
    public Set<ServiceInfo> getEnabledProviders() {
        return mEnabledServiceInfos;
    }

    /**
     * Returns the enabled credential manager provider flattened component names that can be stored
     * in the setting.
     */
    @VisibleForTesting
    public List<String> getEnabledSettings() {
        // Get all the component names that match the enabled package names.
        List<String> enabledServices = new ArrayList<>();
        for (ServiceInfo service : mEnabledServiceInfos) {
            enabledServices.add(service.getComponentName().flattenToString());
        }

        return enabledServices;
    }

    private SwitchPreference addProviderPreference(
            @NonNull Context prefContext,
            @NonNull CharSequence title,
            @Nullable Drawable icon,
            @NonNull String packageName,
            @Nullable CharSequence subtitle,
            @NonNull ServiceInfo service) {
        final SwitchPreference pref = new SwitchPreference(prefContext);
        pref.setTitle(title);
        pref.setChecked(mEnabledServiceInfos.contains(service));

        if (icon != null) {
            pref.setIcon(Utils.getSafeIcon(icon));
        }

        if (subtitle != null) {
            pref.setSummary(subtitle);
        }

        pref.setOnPreferenceClickListener(
                p -> {
                    boolean isChecked = pref.isChecked();

                    if (isChecked) {
                        // Since we are enabling it we should confirm the user decision with a
                        // dialog box.
                        NewProviderConfirmationDialogFragment fragment =
                                newNewProviderConfirmationDialogFragment(
                                        service, title, /* setActivityResult= */ false);
                        if (fragment == null || mFragmentManager == null) {
                            return true;
                        }

                        fragment.show(mFragmentManager, NewProviderConfirmationDialogFragment.TAG);

                        return true;
                    } else {
                        // If we are disabling the last enabled provider then show a warning.
                        if (mEnabledServiceInfos.size() <= 1) {
                            final DialogFragment fragment =
                                    newConfirmationDialogFragment(service, title, pref);

                            if (fragment == null || mFragmentManager == null) {
                                return true;
                            }

                            fragment.show(mFragmentManager, ConfirmationDialogFragment.TAG);
                        } else {
                            toggleServiceInfoDisabled(service);
                        }
                    }

                    return true;
                });

        return pref;
    }

    private void commitEnabledServices() {
        // Commit using the CredMan API.
        if (mCredentialManager == null) {
            return;
        }

        List<String> enabledServices = getEnabledSettings();
        mCredentialManager.setEnabledProviders(
                enabledServices,
                getUser(),
                mExecutor,
                new OutcomeReceiver<Void, SetEnabledProvidersException>() {
                    @Override
                    public void onResult(Void result) {
                        Log.i(TAG, "setEnabledProviders success");
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
                    @NonNull ServiceInfo service,
                    @NonNull CharSequence appName,
                    boolean setActivityResult) {
        DialogHost host =
                new DialogHost() {
                    @Override
                    public void onDialogClick(int whichButton) {
                        completeEnableProviderDialogBox(whichButton, service, setActivityResult);
                    }
                };

        return new NewProviderConfirmationDialogFragment(host, service, appName);
    }

    @VisibleForTesting
    void completeEnableProviderDialogBox(int whichButton, ServiceInfo service, boolean setActivityResult) {
        int activityResult = -1;
        if (whichButton == DialogInterface.BUTTON_POSITIVE) {
            if (toggleServiceInfoEnabled(service)) {
                // Enable all prefs.
                if (mPrefs.containsKey(service)) {
                    mPrefs.get(service).setChecked(true);
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
                };

        return new ErrorDialogFragment(host);
    }

    private @Nullable ConfirmationDialogFragment newConfirmationDialogFragment(
            @NonNull ServiceInfo service,
            @NonNull CharSequence appName,
            @NonNull SwitchPreference pref) {
        DialogHost host =
                new DialogHost() {
                    @Override
                    public void onDialogClick(int whichButton) {
                        if (whichButton == DialogInterface.BUTTON_POSITIVE) {
                            // Since the package is now enabled then we
                            // should remove it from the enabled list.
                            toggleServiceInfoDisabled(service);
                        } else if (whichButton == DialogInterface.BUTTON_NEGATIVE) {
                            // Set the checked back to true because we
                            // backed out of turning this off.
                            pref.setChecked(true);
                        }
                    }
                };

        return new ConfirmationDialogFragment(host, service, appName);
    }

    private int getUser() {
        UserHandle workUser = getWorkProfileUser();
        return workUser != null ? workUser.getIdentifier() : UserHandle.myUserId();
    }

    /** Called when the dialog button is clicked. */
    private static interface DialogHost {
        void onDialogClick(int whichButton);
    }

    /** Called to send messages back to the parent fragment. */
    public static interface Delegate {
        void setActivityResult(int resultCode);
    }

    /**
     * Monitor coming and going credman services and calls {@link #update()} when necessary
     */
    private final PackageMonitor mSettingsPackageMonitor = new PackageMonitor() {
        @Override
        public void onPackageAdded(String packageName, int uid) {
            ThreadUtils.postOnMainThread(() -> update());
        }

        @Override
        public void onPackageModified(String packageName) {
            ThreadUtils.postOnMainThread(() -> update());
        }

        @Override
        public void onPackageRemoved(String packageName, int uid) {
            ThreadUtils.postOnMainThread(() -> update());
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
     * Confirmation dialog fragment shows a dialog to the user to confirm that they are disabling a
     * provider.
     */
    public static class ConfirmationDialogFragment extends CredentialManagerDialogFragment {

        ConfirmationDialogFragment(
                DialogHost dialogHost,
                @NonNull ServiceInfo serviceInfo,
                @NonNull CharSequence appName) {
            super(dialogHost);

            final Bundle argument = new Bundle();
            argument.putString(PACKAGE_NAME_KEY, serviceInfo.packageName);
            argument.putCharSequence(APP_NAME_KEY, appName);
            setArguments(argument);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Bundle bundle = getArguments();
            final String title =
                    getContext()
                            .getString(
                                    R.string.credman_confirmation_message_title,
                                    bundle.getCharSequence(
                                            CredentialManagerDialogFragment.APP_NAME_KEY));

            return new AlertDialog.Builder(getActivity())
                    .setTitle(title)
                    .setMessage(getContext().getString(R.string.credman_confirmation_message))
                    .setPositiveButton(R.string.credman_confirmation_message_positive_button, this)
                    .setNegativeButton(android.R.string.cancel, this)
                    .create();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            getDialogHost().onDialogClick(which);
        }
    }

    /**
     * Confirmation dialog fragment shows a dialog to the user to confirm that they would like to
     * enable the new provider.
     */
    public static class NewProviderConfirmationDialogFragment
            extends CredentialManagerDialogFragment {

        NewProviderConfirmationDialogFragment(
                DialogHost dialogHost,
                @NonNull ServiceInfo service,
                @NonNull CharSequence appName) {
            super(dialogHost);

            final Bundle argument = new Bundle();
            argument.putString(PACKAGE_NAME_KEY, service.packageName);
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

        public SettingContentObserver(Handler handler) {
            super(handler);
        }

        public void register(ContentResolver contentResolver) {
            contentResolver.registerContentObserver(mAutofillService, false, this, getUser());
            contentResolver.registerContentObserver(mCredentialService, false, this, getUser());
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            updateFromExternal();
        }
    }
}
