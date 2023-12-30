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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.credentials.CredentialManager;
import android.credentials.CredentialProviderInfo;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.autofill.AutofillService;
import android.service.autofill.AutofillServiceInfo;
import android.text.TextUtils;
import android.view.autofill.AutofillManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.applications.defaultapps.DefaultAppPreferenceController;
import com.android.settingslib.applications.DefaultAppInfo;

import java.util.ArrayList;
import java.util.List;

public class DefaultCombinedPreferenceController extends DefaultAppPreferenceController
        implements Preference.OnPreferenceClickListener {

    private static final Intent AUTOFILL_PROBE = new Intent(AutofillService.SERVICE_INTERFACE);
    private static final String TAG = "DefaultCombinedPreferenceController";

    private final AutofillManager mAutofillManager;
    private final CredentialManager mCredentialManager;

    public DefaultCombinedPreferenceController(Context context) {
        super(context);

        mAutofillManager = mContext.getSystemService(AutofillManager.class);

        if (CredentialManager.isServiceEnabled(context)) {
            mCredentialManager = mContext.getSystemService(CredentialManager.class);
        } else {
            mCredentialManager = null;
        }
    }

    @Override
    public boolean isAvailable() {
        return mAutofillManager != null
                && mCredentialManager != null
                && mAutofillManager.hasAutofillFeature()
                && mAutofillManager.isAutofillSupported();
    }

    @Override
    public String getPreferenceKey() {
        return "default_credman_autofill_main";
    }

    @Override
    protected Intent getSettingIntent(DefaultAppInfo info) {
        // Despite this method being called getSettingIntent this intent actually
        // opens the primary picker. This is so that we can swap the cog and the left
        // hand side presses to align the UX.
        return new Intent(mContext, CredentialsPickerActivity.class);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        final String prefKey = getPreferenceKey();
        final Preference preference = screen.findPreference(prefKey);
        if (preference != null) {
            preference.setOnPreferenceClickListener((Preference.OnPreferenceClickListener) this);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        // Get the selected provider.
        final CombinedProviderInfo topProvider = getTopProvider();
        if (topProvider == null) {
            return false;
        }

        // If the top provider has a defined Credential Manager settings
        // provider then we should open that up.
        final String settingsActivity = topProvider.getSettingsActivity();
        if (!TextUtils.isEmpty(settingsActivity)) {
            final Intent intent =
                    new Intent(Intent.ACTION_MAIN)
                            .setComponent(
                                    new ComponentName(
                                            topProvider.getPackageName(), settingsActivity));
            startActivity(intent);
            return true;
        }

        return false;
    }

    private @Nullable CombinedProviderInfo getTopProvider() {
        List<CombinedProviderInfo> providers = getAllProviders(getUser());
        return CombinedProviderInfo.getTopProvider(providers);
    }

    @Override
    protected DefaultAppInfo getDefaultAppInfo() {
        CombinedProviderInfo topProvider = getTopProvider();
        if (topProvider != null) {
            ServiceInfo brandingService = topProvider.getBrandingService();
            if (brandingService == null) {
                return new DefaultAppInfo(
                        mContext,
                        mPackageManager,
                        getUser(),
                        topProvider.getApplicationInfo(),
                        topProvider.getSettingsSubtitle(),
                        true);
            } else {
                return new DefaultAppInfo(
                        mContext,
                        mPackageManager,
                        getUser(),
                        brandingService,
                        topProvider.getSettingsSubtitle(),
                        true);
            }
        }
        return null;
    }

    private List<CombinedProviderInfo> getAllProviders(int userId) {
        final List<AutofillServiceInfo> autofillProviders =
                AutofillServiceInfo.getAvailableServices(mContext, userId);
        final String selectedAutofillProvider =
                Settings.Secure.getStringForUser(
                        mContext.getContentResolver(),
                        DefaultCombinedPicker.AUTOFILL_SETTING,
                        userId);

        final List<CredentialProviderInfo> credManProviders = new ArrayList<>();
        if (mCredentialManager != null) {
            credManProviders.addAll(
                    mCredentialManager.getCredentialProviderServices(
                            userId, CredentialManager.PROVIDER_FILTER_USER_PROVIDERS_ONLY));
        }

        return CombinedProviderInfo.buildMergedList(
                autofillProviders, credManProviders, selectedAutofillProvider);
    }

    @Override
    protected boolean showLabelAsTitle() {
        return true;
    }

    @Override
    protected boolean showAppSummary() {
        return true;
    }

    protected int getUser() {
        return UserHandle.myUserId();
    }
}
