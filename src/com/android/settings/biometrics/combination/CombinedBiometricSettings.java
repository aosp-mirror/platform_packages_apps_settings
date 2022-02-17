/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.settings.biometrics.combination;

import android.app.settings.SettingsEnums;
import android.content.Context;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

/**
 * Settings screen for multiple biometrics.
 */
@SearchIndexable
public class CombinedBiometricSettings extends BiometricsSettingsBase {
    private static final String TAG = "BiometricSettings";
    private static final String KEY_FACE_SETTINGS = "biometric_face_settings";
    private static final String KEY_FINGERPRINT_SETTINGS = "biometric_fingerprint_settings";
    private static final String KEY_UNLOCK_PHONE = "biometric_settings_biometric_keyguard";
    private static final String KEY_USE_IN_APPS = "biometric_settings_biometric_app";

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        use(BiometricSettingsKeyguardPreferenceController.class).setUserId(mUserId);
        use(BiometricSettingsAppPreferenceController.class).setUserId(mUserId);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.security_settings_combined_biometric;
    }

    @Override
    public String getFacePreferenceKey() {
        return KEY_FACE_SETTINGS;
    }

    @Override
    public String getFingerprintPreferenceKey() {
        return KEY_FINGERPRINT_SETTINGS;
    }

    @Override
    public String getUnlockPhonePreferenceKey() {
        return KEY_UNLOCK_PHONE;
    }

    @Override
    public String getUseInAppsPreferenceKey() {
        return KEY_USE_IN_APPS;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.COMBINED_BIOMETRIC;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new CombinedBiometricSearchIndexProvider(R.xml.security_settings_combined_biometric);
}
