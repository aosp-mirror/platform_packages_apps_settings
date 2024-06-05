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

package com.android.settings.biometrics.fingerprint2.ui.settings.viewmodel

import android.hardware.fingerprint.FingerprintManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.settings.biometrics.BiometricEnrollBase
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.FingerprintManagerInteractor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A Viewmodel that represents the navigation of the FingerprintSettings activity. */
class FingerprintSettingsNavigationViewModel(
  private val userId: Int,
  private val fingerprintManagerInteractor: FingerprintManagerInteractor,
  private val backgroundDispatcher: CoroutineDispatcher,
  tokenInit: ByteArray?,
  challengeInit: Long?,
) : ViewModel() {

  private var token = tokenInit
  private var challenge = challengeInit

  private val _nextStep: MutableStateFlow<NextStepViewModel?> = MutableStateFlow(null)

  /** This flow represents the high level state for the FingerprintSettingsV2Fragment. */
  val nextStep: StateFlow<NextStepViewModel?> = _nextStep.asStateFlow()

  init {
    if (challengeInit == null || tokenInit == null) {
      _nextStep.update { LaunchConfirmDeviceCredential(userId) }
    } else {
      viewModelScope.launch {
        if (fingerprintManagerInteractor.enrolledFingerprints.last().isEmpty()) {
          _nextStep.update { EnrollFirstFingerprint(userId, null, challenge, token) }
        } else {
          showSettingsHelper()
        }
      }
    }
  }

  /** Used to indicate that FingerprintSettings is complete. */
  fun finish() {
    _nextStep.update { null }
  }

  /** Used to finish settings in certain cases. */
  fun maybeFinishActivity(changingConfig: Boolean) {
    val isConfirmingOrEnrolling =
      _nextStep.value is LaunchConfirmDeviceCredential ||
        _nextStep.value is EnrollAdditionalFingerprint ||
        _nextStep.value is EnrollFirstFingerprint ||
        _nextStep.value is LaunchedActivity
    if (!isConfirmingOrEnrolling && !changingConfig)
      _nextStep.update {
        FinishSettingsWithResult(BiometricEnrollBase.RESULT_TIMEOUT, "onStop finishing settings")
      }
  }

  /** Used to indicate that we have launched another activity and we should await its result. */
  fun setStepToLaunched() {
    _nextStep.update { LaunchedActivity }
  }

  /** Indicates a successful enroll has occurred */
  fun onEnrollSuccess() {
    showSettingsHelper()
  }

  /** Add fingerprint clicked */
  fun onAddFingerprintClicked() {
    _nextStep.update { EnrollAdditionalFingerprint(userId, token) }
  }

  /** Enrolling of an additional fingerprint failed */
  fun onEnrollAdditionalFailure() {
    launchFinishSettings("Failed to enroll additional fingerprint")
  }

  /** The first fingerprint enrollment failed */
  fun onEnrollFirstFailure(reason: String) {
    launchFinishSettings(reason)
  }

  /** The first fingerprint enrollment failed with a result code */
  fun onEnrollFirstFailure(reason: String, resultCode: Int) {
    launchFinishSettings(reason, resultCode)
  }

  /** Notifies that a users first enrollment succeeded. */
  fun onEnrollFirst(theToken: ByteArray?, theChallenge: Long?) {
    if (theToken == null) {
      launchFinishSettings("Error, empty token")
      return
    }
    if (theChallenge == null) {
      launchFinishSettings("Error, empty keyChallenge")
      return
    }
    token = theToken
    challenge = theChallenge

    showSettingsHelper()
  }

  /**
   * Indicates to the view model that a confirm device credential action has been completed with a
   * [theGateKeeperPasswordHandle] which will be used for [FingerprintManager] operations such as
   * [FingerprintManager.enroll].
   */
  suspend fun onConfirmDevice(wasSuccessful: Boolean, theGateKeeperPasswordHandle: Long?) {
    if (!wasSuccessful) {
      launchFinishSettings("ConfirmDeviceCredential was unsuccessful")
      return
    }
    if (theGateKeeperPasswordHandle == null) {
      launchFinishSettings("ConfirmDeviceCredential gatekeeper password was null")
      return
    }

    launchEnrollNextStep(theGateKeeperPasswordHandle)
  }

  private fun showSettingsHelper() {
    _nextStep.update { ShowSettings }
  }

  private suspend fun launchEnrollNextStep(gateKeeperPasswordHandle: Long?) {
    fingerprintManagerInteractor.enrolledFingerprints.collect {
      if (it.isEmpty()) {
        _nextStep.update { EnrollFirstFingerprint(userId, gateKeeperPasswordHandle, null, null) }
      } else {
        viewModelScope.launch(backgroundDispatcher) {
          val challengePair =
            fingerprintManagerInteractor.generateChallenge(gateKeeperPasswordHandle!!)
          challenge = challengePair.first
          token = challengePair.second

          showSettingsHelper()
        }
      }
    }
  }

  private fun launchFinishSettings(reason: String) {
    _nextStep.update { FinishSettings(reason) }
  }

  private fun launchFinishSettings(reason: String, errorCode: Int) {
    _nextStep.update { FinishSettingsWithResult(errorCode, reason) }
  }

  class FingerprintSettingsNavigationModelFactory(
    private val userId: Int,
    private val interactor: FingerprintManagerInteractor,
    private val backgroundDispatcher: CoroutineDispatcher,
    private val token: ByteArray?,
    private val challenge: Long?,
  ) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

      return FingerprintSettingsNavigationViewModel(
        userId,
        interactor,
        backgroundDispatcher,
        token,
        challenge,
      )
        as T
    }
  }
}
