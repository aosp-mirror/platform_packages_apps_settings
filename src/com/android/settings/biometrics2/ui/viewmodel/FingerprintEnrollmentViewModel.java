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

import static com.android.settings.biometrics.BiometricEnrollBase.EXTRA_FROM_SETTINGS_SUMMARY;
import static com.android.settings.biometrics.BiometricEnrollBase.RESULT_FINISHED;
import static com.android.settings.biometrics.BiometricEnrollBase.RESULT_SKIP;
import static com.android.settings.biometrics.BiometricEnrollBase.RESULT_TIMEOUT;
import static com.android.settings.biometrics.fingerprint.SetupFingerprintEnrollIntroduction.EXTRA_FINGERPRINT_ENROLLED_COUNT;

import android.app.Application;
import android.app.KeyguardManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.result.ActivityResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.android.settings.biometrics.BiometricEnrollBase;
import com.android.settings.biometrics2.data.repository.FingerprintRepository;
import com.android.settings.biometrics2.ui.model.EnrollmentRequest;
import com.android.settings.password.SetupSkipDialog;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Fingerprint enrollment view model implementation
 */
public class FingerprintEnrollmentViewModel extends AndroidViewModel implements
        DefaultLifecycleObserver {

    private static final String TAG = "FingerprintEnrollmentViewModel";

    @VisibleForTesting
    static final String SAVED_STATE_IS_WAITING_ACTIVITY_RESULT = "is_waiting_activity_result";

    @NonNull private final FingerprintRepository mFingerprintRepository;
    @Nullable private final KeyguardManager mKeyguardManager;

    private final AtomicBoolean mIsWaitingActivityResult = new AtomicBoolean(false);
    private final MutableLiveData<ActivityResult> mSetResultLiveData = new MutableLiveData<>();

    /**
     * Even this variable may be nullable, but activity will call setIntent() immediately during
     * its onCreate(), we do not assign @Nullable for this variable here.
     */
    private EnrollmentRequest mRequest = null;

    public FingerprintEnrollmentViewModel(
            @NonNull Application application,
            @NonNull FingerprintRepository fingerprintRepository,
            @Nullable KeyguardManager keyguardManager) {
        super(application);
        mFingerprintRepository = fingerprintRepository;
        mKeyguardManager = keyguardManager;
    }

    /**
     * Set EnrollmentRequest
     */
    public void setRequest(@NonNull EnrollmentRequest request) {
        mRequest = request;
    }

    /**
     * Get EnrollmentRequest
     */
    public EnrollmentRequest getRequest() {
        return mRequest;
    }

    /**
     * Copy necessary extra data from activity intent
     */
    @NonNull
    public Bundle getNextActivityBaseIntentExtras() {
        final Bundle bundle = mRequest.getSuwExtras();
        bundle.putBoolean(EXTRA_FROM_SETTINGS_SUMMARY, mRequest.isFromSettingsSummery());
        return bundle;
    }

    /**
     * Handle activity result from FingerprintFindSensor
     */
    public void onContinueEnrollActivityResult(@NonNull ActivityResult result, int userId) {
        if (!mIsWaitingActivityResult.compareAndSet(true, false)) {
            Log.w(TAG, "fail to reset isWaiting flag for enrollment");
        }
        if (result.getResultCode() == RESULT_FINISHED
                || result.getResultCode() == RESULT_TIMEOUT) {
            Intent data = result.getData();
            if (mRequest.isSuw() && isKeyguardSecure()
                    && result.getResultCode() == RESULT_FINISHED) {
                if (data == null) {
                    data = new Intent();
                }
                data.putExtras(getSuwFingerprintCountExtra(userId));
            }
            mSetResultLiveData.postValue(new ActivityResult(result.getResultCode(), data));
        } else if (result.getResultCode() == RESULT_SKIP
                || result.getResultCode() == SetupSkipDialog.RESULT_SKIP) {
            mSetResultLiveData.postValue(result);
        }
    }



    private boolean isKeyguardSecure() {
        return mKeyguardManager != null && mKeyguardManager.isKeyguardSecure();
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

        mSetResultLiveData.postValue(new ActivityResult(BiometricEnrollBase.RESULT_TIMEOUT, null));
    }

    @NonNull
    private Bundle getSuwFingerprintCountExtra(int userId) {
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
    }

    /**
     * Handle onSaveInstanceState from activity
     */
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(SAVED_STATE_IS_WAITING_ACTIVITY_RESULT, mIsWaitingActivityResult.get());
    }
}
