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

import static android.hardware.biometrics.BiometricFingerprintConstants.FINGERPRINT_ERROR_CANCELED;
import static android.hardware.fingerprint.FingerprintManager.ENROLL_ENROLL;

import static com.android.settings.biometrics2.ui.model.EnrollmentProgress.INITIAL_REMAINING;
import static com.android.settings.biometrics2.ui.model.EnrollmentProgress.INITIAL_STEPS;

import android.app.Application;
import android.content.res.Resources;
import android.hardware.fingerprint.FingerprintManager.EnrollReason;
import android.hardware.fingerprint.FingerprintManager.EnrollmentCallback;
import android.os.CancellationSignal;
import android.os.SystemClock;
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
import com.android.settings.biometrics2.ui.model.EnrollmentStatusMessage;

import java.util.LinkedList;

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
    private final MutableLiveData<EnrollmentStatusMessage> mHelpMessageLiveData =
            new MutableLiveData<>();
    private final MutableLiveData<EnrollmentStatusMessage> mErrorMessageLiveData =
            new MutableLiveData<>();
    private final MutableLiveData<Object> mCanceledSignalLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> mAcquireLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> mPointerDownLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> mPointerUpLiveData = new MutableLiveData<>();

    private byte[] mToken = null;
    private final int mUserId;

    private final FingerprintUpdater mFingerprintUpdater;
    @Nullable private CancellationSignal mCancellationSignal = null;
    @NonNull private final LinkedList<CancellationSignal> mCancelingSignalQueue =
            new LinkedList<>();
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
            mHelpMessageLiveData.setValue(null);
            mProgressLiveData.postValue(progress);
        }

        @Override
        public void onEnrollmentHelp(int helpMsgId, CharSequence helpString) {
            if (DEBUG) {
                Log.d(TAG, "onEnrollmentHelp(" + helpMsgId + ", " + helpString + ")");
            }
            mHelpMessageLiveData.postValue(new EnrollmentStatusMessage(helpMsgId, helpString));
        }

        @Override
        public void onEnrollmentError(int errMsgId, CharSequence errString) {
            Log.d(TAG, "onEnrollmentError(" + errMsgId + ", " + errString
                    + "), cancelingQueueSize:" + mCancelingSignalQueue.size());
            if (FINGERPRINT_ERROR_CANCELED == errMsgId && mCancelingSignalQueue.size() > 0) {
                mCanceledSignalLiveData.postValue(mCancelingSignalQueue.poll());
            } else {
                mErrorMessageLiveData.postValue(new EnrollmentStatusMessage(errMsgId, errString));
            }
        }

        @Override
        public void onAcquired(boolean isAcquiredGood) {
            mAcquireLiveData.postValue(isAcquiredGood);
        }

        @Override
        public void onUdfpsPointerDown(int sensorId) {
            mPointerDownLiveData.postValue(sensorId);
        }

        @Override
        public void onUdfpsPointerUp(int sensorId) {
            mPointerUpLiveData.postValue(sensorId);
        }
    };

    public FingerprintEnrollProgressViewModel(@NonNull Application application,
            @NonNull FingerprintUpdater fingerprintUpdater, int userId) {
        super(application);
        mFingerprintUpdater = fingerprintUpdater;
        mUserId = userId;
    }

    public void setToken(byte[] token) {
        mToken = token;
    }

    /**
     * clear progress
     */
    public void clearProgressLiveData() {
        mProgressLiveData.setValue(new EnrollmentProgress(INITIAL_STEPS, INITIAL_REMAINING));
        mHelpMessageLiveData.setValue(null);
        mErrorMessageLiveData.setValue(null);
    }

    /**
     * clear error message
     */
    public void clearErrorMessageLiveData() {
        mErrorMessageLiveData.setValue(null);
    }

    public LiveData<EnrollmentProgress> getProgressLiveData() {
        return mProgressLiveData;
    }

    public LiveData<EnrollmentStatusMessage> getHelpMessageLiveData() {
        return mHelpMessageLiveData;
    }

    public LiveData<EnrollmentStatusMessage> getErrorMessageLiveData() {
        return mErrorMessageLiveData;
    }

    public LiveData<Object> getCanceledSignalLiveData() {
        return mCanceledSignalLiveData;
    }

    public LiveData<Boolean> getAcquireLiveData() {
        return mAcquireLiveData;
    }

    public LiveData<Integer> getPointerDownLiveData() {
        return mPointerDownLiveData;
    }

    public LiveData<Integer> getPointerUpLiveData() {
        return mPointerUpLiveData;
    }

    /**
     * Starts enrollment and return latest isEnrolling() result
     */
    public Object startEnrollment(@EnrollReason int reason) {
        if (mToken == null) {
            Log.e(TAG, "Null hardware auth token for enroll");
            return null;
        }
        if (mCancellationSignal != null) {
            Log.w(TAG, "Enrolling is running, shall not start again");
            return mCancellationSignal;
        }
        if (DEBUG) {
            Log.e(TAG, "startEnrollment(" + reason + ")");
        }

        // Clear data
        mProgressLiveData.setValue(new EnrollmentProgress(INITIAL_STEPS, INITIAL_REMAINING));
        mHelpMessageLiveData.setValue(null);
        mErrorMessageLiveData.setValue(null);

        mCancellationSignal = new CancellationSignal();

        final Resources res = getApplication().getResources();
        if (reason == ENROLL_ENROLL
                && res.getBoolean(R.bool.enrollment_message_display_controller_flag)) {
            final EnrollmentCallback callback = new MessageDisplayController(
                    getApplication().getMainThreadHandler(),
                    mEnrollmentCallback,
                    SystemClock.elapsedRealtimeClock(),
                    res.getInteger(R.integer.enrollment_help_minimum_time_display),
                    res.getInteger(R.integer.enrollment_progress_minimum_time_display),
                    res.getBoolean(R.bool.enrollment_progress_priority_over_help),
                    res.getBoolean(R.bool.enrollment_prioritize_acquire_messages),
                    res.getInteger(R.integer.enrollment_collect_time));
            mFingerprintUpdater.enroll(mToken, mCancellationSignal, mUserId, callback, reason);
        } else {
            mFingerprintUpdater.enroll(mToken, mCancellationSignal, mUserId, mEnrollmentCallback,
                    reason);
        }
        return mCancellationSignal;
    }

    /**
     * Cancels enrollment and return latest isEnrolling result
     */
    public boolean cancelEnrollment() {
        final CancellationSignal cancellationSignal = mCancellationSignal;
        mCancellationSignal = null;

        if (cancellationSignal == null) {
            Log.e(TAG, "Fail to cancel enrollment, has cancelled or not start");
            return false;
        } else {
            Log.d(TAG, "enrollment cancelled");
        }
        mCancelingSignalQueue.add(cancellationSignal);
        cancellationSignal.cancel();

        return true;
    }

    public boolean isEnrolling() {
        return (mCancellationSignal != null);
    }

    private int getSteps() {
        return mProgressLiveData.getValue().getSteps();
    }
}
