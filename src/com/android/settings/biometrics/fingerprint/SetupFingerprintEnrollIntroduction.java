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
import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.view.View;
import android.widget.TextView;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.SetupWizardUtils;
import com.android.settings.Utils;
import com.android.settings.password.ChooseLockGeneric.ChooseLockGenericFragment;
import com.android.settings.password.SetupChooseLockGeneric;
import com.android.settings.password.SetupSkipDialog;

import com.google.android.setupcompat.template.FooterButton;

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
    protected Intent getChooseLockIntent() {
        Intent intent = new Intent(this, SetupChooseLockGeneric.class);

        if (StorageManager.isFileEncryptedNativeOrEmulated()) {
            intent.putExtra(
                    LockPatternUtils.PASSWORD_TYPE_KEY,
                    DevicePolicyManager.PASSWORD_QUALITY_NUMERIC);
            intent.putExtra(ChooseLockGenericFragment.EXTRA_SHOW_OPTIONS_BUTTON, true);
        }
        SetupWizardUtils.copySetupExtras(getIntent(), intent);
        return intent;
    }

    @Override
    protected Intent getEnrollingIntent() {
        final Intent intent = new Intent(this, SetupFingerprintEnrollFindSensor.class);
        SetupWizardUtils.copySetupExtras(getIntent(), intent);
        return intent;
    }

    @Override
    protected void initViews() {
        super.initViews();

        TextView description = (TextView) findViewById(R.id.sud_layout_description);
        description.setText(
                R.string.security_settings_fingerprint_enroll_introduction_message_setup);

        FooterButton nextButton = getNextButton();
        nextButton.setText(
                this, R.string.security_settings_fingerprint_enroll_introduction_continue_setup);

        final FooterButton cancelButton = getCancelButton();
        cancelButton.setText(
                this, R.string.security_settings_fingerprint_enroll_introduction_cancel_setup);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // if lock was already present, do not return intent data since it must have been
        // reported in previous attempts
        if (requestCode == BIOMETRIC_FIND_SENSOR_REQUEST && isKeyguardSecure()) {
            if(!mAlreadyHadLockScreenSetup) {
                data = getMetricIntent(data);
            }

            // Report fingerprint count if user adding a new fingerprint
            if(resultCode == RESULT_FINISHED) {
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
        if (isKeyguardSecure()) {
            // If the keyguard is already set up securely (maybe the user added a backup screen
            // lock and skipped fingerprint), return RESULT_SKIP directly.
            setResult(RESULT_SKIP, mAlreadyHadLockScreenSetup ? null : getMetricIntent(null));
            finish();
        } else {
            setResult(SetupSkipDialog.RESULT_SKIP);
            finish();
        }
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
