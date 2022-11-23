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

import static android.hardware.fingerprint.FingerprintSensorProperties.TYPE_REAR;
import static android.hardware.fingerprint.FingerprintSensorProperties.TYPE_UDFPS_OPTICAL;
import static android.hardware.fingerprint.FingerprintSensorProperties.TYPE_UDFPS_ULTRASONIC;

import static com.android.settings.biometrics2.data.repository.FingerprintRepositoryTest.setupSuwMaxFingerprintsEnrollable;
import static com.android.settings.biometrics2.ui.model.FingerprintEnrollIntroStatus.FINGERPRINT_ENROLLABLE_ERROR_REACH_MAX;
import static com.android.settings.biometrics2.ui.model.FingerprintEnrollIntroStatus.FINGERPRINT_ENROLLABLE_OK;
import static com.android.settings.biometrics2.ui.model.FingerprintEnrollIntroStatus.FINGERPRINT_ENROLLABLE_UNKNOWN;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollIntroViewModel.FINGERPRINT_ENROLL_INTRO_ACTION_CONTINUE_ENROLL;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollIntroViewModel.FINGERPRINT_ENROLL_INTRO_ACTION_DONE_AND_FINISH;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollIntroViewModel.FINGERPRINT_ENROLL_INTRO_ACTION_SKIP_OR_CANCEL;
import static com.android.settings.biometrics2.util.EnrollmentRequestUtil.newAllFalseRequest;
import static com.android.settings.biometrics2.util.EnrollmentRequestUtil.newIsSuwDeferredRequest;
import static com.android.settings.biometrics2.util.EnrollmentRequestUtil.newIsSuwPortalRequest;
import static com.android.settings.biometrics2.util.EnrollmentRequestUtil.newIsSuwRequest;
import static com.android.settings.biometrics2.util.EnrollmentRequestUtil.newIsSuwSuggestedActionFlowRequest;
import static com.android.settings.biometrics2.util.FingerprintManagerUtil.setupFingerprintEnrolledFingerprints;
import static com.android.settings.biometrics2.util.FingerprintManagerUtil.setupFingerprintFirstSensor;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.res.Resources;
import android.hardware.fingerprint.FingerprintManager;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.biometrics2.data.repository.FingerprintRepository;
import com.android.settings.biometrics2.ui.model.EnrollmentRequest;
import com.android.settings.biometrics2.ui.model.FingerprintEnrollIntroStatus;
import com.android.settings.testutils.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class FingerprintEnrollIntroViewModelTest {

    @Rule public final MockitoRule mockito = MockitoJUnit.rule();
    @Rule public final InstantTaskExecutorRule mTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock private Resources mResources;
    @Mock private LifecycleOwner mLifecycleOwner;
    @Mock private FingerprintManager mFingerprintManager;

    private Application mApplication;
    private FingerprintRepository mFingerprintRepository;
    private FingerprintEnrollIntroViewModel mViewModel;

    @Before
    public void setUp() {
        mApplication = ApplicationProvider.getApplicationContext();
        mFingerprintRepository = new FingerprintRepository(mFingerprintManager);
        mViewModel = new FingerprintEnrollIntroViewModel(mApplication, mFingerprintRepository);
        // MediatorLiveData won't update itself unless observed
        mViewModel.getPageStatusLiveData().observeForever(event -> {});
    }

    @Test
    public void testPageStatusLiveDataDefaultValue() {
        final FingerprintEnrollIntroStatus status = mViewModel.getPageStatusLiveData().getValue();
        assertThat(status.hasScrollToBottom()).isFalse();
        assertThat(status.getEnrollableStatus()).isEqualTo(FINGERPRINT_ENROLLABLE_UNKNOWN);
    }

    @Test
    public void testClearActionLiveData() {
        final MutableLiveData<Integer> actionLiveData =
                (MutableLiveData<Integer>) mViewModel.getActionLiveData();
        actionLiveData.postValue(1);
        assertThat(actionLiveData.getValue()).isEqualTo(1);

        mViewModel.clearActionLiveData();

        assertThat(actionLiveData.getValue()).isNull();
    }

    @Test
    public void testGetEnrollmentRequest() {
        final EnrollmentRequest request = newAllFalseRequest(mApplication);

        mViewModel.setEnrollmentRequest(request);

        assertThat(mViewModel.getEnrollmentRequest()).isEqualTo(request);
    }

    @Test
    public void testOnStartToUpdateEnrollableStatus_isSuw() {
        final int userId = 44;
        mViewModel.setUserId(userId);
        mViewModel.setEnrollmentRequest(newIsSuwRequest(mApplication));

        setupFingerprintEnrolledFingerprints(mFingerprintManager, userId, 0);
        setupSuwMaxFingerprintsEnrollable(mApplication, mResources, 1);
        mViewModel.onStart(mLifecycleOwner);
        FingerprintEnrollIntroStatus status = mViewModel.getPageStatusLiveData().getValue();
        assertThat(status.getEnrollableStatus()).isEqualTo(FINGERPRINT_ENROLLABLE_OK);

        setupFingerprintEnrolledFingerprints(mFingerprintManager, userId, 1);
        setupSuwMaxFingerprintsEnrollable(mApplication, mResources, 1);
        mViewModel.onStart(mLifecycleOwner);
        status = mViewModel.getPageStatusLiveData().getValue();
        assertThat(status.getEnrollableStatus()).isEqualTo(FINGERPRINT_ENROLLABLE_ERROR_REACH_MAX);
    }

    @Test
    public void testOnStartToUpdateEnrollableStatus_isNotSuw() {
        testOnStartToUpdateEnrollableStatus(newAllFalseRequest(mApplication));
    }

    @Test
    public void testOnStartToUpdateEnrollableStatus_isSuwDeferred() {
        testOnStartToUpdateEnrollableStatus(newIsSuwDeferredRequest(mApplication));
    }

    @Test
    public void testOnStartToUpdateEnrollableStatus_isSuwPortal() {
        testOnStartToUpdateEnrollableStatus(newIsSuwPortalRequest(mApplication));
    }

    @Test
    public void testOnStartToUpdateEnrollableStatus_isSuwSuggestedActionFlow() {
        testOnStartToUpdateEnrollableStatus(newIsSuwSuggestedActionFlowRequest(mApplication));
    }

    private void testOnStartToUpdateEnrollableStatus(@NonNull EnrollmentRequest request) {
        final int userId = 45;
        mViewModel.setUserId(userId);
        mViewModel.setEnrollmentRequest(request);

        setupFingerprintEnrolledFingerprints(mFingerprintManager, userId, 0);
        setupFingerprintFirstSensor(mFingerprintManager, TYPE_UDFPS_OPTICAL, 5);
        mViewModel.onStart(mLifecycleOwner);
        FingerprintEnrollIntroStatus status = mViewModel.getPageStatusLiveData().getValue();
        assertThat(status.getEnrollableStatus()).isEqualTo(FINGERPRINT_ENROLLABLE_OK);

        setupFingerprintEnrolledFingerprints(mFingerprintManager, userId, 5);
        setupFingerprintFirstSensor(mFingerprintManager, TYPE_UDFPS_OPTICAL, 5);
        mViewModel.onStart(mLifecycleOwner);
        status = mViewModel.getPageStatusLiveData().getValue();
        assertThat(status.getEnrollableStatus()).isEqualTo(FINGERPRINT_ENROLLABLE_ERROR_REACH_MAX);
    }

    @Test
    public void textCanAssumeUdfps() {
        setupFingerprintFirstSensor(mFingerprintManager, TYPE_UDFPS_ULTRASONIC, 1);
        assertThat(mViewModel.canAssumeUdfps()).isEqualTo(true);

        setupFingerprintFirstSensor(mFingerprintManager, TYPE_REAR, 1);
        assertThat(mViewModel.canAssumeUdfps()).isEqualTo(false);
    }

    @Test
    public void testIsParentalConsentRequired() {
        // We shall not mock FingerprintRepository, but
        // FingerprintRepository.isParentalConsentRequired() calls static method inside, we can't
        // mock static method
        final FingerprintRepository fingerprintRepository = mock(FingerprintRepository.class);
        mViewModel = new FingerprintEnrollIntroViewModel(mApplication, fingerprintRepository);

        when(fingerprintRepository.isParentalConsentRequired(mApplication)).thenReturn(true);
        assertThat(mViewModel.isParentalConsentRequired()).isEqualTo(true);

        when(fingerprintRepository.isParentalConsentRequired(mApplication)).thenReturn(false);
        assertThat(mViewModel.isParentalConsentRequired()).isEqualTo(false);
    }

    @Test
    public void testIsBiometricUnlockDisabledByAdmin() {
        // We shall not mock FingerprintRepository, but
        // FingerprintRepository.isDisabledByAdmin() calls static method inside, we can't mock
        // static method
        final FingerprintRepository fingerprintRepository = mock(FingerprintRepository.class);
        mViewModel = new FingerprintEnrollIntroViewModel(mApplication, fingerprintRepository);

        final int userId = 33;
        mViewModel.setUserId(userId);

        when(fingerprintRepository.isDisabledByAdmin(mApplication, userId)).thenReturn(true);
        assertThat(mViewModel.isBiometricUnlockDisabledByAdmin()).isEqualTo(true);

        when(fingerprintRepository.isDisabledByAdmin(mApplication, userId)).thenReturn(false);
        assertThat(mViewModel.isBiometricUnlockDisabledByAdmin()).isEqualTo(false);
    }

    @Test
    public void testSetHasScrolledToBottom() {
        mViewModel.setHasScrolledToBottom();

        FingerprintEnrollIntroStatus status = mViewModel.getPageStatusLiveData().getValue();

        assertThat(status.hasScrollToBottom()).isEqualTo(true);
    }

    @Test
    public void testOnNextButtonClick_enrollNext() {
        final int userId = 46;
        mViewModel.setUserId(userId);
        mViewModel.setEnrollmentRequest(newIsSuwRequest(mApplication));

        // Set latest status to FINGERPRINT_ENROLLABLE_OK
        setupFingerprintEnrolledFingerprints(mFingerprintManager, userId, 0);
        setupSuwMaxFingerprintsEnrollable(mApplication, mResources, 1);
        mViewModel.onStart(mLifecycleOwner);
        FingerprintEnrollIntroStatus status = mViewModel.getPageStatusLiveData().getValue();
        assertThat(status.getEnrollableStatus()).isEqualTo(FINGERPRINT_ENROLLABLE_OK);

        // Perform click on `next`
        mViewModel.onNextButtonClick(null);

        assertThat(mViewModel.getActionLiveData().getValue())
                .isEqualTo(FINGERPRINT_ENROLL_INTRO_ACTION_CONTINUE_ENROLL);
    }

    @Test
    public void testOnNextButtonClick_doneAndFinish() {
        final int userId = 46;
        mViewModel.setUserId(userId);
        mViewModel.setEnrollmentRequest(newIsSuwRequest(mApplication));

        // Set latest status to FINGERPRINT_ENROLLABLE_ERROR_REACH_MAX
        setupFingerprintEnrolledFingerprints(mFingerprintManager, userId, 1);
        setupSuwMaxFingerprintsEnrollable(mApplication, mResources, 1);
        mViewModel.onStart(mLifecycleOwner);
        FingerprintEnrollIntroStatus status = mViewModel.getPageStatusLiveData().getValue();
        assertThat(status.getEnrollableStatus()).isEqualTo(FINGERPRINT_ENROLLABLE_ERROR_REACH_MAX);

        // Perform click on `next`
        mViewModel.onNextButtonClick(null);

        assertThat(mViewModel.getActionLiveData().getValue())
                .isEqualTo(FINGERPRINT_ENROLL_INTRO_ACTION_DONE_AND_FINISH);
    }

    @Test
    public void testOnSkipOrCancelButtonClick() {
        mViewModel.onSkipOrCancelButtonClick(null);

        assertThat(mViewModel.getActionLiveData().getValue())
                .isEqualTo(FINGERPRINT_ENROLL_INTRO_ACTION_SKIP_OR_CANCEL);
    }
}
