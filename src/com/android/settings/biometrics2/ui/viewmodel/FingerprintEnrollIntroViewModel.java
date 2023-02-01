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

import static com.android.settings.biometrics2.ui.model.FingerprintEnrollIntroStatus.FINGERPRINT_ENROLLABLE_ERROR_REACH_MAX;
import static com.android.settings.biometrics2.ui.model.FingerprintEnrollIntroStatus.FINGERPRINT_ENROLLABLE_OK;
import static com.android.settings.biometrics2.ui.model.FingerprintEnrollIntroStatus.FINGERPRINT_ENROLLABLE_UNKNOWN;

import android.annotation.IntDef;
import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.android.settings.biometrics2.data.repository.FingerprintRepository;
import com.android.settings.biometrics2.ui.model.EnrollmentRequest;
import com.android.settings.biometrics2.ui.model.FingerprintEnrollIntroStatus;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Fingerprint intro onboarding page view model implementation
 */
public class FingerprintEnrollIntroViewModel extends AndroidViewModel {

    private static final String TAG = "FingerprintEnrollIntroViewModel";
    private static final boolean HAS_SCROLLED_TO_BOTTOM_DEFAULT = false;
    private static final int ENROLLABLE_STATUS_DEFAULT = FINGERPRINT_ENROLLABLE_UNKNOWN;

    /**
     * User clicks 'Done' button on this page
     */
    public static final int FINGERPRINT_ENROLL_INTRO_ACTION_DONE_AND_FINISH = 0;

    /**
     * User clicks 'Agree' button on this page
     */
    public static final int FINGERPRINT_ENROLL_INTRO_ACTION_CONTINUE_ENROLL = 1;

    /**
     * User clicks 'Skip' button on this page
     */
    public static final int FINGERPRINT_ENROLL_INTRO_ACTION_SKIP_OR_CANCEL = 2;

    @IntDef(prefix = { "FINGERPRINT_ENROLL_INTRO_ACTION_" }, value = {
            FINGERPRINT_ENROLL_INTRO_ACTION_DONE_AND_FINISH,
            FINGERPRINT_ENROLL_INTRO_ACTION_CONTINUE_ENROLL,
            FINGERPRINT_ENROLL_INTRO_ACTION_SKIP_OR_CANCEL
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface FingerprintEnrollIntroAction {}

    @NonNull private final FingerprintRepository mFingerprintRepository;

    private final MutableLiveData<Boolean> mHasScrolledToBottomLiveData =
            new MutableLiveData<>(HAS_SCROLLED_TO_BOTTOM_DEFAULT);
    private final MutableLiveData<Integer> mEnrollableStatusLiveData =
            new MutableLiveData<>(ENROLLABLE_STATUS_DEFAULT);
    private final MediatorLiveData<FingerprintEnrollIntroStatus> mPageStatusLiveData =
            new MediatorLiveData<>();
    private final MutableLiveData<Integer> mActionLiveData = new MutableLiveData<>();
    private final int mUserId;
    @NonNull private final EnrollmentRequest mRequest;

    public FingerprintEnrollIntroViewModel(@NonNull Application application,
            @NonNull FingerprintRepository fingerprintRepository,
            @NonNull EnrollmentRequest request, int userId) {
        super(application);
        mFingerprintRepository = fingerprintRepository;
        mRequest = request;
        mUserId = userId;

        mPageStatusLiveData.addSource(
                mEnrollableStatusLiveData,
                enrollable -> {
                    final Boolean toBottomValue = mHasScrolledToBottomLiveData.getValue();
                    final FingerprintEnrollIntroStatus status = new FingerprintEnrollIntroStatus(
                            toBottomValue != null ? toBottomValue : HAS_SCROLLED_TO_BOTTOM_DEFAULT,
                            enrollable);
                    mPageStatusLiveData.setValue(status);
                });
        mPageStatusLiveData.addSource(
                mHasScrolledToBottomLiveData,
                hasScrolledToBottom -> {
                    final Integer enrollableValue = mEnrollableStatusLiveData.getValue();
                    final FingerprintEnrollIntroStatus status = new FingerprintEnrollIntroStatus(
                            hasScrolledToBottom,
                            enrollableValue != null ? enrollableValue : ENROLLABLE_STATUS_DEFAULT);
                    mPageStatusLiveData.setValue(status);
                });

        updateEnrollableStatus();
    }

    /**
     * Get enrollment request
     */
    public EnrollmentRequest getRequest() {
        return mRequest;
    }

    private void updateEnrollableStatus() {
        final int num = mFingerprintRepository.getNumOfEnrolledFingerprintsSize(mUserId);
        final int max =
                mRequest.isSuw() && !mRequest.isAfterSuwOrSuwSuggestedAction()
                ? mFingerprintRepository.getMaxFingerprintsInSuw(getApplication().getResources())
                : mFingerprintRepository.getMaxFingerprints();
        mEnrollableStatusLiveData.postValue(num >= max
                ? FINGERPRINT_ENROLLABLE_ERROR_REACH_MAX
                : FINGERPRINT_ENROLLABLE_OK);
    }

    /**
     * Get enrollable status and hasScrollToBottom live data
     */
    public LiveData<FingerprintEnrollIntroStatus> getPageStatusLiveData() {
        return mPageStatusLiveData;
    }

    /**
     * Clear user's action live data
     */
    public void clearActionLiveData() {
        mActionLiveData.setValue(null);
    }

    /**
     * Get user's action live data (like clicking Agree, Skip, or Done)
     */
    public LiveData<Integer> getActionLiveData() {
        return mActionLiveData;
    }

    /**
     * The first sensor type is UDFPS sensor or not
     */
    public boolean canAssumeUdfps() {
        return mFingerprintRepository.canAssumeUdfps();
    }

    /**
     * Update onboarding intro page has scrolled to bottom
     */
    public void setHasScrolledToBottom(boolean value) {
        mHasScrolledToBottomLiveData.postValue(value);
    }

    /**
     * Get parental consent required or not during enrollment process
     */
    public boolean isParentalConsentRequired() {
        return mFingerprintRepository.isParentalConsentRequired(getApplication());
    }

    /**
     * Get fingerprint is disable by admin or not
     */
    public boolean isBiometricUnlockDisabledByAdmin() {
        return mFingerprintRepository.isDisabledByAdmin(getApplication(), mUserId);
    }

    /**
     * User clicks next button
     */
    public void onNextButtonClick() {
        final Integer status = mEnrollableStatusLiveData.getValue();
        switch (status != null ? status : ENROLLABLE_STATUS_DEFAULT) {
            case FINGERPRINT_ENROLLABLE_ERROR_REACH_MAX:
                mActionLiveData.postValue(FINGERPRINT_ENROLL_INTRO_ACTION_DONE_AND_FINISH);
                break;
            case FINGERPRINT_ENROLLABLE_OK:
                mActionLiveData.postValue(FINGERPRINT_ENROLL_INTRO_ACTION_CONTINUE_ENROLL);
                break;
            default:
                Log.w(TAG, "fail to click next, enrolled:" + status);
        }
    }

    /**
     * User clicks skip/cancel button
     */
    public void onSkipOrCancelButtonClick() {
        mActionLiveData.postValue(FINGERPRINT_ENROLL_INTRO_ACTION_SKIP_OR_CANCEL);
    }
}
