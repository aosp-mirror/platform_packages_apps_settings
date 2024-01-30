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
package com.android.settings.biometrics2.ui.viewmodel

import android.app.Application
import android.content.res.Resources
import android.hardware.fingerprint.FingerprintManager
import android.hardware.fingerprint.FingerprintSensorProperties.TYPE_UDFPS_OPTICAL
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.biometrics2.data.repository.FingerprintRepository
import com.android.settings.biometrics2.ui.model.EnrollmentRequest
import com.android.settings.biometrics2.ui.model.FingerprintEnrollIntroStatus
import com.android.settings.biometrics2.ui.model.FingerprintEnrollable.FINGERPRINT_ENROLLABLE_ERROR_REACH_MAX
import com.android.settings.biometrics2.ui.model.FingerprintEnrollable.FINGERPRINT_ENROLLABLE_OK
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollIntroAction.CONTINUE_ENROLL
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollIntroAction.DONE_AND_FINISH
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollIntroAction.SKIP_OR_CANCEL
import com.android.settings.biometrics2.utils.EnrollmentRequestUtils.newAllFalseRequest
import com.android.settings.biometrics2.utils.EnrollmentRequestUtils.newIsSuwDeferredRequest
import com.android.settings.biometrics2.utils.EnrollmentRequestUtils.newIsSuwPortalRequest
import com.android.settings.biometrics2.utils.EnrollmentRequestUtils.newIsSuwRequest
import com.android.settings.biometrics2.utils.EnrollmentRequestUtils.newIsSuwSuggestedActionFlowRequest
import com.android.settings.biometrics2.utils.FingerprintRepositoryUtils.newFingerprintRepository
import com.android.settings.biometrics2.utils.FingerprintRepositoryUtils.setupFingerprintEnrolledFingerprints
import com.android.settings.biometrics2.utils.FingerprintRepositoryUtils.setupSuwMaxFingerprintsEnrollable
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit

@RunWith(AndroidJUnit4::class)
class FingerprintEnrollIntroViewModelTest {

    @get:Rule val mockito = MockitoJUnit.rule()

    @Mock private lateinit var resources: Resources
    @Mock private lateinit var fingerprintManager: FingerprintManager

    private var application: Application = ApplicationProvider.getApplicationContext()

    private fun newFingerprintEnrollIntroViewModel(
        fingerprintRepository: FingerprintRepository,
        enrollmentRequest: EnrollmentRequest
    ) = FingerprintEnrollIntroViewModel(
        application,
        fingerprintRepository,
        enrollmentRequest,
        TEST_USER_ID
    )

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testPageStatusFlowDefaultAndUpdate() = runTest {
        val viewModel = newFingerprintEnrollIntroViewModel(
            newFingerprintRepository(fingerprintManager, TYPE_UDFPS_OPTICAL, 1),
            newAllFalseRequest(application)
        )

        val statusList = listOfPageStatusFlow(viewModel)

        runCurrent()

        // assert default values
        assertThat(statusList.size).isEqualTo(1)
        assertThat(statusList[0].hasScrollToBottom()).isFalse()
        assertThat(statusList[0].enrollableStatus).isEqualTo(FINGERPRINT_ENROLLABLE_OK)

        setupFingerprintEnrolledFingerprints(fingerprintManager, TEST_USER_ID, 1)
        viewModel.updateEnrollableStatus(backgroundScope)
        runCurrent()

        // assert new updated value
        assertThat(statusList.size).isEqualTo(2)
        assertThat(statusList[1].hasScrollToBottom()).isFalse()
        assertThat(statusList[1].enrollableStatus).isEqualTo(FINGERPRINT_ENROLLABLE_ERROR_REACH_MAX)
    }

    fun testOnStartToUpdateEnrollableStatusOk_isSuw() = runTest {
        setupFingerprintEnrolledFingerprints(fingerprintManager, TEST_USER_ID, 0)
        setupSuwMaxFingerprintsEnrollable(application, resources, 1)
        val viewModel = newFingerprintEnrollIntroViewModel(
            newFingerprintRepository(fingerprintManager, TYPE_UDFPS_OPTICAL, 5),
            newIsSuwRequest(application)
        )

        val statusList = listOfPageStatusFlow(viewModel)

        runCurrent()

        assertThat(statusList.size).isEqualTo(1)
        assertThat(statusList[0].enrollableStatus).isEqualTo(FINGERPRINT_ENROLLABLE_OK)
    }

    @Test
    fun testOnStartToUpdateEnrollableStatusReachMax_isSuw() = runTest {
        setupFingerprintEnrolledFingerprints(fingerprintManager, TEST_USER_ID, 1)
        setupSuwMaxFingerprintsEnrollable(application, resources, 1)
        val viewModel = newFingerprintEnrollIntroViewModel(
            newFingerprintRepository(fingerprintManager, TYPE_UDFPS_OPTICAL, 5),
            newIsSuwRequest(application)
        )

        val statusList = listOfPageStatusFlow(viewModel)

        runCurrent()

        assertThat(statusList.size).isEqualTo(1)
        assertThat(statusList[0].enrollableStatus).isEqualTo(FINGERPRINT_ENROLLABLE_ERROR_REACH_MAX)
    }

    @Test
    fun testOnStartToUpdateEnrollableStatusOk_isNotSuw() = runTest {
        testOnStartToUpdateEnrollableStatusOk(newAllFalseRequest(application))
    }

    @Test
    fun testOnStartToUpdateEnrollableStatusReachMax_isNotSuw() = runTest {
        testOnStartToUpdateEnrollableStatusReachMax(newAllFalseRequest(application))
    }

    @Test
    fun testOnStartToUpdateEnrollableStatusOk_isSuwDeferred() = runTest {
        testOnStartToUpdateEnrollableStatusOk(newIsSuwDeferredRequest(application))
    }

    @Test
    fun testOnStartToUpdateEnrollableStatusReachMax_isSuwDeferred() = runTest {
        testOnStartToUpdateEnrollableStatusReachMax(newIsSuwDeferredRequest(application))
    }

    @Test
    fun testOnStartToUpdateEnrollableStatusOk_isSuwPortal() = runTest {
        testOnStartToUpdateEnrollableStatusOk(newIsSuwPortalRequest(application))
    }

    @Test
    fun testOnStartToUpdateEnrollableStatusReachMax_isSuwPortal() = runTest {
        testOnStartToUpdateEnrollableStatusReachMax(newIsSuwPortalRequest(application))
    }

    @Test
    fun testOnStartToUpdateEnrollableStatusOk_isSuwSuggestedActionFlow() = runTest {
        testOnStartToUpdateEnrollableStatusOk(newIsSuwSuggestedActionFlowRequest(application))
    }

    @Test
    fun testOnStartToUpdateEnrollableStatusReachMax_isSuwSuggestedActionFlow() = runTest {
        testOnStartToUpdateEnrollableStatusReachMax(
            newIsSuwSuggestedActionFlowRequest(application)
        )
    }

    private fun TestScope.testOnStartToUpdateEnrollableStatusOk(request: EnrollmentRequest) {
        setupFingerprintEnrolledFingerprints(fingerprintManager, TEST_USER_ID, 0)
        val viewModel = newFingerprintEnrollIntroViewModel(
            newFingerprintRepository(fingerprintManager, TYPE_UDFPS_OPTICAL, 5),
            request
        )

        val statusList = listOfPageStatusFlow(viewModel)

        runCurrent()

        assertThat(statusList.size).isEqualTo(1)
        assertThat(statusList[0].enrollableStatus).isEqualTo(FINGERPRINT_ENROLLABLE_OK)
    }

    private fun TestScope.testOnStartToUpdateEnrollableStatusReachMax(request: EnrollmentRequest) {
        setupFingerprintEnrolledFingerprints(fingerprintManager, TEST_USER_ID, 5)
        val viewModel = newFingerprintEnrollIntroViewModel(
            newFingerprintRepository(fingerprintManager, TYPE_UDFPS_OPTICAL, 5),
            request
        )

        val statusList = listOfPageStatusFlow(viewModel)

        runCurrent()

        assertThat(statusList.size).isEqualTo(1)
        assertThat(statusList[0].enrollableStatus).isEqualTo(FINGERPRINT_ENROLLABLE_ERROR_REACH_MAX)
    }

    @Test
    fun testIsParentalConsentRequired() {
        // We shall not mock FingerprintRepository, but
        // FingerprintRepository.isParentalConsentRequired() calls static method inside, we can't
        // mock static method
        val fingerprintRepository = Mockito.mock(
            FingerprintRepository::class.java
        )
        val viewModel = FingerprintEnrollIntroViewModel(
            application,
            fingerprintRepository,
            newAllFalseRequest(application),
            TEST_USER_ID
        )
        Mockito.`when`(
            fingerprintRepository.isParentalConsentRequired(application)
        ).thenReturn(true)
        assertThat(viewModel.isParentalConsentRequired).isEqualTo(true)
        Mockito.`when`(
            fingerprintRepository.isParentalConsentRequired(application)
        ).thenReturn(false)
        assertThat(viewModel.isParentalConsentRequired).isEqualTo(false)
    }

    @Test
    fun testIsBiometricUnlockDisabledByAdmin() {
        // We shall not mock FingerprintRepository, but
        // FingerprintRepository.isDisabledByAdmin() calls static method inside, we can't mock
        // static method
        val fingerprintRepository = Mockito.mock(FingerprintRepository::class.java)
        val viewModel = FingerprintEnrollIntroViewModel(
            application,
            fingerprintRepository,
            newAllFalseRequest(application),
            TEST_USER_ID
        )
        Mockito.`when`(
            fingerprintRepository.isDisabledByAdmin(application, TEST_USER_ID)
        ).thenReturn(true)
        assertThat(viewModel.isBiometricUnlockDisabledByAdmin).isEqualTo(true)
        Mockito.`when`(
            fingerprintRepository.isDisabledByAdmin(application, TEST_USER_ID)
        ).thenReturn(false)
        assertThat(viewModel.isBiometricUnlockDisabledByAdmin).isEqualTo(false)
    }

    @Test
    fun testSetHasScrolledToBottom() = runTest {
        val viewModel = newFingerprintEnrollIntroViewModel(
            newFingerprintRepository(fingerprintManager, TYPE_UDFPS_OPTICAL, 5),
            newAllFalseRequest(application)
        )

        val pageStatusList = listOfPageStatusFlow(viewModel)

        viewModel.setHasScrolledToBottom(true, backgroundScope)
        runCurrent()

        assertThat(pageStatusList[pageStatusList.size-1].hasScrollToBottom()).isEqualTo(true)
    }

    @Test
    fun testOnNextButtonClick_enrollNext() = runTest {
        // Set latest status to FINGERPRINT_ENROLLABLE_OK
        setupFingerprintEnrolledFingerprints(fingerprintManager, TEST_USER_ID, 0)
        setupSuwMaxFingerprintsEnrollable(application, resources, 1)
        val viewModel = newFingerprintEnrollIntroViewModel(
            newFingerprintRepository(fingerprintManager, TYPE_UDFPS_OPTICAL, 5),
            newIsSuwRequest(application)
        )

        val actions = listOfActionFlow(viewModel)

        // Perform click on `next`
        viewModel.onNextButtonClick(backgroundScope)
        runCurrent()

        assertThat(actions.size).isEqualTo(1)
        assertThat(actions[0]).isEqualTo(CONTINUE_ENROLL)
    }

    @Test
    fun testOnNextButtonClick_doneAndFinish() = runTest {
        // Set latest status to FINGERPRINT_ENROLLABLE_ERROR_REACH_MAX
        setupFingerprintEnrolledFingerprints(fingerprintManager, TEST_USER_ID, 1)
        setupSuwMaxFingerprintsEnrollable(application, resources, 1)
        val viewModel = newFingerprintEnrollIntroViewModel(
            newFingerprintRepository(fingerprintManager, TYPE_UDFPS_OPTICAL, 5),
            newIsSuwRequest(application)
        )

        val statusList = listOfPageStatusFlow(viewModel)
        val actionList = listOfActionFlow(viewModel)

        runCurrent()

        assertThat(statusList.size).isEqualTo(1)
        assertThat(statusList[0].enrollableStatus).isEqualTo(FINGERPRINT_ENROLLABLE_ERROR_REACH_MAX)

        val actions = listOfActionFlow(viewModel)

        // Perform click on `next`
        viewModel.onNextButtonClick(backgroundScope)
        runCurrent()

        assertThat(actionList.size).isEqualTo(1)
        assertThat(actionList[0]).isEqualTo(DONE_AND_FINISH)
    }

    @Test
    fun testOnSkipOrCancelButtonClick() = runTest {
        val viewModel = newFingerprintEnrollIntroViewModel(
            newFingerprintRepository(fingerprintManager, TYPE_UDFPS_OPTICAL, 5),
            newAllFalseRequest(application)
        )

        val actions = listOfActionFlow(viewModel)

        viewModel.onSkipOrCancelButtonClick(backgroundScope)
        runCurrent()

        assertThat(actions.size).isEqualTo(1)
        assertThat(actions[0]).isEqualTo(SKIP_OR_CANCEL)
    }

    private fun TestScope.listOfActionFlow(
        viewModel: FingerprintEnrollIntroViewModel
    ): List<FingerprintEnrollIntroAction> =
        mutableListOf<FingerprintEnrollIntroAction>().also {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actionFlow.toList(it)
            }
        }

    private fun TestScope.listOfPageStatusFlow(
        viewModel: FingerprintEnrollIntroViewModel
    ): List<FingerprintEnrollIntroStatus> =
        mutableListOf<FingerprintEnrollIntroStatus>().also {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.pageStatusFlow.toList(it)
            }
        }

    companion object {
        private const val TEST_USER_ID = 33
    }
}
