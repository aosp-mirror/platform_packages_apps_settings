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

import static android.hardware.fingerprint.FingerprintManager.ENROLL_ENROLL;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.SystemClock;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.biometrics.BiometricEnrollSidecar;

/**
 * Sidecar fragment to handle the state around fingerprint enrollment.
 */
public class FingerprintEnrollSidecar extends BiometricEnrollSidecar {
    private static final String TAG = "FingerprintEnrollSidecar";

    private FingerprintUpdater mFingerprintUpdater;
    private @FingerprintManager.EnrollReason int mEnrollReason;
    private final MessageDisplayController mMessageDisplayController;
    private final boolean mMessageDisplayControllerFlag;

    /**
     * Create a new FingerprintEnrollSidecar object.
     * @param context associated context
     * @param enrollReason reason for enrollment
     */
    public FingerprintEnrollSidecar(Context context,
            @FingerprintManager.EnrollReason int enrollReason) {
        mEnrollReason = enrollReason;

        int helpMinimumDisplayTime = context.getResources().getInteger(
                R.integer.enrollment_help_minimum_time_display);
        int progressMinimumDisplayTime = context.getResources().getInteger(
                R.integer.enrollment_progress_minimum_time_display);
        boolean progressPriorityOverHelp = context.getResources().getBoolean(
                R.bool.enrollment_progress_priority_over_help);
        boolean prioritizeAcquireMessages = context.getResources().getBoolean(
                R.bool.enrollment_prioritize_acquire_messages);
        int collectTime = context.getResources().getInteger(
                R.integer.enrollment_collect_time);
        mMessageDisplayControllerFlag = context.getResources().getBoolean(
                R.bool.enrollment_message_display_controller_flag);

        mMessageDisplayController = new MessageDisplayController(context.getMainThreadHandler(),
                mEnrollmentCallback, SystemClock.elapsedRealtimeClock(), helpMinimumDisplayTime,
                progressMinimumDisplayTime, progressPriorityOverHelp, prioritizeAcquireMessages,
                collectTime);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mFingerprintUpdater = new FingerprintUpdater(activity);
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

        if (mEnrollReason == ENROLL_ENROLL && mMessageDisplayControllerFlag) {
            //API calls need to be processed for {@link FingerprintEnrollEnrolling}
            mFingerprintUpdater.enroll(mToken, mEnrollmentCancel, mUserId,
                    mMessageDisplayController, mEnrollReason);
        } else {
            //No processing required for {@link FingerprintEnrollFindSensor}
            mFingerprintUpdater.enroll(mToken, mEnrollmentCancel, mUserId, mEnrollmentCallback,
                    mEnrollReason);
        }
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
