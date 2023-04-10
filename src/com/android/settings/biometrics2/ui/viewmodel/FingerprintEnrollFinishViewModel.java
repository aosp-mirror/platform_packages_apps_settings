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
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.android.settings.biometrics2.data.repository.FingerprintRepository;
import com.android.settings.biometrics2.ui.model.EnrollmentRequest;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Finish ViewModel handles the state of the fingerprint renroll final stage
 */
public class FingerprintEnrollFinishViewModel extends AndroidViewModel {

    private static final String TAG = FingerprintEnrollFinishViewModel.class.getSimpleName();
    private static final boolean DEBUG = false;

    /**
     * User clicks "Add" button
     */
    public static final int FINGERPRINT_ENROLL_FINISH_ACTION_ADD_BUTTON_CLICK = 0;

    /**
     * User clicks "Next" button
     */
    public static final int FINGERPRINT_ENROLL_FINISH_ACTION_NEXT_BUTTON_CLICK = 1;

    @IntDef(prefix = { "FINGERPRINT_ENROLL_FINISH_ACTION_" }, value = {
            FINGERPRINT_ENROLL_FINISH_ACTION_ADD_BUTTON_CLICK,
            FINGERPRINT_ENROLL_FINISH_ACTION_NEXT_BUTTON_CLICK
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface FingerprintEnrollFinishAction {}

    @NonNull private final FingerprintRepository mFingerprintRepository;
    @NonNull private final EnrollmentRequest mRequest;
    private final int mUserId;

    private final MutableLiveData<Integer> mActionLiveData = new MutableLiveData<>();

    public FingerprintEnrollFinishViewModel(@NonNull Application application, int userId,
            @NonNull EnrollmentRequest request,
            @NonNull FingerprintRepository fingerprintRepository) {
        super(application);
        mUserId = userId;
        mRequest = request;
        mFingerprintRepository = fingerprintRepository;
    }

    @NonNull
    public EnrollmentRequest getRequest() {
        return mRequest;
    }

    /**
     * The first sensor type is Side fps sensor or not
     */
    public boolean canAssumeSfps() {
        return mFingerprintRepository.canAssumeSfps();
    }

    /**
     * Device allows user to enroll another fingerprint or not.
     */
    public boolean isAnotherFingerprintEnrollable() {
        return mFingerprintRepository.getNumOfEnrolledFingerprintsSize(mUserId)
                < mFingerprintRepository.getMaxFingerprints();
    }

    /**
     * Clear action LiveData
     */
    public void clearActionLiveData() {
        mActionLiveData.setValue(null);
    }

    /**
     * Get action LiveData
     */
    public LiveData<Integer> getActionLiveData() {
        return mActionLiveData;
    }

    /**
     * Handle add button Click
     */
    public void onAddButtonClick() {
        final int action = FINGERPRINT_ENROLL_FINISH_ACTION_ADD_BUTTON_CLICK;
        if (DEBUG) {
            Log.d(TAG, "onAddButtonClick post(" + action + ")");
        }
        mActionLiveData.postValue(action);
    }

    /**
     * Handle next button Click
     */
    public void onNextButtonClick() {
        final int action = FINGERPRINT_ENROLL_FINISH_ACTION_NEXT_BUTTON_CLICK;
        if (DEBUG) {
            Log.d(TAG, "onNextButtonClick post(" + action + ")");
        }
        mActionLiveData.postValue(action);
    }
}
