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

import android.hardware.biometrics.ComponentInfoInternal
import android.hardware.biometrics.SensorLocationInternal
import android.hardware.biometrics.SensorProperties
import android.hardware.fingerprint.FingerprintSensorProperties
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.android.settings.biometrics.fingerprint2.lib.model.FingerprintAuthAttemptModel
import com.android.settings.biometrics.fingerprint2.lib.model.FingerprintData
import com.android.settings.biometrics.fingerprint2.ui.settings.viewmodel.FingerprintSettingsNavigationViewModel
import com.android.settings.biometrics.fingerprint2.ui.settings.viewmodel.FingerprintSettingsViewModel
import com.android.settings.biometrics.fingerprint2.ui.settings.viewmodel.PreferenceViewModel
import com.android.settings.testutils2.FakeFingerprintManagerInteractor
import com.android.systemui.biometrics.shared.model.toFingerprintSensor
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
          backgroundDispatcher,
          null,
          null,
          fakeFingerprintManagerInteractor,
          fakeFingerprintManagerInteractor,
        )
        .create(FingerprintSettingsNavigationViewModel::class.java)

    underTest =
      FingerprintSettingsViewModel.FingerprintSettingsViewModelFactory(
          defaultUserId,
          backgroundDispatcher,
          navigationViewModel,
          fakeFingerprintManagerInteractor,
          fakeFingerprintManagerInteractor,
          fakeFingerprintManagerInteractor,
          fakeFingerprintManagerInteractor,
          fakeFingerprintManagerInteractor,
          fakeFingerprintManagerInteractor,
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
      fakeFingerprintManagerInteractor.sensorProp =
        FingerprintSensorPropertiesInternal(
            0 /* sensorId */,
            SensorProperties.STRENGTH_STRONG,
            5 /* maxEnrollmentsPerUser */,
            listOf<ComponentInfoInternal>(),
            FingerprintSensorProperties.TYPE_UDFPS_OPTICAL,
            false /* halControlsIllumination */,
            true /* resetLockoutRequiresHardwareAuthToken */,
            listOf<SensorLocationInternal>(SensorLocationInternal.DEFAULT),
          )
          .toFingerprintSensor()

      fakeFingerprintManagerInteractor.enrolledFingerprintsInternal =
        mutableListOf(FingerprintData("a", 1, 3L))

      recreateSettingsViewModel()

      var authAttempt: FingerprintAuthAttemptModel? = null
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
      fakeFingerprintManagerInteractor.sensorProp =
        FingerprintSensorPropertiesInternal(
            0 /* sensorId */,
            SensorProperties.STRENGTH_STRONG,
            5 /* maxEnrollmentsPerUser */,
            listOf<ComponentInfoInternal>(),
            FingerprintSensorProperties.TYPE_UDFPS_ULTRASONIC,
            false /* halControlsIllumination */,
            true /* resetLockoutRequiresHardwareAuthToken */,
            listOf<SensorLocationInternal>(SensorLocationInternal.DEFAULT),
          )
          .toFingerprintSensor()

      fakeFingerprintManagerInteractor.enrolledFingerprintsInternal =
        mutableListOf(FingerprintData("a", 1, 3L))

      recreateSettingsViewModel()

      var authAttempt: FingerprintAuthAttemptModel? = null
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
      fakeFingerprintManagerInteractor.sensorProp =
        FingerprintSensorPropertiesInternal(
            0 /* sensorId */,
            SensorProperties.STRENGTH_STRONG,
            5 /* maxEnrollmentsPerUser */,
            listOf<ComponentInfoInternal>(),
            FingerprintSensorProperties.TYPE_POWER_BUTTON,
            false /* halControlsIllumination */,
            true /* resetLockoutRequiresHardwareAuthToken */,
            listOf<SensorLocationInternal>(SensorLocationInternal.DEFAULT),
          )
          .toFingerprintSensor()

      fakeFingerprintManagerInteractor.enrolledFingerprintsInternal =
        mutableListOf(FingerprintData("a", 1, 3L))
      val success = FingerprintAuthAttemptModel.Success(1)
      fakeFingerprintManagerInteractor.authenticateAttempt = success

      recreateSettingsViewModel()

      var authAttempt: FingerprintAuthAttemptModel? = null

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
    val fingerprintToDelete = FingerprintData("A", 1, 10L)
    fakeFingerprintManagerInteractor.enrolledFingerprintsInternal =
      mutableListOf(fingerprintToDelete)

    recreateSettingsViewModel()

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

  @Test
  fun renameDialog_showAndDismiss() = runTest {
    val fingerprintToRename = FingerprintData("World", 1, 10L)
    fakeFingerprintManagerInteractor.enrolledFingerprintsInternal =
      mutableListOf(fingerprintToRename)

    recreateSettingsViewModel()

    var dialog: PreferenceViewModel? = null
    val dialogJob = launch { underTest.isShowingDialog.collect { dialog = it } }

    // Move to the ShowSettings state
    navigationViewModel.onConfirmDevice(true, 10L)
    runCurrent()
    underTest.onPrefClicked(fingerprintToRename)
    runCurrent()

    assertThat(dialog is PreferenceViewModel.DeleteDialog)
    assertThat(dialog).isEqualTo(PreferenceViewModel.RenameDialog(fingerprintToRename))

    underTest.renameFingerprint(fingerprintToRename, "Hello")
    underTest.onRenameDialogFinished()
    runCurrent()

    assertThat(dialog).isNull()
    assertThat(fakeFingerprintManagerInteractor.enrolledFingerprintsInternal.first().name)
      .isEqualTo("Hello")

    dialogJob.cancel()
  }

  @Test
  fun testTwoDialogsCannotShow_atSameTime() = runTest {
    val fingerprintToDelete = FingerprintData("A", 1, 10L)
    fakeFingerprintManagerInteractor.enrolledFingerprintsInternal =
      mutableListOf(fingerprintToDelete)

    recreateSettingsViewModel()

    var dialog: PreferenceViewModel? = null
    val dialogJob = launch { underTest.isShowingDialog.collect { dialog = it } }

    // Move to the ShowSettings state
    navigationViewModel.onConfirmDevice(true, 10L)
    runCurrent()
    underTest.onDeleteClicked(fingerprintToDelete)
    runCurrent()

    assertThat(dialog is PreferenceViewModel.DeleteDialog)
    assertThat(dialog).isEqualTo(PreferenceViewModel.DeleteDialog(fingerprintToDelete))

    underTest.onPrefClicked(fingerprintToDelete)
    runCurrent()
    assertThat(dialog is PreferenceViewModel.DeleteDialog)
    assertThat(dialog).isEqualTo(PreferenceViewModel.DeleteDialog(fingerprintToDelete))

    dialogJob.cancel()
  }

  @Test
  fun authenticatePauses_whenPaused() =
    testScope.runTest {
      val fingerprints = setupAuth()
      val success = FingerprintAuthAttemptModel.Success(fingerprints.first().fingerId)

      var authAttempt: FingerprintAuthAttemptModel? = null

      val job = launch { underTest.authFlow.take(5).collectLatest { authAttempt = it } }

      underTest.shouldAuthenticate(true)
      navigationViewModel.onConfirmDevice(true, 10L)

      advanceTimeBy(400)
      runCurrent()
      assertThat(authAttempt).isEqualTo(success)

      fakeFingerprintManagerInteractor.authenticateAttempt = FingerprintAuthAttemptModel.Success(10)
      underTest.shouldAuthenticate(false)
      advanceTimeBy(400)
      runCurrent()

      // The most recent auth attempt shouldn't have changed.
      assertThat(authAttempt).isEqualTo(success)
      job.cancel()
    }

  @Test
  fun dialog_pausesAuth() =
    testScope.runTest {
      val fingerprints = setupAuth()

      var authAttempt: FingerprintAuthAttemptModel? = null
      val job = launch { underTest.authFlow.take(1).collectLatest { authAttempt = it } }
      underTest.shouldAuthenticate(true)
      navigationViewModel.onConfirmDevice(true, 10L)

      underTest.onPrefClicked(fingerprints[0])
      advanceTimeBy(400)

      job.cancel()
      assertThat(authAttempt).isEqualTo(null)
    }

  @Test
  fun cannotAuth_when_notShowingSettings() =
    testScope.runTest {
      val fingerprints = setupAuth()

      var authAttempt: FingerprintAuthAttemptModel? = null
      val job = launch { underTest.authFlow.take(1).collectLatest { authAttempt = it } }
      underTest.shouldAuthenticate(true)
      navigationViewModel.onConfirmDevice(true, 10L)

      // This should cause the state to change to FingerprintEnrolling
      navigationViewModel.onAddFingerprintClicked()
      advanceTimeBy(400)

      job.cancel()
      assertThat(authAttempt).isEqualTo(null)
    }

  private fun recreateSettingsViewModel() {
    underTest =
      FingerprintSettingsViewModel.FingerprintSettingsViewModelFactory(
          defaultUserId,
          backgroundDispatcher,
          navigationViewModel,
          fakeFingerprintManagerInteractor,
          fakeFingerprintManagerInteractor,
          fakeFingerprintManagerInteractor,
          fakeFingerprintManagerInteractor,
          fakeFingerprintManagerInteractor,
          fakeFingerprintManagerInteractor,
        )
        .create(FingerprintSettingsViewModel::class.java)
  }

  private fun setupAuth(): MutableList<FingerprintData> {
    fakeFingerprintManagerInteractor.sensorProp =
      FingerprintSensorPropertiesInternal(
          0 /* sensorId */,
          SensorProperties.STRENGTH_STRONG,
          5 /* maxEnrollmentsPerUser */,
          listOf<ComponentInfoInternal>(),
          FingerprintSensorProperties.TYPE_POWER_BUTTON,
          false /* halControlsIllumination */,
          true /* resetLockoutRequiresHardwareAuthToken */,
          listOf<SensorLocationInternal>(SensorLocationInternal.DEFAULT),
        )
        .toFingerprintSensor()

    val fingerprints = mutableListOf(FingerprintData("a", 1, 3L), FingerprintData("b", 2, 5L))
    fakeFingerprintManagerInteractor.enrolledFingerprintsInternal = fingerprints
    val success = FingerprintAuthAttemptModel.Success(1)
    fakeFingerprintManagerInteractor.authenticateAttempt = success

    recreateSettingsViewModel()

    return fingerprints
  }
}
