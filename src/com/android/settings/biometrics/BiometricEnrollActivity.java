/*
 * Copyright (C) 2018 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.biometrics;

import static com.android.settings.Utils.SETTINGS_PACKAGE_NAME;

import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.android.settings.biometrics.face.FaceEnrollIntroduction;
import com.android.settings.biometrics.fingerprint.FingerprintEnrollIntroduction;
import com.android.settings.core.InstrumentedActivity;

/**
 * Trampoline activity launched by the {@code android.settings.BIOMETRIC_ENROLL} action which
 * shows the user an appropriate enrollment flow depending on the device's biometric hardware.
 * This activity must only allow enrollment of biometrics that can be used by
 * {@link android.hardware.biometrics.BiometricPrompt}.
 */
public class BiometricEnrollActivity extends InstrumentedActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final PackageManager pm = getApplicationContext().getPackageManager();
        final Intent intent = new Intent();

        // This logic may have to be modified on devices with multiple biometrics.
        if (pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
            intent.setClassName(SETTINGS_PACKAGE_NAME,
                    FingerprintEnrollIntroduction.class.getName());
        } else if (pm.hasSystemFeature(PackageManager.FEATURE_FACE)) {
            intent.setClassName(SETTINGS_PACKAGE_NAME, FaceEnrollIntroduction.class.getName());
        }

        startActivity(intent);
        finish();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.BIOMETRIC_ENROLL_ACTIVITY;
    }
}
