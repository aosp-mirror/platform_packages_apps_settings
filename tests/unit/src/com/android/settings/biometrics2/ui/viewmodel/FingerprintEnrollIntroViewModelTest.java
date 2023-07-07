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

import static android.hardware.fingerprint.FingerprintSensorProperties.TYPE_REAR;
import static android.hardware.fingerprint.FingerprintSensorProperties.TYPE_UDFPS_OPTICAL;
import static android.hardware.fingerprint.FingerprintSensorProperties.TYPE_UDFPS_ULTRASONIC;

import static com.android.settings.biometrics2.ui.model.FingerprintEnrollIntroStatus.FINGERPRINT_ENROLLABLE_ERROR_REACH_MAX;
import static com.android.settings.biometrics2.ui.model.FingerprintEnrollIntroStatus.FINGERPRINT_ENROLLABLE_OK;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollIntroViewModel.FINGERPRINT_ENROLL_INTRO_ACTION_CONTINUE_ENROLL;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollIntroViewModel.FINGERPRINT_ENROLL_INTRO_ACTION_DONE_AND_FINISH;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollIntroViewModel.FINGERPRINT_ENROLL_INTRO_ACTION_SKIP_OR_CANCEL;
import static com.android.settings.biometrics2.utils.EnrollmentRequestUtils.newAllFalseRequest;
import static com.android.settings.biometrics2.utils.EnrollmentRequestUtils.newIsSuwDeferredRequest;
import static com.android.settings.biometrics2.utils.EnrollmentRequestUtils.newIsSuwPortalRequest;
import static com.android.settings.biometrics2.utils.EnrollmentRequestUtils.newIsSuwRequest;
import static com.android.settings.biometrics2.utils.EnrollmentRequestUtils.newIsSuwSuggestedActionFlowRequest;
import static com.android.settings.biometrics2.utils.FingerprintRepositoryUtils.newFingerprintRepository;
import static com.android.settings.biometrics2.utils.FingerprintRepositoryUtils.setupFingerprintEnrolledFingerprints;
import static com.android.settings.biometrics2.utils.FingerprintRepositoryUtils.setupSuwMaxFingerprintsEnrollable;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.res.Resources;
import android.hardware.fingerprint.FingerprintManager;

import androidx.annotation.NonNull;
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

    private static final int TEST_USER_ID = 33;

    @Rule public final MockitoRule mockito = MockitoJUnit.rule();
    @Rule public final InstantTaskExecutorRule mTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock private Resources mResources;
    @Mock private FingerprintManager mFingerprintManager;

    private Application mApplication;

    private FingerprintEnrollIntroViewModel newFingerprintEnrollIntroViewModel(
            @NonNull FingerprintRepository fingerprintRepository,
            @NonNull EnrollmentRequest enrollmentRequest) {
        final FingerprintEnrollIntroViewModel viewModel =
                new FingerprintEnrollIntroViewModel(mApplication, fingerprintRepository,
                        enrollmentRequest, TEST_USER_ID);
        // MediatorLiveData won't update itself unless observed
        viewModel.getPageStatusLiveData().observeForever(event -> {});
        return viewModel;
    }

    @Before
    public void setUp() {
        mApplication = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void testPageStatusLiveDataDefaultValue() {
        final FingerprintEnrollIntroViewModel viewModel = newFingerprintEnrollIntroViewModel(
                newFingerprintRepository(mFingerprintManager, TYPE_UDFPS_OPTICAL, 5),
                newAllFalseRequest(mApplication));
        final FingerprintEnrollIntroStatus status = viewModel.getPageStatusLiveData().getValue();
        assertThat(status.hasScrollToBottom()).isFalse();
        assertThat(status.getEnrollableStatus()).isEqualTo(FINGERPRINT_ENROLLABLE_OK);
    }

    @Test
    public void testClearActionLiveData() {
        final FingerprintEnrollIntroViewModel viewModel = newFingerprintEnrollIntroViewModel(
                newFingerprintRepository(mFingerprintManager, TYPE_UDFPS_OPTICAL, 5),
                newAllFalseRequest(mApplication));

        final MutableLiveData<Integer> actionLiveData =
                (MutableLiveData<Integer>) viewModel.getActionLiveData();
        actionLiveData.postValue(1);
        assertThat(actionLiveData.getValue()).isEqualTo(1);

        viewModel.clearActionLiveData();

        assertThat(actionLiveData.getValue()).isNull();
    }

    @Test
    public void testGetEnrollmentRequest() {
        final FingerprintEnrollIntroViewModel viewModel = newFingerprintEnrollIntroViewModel(
                newFingerprintRepository(mFingerprintManager, TYPE_UDFPS_OPTICAL, 5),
                newAllFalseRequest(mApplication));

        assertThat(viewModel.getRequest()).isNotNull();
    }

    @Test
    public void testOnStartToUpdateEnrollableStatusOk_isSuw() {
        setupFingerprintEnrolledFingerprints(mFingerprintManager, TEST_USER_ID, 0);
        setupSuwMaxFingerprintsEnrollable(mApplication, mResources, 1);

        final FingerprintEnrollIntroViewModel viewModel = newFingerprintEnrollIntroViewModel(
                newFingerprintRepository(mFingerprintManager, TYPE_UDFPS_OPTICAL, 5),
                newIsSuwRequest(mApplication));
        final FingerprintEnrollIntroStatus status = viewModel.getPageStatusLiveData().getValue();
        assertThat(status.getEnrollableStatus()).isEqualTo(FINGERPRINT_ENROLLABLE_OK);
    }

    @Test
    public void testOnStartToUpdateEnrollableStatusReachMax_isSuw() {
        setupFingerprintEnrolledFingerprints(mFingerprintManager, TEST_USER_ID, 1);
        setupSuwMaxFingerprintsEnrollable(mApplication, mResources, 1);

        final FingerprintEnrollIntroViewModel viewModel = newFingerprintEnrollIntroViewModel(
                newFingerprintRepository(mFingerprintManager, TYPE_UDFPS_OPTICAL, 5),
                newIsSuwRequest(mApplication));
        final FingerprintEnrollIntroStatus status = viewModel.getPageStatusLiveData().getValue();
        assertThat(status.getEnrollableStatus()).isEqualTo(FINGERPRINT_ENROLLABLE_ERROR_REACH_MAX);
    }

    @Test
    public void testOnStartToUpdateEnrollableStatusOk_isNotSuw() {
        testOnStartToUpdateEnrollableStatusOk(newAllFalseRequest(mApplication));
    }

    @Test
    public void testOnStartToUpdateEnrollableStatusReachMax_isNotSuw() {
        testOnStartToUpdateEnrollableStatusReachMax(newAllFalseRequest(mApplication));
    }

    @Test
    public void testOnStartToUpdateEnrollableStatusOk_isSuwDeferred() {
        testOnStartToUpdateEnrollableStatusOk(newIsSuwDeferredRequest(mApplication));
    }

    @Test
    public void testOnStartToUpdateEnrollableStatusReachMax_isSuwDeferred() {
        testOnStartToUpdateEnrollableStatusReachMax(newIsSuwDeferredRequest(mApplication));
    }

    @Test
    public void testOnStartToUpdateEnrollableStatusOk_isSuwPortal() {
        testOnStartToUpdateEnrollableStatusOk(newIsSuwPortalRequest(mApplication));
    }

    @Test
    public void testOnStartToUpdateEnrollableStatusReachMax_isSuwPortal() {
        testOnStartToUpdateEnrollableStatusReachMax(newIsSuwPortalRequest(mApplication));
    }

    @Test
    public void testOnStartToUpdateEnrollableStatusOk_isSuwSuggestedActionFlow() {
        testOnStartToUpdateEnrollableStatusOk(newIsSuwSuggestedActionFlowRequest(mApplication));
    }

    @Test
    public void testOnStartToUpdateEnrollableStatusReachMax_isSuwSuggestedActionFlow() {
        testOnStartToUpdateEnrollableStatusReachMax(
                newIsSuwSuggestedActionFlowRequest(mApplication));
    }

    private void testOnStartToUpdateEnrollableStatusOk(@NonNull EnrollmentRequest request) {
        setupFingerprintEnrolledFingerprints(mFingerprintManager, TEST_USER_ID, 0);

        final FingerprintEnrollIntroViewModel viewModel = newFingerprintEnrollIntroViewModel(
                newFingerprintRepository(mFingerprintManager, TYPE_UDFPS_OPTICAL, 5),
                request);
        FingerprintEnrollIntroStatus status = viewModel.getPageStatusLiveData().getValue();
        assertThat(status.getEnrollableStatus()).isEqualTo(FINGERPRINT_ENROLLABLE_OK);
    }

    private void testOnStartToUpdateEnrollableStatusReachMax(@NonNull EnrollmentRequest request) {
        setupFingerprintEnrolledFingerprints(mFingerprintManager, TEST_USER_ID, 5);

        final FingerprintEnrollIntroViewModel viewModel = newFingerprintEnrollIntroViewModel(
                newFingerprintRepository(mFingerprintManager, TYPE_UDFPS_OPTICAL, 5),
                request);
        FingerprintEnrollIntroStatus status = viewModel.getPageStatusLiveData().getValue();
        assertThat(status.getEnrollableStatus()).isEqualTo(FINGERPRINT_ENROLLABLE_ERROR_REACH_MAX);
    }

    @Test
    public void testCanAssumeUdfps_forUdfpsUltrasonicSensor() {
        final FingerprintEnrollIntroViewModel viewModel = newFingerprintEnrollIntroViewModel(
                newFingerprintRepository(mFingerprintManager, TYPE_UDFPS_ULTRASONIC, 5),
                newAllFalseRequest(mApplication));

        assertThat(viewModel.canAssumeUdfps()).isEqualTo(true);
    }

    @Test
    public void testCanAssumeUdfps_forRearSensor() {
        final FingerprintEnrollIntroViewModel viewModel = newFingerprintEnrollIntroViewModel(
                newFingerprintRepository(mFingerprintManager, TYPE_REAR, 5),
                newAllFalseRequest(mApplication));

        assertThat(viewModel.canAssumeUdfps()).isEqualTo(false);
    }

    @Test
    public void testIsParentalConsentRequired() {
        // We shall not mock FingerprintRepository, but
        // FingerprintRepository.isParentalConsentRequired() calls static method inside, we can't
        // mock static method
        final FingerprintRepository fingerprintRepository = mock(FingerprintRepository.class);
        final FingerprintEnrollIntroViewModel viewModel = new FingerprintEnrollIntroViewModel(
                mApplication, fingerprintRepository, newAllFalseRequest(mApplication),
                TEST_USER_ID);

        when(fingerprintRepository.isParentalConsentRequired(mApplication)).thenReturn(true);
        assertThat(viewModel.isParentalConsentRequired()).isEqualTo(true);

        when(fingerprintRepository.isParentalConsentRequired(mApplication)).thenReturn(false);
        assertThat(viewModel.isParentalConsentRequired()).isEqualTo(false);
    }

    @Test
    public void testIsBiometricUnlockDisabledByAdmin() {
        // We shall not mock FingerprintRepository, but
        // FingerprintRepository.isDisabledByAdmin() calls static method inside, we can't mock
        // static method
        final FingerprintRepository fingerprintRepository = mock(FingerprintRepository.class);
        final FingerprintEnrollIntroViewModel viewModel = new FingerprintEnrollIntroViewModel(
                mApplication, fingerprintRepository, newAllFalseRequest(mApplication),
                TEST_USER_ID);

        when(fingerprintRepository.isDisabledByAdmin(mApplication, TEST_USER_ID)).thenReturn(true);
        assertThat(viewModel.isBiometricUnlockDisabledByAdmin()).isEqualTo(true);

        when(fingerprintRepository.isDisabledByAdmin(mApplication, TEST_USER_ID)).thenReturn(false);
        assertThat(viewModel.isBiometricUnlockDisabledByAdmin()).isEqualTo(false);
    }

    @Test
    public void testSetHasScrolledToBottom() {
        final FingerprintEnrollIntroViewModel viewModel = newFingerprintEnrollIntroViewModel(
                newFingerprintRepository(mFingerprintManager, TYPE_UDFPS_OPTICAL, 5),
                newAllFalseRequest(mApplication));

        viewModel.setHasScrolledToBottom(true);
        FingerprintEnrollIntroStatus status = viewModel.getPageStatusLiveData().getValue();
        assertThat(status.hasScrollToBottom()).isEqualTo(true);

        viewModel.setHasScrolledToBottom(false);
        status = viewModel.getPageStatusLiveData().getValue();
        assertThat(status.hasScrollToBottom()).isEqualTo(false);
    }

    @Test
    public void testOnNextButtonClick_enrollNext() {
        // Set latest status to FINGERPRINT_ENROLLABLE_OK
        setupFingerprintEnrolledFingerprints(mFingerprintManager, TEST_USER_ID, 0);
        setupSuwMaxFingerprintsEnrollable(mApplication, mResources, 1);

        final FingerprintEnrollIntroViewModel viewModel = newFingerprintEnrollIntroViewModel(
                newFingerprintRepository(mFingerprintManager, TYPE_UDFPS_OPTICAL, 5),
                newIsSuwRequest(mApplication));
        FingerprintEnrollIntroStatus status = viewModel.getPageStatusLiveData().getValue();
        assertThat(status.getEnrollableStatus()).isEqualTo(FINGERPRINT_ENROLLABLE_OK);

        // Perform click on `next`
        viewModel.onNextButtonClick();

        assertThat(viewModel.getActionLiveData().getValue())
                .isEqualTo(FINGERPRINT_ENROLL_INTRO_ACTION_CONTINUE_ENROLL);
    }

    @Test
    public void testOnNextButtonClick_doneAndFinish() {
        // Set latest status to FINGERPRINT_ENROLLABLE_ERROR_REACH_MAX
        setupFingerprintEnrolledFingerprints(mFingerprintManager, TEST_USER_ID, 1);
        setupSuwMaxFingerprintsEnrollable(mApplication, mResources, 1);

        final FingerprintEnrollIntroViewModel viewModel = newFingerprintEnrollIntroViewModel(
                newFingerprintRepository(mFingerprintManager, TYPE_UDFPS_OPTICAL, 5),
                newIsSuwRequest(mApplication));
        FingerprintEnrollIntroStatus status = viewModel.getPageStatusLiveData().getValue();
        assertThat(status.getEnrollableStatus()).isEqualTo(FINGERPRINT_ENROLLABLE_ERROR_REACH_MAX);

        // Perform click on `next`
        viewModel.onNextButtonClick();

        assertThat(viewModel.getActionLiveData().getValue())
                .isEqualTo(FINGERPRINT_ENROLL_INTRO_ACTION_DONE_AND_FINISH);
    }

    @Test
    public void testOnSkipOrCancelButtonClick() {
        final FingerprintEnrollIntroViewModel viewModel = newFingerprintEnrollIntroViewModel(
                newFingerprintRepository(mFingerprintManager, TYPE_UDFPS_OPTICAL, 5),
                newAllFalseRequest(mApplication));

        viewModel.onSkipOrCancelButtonClick();

        assertThat(viewModel.getActionLiveData().getValue())
                .isEqualTo(FINGERPRINT_ENROLL_INTRO_ACTION_SKIP_OR_CANCEL);
    }
}
