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
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.android.settings.biometrics2.data.repository.FingerprintRepository;

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
     * Enrolling finished
     */
    public static final int FINGERPRINT_ENROLL_ENROLLING_ACTION_DONE = 0;

    /**
     * Icon touch dialog show
     */
    public static final int FINGERPRINT_ENROLL_ENROLLING_ACTION_SHOW_ICON_TOUCH_DIALOG = 1;

    /**
     * Has got latest cancelled event due to user skip
     */
    public static final int FINGERPRINT_ENROLL_ENROLLING_CANCELED_BECAUSE_USER_SKIP = 2;

    /**
     * Has got latest cancelled event due to back key
     */
    public static final int FINGERPRINT_ENROLL_ENROLLING_CANCELED_BECAUSE_BACK_PRESSED = 3;

    @IntDef(prefix = { "FINGERPRINT_ENROLL_ENROLLING_ACTION_" }, value = {
            FINGERPRINT_ENROLL_ENROLLING_ACTION_DONE,
            FINGERPRINT_ENROLL_ENROLLING_ACTION_SHOW_ICON_TOUCH_DIALOG,
            FINGERPRINT_ENROLL_ENROLLING_CANCELED_BECAUSE_USER_SKIP
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface FingerprintEnrollEnrollingAction {}

    private final int mUserId;
    private boolean mOnBackPressed;
    private boolean mOnSkipPressed;
    @NonNull private final FingerprintRepository mFingerprintRepository;
    private final AccessibilityManager mAccessibilityManager;
    private final Vibrator mVibrator;

    private final MutableLiveData<Integer> mActionLiveData = new MutableLiveData<>();

    public FingerprintEnrollEnrollingViewModel(
            @NonNull Application application,
            int userId,
            @NonNull FingerprintRepository fingerprintRepository
    ) {
        super(application);
        mUserId = userId;
        mFingerprintRepository = fingerprintRepository;
        mAccessibilityManager = application.getSystemService(AccessibilityManager.class);
        mVibrator = application.getSystemService(Vibrator.class);
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
     * Enrolling is cancelled because user clicks skip
     */
    public void onCancelledDueToOnSkipPressed() {
        final int action = FINGERPRINT_ENROLL_ENROLLING_CANCELED_BECAUSE_USER_SKIP;
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
    public void showIconTouchDialog() {
        final int action = FINGERPRINT_ENROLL_ENROLLING_ACTION_SHOW_ICON_TOUCH_DIALOG;
        if (DEBUG) {
            Log.d(TAG, "onIconTouchDialogShow, post action " + action);
        }
        mActionLiveData.postValue(action);
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
        mAccessibilityManager.interrupt();
    }

    /**
     * Returns if the {@link AccessibilityManager} is enabled.
     *
     * @return True if this {@link AccessibilityManager} is enabled, false otherwise.
     */
    public boolean isAccessibilityEnabled() {
        return mAccessibilityManager.isEnabled();
    }

    /**
     * Sends an {@link AccessibilityEvent}.
     */
    public void sendAccessibilityEvent(CharSequence announcement) {
        AccessibilityEvent e = AccessibilityEvent.obtain();
        e.setEventType(AccessibilityEvent.TYPE_ANNOUNCEMENT);
        e.setClassName(getClass().getName());
        e.setPackageName(getApplication().getPackageName());
        e.getText().add(announcement);
        mAccessibilityManager.sendAccessibilityEvent(e);
    }

     /**
     * Returns if the touch exploration in the system is enabled.
     *
     * @return True if touch exploration is enabled, false otherwise.
     */
    public boolean isTouchExplorationEnabled() {
        return mAccessibilityManager.isTouchExplorationEnabled();
    }

    /**
     * Like {@link #vibrate(VibrationEffect, VibrationAttributes)}, but allows the
     * caller to specify the vibration is owned by someone else and set a reason for vibration.
     */
    public void vibrateError(String reason) {
        mVibrator.vibrate(mUserId, getApplication().getOpPackageName(),
                VIBRATE_EFFECT_ERROR, reason, FINGERPRINT_ENROLLING_SONFICATION_ATTRIBUTES);
    }

    /**
     * Gets the first FingerprintSensorPropertiesInternal from FingerprintManager
     */
    @Nullable
    public FingerprintSensorPropertiesInternal getFirstFingerprintSensorPropertiesInternal() {
        return mFingerprintRepository.getFirstFingerprintSensorPropertiesInternal();
    }
}
