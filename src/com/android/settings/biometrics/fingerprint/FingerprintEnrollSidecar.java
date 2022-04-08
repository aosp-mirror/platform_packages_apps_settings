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
import android.os.UserHandle;

import com.android.settings.Utils;
import com.android.settings.biometrics.BiometricEnrollSidecar;

/**
 * Sidecar fragment to handle the state around fingerprint enrollment.
 */
public class FingerprintEnrollSidecar extends BiometricEnrollSidecar {

    private FingerprintManager mFingerprintManager;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mFingerprintManager = Utils.getFingerprintManagerOrNull(activity);
    }

    @Override
    protected void startEnrollment() {
        super.startEnrollment();
        if (mUserId != UserHandle.USER_NULL) {
            mFingerprintManager.setActiveUser(mUserId);
        }
        mFingerprintManager.enroll(mToken, mEnrollmentCancel,
                0 /* flags */, mUserId, mEnrollmentCallback);
    }

    private FingerprintManager.EnrollmentCallback mEnrollmentCallback
            = new FingerprintManager.EnrollmentCallback() {

        @Override
        public void onEnrollmentProgress(int remaining) {
            FingerprintEnrollSidecar.super.onEnrollmentProgress(remaining);
        }

        @Override
        public void onEnrollmentHelp(int helpMsgId, CharSequence helpString) {
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
