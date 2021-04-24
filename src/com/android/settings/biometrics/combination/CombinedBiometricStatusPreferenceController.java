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

import android.content.Context;

import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.Utils;
import com.android.settings.biometrics.BiometricStatusPreferenceController;

/**
 * Preference controller for biometrics settings page controlling the ability to unlock the phone
 * with face and fingerprint.
 */
public class CombinedBiometricStatusPreferenceController extends
        BiometricStatusPreferenceController {
    private static final String KEY_BIOMETRIC_SETTINGS = "biometric_settings";


    public CombinedBiometricStatusPreferenceController(Context context) {
        this(context, KEY_BIOMETRIC_SETTINGS);
    }

    public CombinedBiometricStatusPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    protected boolean isDeviceSupported() {
        return Utils.hasFingerprintHardware(mContext) && Utils.hasFaceHardware(mContext);
    }

    @Override
    protected boolean hasEnrolledBiometrics() {
        return false;
    }

    @Override
    protected String getSummaryTextEnrolled() {
        return mContext.getString(R.string.security_settings_biometric_preference_summary);
    }

    @Override
    protected String getSummaryTextNoneEnrolled() {
        return mContext.getString(R.string.security_settings_biometric_preference_summary);
    }

    @Override
    protected String getSettingsClassName() {
        return Settings.CombinedBiometricSettingsActivity.class.getName();
    }

    @Override
    protected String getEnrollClassName() {
        return Settings.CombinedBiometricSettingsActivity.class.getName();
    }
}
