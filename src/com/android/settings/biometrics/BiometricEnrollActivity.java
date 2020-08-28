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

import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.BiometricManager.Authenticators;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.SetupWizardUtils;
import com.android.settings.biometrics.face.FaceEnrollIntroduction;
import com.android.settings.biometrics.fingerprint.FingerprintEnrollFindSensor;
import com.android.settings.biometrics.fingerprint.FingerprintEnrollIntroduction;
import com.android.settings.biometrics.fingerprint.SetupFingerprintEnrollIntroduction;
import com.android.settings.core.InstrumentedActivity;
import com.android.settings.password.ChooseLockGeneric;
import com.android.settings.password.ChooseLockSettingsHelper;

import com.google.android.setupcompat.util.WizardManagerHelper;

/**
 * Trampoline activity launched by the {@code android.settings.BIOMETRIC_ENROLL} action which
 * shows the user an appropriate enrollment flow depending on the device's biometric hardware.
 * This activity must only allow enrollment of biometrics that can be used by
 * {@link android.hardware.biometrics.BiometricPrompt}.
 */
public class BiometricEnrollActivity extends InstrumentedActivity {

    private static final String TAG = "BiometricEnrollActivity";

    public static final String EXTRA_SKIP_INTRO = "skip_intro";

    public static final class InternalActivity extends BiometricEnrollActivity {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Default behavior is to enroll BIOMETRIC_WEAK or above. See ACTION_BIOMETRIC_ENROLL.
        final int authenticators = getIntent().getIntExtra(
                Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, Authenticators.BIOMETRIC_WEAK);

        Log.d(TAG, "Authenticators: " + authenticators);

        final BiometricManager bm = getSystemService(BiometricManager.class);
        final PackageManager pm = getApplicationContext().getPackageManager();
        Intent intent = null;

        final int result = bm.canAuthenticate(authenticators);

        if (!WizardManagerHelper.isAnySetupWizard(getIntent())) {
            if (result == BiometricManager.BIOMETRIC_SUCCESS
                    || result == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE) {
                Log.e(TAG, "Unexpected result: " + result);
                finish();
                return;
            }
        }

        if (authenticators == BiometricManager.Authenticators.DEVICE_CREDENTIAL) {
            // If only device credential was specified, ask the user to only set that up.
            intent = new Intent(this, ChooseLockGeneric.class);
            intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.MINIMUM_QUALITY_KEY,
                    DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);
        } else if (pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
            // This logic may have to be modified on devices with multiple biometrics.
            // ChooseLockGeneric can request to start fingerprint enroll bypassing the intro screen.
            if (getIntent().getBooleanExtra(EXTRA_SKIP_INTRO, false)
                    && this instanceof InternalActivity) {
                intent = getFingerprintFindSensorIntent();
            } else {
                intent = getFingerprintIntroIntent();
            }
        } else if (pm.hasSystemFeature(PackageManager.FEATURE_FACE)) {
            intent = getFaceIntroIntent();
        }

        if (intent != null) {
            intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);

            if (this instanceof InternalActivity) {
                // Propagate challenge and user Id from ChooseLockGeneric.
                final byte[] token = getIntent()
                        .getByteArrayExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
                final int userId = getIntent()
                        .getIntExtra(Intent.EXTRA_USER_ID, UserHandle.USER_NULL);

                intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, token);
                intent.putExtra(Intent.EXTRA_USER_ID, userId);
            }

            startActivity(intent);
            finish();
        } else {
            Log.e(TAG, "Intent was null, finishing");
            finish();
        }
    }

    private Intent getFingerprintFindSensorIntent() {
        Intent intent = new Intent(this, FingerprintEnrollFindSensor.class);
        SetupWizardUtils.copySetupExtras(getIntent(), intent);
        return intent;
    }

    private Intent getFingerprintIntroIntent() {
        if (WizardManagerHelper.isAnySetupWizard(getIntent())) {
            Intent intent = new Intent(this, SetupFingerprintEnrollIntroduction.class);
            WizardManagerHelper.copyWizardManagerExtras(getIntent(), intent);
            return intent;
        } else {
            return new Intent(this, FingerprintEnrollIntroduction.class);
        }
    }

    private Intent getFaceIntroIntent() {
        Intent intent = new Intent(this, FaceEnrollIntroduction.class);
        WizardManagerHelper.copyWizardManagerExtras(getIntent(), intent);
        return intent;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.BIOMETRIC_ENROLL_ACTIVITY;
    }
}
