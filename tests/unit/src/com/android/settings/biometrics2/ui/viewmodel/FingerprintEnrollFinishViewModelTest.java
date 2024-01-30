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

import static android.hardware.fingerprint.FingerprintSensorProperties.TYPE_POWER_BUTTON;
import static android.hardware.fingerprint.FingerprintSensorProperties.TYPE_REAR;
import static android.hardware.fingerprint.FingerprintSensorProperties.TYPE_UDFPS_OPTICAL;

import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollFinishViewModel.FINGERPRINT_ENROLL_FINISH_ACTION_ADD_BUTTON_CLICK;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollFinishViewModel.FINGERPRINT_ENROLL_FINISH_ACTION_NEXT_BUTTON_CLICK;
import static com.android.settings.biometrics2.utils.FingerprintRepositoryUtils.newFingerprintRepository;
import static com.android.settings.biometrics2.utils.FingerprintRepositoryUtils.setupFingerprintEnrolledFingerprints;

import static com.google.common.truth.Truth.assertThat;

import android.app.Application;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;

import androidx.lifecycle.LiveData;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.biometrics2.ui.model.EnrollmentRequest;
import com.android.settings.testutils.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class FingerprintEnrollFinishViewModelTest {

    private static final int USER_ID = 334;
    private static final int MAX_ENROLLABLE = 5;

    @Rule public final MockitoRule mockito = MockitoJUnit.rule();
    @Rule public final InstantTaskExecutorRule mTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock private FingerprintManager mFingerprintManager;

    private Application mApplication;
    private EnrollmentRequest mRequest;
    private FingerprintEnrollFinishViewModel mViewModel;

    @Before
    public void setUp() {
        mApplication = ApplicationProvider.getApplicationContext();
        mRequest = new EnrollmentRequest(new Intent(), mApplication, true);
        mViewModel = new FingerprintEnrollFinishViewModel(mApplication, USER_ID, mRequest,
                newFingerprintRepository(mFingerprintManager, TYPE_UDFPS_OPTICAL, MAX_ENROLLABLE));
    }

    @Test
    public void testCanAssumeSfps() {
        mViewModel = new FingerprintEnrollFinishViewModel(mApplication, USER_ID, mRequest,
                newFingerprintRepository(mFingerprintManager, TYPE_UDFPS_OPTICAL, MAX_ENROLLABLE));
        assertThat(mViewModel.canAssumeSfps()).isFalse();

        mViewModel = new FingerprintEnrollFinishViewModel(mApplication, USER_ID, mRequest,
                newFingerprintRepository(mFingerprintManager, TYPE_REAR, MAX_ENROLLABLE));
        assertThat(mViewModel.canAssumeSfps()).isFalse();

        mViewModel = new FingerprintEnrollFinishViewModel(mApplication, USER_ID, mRequest,
                newFingerprintRepository(mFingerprintManager, TYPE_POWER_BUTTON, MAX_ENROLLABLE));
        assertThat(mViewModel.canAssumeSfps()).isTrue();
    }

    @Test
    public void testIsAnotherFingerprintEnrollable() {
        setupFingerprintEnrolledFingerprints(mFingerprintManager, USER_ID, MAX_ENROLLABLE);
        assertThat(mViewModel.isAnotherFingerprintEnrollable()).isFalse();

        setupFingerprintEnrolledFingerprints(mFingerprintManager, USER_ID, MAX_ENROLLABLE - 1);
        assertThat(mViewModel.isAnotherFingerprintEnrollable()).isTrue();
    }

    @Test
    public void testGetRequest() {
        assertThat(mViewModel.getRequest()).isEqualTo(mRequest);
    }

    @Test
    public void testOnAddButtonClick() {
        final LiveData<Integer> actionLiveData = mViewModel.getActionLiveData();

        // Test init value
        assertThat(actionLiveData.getValue()).isNull();

        // Test onAddButtonClick()
        mViewModel.onAddButtonClick();
        assertThat(actionLiveData.getValue()).isEqualTo(
                FINGERPRINT_ENROLL_FINISH_ACTION_ADD_BUTTON_CLICK);

        // Clear
        mViewModel.clearActionLiveData();
        assertThat(actionLiveData.getValue()).isNull();
    }

    @Test
    public void testOnNextButtonClick() {
        final LiveData<Integer> actionLiveData = mViewModel.getActionLiveData();

        // Test init value
        assertThat(actionLiveData.getValue()).isNull();

        // Test onNextButtonClick()
        mViewModel.onNextButtonClick();
        assertThat(actionLiveData.getValue()).isEqualTo(
                FINGERPRINT_ENROLL_FINISH_ACTION_NEXT_BUTTON_CLICK);

        // Clear
        mViewModel.clearActionLiveData();
        assertThat(actionLiveData.getValue()).isNull();
    }
}
