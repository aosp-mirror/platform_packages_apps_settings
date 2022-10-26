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
import android.app.settings.SettingsEnums;
import android.hardware.fingerprint.FingerprintManager;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.biometrics.BiometricEnrollSidecar;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Sidecar fragment to handle the state around fingerprint enrollment.
 */
public class FingerprintEnrollSidecar extends BiometricEnrollSidecar {
    private static final String TAG = "FingerprintEnrollSidecar";

    private FingerprintUpdater mFingerprintUpdater;
    private @FingerprintManager.EnrollReason int mEnrollReason;
    private Set<Integer> mHelpIgnore;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mFingerprintUpdater = new FingerprintUpdater(activity);
        final int[] ignoreAcquiredInfo = getResources().getIntArray(
                R.array.fingerprint_acquired_ignore_list);
        mHelpIgnore = new HashSet<>();
        for (int acquiredInfo: ignoreAcquiredInfo) {
            mHelpIgnore.add(acquiredInfo);
        }
        mHelpIgnore = Collections.unmodifiableSet(mHelpIgnore);
    }

    @Override
    protected void startEnrollment() {
        super.startEnrollment();

        if (mToken == null) {
            Log.e(TAG, "Null hardware auth token for enroll");
            onEnrollmentError(FingerprintManager.FINGERPRINT_ERROR_HW_UNAVAILABLE,
                    getString(R.string.fingerprint_intro_error_unknown));
            return;
        }
        mFingerprintUpdater.enroll(mToken, mEnrollmentCancel, mUserId, mEnrollmentCallback,
                mEnrollReason);
    }

    public void setEnrollReason(@FingerprintManager.EnrollReason int enrollReason) {
        mEnrollReason = enrollReason;
    }

    @VisibleForTesting FingerprintManager.EnrollmentCallback mEnrollmentCallback
            = new FingerprintManager.EnrollmentCallback() {

        @Override
        public void onEnrollmentProgress(int remaining) {
            FingerprintEnrollSidecar.super.onEnrollmentProgress(remaining);
        }

        @Override
        public void onEnrollmentHelp(int helpMsgId, CharSequence helpString) {
            if (mHelpIgnore.contains(helpMsgId)) {
                return;
            }
            FingerprintEnrollSidecar.super.onEnrollmentHelp(helpMsgId, helpString);
        }

        @Override
        public void onEnrollmentError(int errMsgId, CharSequence errString) {
            FingerprintEnrollSidecar.super.onEnrollmentError(errMsgId, errString);
        }
    };

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.FINGERPRINT_ENROLL_SIDECAR;
    }
}
