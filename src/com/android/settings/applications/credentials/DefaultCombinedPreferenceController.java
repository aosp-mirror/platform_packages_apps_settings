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
import android.provider.Settings;
import android.service.autofill.AutofillServiceInfo;
import android.text.TextUtils;
import android.util.Log;
import android.view.autofill.AutofillManager;

import com.android.settings.applications.defaultapps.DefaultAppPreferenceController;
import com.android.settingslib.applications.DefaultAppInfo;

import java.util.List;

public class DefaultCombinedPreferenceController extends DefaultAppPreferenceController {

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
                new AutofillSettingIntentProvider(mContext, mUserId, info.getKey());
        return intentProvider.getIntent();
    }

    @Override
    protected DefaultAppInfo getDefaultAppInfo() {
        final String flattenComponent =
                Settings.Secure.getString(
                        mContext.getContentResolver(), DefaultCombinedPicker.SETTING);
        if (!TextUtils.isEmpty(flattenComponent)) {
            DefaultAppInfo appInfo =
                    new DefaultAppInfo(
                            mContext,
                            mPackageManager,
                            mUserId,
                            ComponentName.unflattenFromString(flattenComponent));
            return appInfo;
        }
        return null;
    }

    @Override
    protected boolean showLabelAsTitle() {
        return true;
    }

    /** Provides Intent to setting activity for the specified autofill service. */
    static final class AutofillSettingIntentProvider {

        private final String mSelectedKey;
        private final Context mContext;
        private final int mUserId;

        public AutofillSettingIntentProvider(Context context, int userId, String key) {
            mSelectedKey = key;
            mContext = context;
            mUserId = userId;
        }

        public Intent getIntent() {
            final List<ResolveInfo> resolveInfos =
                    mContext.getPackageManager()
                            .queryIntentServicesAsUser(
                                    DefaultCombinedPicker.AUTOFILL_PROBE,
                                    PackageManager.GET_META_DATA,
                                    mUserId);

            for (ResolveInfo resolveInfo : resolveInfos) {
                final ServiceInfo serviceInfo = resolveInfo.serviceInfo;
                final String flattenKey =
                        new ComponentName(serviceInfo.packageName, serviceInfo.name)
                                .flattenToString();
                if (TextUtils.equals(mSelectedKey, flattenKey)) {
                    final String settingsActivity;
                    try {
                        settingsActivity =
                                new AutofillServiceInfo(mContext, serviceInfo)
                                        .getSettingsActivity();
                    } catch (SecurityException e) {
                        // Service does not declare the proper permission, ignore it.
                        Log.w(
                                "AutofillSettingIntentProvider",
                                "Error getting info for " + serviceInfo + ": " + e);
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
}
