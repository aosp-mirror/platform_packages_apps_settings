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

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.view.View;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.SetupWizardUtils;
import com.android.settings.Utils;
import com.android.settings.biometrics.BiometricUtils;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.password.SetupChooseLockGeneric;
import com.android.settings.password.SetupSkipDialog;

public class SetupFingerprintEnrollIntroduction extends FingerprintEnrollIntroduction {
    /**
     * Returns the number of fingerprint enrolled.
     */
    private static final String EXTRA_FINGERPRINT_ENROLLED_COUNT = "fingerprint_enrolled_count";

    private static final String KEY_LOCK_SCREEN_PRESENT = "wasLockScreenPresent";
    private boolean mAlreadyHadLockScreenSetup = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            mAlreadyHadLockScreenSetup = isKeyguardSecure();
        } else {
            mAlreadyHadLockScreenSetup = savedInstanceState.getBoolean(
                    KEY_LOCK_SCREEN_PRESENT, false);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_LOCK_SCREEN_PRESENT, mAlreadyHadLockScreenSetup);
    }

    @Override
    protected Intent getEnrollingIntent() {
        final Intent intent = new Intent(this, SetupFingerprintEnrollFindSensor.class);
        if (BiometricUtils.containsGatekeeperPasswordHandle(getIntent())) {
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE,
                    BiometricUtils.getGatekeeperPasswordHandle(getIntent()));
        }
        SetupWizardUtils.copySetupExtras(getIntent(), intent);
        return intent;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BIOMETRIC_FIND_SENSOR_REQUEST && isKeyguardSecure()) {
            // if lock was already present, do not return intent data since it must have been
            // reported in previous attempts
            if (!mAlreadyHadLockScreenSetup) {
                data = getMetricIntent(data);
            }

            // Report fingerprint count if user adding a new fingerprint
            if (resultCode == RESULT_FINISHED) {
                data = setFingerprintCount(data);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private Intent getMetricIntent(Intent data) {
        if (data == null) {
            data = new Intent();
        }
        LockPatternUtils lockPatternUtils = new LockPatternUtils(this);
        data.putExtra(SetupChooseLockGeneric.
                SetupChooseLockGenericFragment.EXTRA_PASSWORD_QUALITY,
                lockPatternUtils.getKeyguardStoredPasswordQuality(UserHandle.myUserId()));

        return data;
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
            resultCode = RESULT_SKIP;
            data = mAlreadyHadLockScreenSetup ? null : getMetricIntent(null);
        } else {
            resultCode = SetupSkipDialog.RESULT_SKIP;
            data = null;
        }

        // User has explicitly canceled enroll. Don't restart it automatically.
        data = setSkipPendingEnroll(data);

        setResult(resultCode, data);
        finish();
    }

    /**
     * Propagate lock screen metrics if the user goes back from the fingerprint setup screen
     * after having added lock screen to his device.
     */
    @Override
    public void onBackPressed() {
        if (!mAlreadyHadLockScreenSetup && isKeyguardSecure()) {
            setResult(Activity.RESULT_CANCELED, getMetricIntent(null));
        }
        super.onBackPressed();
    }

    private boolean isKeyguardSecure() {
        return getSystemService(KeyguardManager.class).isKeyguardSecure();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.FINGERPRINT_ENROLL_INTRO_SETUP;
    }
}
