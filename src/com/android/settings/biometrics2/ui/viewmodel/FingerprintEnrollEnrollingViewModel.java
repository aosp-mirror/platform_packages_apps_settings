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

import android.app.Application;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.MutableLiveData;

import com.android.settings.biometrics2.data.repository.AccessibilityRepository;
import com.android.settings.biometrics2.data.repository.FingerprintRepository;
import com.android.settings.biometrics2.data.repository.VibratorRepository;
import com.android.settings.biometrics2.ui.model.EnrollmentRequest;

/**
 * ViewModel explaining the fingerprint enrolling page
 */
public class FingerprintEnrollEnrollingViewModel extends AndroidViewModel
        implements DefaultLifecycleObserver {

    private static final String TAG = FingerprintEnrollEnrollingViewModel.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final VibrationEffect VIBRATE_EFFECT_ERROR =
            VibrationEffect.createWaveform(new long[]{0, 5, 55, 60}, -1);
    private static final VibrationAttributes FINGERPRINT_ENROLLING_SONFICATION_ATTRIBUTES =
            VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ACCESSIBILITY);

    //Enrolling skip
    public static final int FINGERPRINT_ENROLL_ENROLLING_ACTION_SKIP = 0;

    //Icon touch dialog show
    public static final int FINGERPRINT_ENROLL_ENROLLING_ACTION_SHOW_DIALOG = 0;

    //Icon touch dialog dismiss
    public static final int FINGERPRINT_ENROLL_ENROLLING_ACTION_DISMISS_DIALOG = 1;

    private final FingerprintRepository mFingerprintRepository;
    private final AccessibilityRepository mAccessibilityRepository;
    private final VibratorRepository mVibratorRepository;

    private EnrollmentRequest mEnrollmentRequest = null;
    private final MutableLiveData<Integer> mEnrollingLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> mIconTouchDialogLiveData = new MutableLiveData<>();


    public FingerprintEnrollEnrollingViewModel(Application application,
            FingerprintRepository fingerprintRepository,
            AccessibilityRepository accessibilityRepository,
            VibratorRepository vibratorRepository) {
        super(application);
        mFingerprintRepository = fingerprintRepository;
        mAccessibilityRepository = accessibilityRepository;
        mVibratorRepository = vibratorRepository;
    }

    /**
     * User clicks skip button
     */
    public void onSkipButtonClick() {
        final int action = FINGERPRINT_ENROLL_ENROLLING_ACTION_SKIP;
        if (DEBUG) {
            Log.d(TAG, "onSkipButtonClick, post action " + action);
        }
        mEnrollingLiveData.postValue(action);
    }

    /**
     * Icon touch dialog show
     */
    public void onIconTouchDialogShow() {
        final int action = FINGERPRINT_ENROLL_ENROLLING_ACTION_SHOW_DIALOG;
        if (DEBUG) {
            Log.d(TAG, "onIconTouchDialogShow, post action " + action);
        }
        mIconTouchDialogLiveData.postValue(action);
    }

    /**
     * Icon touch dialog dismiss
     */
    public void onIconTouchDialogDismiss() {
        final int action = FINGERPRINT_ENROLL_ENROLLING_ACTION_DISMISS_DIALOG;
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
     * The first sensor type is UDFPS sensor or not
     */
    public boolean canAssumeUdfps() {
        return mFingerprintRepository.canAssumeUdfps();
    }

    /**
     * The first sensor type is SFPS sensor or not
     */
    public boolean canAssumeSfps() {
        return mFingerprintRepository.canAssumeSfps();
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
    public void vibrateError(int uid, String opPkg, String reason) {
        mVibratorRepository.vibrate(uid, opPkg, VIBRATE_EFFECT_ERROR, reason,
                FINGERPRINT_ENROLLING_SONFICATION_ATTRIBUTES);
    }
}
