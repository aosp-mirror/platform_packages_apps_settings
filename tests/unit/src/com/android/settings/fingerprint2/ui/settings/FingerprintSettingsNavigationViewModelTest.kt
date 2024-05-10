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

package com.android.settings.fingerprint2.ui.settings

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.android.settings.biometrics.BiometricEnrollBase
import com.android.settings.biometrics.fingerprint2.shared.model.FingerprintData
import com.android.settings.biometrics.fingerprint2.ui.settings.viewmodel.EnrollFirstFingerprint
import com.android.settings.biometrics.fingerprint2.ui.settings.viewmodel.FingerprintSettingsNavigationViewModel
import com.android.settings.biometrics.fingerprint2.ui.settings.viewmodel.FinishSettings
import com.android.settings.biometrics.fingerprint2.ui.settings.viewmodel.FinishSettingsWithResult
import com.android.settings.biometrics.fingerprint2.ui.settings.viewmodel.LaunchConfirmDeviceCredential
import com.android.settings.biometrics.fingerprint2.ui.settings.viewmodel.NextStepViewModel
import com.android.settings.biometrics.fingerprint2.ui.settings.viewmodel.ShowSettings
import com.android.settings.testutils2.FakeFingerprintManagerInteractor
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class FingerprintSettingsNavigationViewModelTest {

  @JvmField @Rule var rule = MockitoJUnit.rule()

  @get:Rule val instantTaskRule = InstantTaskExecutorRule()

  private lateinit var underTest: FingerprintSettingsNavigationViewModel
  private val defaultUserId = 0
  private var backgroundDispatcher = StandardTestDispatcher()
  private var testScope = TestScope(backgroundDispatcher)
  private lateinit var fakeFingerprintManagerInteractor: FakeFingerprintManagerInteractor

  @Before
  fun setup() {
    fakeFingerprintManagerInteractor = FakeFingerprintManagerInteractor()
    backgroundDispatcher = StandardTestDispatcher()
    testScope = TestScope(backgroundDispatcher)
    Dispatchers.setMain(backgroundDispatcher)

    underTest =
      FingerprintSettingsNavigationViewModel.FingerprintSettingsNavigationModelFactory(
          defaultUserId,
          fakeFingerprintManagerInteractor,
          backgroundDispatcher,
          null,
          null,
        )
        .create(FingerprintSettingsNavigationViewModel::class.java)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun testNoGateKeeper_launchesConfirmDeviceCredential() =
    testScope.runTest {
      var nextStep: NextStepViewModel? = null
      val job = launch { underTest.nextStep.collect { nextStep = it } }

      runCurrent()
      assertThat(nextStep).isEqualTo(LaunchConfirmDeviceCredential(defaultUserId))
      job.cancel()
    }

  @Test
  fun testConfirmDevice_fails() =
    testScope.runTest {
      var nextStep: NextStepViewModel? = null
      val job = launch { underTest.nextStep.collect { nextStep = it } }

      underTest.onConfirmDevice(false, null)
      runCurrent()

      assertThat(nextStep).isInstanceOf(FinishSettings::class.java)
      job.cancel()
    }

  @Test
  fun confirmDeviceSuccess_noGateKeeper() =
    testScope.runTest {
      var nextStep: NextStepViewModel? = null
      val job = launch { underTest.nextStep.collect { nextStep = it } }

      underTest.onConfirmDevice(true, null)
      runCurrent()

      assertThat(nextStep).isInstanceOf(FinishSettings::class.java)
      job.cancel()
    }

  @Test
  fun confirmDeviceSuccess_launchesEnrollment_ifNoPreviousEnrollments() =
    testScope.runTest {
      fakeFingerprintManagerInteractor.enrolledFingerprintsInternal = mutableListOf()

      var nextStep: NextStepViewModel? = null
      val job = launch { underTest.nextStep.collect { nextStep = it } }

      underTest.onConfirmDevice(true, 10L)
      runCurrent()

      assertThat(nextStep).isEqualTo(EnrollFirstFingerprint(defaultUserId, 10L, null, null))
      job.cancel()
    }

  @Test
  fun firstEnrollment_failsWithReason() =
    testScope.runTest {
      fakeFingerprintManagerInteractor.enrolledFingerprintsInternal = mutableListOf()

      var nextStep: NextStepViewModel? = null
      val job = launch { underTest.nextStep.collect { nextStep = it } }

      val failStr = "We failed!!"
      val failReason = 101

      underTest.onConfirmDevice(true, 10L)
      underTest.onEnrollFirstFailure(failStr, failReason)
      runCurrent()

      assertThat(nextStep).isEqualTo(FinishSettingsWithResult(failReason, failStr))
      job.cancel()
    }

  @Test
  fun firstEnrollmentSucceeds_noToken() =
    testScope.runTest {
      fakeFingerprintManagerInteractor.enrolledFingerprintsInternal = mutableListOf()

      var nextStep: NextStepViewModel? = null
      val job = launch { underTest.nextStep.collect { nextStep = it } }

      underTest.onConfirmDevice(true, 10L)
      underTest.onEnrollFirst(null, null)
      runCurrent()

      assertThat(nextStep).isEqualTo(FinishSettings("Error, empty token"))
      job.cancel()
    }

  @Test
  fun firstEnrollmentSucceeds_noKeyChallenge() =
    testScope.runTest {
      fakeFingerprintManagerInteractor.enrolledFingerprintsInternal = mutableListOf()

      var nextStep: NextStepViewModel? = null
      val job = launch { underTest.nextStep.collect { nextStep = it } }

      val byteArray = ByteArray(1) { 3 }

      underTest.onConfirmDevice(true, 10L)
      underTest.onEnrollFirst(byteArray, null)
      runCurrent()

      assertThat(nextStep).isEqualTo(FinishSettings("Error, empty keyChallenge"))
      job.cancel()
    }

  @Test
  fun firstEnrollment_succeeds() =
    testScope.runTest {
      fakeFingerprintManagerInteractor.enrolledFingerprintsInternal = mutableListOf()

      var nextStep: NextStepViewModel? = null
      val job = testScope.launch { underTest.nextStep.collect { nextStep = it } }

      val byteArray = ByteArray(1) { 3 }
      val keyChallenge = 89L

      underTest.onConfirmDevice(true, 10L)
      underTest.onEnrollFirst(byteArray, keyChallenge)
      runCurrent()

      assertThat(nextStep).isEqualTo(ShowSettings)
      job.cancel()
    }

  @Test
  fun enrollAdditionalFingerprints_fails() =
    testScope.runTest {
      fakeFingerprintManagerInteractor.enrolledFingerprintsInternal =
        mutableListOf(FingerprintData("a", 1, 3L))
      fakeFingerprintManagerInteractor.challengeToGenerate = Pair(4L, byteArrayOf(3, 3, 1))

      var nextStep: NextStepViewModel? = null
      val job = launch { underTest.nextStep.collect { nextStep = it } }

      underTest.onConfirmDevice(true, 10L)
      runCurrent()
      underTest.onEnrollAdditionalFailure()
      runCurrent()

      assertThat(nextStep).isInstanceOf(FinishSettings::class.java)
      job.cancel()
    }

  @Test
  fun enrollAdditional_success() =
    testScope.runTest {
      fakeFingerprintManagerInteractor.enrolledFingerprintsInternal =
        mutableListOf(FingerprintData("a", 1, 3L))

      var nextStep: NextStepViewModel? = null
      val job = launch { underTest.nextStep.collect { nextStep = it } }

      underTest.onConfirmDevice(true, 10L)
      underTest.onEnrollSuccess()

      runCurrent()

      assertThat(nextStep).isEqualTo(ShowSettings)
      job.cancel()
    }

  @Test
  fun confirmDeviceCredential_withEnrolledFingerprint_showsSettings() =
    testScope.runTest {
      fakeFingerprintManagerInteractor.enrolledFingerprintsInternal =
        mutableListOf(FingerprintData("a", 1, 3L))
      fakeFingerprintManagerInteractor.challengeToGenerate = Pair(10L, byteArrayOf(1, 2, 3))

      var nextStep: NextStepViewModel? = null
      val job = launch { underTest.nextStep.collect { nextStep = it } }

      underTest.onConfirmDevice(true, 10L)
      runCurrent()

      assertThat(nextStep).isEqualTo(ShowSettings)
      job.cancel()
    }

  @Test
  fun enrollWithToken_andNoUsers_startsFingerprintEnrollment() =
    testScope.runTest {
      fakeFingerprintManagerInteractor.enrolledFingerprintsInternal = mutableListOf()

      var nextStep: NextStepViewModel? = null
      val job = launch { underTest.nextStep.collect { nextStep = it } }

      val token = byteArrayOf(1)
      val challenge = 5L

      underTest =
        FingerprintSettingsNavigationViewModel.FingerprintSettingsNavigationModelFactory(
            defaultUserId,
            fakeFingerprintManagerInteractor,
            backgroundDispatcher,
            token,
            challenge,
          )
          .create(FingerprintSettingsNavigationViewModel::class.java)

      runCurrent()

      assertThat(nextStep).isEqualTo(EnrollFirstFingerprint(defaultUserId, null, challenge, token))
      job.cancel()
    }

  @Test
  fun enroll_shouldNotFinish() =
    testScope.runTest {
      fakeFingerprintManagerInteractor.enrolledFingerprintsInternal = mutableListOf()

      var nextStep: NextStepViewModel? = null
      val job = launch { underTest.nextStep.collect { nextStep = it } }

      val token = byteArrayOf(1)
      val challenge = 5L

      underTest =
        FingerprintSettingsNavigationViewModel.FingerprintSettingsNavigationModelFactory(
            defaultUserId,
            fakeFingerprintManagerInteractor,
            backgroundDispatcher,
            token,
            challenge,
          )
          .create(FingerprintSettingsNavigationViewModel::class.java)

      runCurrent()

      assertThat(nextStep).isEqualTo(EnrollFirstFingerprint(defaultUserId, null, challenge, token))
      underTest.maybeFinishActivity(false)

      runCurrent()
      assertThat(nextStep).isEqualTo(EnrollFirstFingerprint(defaultUserId, null, challenge, token))
      job.cancel()
    }

  @Test
  fun showSettings_shouldFinish() =
    testScope.runTest {
      fakeFingerprintManagerInteractor.enrolledFingerprintsInternal =
        mutableListOf(FingerprintData("a", 1, 3L))

      var nextStep: NextStepViewModel? = null
      val job = launch { underTest.nextStep.collect { nextStep = it } }

      val token = byteArrayOf(1)
      val challenge = 5L

      underTest =
        FingerprintSettingsNavigationViewModel.FingerprintSettingsNavigationModelFactory(
            defaultUserId,
            fakeFingerprintManagerInteractor,
            backgroundDispatcher,
            token,
            challenge,
          )
          .create(FingerprintSettingsNavigationViewModel::class.java)

      runCurrent()
      assertThat(nextStep).isEqualTo(ShowSettings)

      underTest.maybeFinishActivity(false)

      runCurrent()
      assertThat(nextStep)
        .isEqualTo(
          FinishSettingsWithResult(BiometricEnrollBase.RESULT_TIMEOUT, "onStop finishing settings")
        )
      job.cancel()
    }
}
