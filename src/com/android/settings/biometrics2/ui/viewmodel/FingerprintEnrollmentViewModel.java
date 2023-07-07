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

import static com.android.settings.biometrics.fingerprint.FingerprintEnrollFinish.FINGERPRINT_SUGGESTION_ACTIVITY;
import static com.android.settings.biometrics.fingerprint.SetupFingerprintEnrollIntroduction.EXTRA_FINGERPRINT_ENROLLED_COUNT;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.result.ActivityResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.android.settings.biometrics.BiometricEnrollBase;
import com.android.settings.biometrics2.data.repository.FingerprintRepository;
import com.android.settings.biometrics2.ui.model.EnrollmentRequest;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Fingerprint enrollment view model implementation
 */
public class FingerprintEnrollmentViewModel extends AndroidViewModel {

    private static final String TAG = "FingerprintEnrollmentViewModel";

    @VisibleForTesting
    static final String SAVED_STATE_IS_WAITING_ACTIVITY_RESULT = "is_waiting_activity_result";

    @VisibleForTesting
    static final String SAVED_STATE_IS_NEW_FINGERPRINT_ADDED = "is_new_fingerprint_added";

    @NonNull private final FingerprintRepository mFingerprintRepository;

    private final AtomicBoolean mIsWaitingActivityResult = new AtomicBoolean(false);
    private final MutableLiveData<ActivityResult> mSetResultLiveData = new MutableLiveData<>();
    @NonNull private final EnrollmentRequest mRequest;
    private boolean mIsNewFingerprintAdded = false;

    public FingerprintEnrollmentViewModel(
            @NonNull Application application,
            @NonNull FingerprintRepository fingerprintRepository,
            @NonNull EnrollmentRequest request) {
        super(application);
        mFingerprintRepository = fingerprintRepository;
        mRequest = request;
    }

    /**
     * Get EnrollmentRequest
     */
    @NonNull
    public EnrollmentRequest getRequest() {
        return mRequest;
    }

    /**
     * Get override activity result as current ViewModel status.
     *
     * FingerprintEnrollmentActivity supports user enrolls 2nd fingerprint or starts a new flow
     * through Deferred-SUW, Portal-SUW, or SUW Suggestion. Use a method to get override activity
     * result instead of putting these if-else on every setResult(), .
     */
    @NonNull
    public ActivityResult getOverrideActivityResult(@NonNull ActivityResult result,
            @Nullable Bundle generatingChallengeExtras) {
        // TODO write tests
        final int newResultCode = mIsNewFingerprintAdded
                ? BiometricEnrollBase.RESULT_FINISHED
                : (mRequest.isAfterSuwOrSuwSuggestedAction()
                        ? BiometricEnrollBase.RESULT_CANCELED
                        : result.getResultCode());

        Intent newData = result.getData();
        if (newResultCode == BiometricEnrollBase.RESULT_FINISHED
                && generatingChallengeExtras != null) {
            if (newData == null) {
                newData = new Intent();
            }
            newData.putExtras(generatingChallengeExtras);
        }
        return new ActivityResult(newResultCode, newData);
    }

    /**
     * Activity calls this method during onPause() to finish itself when back to background.
     *
     * @param isActivityFinishing Activity has called finish() or not
     * @param isChangingConfigurations Activity is finished because of configuration changed or not.
     */
    public void checkFinishActivityDuringOnPause(boolean isActivityFinishing,
            boolean isChangingConfigurations) {
        if (isChangingConfigurations || isActivityFinishing || mRequest.isSuw()
                || isWaitingActivityResult().get()) {
            return;
        }

        mSetResultLiveData.postValue(
                new ActivityResult(BiometricEnrollBase.RESULT_TIMEOUT, null));
    }

    /**
     * Get Suw fingerprint count extra for statistics
     */
    @NonNull
    public Bundle getSuwFingerprintCountExtra(int userId) {
        final Bundle bundle = new Bundle();
        bundle.putInt(EXTRA_FINGERPRINT_ENROLLED_COUNT,
                mFingerprintRepository.getNumOfEnrolledFingerprintsSize(userId));
        return bundle;
    }

    @NonNull
    public LiveData<ActivityResult> getSetResultLiveData() {
        return mSetResultLiveData;
    }

    @NonNull
    public AtomicBoolean isWaitingActivityResult() {
        return mIsWaitingActivityResult;
    }

    /**
     * Handle savedInstanceState from activity onCreated()
     */
    public void setSavedInstanceState(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        mIsWaitingActivityResult.set(
                savedInstanceState.getBoolean(SAVED_STATE_IS_WAITING_ACTIVITY_RESULT, false)
        );
        mIsNewFingerprintAdded = savedInstanceState.getBoolean(
                SAVED_STATE_IS_NEW_FINGERPRINT_ADDED, false);
    }

    /**
     * Handle onSaveInstanceState from activity
     */
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(SAVED_STATE_IS_WAITING_ACTIVITY_RESULT, mIsWaitingActivityResult.get());
        outState.putBoolean(SAVED_STATE_IS_NEW_FINGERPRINT_ADDED, mIsNewFingerprintAdded);
    }

    /**
     * The first sensor type is UDFPS sensor or not
     */
    public boolean canAssumeUdfps() {
        return mFingerprintRepository.canAssumeUdfps();
    }

    /**
     * The first sensor type is side fps sensor or not
     */
    public boolean canAssumeSfps() {
        return mFingerprintRepository.canAssumeSfps();
    }

    /**
     * Sets mIsNewFingerprintAdded to true
     */
    public void setIsNewFingerprintAdded() {
        mIsNewFingerprintAdded = true;
    }

    /**
     * Update FINGERPRINT_SUGGESTION_ACTIVITY into package manager
     */
    public void updateFingerprintSuggestionEnableState(int userId) {
        final int enrolled = mFingerprintRepository.getNumOfEnrolledFingerprintsSize(userId);

        // Only show "Add another fingerprint" if the user already enrolled one.
        // "Add fingerprint" will be shown in the main flow if the user hasn't enrolled any
        // fingerprints. If the user already added more than one fingerprint, they already know
        // to add multiple fingerprints so we don't show the suggestion.
        final int flag = (enrolled == 1) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;

        ComponentName componentName = new ComponentName(getApplication(),
                FINGERPRINT_SUGGESTION_ACTIVITY);
        getApplication().getPackageManager().setComponentEnabledSetting(componentName, flag,
                PackageManager.DONT_KILL_APP);
        Log.d(TAG, FINGERPRINT_SUGGESTION_ACTIVITY + " enabled state = " + (enrolled == 1));
    }
}
