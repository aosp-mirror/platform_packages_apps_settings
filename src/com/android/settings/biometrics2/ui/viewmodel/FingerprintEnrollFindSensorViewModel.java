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

import android.annotation.IntDef;
import android.app.Application;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * ViewModel explaining the fingerprint sensor location for fingerprint enrollment.
 */
public class FingerprintEnrollFindSensorViewModel extends AndroidViewModel {

    private static final boolean DEBUG = false;
    private static final String TAG = "FingerprintEnrollFindSensorViewModel";

    /**
     * User clicks 'Skip' button on this page in Settings
     */
    public static final int FINGERPRINT_ENROLL_FIND_SENSOR_ACTION_SKIP = 0;

    /**
     * User clicks 'Skip' button on this page in SetupWizard flow
     */
    public static final int FINGERPRINT_ENROLL_FIND_SENSOR_ACTION_DIALOG = 1;

    /**
     * User clicks 'Start' button on this page
     */
    public static final int FINGERPRINT_ENROLL_FIND_SENSOR_ACTION_START = 2;

    @IntDef(prefix = { "FINGERPRINT_ENROLL_FIND_SENSOR_ACTION_" }, value = {
            FINGERPRINT_ENROLL_FIND_SENSOR_ACTION_SKIP,
            FINGERPRINT_ENROLL_FIND_SENSOR_ACTION_DIALOG,
            FINGERPRINT_ENROLL_FIND_SENSOR_ACTION_START
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface FingerprintEnrollFindSensorAction {}

    private final AccessibilityManager mAccessibilityManager;

    private final boolean mIsSuw;
    @NonNull private final MutableLiveData<Integer> mActionLiveData = new MutableLiveData<>();

    public FingerprintEnrollFindSensorViewModel(@NonNull Application application, boolean isSuw) {
        super(application);
        mAccessibilityManager = application.getSystemService(AccessibilityManager.class);
        mIsSuw = isSuw;
    }

    /**
     * Returns action live data that user chooses
     */
    public LiveData<Integer> getActionLiveData() {
        return mActionLiveData;
    }

    /**
     * Clear ActionLiveData to prevent get obsolete data
     */
    public void clearActionLiveData() {
        mActionLiveData.setValue(null);
    }

    /**
     * User clicks skip button on dialog
     */
    public void onSkipDialogButtonClick() {
        final int action = FINGERPRINT_ENROLL_FIND_SENSOR_ACTION_SKIP;
        if (DEBUG) {
            Log.d(TAG, "onSkipDialogButtonClick, post " + action);
        }
        mActionLiveData.postValue(action);
    }

    /**
     * User clicks skip button
     */
    public void onSkipButtonClick() {
        final int action = mIsSuw
                ? FINGERPRINT_ENROLL_FIND_SENSOR_ACTION_DIALOG
                : FINGERPRINT_ENROLL_FIND_SENSOR_ACTION_SKIP;
        if (DEBUG) {
            Log.d(TAG, "onSkipButtonClick, post action " + action);
        }
        mActionLiveData.postValue(action);
    }

    /**
     * User clicks start button
     */
    public void onStartButtonClick() {
        final int action = FINGERPRINT_ENROLL_FIND_SENSOR_ACTION_START;
        if (DEBUG) {
            Log.d(TAG, "onStartButtonClick, post action " + action);
        }
        mActionLiveData.postValue(action);
    }

    /**
     * Returns the info about accessibility is enabled or not
     */
    public boolean isAccessibilityEnabled() {
        return mAccessibilityManager.isEnabled();
    }
}
