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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.credentials.CredentialManager;
import android.credentials.CredentialProviderInfo;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.autofill.AutofillService;
import android.service.autofill.AutofillServiceInfo;
import android.text.TextUtils;
import android.util.Log;
import android.view.autofill.AutofillManager;

import com.android.settings.applications.defaultapps.DefaultAppPreferenceController;
import com.android.settingslib.applications.DefaultAppInfo;

import java.util.ArrayList;
import java.util.List;

public class DefaultCombinedPreferenceController extends DefaultAppPreferenceController {

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
        if (info == null) {
            return null;
        }
        final AutofillSettingIntentProvider intentProvider =
                new AutofillSettingIntentProvider(mContext, getUser(), info.getKey());
        return intentProvider.getIntent();
    }

    @Override
    protected DefaultAppInfo getDefaultAppInfo() {
        List<CombinedProviderInfo> providers = getAllProviders(getUser());
        CombinedProviderInfo topProvider = CombinedProviderInfo.getTopProvider(providers);
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

    /** Provides Intent to setting activity for the specified autofill service. */
    static final class AutofillSettingIntentProvider {

        private final String mKey;
        private final Context mContext;
        private final int mUserId;

        public AutofillSettingIntentProvider(Context context, int userId, String key) {
            mKey = key;
            mContext = context;
            mUserId = userId;
        }

        public Intent getIntent() {
            final List<ResolveInfo> resolveInfos =
                    mContext.getPackageManager()
                            .queryIntentServicesAsUser(
                                    AUTOFILL_PROBE, PackageManager.GET_META_DATA, mUserId);

            for (ResolveInfo resolveInfo : resolveInfos) {
                final ServiceInfo serviceInfo = resolveInfo.serviceInfo;

                // If there are multiple autofill services then pick the first one.
                if (mKey != null && mKey.startsWith(serviceInfo.packageName)) {
                    final String settingsActivity;
                    try {
                        settingsActivity =
                                new AutofillServiceInfo(mContext, serviceInfo)
                                        .getSettingsActivity();
                    } catch (SecurityException e) {
                        // Service does not declare the proper permission, ignore it.
                        Log.e(TAG, "Error getting info for " + serviceInfo + ": " + e);
                        return null;
                    }
                    if (TextUtils.isEmpty(settingsActivity)) {
                        return null;
                    }
                    return new Intent(Intent.ACTION_MAIN)
                            .setComponent(
                                    new ComponentName(serviceInfo.packageName, settingsActivity));
                }
            }

            return null;
        }
    }

    protected int getUser() {
        return UserHandle.myUserId();
    }
}
