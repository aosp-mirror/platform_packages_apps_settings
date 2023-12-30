/*
 * Copyright (C) 2023 The Android Open Source Project
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

import android.annotation.Nullable;
import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.credentials.CredentialManager;
import android.credentials.CredentialProviderInfo;
import android.credentials.SetEnabledProvidersException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.OutcomeReceiver;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.autofill.AutofillServiceInfo;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.preference.Preference;

import com.android.internal.content.PackageMonitor;
import com.android.settings.R;
import com.android.settings.applications.defaultapps.DefaultAppPickerFragment;
import com.android.settingslib.applications.DefaultAppInfo;
import com.android.settingslib.widget.CandidateInfo;

import java.util.ArrayList;
import java.util.List;

public class DefaultCombinedPicker extends DefaultAppPickerFragment {

    private static final String TAG = "DefaultCombinedPicker";

    public static final String AUTOFILL_SETTING = Settings.Secure.AUTOFILL_SERVICE;
    public static final String CREDENTIAL_SETTING = Settings.Secure.CREDENTIAL_SERVICE;

    /** Extra set when the fragment is implementing ACTION_REQUEST_SET_AUTOFILL_SERVICE. */
    public static final String EXTRA_PACKAGE_NAME = "package_name";

    /** Set when the fragment is implementing ACTION_REQUEST_SET_AUTOFILL_SERVICE. */
    private DialogInterface.OnClickListener mCancelListener;

    private CredentialManager mCredentialManager;
    private int mIntentSenderUserId = -1;

    private static final Handler sMainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Activity activity = getActivity();
        if (activity != null && activity.getIntent().getStringExtra(EXTRA_PACKAGE_NAME) != null) {
            mCancelListener =
                    (d, w) -> {
                        activity.setResult(Activity.RESULT_CANCELED);
                        activity.finish();
                    };
            // If mCancelListener is not null, fragment is started from
            // ACTION_REQUEST_SET_AUTOFILL_SERVICE and we should always use the calling uid.
            mIntentSenderUserId = UserHandle.myUserId();
        }

        getUser();

        mSettingsPackageMonitor.register(activity, activity.getMainLooper(), false);
        update();
    }

    @Override
    protected DefaultAppPickerFragment.ConfirmationDialogFragment newConfirmationDialogFragment(
            String selectedKey, CharSequence confirmationMessage) {
        final AutofillPickerConfirmationDialogFragment fragment =
                new AutofillPickerConfirmationDialogFragment();
        fragment.init(this, selectedKey, confirmationMessage);
        return fragment;
    }

    /**
     * Custom dialog fragment that has a cancel listener used to propagate the result back to caller
     * (for the cases where the picker is launched by {@code
     * android.settings.REQUEST_SET_AUTOFILL_SERVICE}.
     */
    public static class AutofillPickerConfirmationDialogFragment
            extends DefaultAppPickerFragment.ConfirmationDialogFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            final DefaultCombinedPicker target = (DefaultCombinedPicker) getTargetFragment();
            setCancelListener(target.mCancelListener);
            super.onCreate(savedInstanceState);
        }
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.default_credman_picker;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DEFAULT_AUTOFILL_PICKER;
    }

    @Override
    protected boolean shouldShowItemNone() {
        return true;
    }

    /** Monitor coming and going auto fill services and calls {@link #update()} when necessary */
    private final PackageMonitor mSettingsPackageMonitor =
            new PackageMonitor() {
                @Override
                public void onPackageAdded(String packageName, int uid) {
                    sMainHandler.post(
                            () -> {
                                // See b/296164461 for context
                                if (getContext() == null) {
                                    Log.w(TAG, "context is null");
                                    return;
                                }

                                update();
                            });
                }

                @Override
                public void onPackageModified(String packageName) {
                    sMainHandler.post(
                            () -> {
                                // See b/296164461 for context
                                if (getContext() == null) {
                                    Log.w(TAG, "context is null");
                                    return;
                                }

                                update();
                            });
                }

                @Override
                public void onPackageRemoved(String packageName, int uid) {
                    sMainHandler.post(
                            () -> {
                                // See b/296164461 for context
                                if (getContext() == null) {
                                    Log.w(TAG, "context is null");
                                    return;
                                }

                                update();
                            });
                }
            };

    /** Update the data in this UI. */
    private void update() {
        updateCandidates();
        addAddServicePreference();
    }

    @Override
    public void onDestroy() {
        mSettingsPackageMonitor.unregister();
        super.onDestroy();
    }

    /**
     * Gets the preference that allows to add a new autofill service.
     *
     * @return The preference or {@code null} if no service can be added
     */
    private Preference newAddServicePreferenceOrNull() {
        final String searchUri =
                Settings.Secure.getStringForUser(
                        getActivity().getContentResolver(),
                        Settings.Secure.AUTOFILL_SERVICE_SEARCH_URI,
                        getUser());
        if (TextUtils.isEmpty(searchUri)) {
            return null;
        }

        final Intent addNewServiceIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(searchUri));
        final Context context = getPrefContext();
        final Preference preference = new Preference(context);
        preference.setOnPreferenceClickListener(
                p -> {
                    context.startActivityAsUser(addNewServiceIntent, UserHandle.of(getUser()));
                    return true;
                });
        preference.setTitle(R.string.print_menu_item_add_service);
        preference.setIcon(R.drawable.ic_add_24dp);
        preference.setOrder(Integer.MAX_VALUE - 1);
        preference.setPersistent(false);
        return preference;
    }

    /**
     * Add a preference that allows the user to add a service if the market link for that is
     * configured.
     */
    private void addAddServicePreference() {
        final Preference addNewServicePreference = newAddServicePreferenceOrNull();
        if (addNewServicePreference != null) {
            getPreferenceScreen().addPreference(addNewServicePreference);
        }
    }

    /**
     * Get the Credential Manager service if we haven't already got it. We need to get the service
     * later because if we do it in onCreate it will fail.
     */
    private @Nullable CredentialManager getCredentialProviderService() {
        if (mCredentialManager == null) {
            mCredentialManager = getContext().getSystemService(CredentialManager.class);
        }
        return mCredentialManager;
    }

    private List<CombinedProviderInfo> getAllProviders() {
        final Context context = getContext();
        final List<AutofillServiceInfo> autofillProviders =
                AutofillServiceInfo.getAvailableServices(context, getUser());

        final CredentialManager service = getCredentialProviderService();
        final List<CredentialProviderInfo> credManProviders = new ArrayList<>();
        if (service != null) {
            credManProviders.addAll(
                    service.getCredentialProviderServices(
                            getUser(), CredentialManager.PROVIDER_FILTER_USER_PROVIDERS_ONLY));
        }

        final String selectedAutofillProvider = getSelectedAutofillProvider(context, getUser());
        return CombinedProviderInfo.buildMergedList(
                autofillProviders, credManProviders, selectedAutofillProvider);
    }

    public static String getSelectedAutofillProvider(Context context, int userId) {
        return Settings.Secure.getStringForUser(
                context.getContentResolver(), AUTOFILL_SETTING, userId);
    }

    protected List<DefaultAppInfo> getCandidates() {
        final Context context = getContext();
        final List<CombinedProviderInfo> allProviders = getAllProviders();
        final List<DefaultAppInfo> candidates = new ArrayList<>();

        for (CombinedProviderInfo cpi : allProviders) {
            ServiceInfo brandingService = cpi.getBrandingService();
            if (brandingService == null) {
                candidates.add(
                        new DefaultAppInfo(
                                context,
                                mPm,
                                getUser(),
                                cpi.getApplicationInfo(),
                                cpi.getSettingsSubtitle(),
                                true));
            } else {
                candidates.add(
                        new DefaultAppInfo(
                                context,
                                mPm,
                                getUser(),
                                brandingService,
                                cpi.getSettingsSubtitle(),
                                true));
            }
        }

        return candidates;
    }

    @Override
    protected String getDefaultKey() {
        final CombinedProviderInfo topProvider =
                CombinedProviderInfo.getTopProvider(getAllProviders());
        return topProvider == null ? "" : topProvider.getApplicationInfo().packageName;
    }

    @Override
    protected CharSequence getConfirmationMessage(CandidateInfo appInfo) {
        // If we are selecting none then show a warning label.
        if (appInfo == null) {
            final String message = getContext().getString(R.string.credman_confirmation_message);
            return Html.fromHtml(message);
        }
        final CharSequence appName = appInfo.loadLabel();
        final String message =
                getContext()
                        .getString(
                                R.string.credman_autofill_confirmation_message,
                                Html.escapeHtml(appName));
        return Html.fromHtml(message);
    }

    @Override
    protected boolean setDefaultKey(String key) {
        // Get the list of providers and see if any match the key (package name).
        final List<CombinedProviderInfo> allProviders = getAllProviders();
        CombinedProviderInfo matchedProvider = null;
        for (CombinedProviderInfo cpi : allProviders) {
            if (cpi.getApplicationInfo().packageName.equals(key)) {
                matchedProvider = cpi;
                break;
            }
        }

        // If there were none then clear the stored providers.
        if (matchedProvider == null) {
            setProviders(null, new ArrayList<>());
            return true;
        }

        // Get the component names and save them.
        final List<String> credManComponents = new ArrayList<>();
        for (CredentialProviderInfo pi : matchedProvider.getCredentialProviderInfos()) {
            credManComponents.add(pi.getServiceInfo().getComponentName().flattenToString());
        }

        String autofillValue = null;
        if (matchedProvider.getAutofillServiceInfo() != null) {
            autofillValue =
                    matchedProvider
                            .getAutofillServiceInfo()
                            .getServiceInfo()
                            .getComponentName()
                            .flattenToString();
        }

        setProviders(autofillValue, credManComponents);

        // Check if activity was launched from Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE
        // intent, and set proper result if so...
        final Activity activity = getActivity();
        if (activity != null) {
            final String packageName = activity.getIntent().getStringExtra(EXTRA_PACKAGE_NAME);
            if (packageName != null) {
                final int result =
                        key != null && key.startsWith(packageName)
                                ? Activity.RESULT_OK
                                : Activity.RESULT_CANCELED;
                activity.setResult(result);
                activity.finish();
            }
        }

        // TODO: Notify the rest

        return true;
    }

    private void setProviders(String autofillProvider, List<String> primaryCredManProviders) {
        if (TextUtils.isEmpty(autofillProvider)) {
            if (primaryCredManProviders.size() > 0) {
                autofillProvider =
                        CredentialManagerPreferenceController
                                .AUTOFILL_CREDMAN_ONLY_PROVIDER_PLACEHOLDER;
            }
        }

        Settings.Secure.putStringForUser(
                getContext().getContentResolver(), AUTOFILL_SETTING, autofillProvider, getUser());

        final CredentialManager service = getCredentialProviderService();
        if (service == null) {
            return;
        }

        // Get the existing secondary providers since we don't touch them in
        // this part of the UI we should just copy them over.
        final List<String> credManProviders = new ArrayList<>();
        for (CredentialProviderInfo cpi :
                service.getCredentialProviderServices(
                        getUser(), CredentialManager.PROVIDER_FILTER_USER_PROVIDERS_ONLY)) {

            if (cpi.isEnabled() && !cpi.isPrimary()) {
                credManProviders.add(cpi.getServiceInfo().getComponentName().flattenToString());
            }
        }

        credManProviders.addAll(primaryCredManProviders);

        // If there is no provider then clear all the providers.
        if (TextUtils.isEmpty(autofillProvider) && primaryCredManProviders.isEmpty()) {
            credManProviders.clear();
        }

        service.setEnabledProviders(
                primaryCredManProviders,
                credManProviders,
                getUser(),
                ContextCompat.getMainExecutor(getContext()),
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

    protected int getUser() {
        if (mIntentSenderUserId >= 0) {
            return mIntentSenderUserId;
        }
        return UserHandle.myUserId();
    }
}
