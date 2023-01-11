/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.biometrics2.ui.viewmodel;

import static com.android.settings.biometrics2.ui.model.EnrollmentProgress.INITIAL_REMAINING;
import static com.android.settings.biometrics2.ui.model.EnrollmentProgress.INITIAL_STEPS;

import android.app.Application;
import android.content.res.Resources;
import android.hardware.fingerprint.FingerprintManager.EnrollReason;
import android.hardware.fingerprint.FingerprintManager.EnrollmentCallback;
import android.os.CancellationSignal;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.android.settings.R;
import com.android.settings.biometrics.fingerprint.FingerprintUpdater;
import com.android.settings.biometrics.fingerprint.MessageDisplayController;
import com.android.settings.biometrics2.ui.model.EnrollmentProgress;

/**
 * Progress ViewModel handles the state around biometric enrollment. It manages the state of
 * enrollment throughout the activity lifecycle so the app can continue after an event like
 * rotation.
 */
public class FingerprintEnrollProgressViewModel extends AndroidViewModel {

    private static final boolean DEBUG = false;
    private static final String TAG = "FingerprintEnrollProgressViewModel";

    private final MutableLiveData<EnrollmentProgress> mProgressLiveData = new MutableLiveData<>(
            new EnrollmentProgress(INITIAL_STEPS, INITIAL_REMAINING));

    private byte[] mToken = null;
    private int mUserId = UserHandle.myUserId();

    private final FingerprintUpdater mFingerprintUpdater;
    private final MessageDisplayController mMessageDisplayController;
    private EnrollmentHelper mEnrollmentHelper;
    private final EnrollmentCallback mEnrollmentCallback = new EnrollmentCallback() {

        @Override
        public void onEnrollmentProgress(int remaining) {
            final int currentSteps = getSteps();
            final EnrollmentProgress progress = new EnrollmentProgress(
                    currentSteps == INITIAL_STEPS ? remaining : getSteps(), remaining);
            if (DEBUG) {
                Log.d(TAG, "onEnrollmentProgress(" + remaining + "), steps: " + currentSteps
                        + ", post progress as " + progress);
            }
            mProgressLiveData.postValue(progress);
            // TODO set enrolling to false when remaining is 0 during implementing b/260957933
        }

        @Override
        public void onEnrollmentHelp(int helpMsgId, CharSequence helpString) {
            // TODO add LiveData for help message during implementing b/260957933
        }

        @Override
        public void onEnrollmentError(int errMsgId, CharSequence errString) {
            // TODO add LiveData for error message during implementing b/260957933
        }
    };

    public FingerprintEnrollProgressViewModel(@NonNull Application application,
            @NonNull FingerprintUpdater fingerprintUpdater) {
        super(application);
        mFingerprintUpdater = fingerprintUpdater;
        final Resources res = application.getResources();
        mMessageDisplayController =
                res.getBoolean(R.bool.enrollment_message_display_controller_flag)
                ? new MessageDisplayController(
                        application.getMainThreadHandler(),
                        mEnrollmentCallback,
                        SystemClock.elapsedRealtimeClock(),
                        res.getInteger(R.integer.enrollment_help_minimum_time_display),
                        res.getInteger(R.integer.enrollment_progress_minimum_time_display),
                        res.getBoolean(R.bool.enrollment_progress_priority_over_help),
                        res.getBoolean(R.bool.enrollment_prioritize_acquire_messages),
                        res.getInteger(R.integer.enrollment_collect_time)) : null;
    }

    public void setToken(byte[] token) {
        mToken = token;
    }

    public void setUserId(int userId) {
        mUserId = userId;
    }

    /**
     * clear progress
     */
    public void clearProgressLiveData() {
        mProgressLiveData.setValue(new EnrollmentProgress(INITIAL_STEPS, INITIAL_REMAINING));
    }

    public LiveData<EnrollmentProgress> getProgressLiveData() {
        return mProgressLiveData;
    }

    /**
     * Starts enrollment and return latest isEnrolling() result
     */
    public boolean startEnrollment(@EnrollReason int reason) {
        if (mToken == null) {
            Log.e(TAG, "Null hardware auth token for enroll");
            return false;
        }
        if (isEnrolling()) {
            Log.w(TAG, "Enrolling has started, shall not start again");
            return true;
        }

        mEnrollmentHelper = new EnrollmentHelper(
                mMessageDisplayController != null
                        ? mMessageDisplayController
                        : mEnrollmentCallback);
        mEnrollmentHelper.startEnrollment(mFingerprintUpdater, mToken, mUserId, reason);
        return true;
    }

    /**
     * Cancels enrollment and return latest isEnrolling result
     */
    public boolean cancelEnrollment() {
        if (!isEnrolling() || mEnrollmentHelper == null) {
            Log.e(TAG, "Fail to cancel enrollment, enrollmentController exist:"
                    + (mEnrollmentHelper != null));
            return false;
        }

        mEnrollmentHelper.cancelEnrollment();
        mEnrollmentHelper = null;
        return true;
    }

    public boolean isEnrolling() {
        return (mEnrollmentHelper != null);
    }

    private int getSteps() {
        return mProgressLiveData.getValue().getSteps();
    }

    /**
     * This class is used to stop latest message from onEnrollmentError() after user cancelled
     * enrollment. This class will not forward message anymore after mCancellationSignal is sent.
     */
    private static class EnrollmentHelper extends EnrollmentCallback {

        @NonNull private final EnrollmentCallback mEnrollmentCallback;
        @Nullable private CancellationSignal mCancellationSignal = new CancellationSignal();

        EnrollmentHelper(@NonNull EnrollmentCallback enrollmentCallback) {
            mEnrollmentCallback = enrollmentCallback;
        }

        @Override
        public void onEnrollmentError(int errMsgId, CharSequence errString) {
            if (mCancellationSignal == null) {
                return;
            }
            mEnrollmentCallback.onEnrollmentError(errMsgId, errString);
        }

        @Override
        public void onEnrollmentHelp(int helpMsgId, CharSequence helpString) {
            if (mCancellationSignal == null) {
                return;
            }
            mEnrollmentCallback.onEnrollmentHelp(helpMsgId, helpString);
        }

        @Override
        public void onEnrollmentProgress(int remaining) {
            if (mCancellationSignal == null) {
                return;
            }
            mEnrollmentCallback.onEnrollmentProgress(remaining);
        }

        /**
         * Starts enrollment
         */
        public boolean startEnrollment(@NonNull FingerprintUpdater fingerprintUpdater,
                @NonNull byte[] token, int userId, @EnrollReason int reason) {
            if (mCancellationSignal == null) {
                // Not allow enrolling twice as same instance. Allocate a new instance for second
                // enrollment.
                return false;
            }
            fingerprintUpdater.enroll(token, mCancellationSignal, userId, this, reason);
            return true;
        }

        /**
         * Cancels current enrollment
         */
        public void cancelEnrollment() {
            final CancellationSignal cancellationSignal = mCancellationSignal;
            mCancellationSignal = null;

            if (cancellationSignal != null) {
                cancellationSignal.cancel();
            }
        }
    }
}
