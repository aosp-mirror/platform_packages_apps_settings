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
 * limitations under the License.
 */

package com.android.settings.biometrics;

import android.content.Intent;
import android.os.UserHandle;
import android.view.View;

import androidx.annotation.Nullable;

import com.android.settings.password.ChooseLockSettingsHelper;

/**
 * Abstract base activity which handles the actual enrolling for biometrics.
 */
public abstract class BiometricsEnrollEnrolling extends BiometricEnrollBase
        implements BiometricEnrollSidecar.Listener {

    private static final String TAG_SIDECAR = "sidecar";

    @Nullable
    protected BiometricEnrollSidecar mSidecar;

    /**
     * @return the intent for the finish activity
     */
    protected abstract Intent getFinishIntent();

    /**
     * @return an instance of the biometric enroll sidecar
     */
    protected abstract BiometricEnrollSidecar getSidecar();

    /**
     * @return true if enrollment should start automatically.
     */
    protected abstract boolean shouldStartAutomatically();

    @Override
    protected void onStart() {
        super.onStart();
        if (shouldStartAutomatically()) {
            startEnrollment();
        }
    }

    @Override
    protected void onStop() {
        if (mSidecar != null) {
            mSidecar.setListener(null);
        }
        if (!isChangingConfigurations()) {
            if (mSidecar != null) {
                mSidecar.cancelEnrollment();
                getSupportFragmentManager()
                        .beginTransaction().remove(mSidecar).commitAllowingStateLoss();
            }
        }

        super.onStop();
    }

    @Override
    public void onBackPressed() {
        cancelEnrollment();
        super.onBackPressed();
    }

    protected void onSkipButtonClick(View view) {
        cancelEnrollment();
        setResult(RESULT_SKIP);
        finish();
    }

    public void cancelEnrollment() {
        if (mSidecar != null) {
            mSidecar.setListener(null);
            mSidecar.cancelEnrollment();
            getSupportFragmentManager()
                    .beginTransaction().remove(mSidecar).commitAllowingStateLoss();
            mSidecar = null;
        }
    }

    public void startEnrollment() {
        // If it's in multi window mode, dialog is shown, do not start enrollment.
        if (shouldShowSplitScreenDialog()) {
            return;
        }
        startEnrollmentInternal();
    }

    protected void startEnrollmentInternal() {
        mSidecar = (BiometricEnrollSidecar) getSupportFragmentManager()
                .findFragmentByTag(TAG_SIDECAR);
        if (mSidecar == null) {
            mSidecar = getSidecar();
            getSupportFragmentManager().beginTransaction().add(mSidecar, TAG_SIDECAR)
                    .commitAllowingStateLoss();
        }
        mSidecar.setListener(this);
    }

    protected void launchFinish(byte[] token) {
        Intent intent = getFinishIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, token);
        intent.putExtra(BiometricEnrollBase.EXTRA_KEY_SENSOR_ID, mSensorId);
        intent.putExtra(BiometricEnrollBase.EXTRA_KEY_CHALLENGE, mChallenge);
        intent.putExtra(EXTRA_FROM_SETTINGS_SUMMARY, mFromSettingsSummary);
        if (mUserId != UserHandle.USER_NULL) {
            intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
        }
        startActivity(intent);
        finish();
    }

}
