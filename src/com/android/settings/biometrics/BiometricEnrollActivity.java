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

import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.android.settings.biometrics.face.FaceEnrollIntroduction;
import com.android.settings.biometrics.fingerprint.FingerprintEnrollIntroduction;
import com.android.settings.biometrics.fingerprint.SetupFingerprintEnrollIntroduction;
import com.android.settings.core.InstrumentedActivity;

import com.google.android.setupcompat.util.WizardManagerHelper;

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
        Intent intent = null;

        // This logic may have to be modified on devices with multiple biometrics.
        if (pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
            intent = getFingerprintEnrollIntent();
        } else if (pm.hasSystemFeature(PackageManager.FEATURE_FACE)) {
            intent = getFaceEnrollIntent();
        }

        if (intent != null) {
            intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
            startActivity(intent);
        }
        finish();
    }

    private Intent getFingerprintEnrollIntent() {
        if (WizardManagerHelper.isAnySetupWizard(getIntent())) {
            Intent intent = new Intent(this, SetupFingerprintEnrollIntroduction.class);
            WizardManagerHelper.copyWizardManagerExtras(getIntent(), intent);
            return intent;
        } else {
            return new Intent(this, FingerprintEnrollIntroduction.class);
        }
    }

    private Intent getFaceEnrollIntent() {
        Intent intent = new Intent(this, FaceEnrollIntroduction.class);
        WizardManagerHelper.copyWizardManagerExtras(getIntent(), intent);
        return intent;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.BIOMETRIC_ENROLL_ACTIVITY;
    }
}
