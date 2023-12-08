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

package com.android.settings.biometrics.fingerprint2.ui.viewmodel

import android.hardware.fingerprint.FingerprintManager
import android.hardware.fingerprint.FingerprintSensorProperties.TYPE_UDFPS_OPTICAL
import android.hardware.fingerprint.FingerprintSensorProperties.TYPE_UDFPS_ULTRASONIC
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.settings.biometrics.fingerprint2.domain.interactor.FingerprintManagerInteractor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "FingerprintSettingsViewModel"
private const val DEBUG = false

/** Models the UI state for fingerprint settings. */
class FingerprintSettingsViewModel(
  private val userId: Int,
  private val fingerprintManagerInteractor: FingerprintManagerInteractor,
  private val backgroundDispatcher: CoroutineDispatcher,
  private val navigationViewModel: FingerprintSettingsNavigationViewModel,
) : ViewModel() {

  private val _consumerShouldAuthenticate: MutableStateFlow<Boolean> = MutableStateFlow(false)

  private val fingerprintSensorPropertiesInternal:
    MutableStateFlow<List<FingerprintSensorPropertiesInternal>?> =
    MutableStateFlow(null)

  private val _isShowingDialog: MutableStateFlow<PreferenceViewModel?> = MutableStateFlow(null)
  val isShowingDialog =
    _isShowingDialog.combine(navigationViewModel.nextStep) { dialogFlow, nextStep ->
      if (nextStep is ShowSettings) {
        return@combine dialogFlow
      } else {
        return@combine null
      }
    }

  init {
    viewModelScope.launch {
      fingerprintSensorPropertiesInternal.update {
        fingerprintManagerInteractor.sensorPropertiesInternal()
      }
    }

    viewModelScope.launch {
      navigationViewModel.nextStep.filterNotNull().collect {
        _isShowingDialog.update { null }
        if (it is ShowSettings) {
          // reset state
          updateSettingsData()
        }
      }
    }
  }

  private val _fingerprintStateViewModel: MutableStateFlow<FingerprintStateViewModel?> =
    MutableStateFlow(null)
  val fingerprintState: Flow<FingerprintStateViewModel?> =
    _fingerprintStateViewModel.combineTransform(navigationViewModel.nextStep) {
      settingsShowingViewModel,
      currStep ->
      if (currStep != null && currStep is ShowSettings) {
        emit(settingsShowingViewModel)
      }
    }

  private val _isLockedOut: MutableStateFlow<FingerprintAuthAttemptViewModel.Error?> =
    MutableStateFlow(null)

  private val _authSucceeded: MutableSharedFlow<FingerprintAuthAttemptViewModel.Success?> =
    MutableSharedFlow()

  private val attemptsSoFar: MutableStateFlow<Int> = MutableStateFlow(0)

  /**
   * This is a very tricky flow. The current fingerprint manager APIs are not robust, and a proper
   * implementation would take quite a lot of code to implement, it might be easier to rewrite
   * FingerprintManager.
   *
   * The hack to note is the sample(400), if we call authentications in too close of proximity
   * without waiting for a response, the fingerprint manager will send us the results of the
   * previous attempt.
   */
  private val canAuthenticate: Flow<Boolean> =
    combine(
        _isShowingDialog,
        navigationViewModel.nextStep,
        _consumerShouldAuthenticate,
        _fingerprintStateViewModel,
        _isLockedOut,
        attemptsSoFar,
        fingerprintSensorPropertiesInternal
      ) { dialogShowing, step, resume, fingerprints, isLockedOut, attempts, sensorProps ->
        if (DEBUG) {
          Log.d(
            TAG,
            "canAuthenticate(isShowingDialog=${dialogShowing != null}," +
              "nextStep=${step}," +
              "resumed=${resume}," +
              "fingerprints=${fingerprints}," +
              "lockedOut=${isLockedOut}," +
              "attempts=${attempts}," +
              "sensorProps=${sensorProps}"
          )
        }
        if (sensorProps.isNullOrEmpty()) {
          return@combine false
        }
        val sensorType = sensorProps[0].sensorType
        if (listOf(TYPE_UDFPS_OPTICAL, TYPE_UDFPS_ULTRASONIC).contains(sensorType)) {
          return@combine false
        }

        if (step != null && step is ShowSettings) {
          if (fingerprints?.fingerprintViewModels?.isNotEmpty() == true) {
            return@combine dialogShowing == null && isLockedOut == null && resume && attempts < 15
          }
        }
        false
      }
      .sample(400)
      .distinctUntilChanged()

  /** Represents a consistent stream of authentication attempts. */
  val authFlow: Flow<FingerprintAuthAttemptViewModel> =
    canAuthenticate
      .transformLatest {
        try {
          Log.d(TAG, "canAuthenticate $it")
          while (it && navigationViewModel.nextStep.value is ShowSettings) {
            Log.d(TAG, "canAuthenticate authing")
            attemptingAuth()
            when (val authAttempt = fingerprintManagerInteractor.authenticate()) {
              is FingerprintAuthAttemptViewModel.Success -> {
                onAuthSuccess(authAttempt)
                emit(authAttempt)
              }
              is FingerprintAuthAttemptViewModel.Error -> {
                if (authAttempt.error == FingerprintManager.FINGERPRINT_ERROR_LOCKOUT) {
                  lockout(authAttempt)
                  emit(authAttempt)
                  return@transformLatest
                }
              }
            }
          }
        } catch (exception: Exception) {
          Log.d(TAG, "shouldAuthenticate exception $exception")
        }
      }
      .flowOn(backgroundDispatcher)

  /** The rename dialog has finished */
  fun onRenameDialogFinished() {
    _isShowingDialog.update { null }
  }

  /** The delete dialog has finished */
  fun onDeleteDialogFinished() {
    _isShowingDialog.update { null }
  }

  override fun toString(): String {
    return "userId: $userId\n" + "fingerprintState: ${_fingerprintStateViewModel.value}\n"
  }

  /** The fingerprint delete button has been clicked. */
  fun onDeleteClicked(fingerprintViewModel: FingerprintViewModel) {
    viewModelScope.launch {
      if (_isShowingDialog.value == null || navigationViewModel.nextStep.value != ShowSettings) {
        _isShowingDialog.tryEmit(PreferenceViewModel.DeleteDialog(fingerprintViewModel))
      } else {
        Log.d(TAG, "Ignoring onDeleteClicked due to dialog showing ${_isShowingDialog.value}")
      }
    }
  }

  /** The rename fingerprint dialog has been clicked. */
  fun onPrefClicked(fingerprintViewModel: FingerprintViewModel) {
    viewModelScope.launch {
      if (_isShowingDialog.value == null || navigationViewModel.nextStep.value != ShowSettings) {
        _isShowingDialog.tryEmit(PreferenceViewModel.RenameDialog(fingerprintViewModel))
      } else {
        Log.d(TAG, "Ignoring onPrefClicked due to dialog showing ${_isShowingDialog.value}")
      }
    }
  }

  /** A request to delete a fingerprint */
  fun deleteFingerprint(fp: FingerprintViewModel) {
    viewModelScope.launch(backgroundDispatcher) {
      if (fingerprintManagerInteractor.removeFingerprint(fp)) {
        updateSettingsData()
      }
    }
  }

  /** A request to rename a fingerprint */
  fun renameFingerprint(fp: FingerprintViewModel, newName: String) {
    viewModelScope.launch {
      fingerprintManagerInteractor.renameFingerprint(fp, newName)
      updateSettingsData()
    }
  }

  private fun attemptingAuth() {
    attemptsSoFar.update { it + 1 }
  }

  private suspend fun onAuthSuccess(success: FingerprintAuthAttemptViewModel.Success) {
    _authSucceeded.emit(success)
    attemptsSoFar.update { 0 }
  }

  private fun lockout(attemptViewModel: FingerprintAuthAttemptViewModel.Error) {
    _isLockedOut.update { attemptViewModel }
  }

  /**
   * This function is sort of a hack, it's used whenever we want to check for fingerprint state
   * updates.
   */
  private suspend fun updateSettingsData() {
    Log.d(TAG, "update settings data called")
    val fingerprints = fingerprintManagerInteractor.enrolledFingerprints.last()
    val canEnrollFingerprint =
      fingerprintManagerInteractor.canEnrollFingerprints(fingerprints.size).last()
    val maxFingerprints = fingerprintManagerInteractor.maxEnrollableFingerprints.last()
    val hasSideFps = fingerprintManagerInteractor.hasSideFps()
    val pressToAuthEnabled = fingerprintManagerInteractor.pressToAuthEnabled()
    _fingerprintStateViewModel.update {
      FingerprintStateViewModel(
        fingerprints,
        canEnrollFingerprint,
        maxFingerprints,
        hasSideFps,
        pressToAuthEnabled
      )
    }
  }

  /** Used to indicate whether the consumer of the view model is ready for authentication. */
  fun shouldAuthenticate(authenticate: Boolean) {
    _consumerShouldAuthenticate.update { authenticate }
  }

  class FingerprintSettingsViewModelFactory(
    private val userId: Int,
    private val interactor: FingerprintManagerInteractor,
    private val backgroundDispatcher: CoroutineDispatcher,
    private val navigationViewModel: FingerprintSettingsNavigationViewModel,
  ) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
      modelClass: Class<T>,
    ): T {

      return FingerprintSettingsViewModel(
        userId,
        interactor,
        backgroundDispatcher,
        navigationViewModel,
      )
        as T
    }
  }
}

private inline fun <T1, T2, T3, T4, T5, T6, T7, R> combine(
  flow: Flow<T1>,
  flow2: Flow<T2>,
  flow3: Flow<T3>,
  flow4: Flow<T4>,
  flow5: Flow<T5>,
  flow6: Flow<T6>,
  flow7: Flow<T7>,
  crossinline transform: suspend (T1, T2, T3, T4, T5, T6, T7) -> R
): Flow<R> {
  return combine(flow, flow2, flow3, flow4, flow5, flow6, flow7) { args: Array<*> ->
    @Suppress("UNCHECKED_CAST")
    transform(
      args[0] as T1,
      args[1] as T2,
      args[2] as T3,
      args[3] as T4,
      args[4] as T5,
      args[5] as T6,
      args[6] as T7,
    )
  }
}
