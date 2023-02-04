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
import android.content.ComponentName;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.android.settings.biometrics2.data.repository.FingerprintRepository;
import com.android.settings.biometrics2.data.repository.PackageManagerRepository;

/**
 * Finish ViewModel handles the state of the fingerprint renroll final stage
 */
public class FingerprintEnrollFinishViewModel extends AndroidViewModel {

    private static final String TAG = FingerprintEnrollFinishViewModel.class.getSimpleName();

    private static final String FINGERPRINT_SUGGESTION_ACTIVITY =
            "com.android.settings.SetupFingerprintSuggestionActivity";

    private static final int ACTION_NONE = -1;
    private static final int ACTION_ADD_BUTTON_CLICK = 0;
    private static final int ACTION_NEXT_BUTTON_CLICK = 1;

    private final FingerprintRepository mFingerprintRepository;
    private final PackageManagerRepository mPackageManagerRepository;
    private final int mUserId;

    private final MutableLiveData<Integer> mActionLiveData = new MutableLiveData<>();

    public FingerprintEnrollFinishViewModel(@NonNull Application application,
            FingerprintRepository fingerprintRepository,
            PackageManagerRepository packageManagerRepository,
            int userId) {
        super(application);
        mFingerprintRepository = fingerprintRepository;
        mPackageManagerRepository = packageManagerRepository;
        mUserId = userId;
        mActionLiveData.setValue(ACTION_NONE);
    }

    /**
     * The first sensor type is Side fps sensor or not
     */
    public boolean canAssumeSfps() {
        return mFingerprintRepository.canAssumeSfps();
    }

    /**
     * Get number of fingerprints that this user enrolled.
     */
    public int getNumOfEnrolledFingerprintsSize() {
        return mFingerprintRepository.getNumOfEnrolledFingerprintsSize(mUserId);
    }

    /**
     * Get max possible number of fingerprints for a user
     */
    public int getMaxFingerprints() {
        return mFingerprintRepository.getMaxFingerprints();
    }

    /**
     * Clear life data
     */
    public void clearLiveData() {
        mActionLiveData.setValue(ACTION_NONE);
    }

    /**
     * Handle add button Click
     */
    public void onAddButtonClick() {
        mActionLiveData.postValue(ACTION_ADD_BUTTON_CLICK);
    }

    /**
     * Handle next button Click
     */
    public void onNextButtonClick() {
        updateFingerprintSuggestionEnableState();
        mActionLiveData.postValue(ACTION_NEXT_BUTTON_CLICK);
    }

    /**
     * Handle back key pressed
     */
    public void onBackKeyPressed() {
        updateFingerprintSuggestionEnableState();
    }

    private void updateFingerprintSuggestionEnableState() {
        final int enrollNum = mFingerprintRepository.getNumOfEnrolledFingerprintsSize(mUserId);
        final int flag = (enrollNum == 1) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;

        ComponentName componentName = new ComponentName(getApplication(),
                FINGERPRINT_SUGGESTION_ACTIVITY);

        mPackageManagerRepository.setComponentEnabledSetting(componentName, flag,
                PackageManager.DONT_KILL_APP);
    }
}
