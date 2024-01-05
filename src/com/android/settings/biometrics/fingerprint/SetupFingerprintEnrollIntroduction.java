/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.biometrics.fingerprint;

import android.app.KeyguardManager;
import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.view.View;

import com.android.settings.SetupWizardUtils;
import com.android.settings.Utils;
import com.android.settings.biometrics.BiometricUtils;
import com.android.settings.flags.Flags;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.password.SetupSkipDialog;

public class SetupFingerprintEnrollIntroduction extends FingerprintEnrollIntroduction {
    /**
     * Returns the number of fingerprint enrolled.
     */
    public static final String EXTRA_FINGERPRINT_ENROLLED_COUNT = "fingerprint_enrolled_count";

    private static final String KEY_LOCK_SCREEN_PRESENT = "wasLockScreenPresent";

    @Override
    protected Intent getEnrollingIntent() {
        final Intent intent = new Intent(this, SetupFingerprintEnrollFindSensor.class);
        BiometricUtils.copyMultiBiometricExtras(getIntent(), intent);
        if (BiometricUtils.containsGatekeeperPasswordHandle(getIntent())) {
            intent.putExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE,
                    BiometricUtils.getGatekeeperPasswordHandle(getIntent()));
        }
        SetupWizardUtils.copySetupExtras(getIntent(), intent);
        if (Flags.udfpsEnrollCalibration()) {
            if (mCalibrator != null) {
                intent.putExtras(mCalibrator.getExtrasForNextIntent(false));
            }
        }
        return intent;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        boolean hasEnrolledFace = false;
        boolean hasEnrolledFingerprint = false;
        if (data != null) {
            hasEnrolledFace = data.getBooleanExtra(EXTRA_FINISHED_ENROLL_FACE, false);
            hasEnrolledFingerprint = data.getBooleanExtra(EXTRA_FINISHED_ENROLL_FINGERPRINT, false);
            // If we've enrolled a face, we can remove the pending intent to launch FaceEnrollIntro.
            if (hasEnrolledFace) {
                removeEnrollNextBiometric();
            }
        }
        if (requestCode == BIOMETRIC_FIND_SENSOR_REQUEST && isKeyguardSecure()) {
            // Report fingerprint count if user adding a new fingerprint
            if (resultCode == RESULT_FINISHED) {
                data = setFingerprintCount(data);
            }

            if (resultCode == RESULT_CANCELED && hasEnrolledFingerprint) {
                // If we are coming from a back press from an already enrolled fingerprint,
                // we can finish this activity.
                setResult(resultCode, data);
                finish();
                return;
            }
        } else if (requestCode == ENROLL_NEXT_BIOMETRIC_REQUEST) {
            // See if we can still enroll a fingerprint
            boolean canEnrollFinger = checkMaxEnrolled() == 0;
            // If we came from the next biometric flow and a user has either
            // finished or skipped, we will also finish.
            if (resultCode == RESULT_SKIP || resultCode == RESULT_FINISHED) {
                // If user skips the enroll next biometric, we will also finish
                setResult(RESULT_FINISHED, data);
                finish();
            } else if (resultCode == RESULT_CANCELED) {
                // Note that result_canceled comes from onBackPressed.
                // If we can enroll a finger, Stay on this page, else we cannot,
                // and finish entirely.
                if (!canEnrollFinger) {
                    finish();
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private Intent setFingerprintCount(Intent data) {
        if (data == null) {
            data = new Intent();
        }
        final FingerprintManager fpm = Utils.getFingerprintManagerOrNull(this);
        if (fpm != null) {
            int enrolled = fpm.getEnrolledFingerprints(mUserId).size();
            data.putExtra(EXTRA_FINGERPRINT_ENROLLED_COUNT, enrolled);
        }
        return data;
    }

    @Override
    protected void onCancelButtonClick(View view) {
        final int resultCode;
        Intent data;
        if (isKeyguardSecure()) {
            // If the keyguard is already set up securely (maybe the user added a backup screen
            // lock and skipped fingerprint), return RESULT_SKIP directly.
            if (!BiometricUtils.tryStartingNextBiometricEnroll(
                    this, ENROLL_NEXT_BIOMETRIC_REQUEST, "cancel")) {
                resultCode = RESULT_SKIP;
                setResult(resultCode);
                finish();
                return;
            }
        } else {
            resultCode = SetupSkipDialog.RESULT_SKIP;
            data = setSkipPendingEnroll(null);
            setResult(resultCode, data);
            finish();
        }

        // User has explicitly canceled enroll. Don't restart it automatically.
    }

    private boolean isKeyguardSecure() {
        return getSystemService(KeyguardManager.class).isKeyguardSecure();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.FINGERPRINT_ENROLL_INTRO_SETUP;
    }
}
