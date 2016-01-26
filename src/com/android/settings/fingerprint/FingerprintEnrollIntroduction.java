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

import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.view.View;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.ChooseLockGeneric;
import com.android.settings.ChooseLockSettingsHelper;
import com.android.settings.HelpUtils;
import com.android.settings.R;

/**
 * Onboarding activity for fingerprint enrollment.
 */
public class FingerprintEnrollIntroduction extends FingerprintEnrollBase {

    private static final int CHOOSE_LOCK_GENERIC_REQUEST = 1;
    private static final int FINGERPRINT_FIND_SENSOR_REQUEST = 2;

    private boolean mHasPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fingerprint_enroll_introduction);
        setHeaderText(R.string.security_settings_fingerprint_enroll_introduction_title);
        findViewById(R.id.cancel_button).setOnClickListener(this);
        findViewById(R.id.learn_more_button).setOnClickListener(this);
        updatePasswordQuality();
    }

    private void updatePasswordQuality() {
        final int passwordQuality = new ChooseLockSettingsHelper(this).utils()
                .getActivePasswordQuality(mUserId);
        mHasPassword = passwordQuality != DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;
    }

    @Override
    protected void onNextButtonClick() {
        if (!mHasPassword) {
            // No fingerprints registered, launch into enrollment wizard.
            launchChooseLock();
        } else {
            // Lock thingy is already set up, launch directly into find sensor step from wizard.
            launchFindSensor(null);
        }
    }

    private void launchChooseLock() {
        Intent intent = getChooseLockIntent();
        long challenge = getSystemService(FingerprintManager.class).preEnroll();
        intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.MINIMUM_QUALITY_KEY,
                DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);
        intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.HIDE_DISABLED_PREFS, true);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, true);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, challenge);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_FOR_FINGERPRINT, true);
        if (mUserId != UserHandle.USER_NULL) {
            intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
        }
        startActivityForResult(intent, CHOOSE_LOCK_GENERIC_REQUEST);
    }

    private void launchFindSensor(byte[] token) {
        Intent intent = getFindSensorIntent();
        if (token != null) {
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, token);
        }
        startActivityForResult(intent, FINGERPRINT_FIND_SENSOR_REQUEST);
    }

    protected Intent getChooseLockIntent() {
        return new Intent(this, ChooseLockGeneric.class);
    }

    protected Intent getFindSensorIntent() {
        Intent intent = new Intent(this, FingerprintEnrollFindSensor.class);
        if (mUserId != UserHandle.USER_NULL) {
            intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
        }
        return intent;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_FINISHED) {
            if (requestCode == FINGERPRINT_FIND_SENSOR_REQUEST) {
                setResult(RESULT_OK);
                finish();
            } else if (requestCode == CHOOSE_LOCK_GENERIC_REQUEST) {
                updatePasswordQuality();
                byte[] token = data.getByteArrayExtra(
                        ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
                launchFindSensor(token);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.cancel_button) {
            finish();
        }
        if (v.getId() == R.id.learn_more_button) {
            launchFingerprintHelp();
        }
        super.onClick(v);
    }

    private void launchFingerprintHelp() {
        Intent helpIntent = HelpUtils.getHelpIntent(this,
                getString(R.string.help_url_fingerprint), getClass().getName());
        startActivity(helpIntent);
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.FINGERPRINT_ENROLL_INTRO;
    }
}
