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

package com.android.settings.fingerprint;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.widget.Button;
import android.widget.TextView;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.SetupWizardUtils;
import com.android.settings.password.ChooseLockGeneric.ChooseLockGenericFragment;
import com.android.settings.password.SetupChooseLockGeneric;
import com.android.settings.password.SetupSkipDialog;
import com.android.settings.password.StorageManagerWrapper;

public class SetupFingerprintEnrollIntroduction extends FingerprintEnrollIntroduction {
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

        if (StorageManagerWrapper.isFileEncryptedNativeOrEmulated()) {
            intent.putExtra(
                    LockPatternUtils.PASSWORD_TYPE_KEY,
                    DevicePolicyManager.PASSWORD_QUALITY_NUMERIC);
            intent.putExtra(ChooseLockGenericFragment.EXTRA_SHOW_OPTIONS_BUTTON, true);
        }
        SetupWizardUtils.copySetupExtras(getIntent(), intent);
        return intent;
    }

    @Override
    protected Intent getFindSensorIntent() {
        final Intent intent = new Intent(this, SetupFingerprintEnrollFindSensor.class);
        SetupWizardUtils.copySetupExtras(getIntent(), intent);
        return intent;
    }

    @Override
    protected void initViews() {
        super.initViews();

        TextView description = (TextView) findViewById(R.id.description_text);
        description.setText(
                R.string.security_settings_fingerprint_enroll_introduction_message_setup);

        Button nextButton = getNextButton();
        nextButton.setText(
                R.string.security_settings_fingerprint_enroll_introduction_continue_setup);

        final Button cancelButton = (Button) findViewById(R.id.fingerprint_cancel_button);
        cancelButton.setText(
                R.string.security_settings_fingerprint_enroll_introduction_cancel_setup);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // if lock was already present, do not return intent data since it must have been
        // reported in previous attempts
        if (requestCode == FINGERPRINT_FIND_SENSOR_REQUEST && isKeyguardSecure()
                && !mAlreadyHadLockScreenSetup) {
            data = getMetricIntent(data);
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

    @Override
    protected void onCancelButtonClick() {
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
        return MetricsEvent.FINGERPRINT_ENROLL_INTRO_SETUP;
    }
}
