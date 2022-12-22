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
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.credentials.CredentialManager;
import android.credentials.ListEnabledProvidersException;
import android.credentials.ListEnabledProvidersResponse;
import android.credentials.SetEnabledProvidersException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.OutcomeReceiver;
import android.os.UserHandle;
import android.service.credentials.CredentialProviderInfo;
import android.util.IconDrawableFactory;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.dashboard.DashboardFragment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

/** Queries available credential manager providers and adds preferences for them. */
public class CredentialManagerPreferenceController extends BasePreferenceController
        implements LifecycleObserver {
    private static final String TAG = "CredentialManagerPreferenceController";
    private static final int MAX_SELECTABLE_PROVIDERS = 5;

    private final PackageManager mPm;
    private final IconDrawableFactory mIconFactory;
    private final List<ServiceInfo> mServices;
    private final Set<String> mEnabledPackageNames;
    private final @Nullable CredentialManager mCredentialManager;
    private final CancellationSignal mCancellationSignal = new CancellationSignal();
    private final Executor mExecutor;

    private @Nullable DashboardFragment mParentFragment = null;

    public CredentialManagerPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mPm = context.getPackageManager();
        mIconFactory = IconDrawableFactory.newInstance(mContext);
        mServices = new ArrayList<>();
        mEnabledPackageNames = new HashSet<>();
        mExecutor = ContextCompat.getMainExecutor(mContext);
        mCredentialManager =
                getCredentialManager(context, preferenceKey.equals("credentials_test"));
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
     * Sets the parent fragment and attaches this controller to the settings lifecycle.
     *
     * @param fragment the fragment to use as the parent
     */
    public void setParentFragment(DashboardFragment fragment) {
        mParentFragment = fragment;
        fragment.getSettingsLifecycle().addObserver(this);
    }

    @OnLifecycleEvent(ON_CREATE)
    void onCreate(LifecycleOwner lifecycleOwner) {
        if (mCredentialManager == null) {
            return;
        }

        mCredentialManager.listEnabledProviders(
                mCancellationSignal,
                mExecutor,
                new OutcomeReceiver<ListEnabledProvidersResponse, ListEnabledProvidersException>() {
                    @Override
                    public void onResult(ListEnabledProvidersResponse result) {
                        Set<String> enabledPackages = new HashSet<>();
                        for (String flattenedComponentName : result.getProviderComponentNames()) {
                            ComponentName cn =
                                    ComponentName.unflattenFromString(flattenedComponentName);
                            if (cn != null) {
                                enabledPackages.add(cn.getPackageName());
                            }
                        }

                        List<ServiceInfo> services = new ArrayList<>();
                        for (CredentialProviderInfo cpi :
                                CredentialProviderInfo.getAvailableServices(mContext, getUser())) {
                            services.add(cpi.getServiceInfo());
                        }

                        init(lifecycleOwner, services, enabledPackages);
                    }

                    @Override
                    public void onError(ListEnabledProvidersException e) {
                        Log.e(TAG, "listEnabledProviders error: " + e.toString());
                    }
                });
    }

    @VisibleForTesting
    void init(
            LifecycleOwner lifecycleOwner,
            List<ServiceInfo> availableServices,
            Set<String> enabledPackages) {
        mServices.clear();
        mServices.addAll(availableServices);

        mEnabledPackageNames.clear();
        mEnabledPackageNames.addAll(enabledPackages);
    }

    @Override
    public int getAvailabilityStatus() {
        return mServices.isEmpty() ? CONDITIONALLY_UNAVAILABLE : AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        PreferenceGroup group = screen.findPreference(getPreferenceKey());
        Context context = screen.getContext();

        for (ServiceInfo serviceInfo : mServices) {
            CharSequence title = "";
            if (serviceInfo.nonLocalizedLabel != null) {
                title = serviceInfo.loadLabel(mPm);
            }

            group.addPreference(
                    addProviderPreference(
                            context,
                            title,
                            mIconFactory.getBadgedIcon(
                                    serviceInfo, serviceInfo.applicationInfo, getUser()),
                            serviceInfo.packageName));
        }
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
        for (ServiceInfo service : mServices) {
            if (mEnabledPackageNames.contains(service.packageName)) {
                enabledServices.add(service.getComponentName().flattenToString());
            }
        }

        return enabledServices;
    }

    private SwitchPreference addProviderPreference(
            @NonNull Context prefContext,
            @NonNull CharSequence title,
            @Nullable Drawable icon,
            @NonNull String packageName) {
        final SwitchPreference pref = new SwitchPreference(prefContext);
        pref.setTitle(title);
        pref.setChecked(mEnabledPackageNames.contains(packageName));

        if (icon != null) {
            pref.setIcon(Utils.getSafeIcon(icon));
        }

        pref.setOnPreferenceClickListener(
                p -> {
                    boolean isChecked = pref.isChecked();

                    if (isChecked) {
                        // Show the error if too many enabled.
                        if (!togglePackageNameEnabled(packageName)) {
                            final DialogFragment fragment = newErrorDialogFragment();

                            if (fragment == null || mParentFragment == null) {
                                return true;
                            }

                            fragment.show(
                                    mParentFragment.getActivity().getSupportFragmentManager(),
                                    ErrorDialogFragment.TAG);

                            // The user set the check to true so we need to set it back.
                            pref.setChecked(false);
                        }

                        return true;
                    } else {
                        // Show the confirm disable dialog.
                        final DialogFragment fragment =
                                newConfirmationDialogFragment(packageName, title, pref);

                        if (fragment == null || mParentFragment == null) {
                            return true;
                        }

                        fragment.show(
                                mParentFragment.getActivity().getSupportFragmentManager(),
                                ConfirmationDialogFragment.TAG);
                    }

                    return true;
                });

        return pref;
    }

    private void commitEnabledPackages() {
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

    private @Nullable ConfirmationDialogFragment newConfirmationDialogFragment(
            @NonNull String packageName,
            @NonNull CharSequence appName,
            @NonNull SwitchPreference pref) {
        DialogHost host =
                new DialogHost() {
                    @Override
                    public DashboardFragment getParentFragment() {
                        return mParentFragment;
                    }

                    @Override
                    public void onDialogClick(int whichButton) {
                        if (whichButton == DialogInterface.BUTTON_POSITIVE) {
                            // Since the package is now enabled then we
                            // should remove it from the enabled list.
                            togglePackageNameDisabled(packageName);
                        } else if (whichButton == DialogInterface.BUTTON_NEGATIVE) {
                            // Set the checked back to true because we
                            // backed out of turning this off.
                            pref.setChecked(true);
                        }
                    }
                };

        if (host.getParentFragment() == null) {
            return null;
        }

        return new ConfirmationDialogFragment(host, packageName, appName);
    }

    private @Nullable ErrorDialogFragment newErrorDialogFragment() {
        DialogHost host =
                new DialogHost() {
                    @Override
                    public DashboardFragment getParentFragment() {
                        return mParentFragment;
                    }

                    @Override
                    public void onDialogClick(int whichButton) {}
                };

        if (host.getParentFragment() == null) {
            return null;
        }

        return new ErrorDialogFragment(host);
    }

    private int getUser() {
        UserHandle workUser = getWorkProfileUser();
        return workUser != null ? workUser.getIdentifier() : UserHandle.myUserId();
    }

    /** Called when the dialog button is clicked. */
    private interface DialogHost {
        void onDialogClick(int whichButton);

        DashboardFragment getParentFragment();
    }

    /** Dialog fragment parent class. */
    private abstract static class CredentialManagerDialogFragment extends InstrumentedDialogFragment
            implements DialogInterface.OnClickListener {

        public static final String TAG = "CredentialManagerDialogFragment";
        public static final String PACKAGE_NAME_KEY = "package_name";
        public static final String APP_NAME_KEY = "app_name";

        private DialogHost mDialogHost;

        CredentialManagerDialogFragment(DialogHost dialogHost) {
            super();
            setTargetFragment(dialogHost.getParentFragment(), 0);
            mDialogHost = dialogHost;
        }

        public DialogHost getDialogHost() {
            return mDialogHost;
        }

        @Override
        public int getMetricsCategory() {
            return SettingsEnums.ACCOUNT;
        }
    }

    /** Dialog showing error when too many providers are selected. */
    private static class ErrorDialogFragment extends CredentialManagerDialogFragment {

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
    private static class ConfirmationDialogFragment extends CredentialManagerDialogFragment {

        ConfirmationDialogFragment(
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
}
