/*
 * Copyright (C) 2023 The Android Open Source Project
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

import android.annotation.IntDef;
import android.app.Application;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;
import android.os.Bundle;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.android.settings.biometrics2.data.repository.AccessibilityRepository;
import com.android.settings.biometrics2.data.repository.FingerprintRepository;
import com.android.settings.biometrics2.data.repository.VibratorRepository;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * ViewModel explaining the fingerprint enrolling page
 */
public class FingerprintEnrollEnrollingViewModel extends AndroidViewModel {

    private static final String TAG = FingerprintEnrollEnrollingViewModel.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final VibrationEffect VIBRATE_EFFECT_ERROR =
            VibrationEffect.createWaveform(new long[]{0, 5, 55, 60}, -1);
    private static final VibrationAttributes FINGERPRINT_ENROLLING_SONFICATION_ATTRIBUTES =
            VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ACCESSIBILITY);

    /**
     * Enrolling skipped
     */
    public static final int FINGERPRINT_ENROLL_ENROLLING_ACTION_SKIP = 0;

    /**
     * Enrolling finished
     */
    public static final int FINGERPRINT_ENROLL_ENROLLING_ACTION_DONE = 1;

    /**
     * Icon touch dialog show
     */
    public static final int FINGERPRINT_ENROLL_ENROLLING_ACTION_SHOW_ICON_TOUCH_DIALOG = 2;

    /**
     * Icon touch dialog dismiss
     */
    public static final int FINGERPRINT_ENROLL_ENROLLING_ACTION_DISMISS_ICON_TOUCH_DIALOG = 3;

    /**
     * Has got latest cancelled event due to back key
     */
    public static final int FINGERPRINT_ENROLL_ENROLLING_CANCELED_BECAUSE_BACK_PRESSED = 4;

    @IntDef(prefix = { "FINGERPRINT_ENROLL_ENROLLING_ACTION_" }, value = {
            FINGERPRINT_ENROLL_ENROLLING_ACTION_SKIP,
            FINGERPRINT_ENROLL_ENROLLING_ACTION_DONE,
            FINGERPRINT_ENROLL_ENROLLING_ACTION_SHOW_ICON_TOUCH_DIALOG,
            FINGERPRINT_ENROLL_ENROLLING_ACTION_DISMISS_ICON_TOUCH_DIALOG,
            FINGERPRINT_ENROLL_ENROLLING_CANCELED_BECAUSE_BACK_PRESSED
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface FingerprintEnrollEnrollingAction {}

    /**
     * Enrolling skipped
     */
    public static final int FINGERPRINT_ERROR_DIALOG_ACTION_SET_RESULT_FINISH = 0;

    /**
     * Enrolling finished
     */
    public static final int FINGERPRINT_ERROR_DIALOG_ACTION_SET_RESULT_TIMEOUT = 1;

    /**
     * Icon touch dialog show
     */
    public static final int FINGERPRINT_ERROR_DIALOG_ACTION_RESTART = 2;

    @IntDef(prefix = { "FINGERPRINT_ERROR_DIALOG_ACTION_" }, value = {
            FINGERPRINT_ERROR_DIALOG_ACTION_SET_RESULT_FINISH,
            FINGERPRINT_ERROR_DIALOG_ACTION_SET_RESULT_TIMEOUT,
            FINGERPRINT_ERROR_DIALOG_ACTION_RESTART
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface FingerprintErrorDialogAction {}

    private final int mUserId;
    private boolean mOnBackPressed;
    private boolean mOnSkipPressed;
    private final FingerprintRepository mFingerprintRepository;
    private final AccessibilityRepository mAccessibilityRepository;
    private final VibratorRepository mVibratorRepository;

    private final MutableLiveData<Integer> mActionLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> mIconTouchDialogLiveData = new MutableLiveData<>();
    private final MutableLiveData<ErrorDialogData> mErrorDialogLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> mErrorDialogActionLiveData = new MutableLiveData<>();

    public FingerprintEnrollEnrollingViewModel(Application application,
            int userId,
            FingerprintRepository fingerprintRepository,
            AccessibilityRepository accessibilityRepository,
            VibratorRepository vibratorRepository) {
        super(application);
        mUserId = userId;
        mFingerprintRepository = fingerprintRepository;
        mAccessibilityRepository = accessibilityRepository;
        mVibratorRepository = vibratorRepository;
    }

    /**
     * Notifies activity to show error dialog
     */
    public void showErrorDialog(@NonNull ErrorDialogData errorDialogData) {
        mErrorDialogLiveData.postValue(errorDialogData);
    }

    public LiveData<ErrorDialogData> getErrorDialogLiveData() {
        return mErrorDialogLiveData;
    }

    public LiveData<Integer> getErrorDialogActionLiveData() {
        return mErrorDialogActionLiveData;
    }

    public LiveData<Integer> getActionLiveData() {
        return mActionLiveData;
    }

    /**
     * Clears action live data
     */
    public void clearActionLiveData() {
        mActionLiveData.setValue(null);
    }

    /**
     * Saves new user dialog action to mErrorDialogActionLiveData
     */
    public void onErrorDialogAction(@FingerprintErrorDialogAction int action) {
        if (DEBUG) {
            Log.d(TAG, "onErrorDialogAction(" + action + ")");
        }
        mErrorDialogActionLiveData.postValue(action);
    }

    public boolean getOnSkipPressed() {
        return mOnSkipPressed;
    }

    /**
     * User clicks skip button
     */
    public void setOnSkipPressed() {
        mOnSkipPressed = true;
    }

    /**
     * Enrolling is cacelled because user clicks skip
     */
    public void onCancelledDueToOnSkipPressed() {
        final int action = FINGERPRINT_ENROLL_ENROLLING_ACTION_SKIP;
        if (DEBUG) {
            Log.d(TAG, "onSkipButtonClick, post action " + action);
        }
        mOnSkipPressed = false;
        mActionLiveData.postValue(action);
    }

    /**
     * Is enrolling finished
     */
    public void onEnrollingDone() {
        final int action = FINGERPRINT_ENROLL_ENROLLING_ACTION_DONE;
        if (DEBUG) {
            Log.d(TAG, "onEnrollingDone, post action " + action);
        }
        mActionLiveData.postValue(action);
    }

    public boolean getOnBackPressed() {
        return mOnBackPressed;
    }

    /**
     * Back key is pressed.
     */
    public void setOnBackPressed() {
        mOnBackPressed = true;
    }

    /**
     * Enrollment is cancelled because back key is pressed.
     */
    public void onCancelledDueToOnBackPressed() {
        final int action = FINGERPRINT_ENROLL_ENROLLING_CANCELED_BECAUSE_BACK_PRESSED;
        if (DEBUG) {
            Log.d(TAG, "onCancelledEventReceivedAfterOnBackPressed, post action " + action);
        }
        mOnBackPressed = false;
        mActionLiveData.postValue(action);
    }

    /**
     * Icon touch dialog show
     */
    public void onIconTouchDialogShow() {
        final int action = FINGERPRINT_ENROLL_ENROLLING_ACTION_SHOW_ICON_TOUCH_DIALOG;
        if (DEBUG) {
            Log.d(TAG, "onIconTouchDialogShow, post action " + action);
        }
        mIconTouchDialogLiveData.postValue(action);
    }

    /**
     * Icon touch dialog dismiss
     */
    public void onIconTouchDialogDismiss() {
        final int action = FINGERPRINT_ENROLL_ENROLLING_ACTION_DISMISS_ICON_TOUCH_DIALOG;
        if (DEBUG) {
            Log.d(TAG, "onIconTouchDialogDismiss, post action " + action);
        }
        mIconTouchDialogLiveData.postValue(action);
    }

    /**
     * get enroll stage threshold
     */
    public float getEnrollStageThreshold(int index) {
        return mFingerprintRepository.getEnrollStageThreshold(index);
    }

    /**
     * Get enroll stage count
     */
    public int getEnrollStageCount() {
        return mFingerprintRepository.getEnrollStageCount();
    }

    /**
     * Requests interruption of the accessibility feedback from all accessibility services.
     */
    public void clearTalkback() {
        mAccessibilityRepository.interrupt();
    }

    /**
     * Returns if the {@link AccessibilityManager} is enabled.
     *
     * @return True if this {@link AccessibilityManager} is enabled, false otherwise.
     */
    public boolean isAccessibilityEnabled() {
        return mAccessibilityRepository.isEnabled();
    }

    /**
     * Like {@link #vibrate(VibrationEffect, VibrationAttributes)}, but allows the
     * caller to specify the vibration is owned by someone else and set a reason for vibration.
     */
    public void vibrateError(String reason) {
        mVibratorRepository.vibrate(mUserId, getApplication().getOpPackageName(),
                VIBRATE_EFFECT_ERROR, reason, FINGERPRINT_ENROLLING_SONFICATION_ATTRIBUTES);
    }

    /**
     * Gets the first FingerprintSensorPropertiesInternal from FingerprintManager
     */
    @Nullable
    public FingerprintSensorPropertiesInternal getFirstFingerprintSensorPropertiesInternal() {
        return mFingerprintRepository.getFirstFingerprintSensorPropertiesInternal();
    }

    /**
     * The first sensor type is UDFPS sensor or not
     */
    public boolean canAssumeUdfps() {
        return mFingerprintRepository.canAssumeUdfps();
    }

    /**
     * Saves current state to outState
     */
    public void onSaveInstanceState(@NonNull Bundle outState) {
        // TODO
//        mRestoring = true;
//        mIsCanceled = savedInstanceState.getBoolean(KEY_STATE_CANCELED, false);
//        mPreviousRotation = savedInstanceState.getInt(KEY_STATE_PREVIOUS_ROTATION,
//                getDisplay().getRotation());
//        mIsOrientationChanged = mPreviousRotation != getDisplay().getRotation();
    }

    /**
     * Restores saved state from previous savedInstanceState
     */
    public void restoreSavedState(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        // TODO
//        outState.putBoolean(KEY_STATE_CANCELED, mIsCanceled);
//        outState.putInt(KEY_STATE_PREVIOUS_ROTATION, mPreviousRotation);
    }

    /**
     * Data for passing to FingerprintEnrollEnrollingErrorDialog
     */
    public static class ErrorDialogData {
        @NonNull private final CharSequence mErrMsg;
        @NonNull private final CharSequence mErrTitle;
        @NonNull private final int mErrMsgId;

        public ErrorDialogData(@NonNull CharSequence errMsg, @NonNull CharSequence errTitle,
                int errMsgId) {
            mErrMsg = errMsg;
            mErrTitle = errTitle;
            mErrMsgId = errMsgId;
        }

        public CharSequence getErrMsg() {
            return mErrMsg;
        }

        public CharSequence getErrTitle() {
            return mErrTitle;
        }

        public int getErrMsgId() {
            return mErrMsgId;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode())
                    + "{id:" + mErrMsgId + "}";
        }
    }
}
