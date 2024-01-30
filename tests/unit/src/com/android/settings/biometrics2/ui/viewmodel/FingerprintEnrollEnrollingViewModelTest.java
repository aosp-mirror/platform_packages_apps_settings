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

import static android.hardware.fingerprint.FingerprintSensorProperties.TYPE_UDFPS_OPTICAL;

import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollEnrollingViewModel.FINGERPRINT_ENROLL_ENROLLING_ACTION_SHOW_ICON_TOUCH_DIALOG;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollEnrollingViewModel.FINGERPRINT_ENROLL_ENROLLING_CANCELED_BECAUSE_BACK_PRESSED;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollEnrollingViewModel.FINGERPRINT_ENROLL_ENROLLING_CANCELED_BECAUSE_USER_SKIP;
import static com.android.settings.biometrics2.utils.FingerprintRepositoryUtils.newFingerprintRepository;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

import android.app.Application;
import android.hardware.biometrics.SensorProperties;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;
import android.hardware.fingerprint.IFingerprintAuthenticatorsRegisteredCallback;

import androidx.lifecycle.LiveData;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.biometrics2.data.repository.FingerprintRepository;
import com.android.settings.testutils.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;

@RunWith(AndroidJUnit4.class)
public class FingerprintEnrollEnrollingViewModelTest {

    private static final int TEST_USER_ID = 33;

    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();
    @Rule
    public final InstantTaskExecutorRule mTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private FingerprintManager mFingerprintManager;

    private Application mApplication;
    private FingerprintEnrollEnrollingViewModel mViewModel;

    @Before
    public void setUp() {
        mApplication = ApplicationProvider.getApplicationContext();
        mViewModel = new FingerprintEnrollEnrollingViewModel(
                mApplication,
                TEST_USER_ID,
                newFingerprintRepository(mFingerprintManager, TYPE_UDFPS_OPTICAL,  5)
            );
    }

    @Test
    public void testIconTouchDialog() {
        final LiveData<Integer> actionLiveData = mViewModel.getActionLiveData();
        assertThat(actionLiveData.getValue()).isEqualTo(null);

        mViewModel.showIconTouchDialog();
        assertThat(actionLiveData.getValue()).isEqualTo(
                FINGERPRINT_ENROLL_ENROLLING_ACTION_SHOW_ICON_TOUCH_DIALOG);
    }

    @Test
    public void tesBackPressedScenario() {
        final LiveData<Integer> actionLiveData = mViewModel.getActionLiveData();
        assertThat(actionLiveData.getValue()).isEqualTo(null);
        assertThat(mViewModel.getOnBackPressed()).isEqualTo(false);

        mViewModel.setOnBackPressed();
        assertThat(mViewModel.getOnBackPressed()).isEqualTo(true);

        mViewModel.onCancelledDueToOnBackPressed();
        assertThat(mViewModel.getOnBackPressed()).isEqualTo(false);
        assertThat(actionLiveData.getValue()).isEqualTo(
                FINGERPRINT_ENROLL_ENROLLING_CANCELED_BECAUSE_BACK_PRESSED);
    }

    @Test
    public void testSkipPressedScenario() {
        final LiveData<Integer> actionLiveData = mViewModel.getActionLiveData();
        assertThat(actionLiveData.getValue()).isEqualTo(null);
        assertThat(mViewModel.getOnSkipPressed()).isEqualTo(false);

        mViewModel.setOnSkipPressed();
        assertThat(mViewModel.getOnSkipPressed()).isEqualTo(true);

        mViewModel.onCancelledDueToOnSkipPressed();
        assertThat(mViewModel.getOnSkipPressed()).isEqualTo(false);
        assertThat(actionLiveData.getValue()).isEqualTo(
                FINGERPRINT_ENROLL_ENROLLING_CANCELED_BECAUSE_USER_SKIP);
    }

    @Test
    public void testGetFirstFingerprintSensorPropertiesInternal() {
        final ArrayList<FingerprintSensorPropertiesInternal> props = new ArrayList<>();
        final FingerprintSensorPropertiesInternal prop = new FingerprintSensorPropertiesInternal(
                0 /* sensorId */,
                SensorProperties.STRENGTH_STRONG,
                5,
                new ArrayList<>() /* componentInfo */,
                TYPE_UDFPS_OPTICAL,
                true /* resetLockoutRequiresHardwareAuthToken */);
        props.add(prop);
        doAnswer(invocation -> {
            final IFingerprintAuthenticatorsRegisteredCallback callback =
                    invocation.getArgument(0);
            callback.onAllAuthenticatorsRegistered(props);
            return null;
        }).when(mFingerprintManager).addAuthenticatorsRegisteredCallback(any());

        mViewModel = new FingerprintEnrollEnrollingViewModel(
                mApplication,
                TEST_USER_ID,
                new FingerprintRepository(mFingerprintManager)
        );

        assertThat(mViewModel.getFirstFingerprintSensorPropertiesInternal()).isEqualTo(prop);
    }

    @Test
    public void testGetEnrollStageCount() {
        final int expectedValue = 24;
        doReturn(expectedValue).when(mFingerprintManager).getEnrollStageCount();

        assertThat(mViewModel.getEnrollStageCount()).isEqualTo(expectedValue);
    }

    @Test
    public void testGetEnrollStageThreshold() {
        final float expectedValue0 = 0.42f;
        final float expectedValue1 = 0.24f;
        final float expectedValue2 = 0.33f;
        final float expectedValue3 = 0.90f;

        doReturn(expectedValue0).when(mFingerprintManager).getEnrollStageThreshold(0);
        doReturn(expectedValue1).when(mFingerprintManager).getEnrollStageThreshold(1);
        doReturn(expectedValue2).when(mFingerprintManager).getEnrollStageThreshold(2);
        doReturn(expectedValue3).when(mFingerprintManager).getEnrollStageThreshold(3);

        assertThat(mViewModel.getEnrollStageThreshold(2)).isEqualTo(expectedValue2);
        assertThat(mViewModel.getEnrollStageThreshold(1)).isEqualTo(expectedValue1);
        assertThat(mViewModel.getEnrollStageThreshold(3)).isEqualTo(expectedValue3);
        assertThat(mViewModel.getEnrollStageThreshold(0)).isEqualTo(expectedValue0);
    }
}
