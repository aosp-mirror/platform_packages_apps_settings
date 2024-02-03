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

package com.android.settings.fingerprint2.viewmodel

import android.hardware.biometrics.SensorProperties
import android.hardware.fingerprint.FingerprintSensorProperties
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.android.settings.biometrics.fingerprint2.ui.viewmodel.FingerprintAuthAttemptViewModel
import com.android.settings.biometrics.fingerprint2.ui.viewmodel.FingerprintSettingsNavigationViewModel
import com.android.settings.biometrics.fingerprint2.ui.viewmodel.FingerprintSettingsViewModel
import com.android.settings.biometrics.fingerprint2.ui.viewmodel.FingerprintViewModel
import com.android.settings.biometrics.fingerprint2.ui.viewmodel.PreferenceViewModel
import com.android.settings.fingerprint2.domain.interactor.FakeFingerprintManagerInteractor
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
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
class FingerprintSettingsViewModelTest {

  @JvmField @Rule var rule = MockitoJUnit.rule()

  @get:Rule val instantTaskRule = InstantTaskExecutorRule()

  private lateinit var underTest: FingerprintSettingsViewModel
  private lateinit var navigationViewModel: FingerprintSettingsNavigationViewModel
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

    navigationViewModel =
      FingerprintSettingsNavigationViewModel.FingerprintSettingsNavigationModelFactory(
          defaultUserId,
          fakeFingerprintManagerInteractor,
          backgroundDispatcher,
          null,
          null,
        )
        .create(FingerprintSettingsNavigationViewModel::class.java)

    underTest =
      FingerprintSettingsViewModel.FingerprintSettingsViewModelFactory(
          defaultUserId,
          fakeFingerprintManagerInteractor,
          backgroundDispatcher,
          navigationViewModel,
        )
        .create(FingerprintSettingsViewModel::class.java)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun authenticate_DoesNotRun_ifOptical() =
    testScope.runTest {
      fakeFingerprintManagerInteractor.sensorProps =
        listOf(
          FingerprintSensorPropertiesInternal(
            0 /* sensorId */,
            SensorProperties.STRENGTH_STRONG,
            5 /* maxEnrollmentsPerUser */,
            emptyList() /* ComponentInfoInternal */,
            FingerprintSensorProperties.TYPE_UDFPS_OPTICAL,
            true /* resetLockoutRequiresHardwareAuthToken */
          )
        )
      fakeFingerprintManagerInteractor.enrolledFingerprintsInternal =
        mutableListOf(FingerprintViewModel("a", 1, 3L))

      underTest =
        FingerprintSettingsViewModel.FingerprintSettingsViewModelFactory(
            defaultUserId,
            fakeFingerprintManagerInteractor,
            backgroundDispatcher,
            navigationViewModel,
          )
          .create(FingerprintSettingsViewModel::class.java)

      var authAttempt: FingerprintAuthAttemptViewModel? = null
      val job = launch { underTest.authFlow.take(5).collectLatest { authAttempt = it } }

      underTest.shouldAuthenticate(true)
      // Ensure we are showing settings
      navigationViewModel.onConfirmDevice(true, 10L)

      runCurrent()
      advanceTimeBy(400)

      assertThat(authAttempt).isNull()
      job.cancel()
    }

  @Test
  fun authenticate_DoesNotRun_ifUltrasonic() =
    testScope.runTest {
      fakeFingerprintManagerInteractor.sensorProps =
        listOf(
          FingerprintSensorPropertiesInternal(
            0 /* sensorId */,
            SensorProperties.STRENGTH_STRONG,
            5 /* maxEnrollmentsPerUser */,
            emptyList() /* ComponentInfoInternal */,
            FingerprintSensorProperties.TYPE_UDFPS_ULTRASONIC,
            true /* resetLockoutRequiresHardwareAuthToken */
          )
        )
      fakeFingerprintManagerInteractor.enrolledFingerprintsInternal =
        mutableListOf(FingerprintViewModel("a", 1, 3L))

      underTest =
        FingerprintSettingsViewModel.FingerprintSettingsViewModelFactory(
            defaultUserId,
            fakeFingerprintManagerInteractor,
            backgroundDispatcher,
            navigationViewModel,
          )
          .create(FingerprintSettingsViewModel::class.java)

      var authAttempt: FingerprintAuthAttemptViewModel? = null
      val job = launch { underTest.authFlow.take(5).collectLatest { authAttempt = it } }

      underTest.shouldAuthenticate(true)
      navigationViewModel.onConfirmDevice(true, 10L)
      advanceTimeBy(400)
      runCurrent()

      assertThat(authAttempt).isNull()
      job.cancel()
    }

  @Test
  fun authenticate_DoesRun_ifNotUdfps() =
    testScope.runTest {
      fakeFingerprintManagerInteractor.sensorProps =
        listOf(
          FingerprintSensorPropertiesInternal(
            0 /* sensorId */,
            SensorProperties.STRENGTH_STRONG,
            5 /* maxEnrollmentsPerUser */,
            emptyList() /* ComponentInfoInternal */,
            FingerprintSensorProperties.TYPE_POWER_BUTTON,
            true /* resetLockoutRequiresHardwareAuthToken */
          )
        )
      fakeFingerprintManagerInteractor.enrolledFingerprintsInternal =
        mutableListOf(FingerprintViewModel("a", 1, 3L))
      val success = FingerprintAuthAttemptViewModel.Success(1)
      fakeFingerprintManagerInteractor.authenticateAttempt = success

      underTest =
        FingerprintSettingsViewModel.FingerprintSettingsViewModelFactory(
            defaultUserId,
            fakeFingerprintManagerInteractor,
            backgroundDispatcher,
            navigationViewModel,
          )
          .create(FingerprintSettingsViewModel::class.java)

      var authAttempt: FingerprintAuthAttemptViewModel? = null

      val job = launch { underTest.authFlow.take(5).collectLatest { authAttempt = it } }
      underTest.shouldAuthenticate(true)
      navigationViewModel.onConfirmDevice(true, 10L)
      advanceTimeBy(400)
      runCurrent()

      assertThat(authAttempt).isEqualTo(success)
      job.cancel()
    }

  @Test
  fun deleteDialog_showAndDismiss() = runTest {
    val fingerprintToDelete = FingerprintViewModel("A", 1, 10L)
    fakeFingerprintManagerInteractor.enrolledFingerprintsInternal = mutableListOf(fingerprintToDelete)

    underTest =
      FingerprintSettingsViewModel.FingerprintSettingsViewModelFactory(
          defaultUserId,
          fakeFingerprintManagerInteractor,
          backgroundDispatcher,
          navigationViewModel,
        )
        .create(FingerprintSettingsViewModel::class.java)

    var dialog: PreferenceViewModel? = null
    val dialogJob = launch { underTest.isShowingDialog.collect { dialog = it } }

    // Move to the ShowSettings state
    navigationViewModel.onConfirmDevice(true, 10L)
    runCurrent()
    underTest.onDeleteClicked(fingerprintToDelete)
    runCurrent()

    assertThat(dialog is PreferenceViewModel.DeleteDialog)
    assertThat(dialog).isEqualTo(PreferenceViewModel.DeleteDialog(fingerprintToDelete))

    underTest.deleteFingerprint(fingerprintToDelete)
    underTest.onDeleteDialogFinished()
    runCurrent()

    assertThat(dialog).isNull()

    dialogJob.cancel()
  }
}
