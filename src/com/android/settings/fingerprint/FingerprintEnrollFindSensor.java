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

import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.ChooseLockSettingsHelper;
import com.android.settings.R;

/**
 * Activity explaining the fingerprint sensor location for fingerprint enrollment.
 */
public class FingerprintEnrollFindSensor extends FingerprintEnrollBase {

    private static final int CONFIRM_REQUEST = 1;
    private static final int ENROLLING = 2;
    public static final String EXTRA_KEY_LAUNCHED_CONFIRM = "launched_confirm_lock";

    private FingerprintLocationAnimationView mAnimation;
    private boolean mLaunchedConfirmLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fingerprint_enroll_find_sensor);
        setHeaderText(R.string.security_settings_fingerprint_enroll_find_sensor_title);
        mLaunchedConfirmLock = savedInstanceState != null && savedInstanceState.getBoolean(
                EXTRA_KEY_LAUNCHED_CONFIRM);
        if (mToken == null && !mLaunchedConfirmLock) {
            launchConfirmLock();
        }
        mAnimation = (FingerprintLocationAnimationView) findViewById(
                R.id.fingerprint_sensor_location_animation);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAnimation.startAnimation();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAnimation.stopAnimation();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXTRA_KEY_LAUNCHED_CONFIRM, mLaunchedConfirmLock);
    }

    @Override
    protected void onNextButtonClick() {
        startActivityForResult(getEnrollingIntent(), ENROLLING);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CONFIRM_REQUEST) {
            if (resultCode == RESULT_OK) {
                mToken = data.getByteArrayExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
                overridePendingTransition(R.anim.suw_slide_next_in, R.anim.suw_slide_next_out);
            } else {
                finish();
            }
        } else if (requestCode == ENROLLING) {
            if (resultCode == RESULT_FINISHED) {
                setResult(RESULT_FINISHED);
                finish();
            } else if (resultCode == RESULT_SKIP) {
                setResult(RESULT_SKIP);
                finish();
            } else if (resultCode == RESULT_TIMEOUT) {
                setResult(RESULT_TIMEOUT);
                finish();
            } else {
                FingerprintManager fpm = getSystemService(FingerprintManager.class);
                int enrolled = fpm.getEnrolledFingerprints().size();
                int max = getResources().getInteger(
                        com.android.internal.R.integer.config_fingerprintMaxTemplatesPerUser);
                if (enrolled >= max) {
                    finish();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void launchConfirmLock() {
        long challenge = getSystemService(FingerprintManager.class).preEnroll();
        ChooseLockSettingsHelper helper = new ChooseLockSettingsHelper(this);
        if (!helper.launchConfirmationActivity(CONFIRM_REQUEST,
                getString(R.string.security_settings_fingerprint_preference_title),
                null, null, challenge)) {

            // This shouldn't happen, as we should only end up at this step if a lock thingy is
            // already set.
            finish();
        } else {
            mLaunchedConfirmLock = true;
        }
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.FINGERPRINT_FIND_SENSOR;
    }
}
