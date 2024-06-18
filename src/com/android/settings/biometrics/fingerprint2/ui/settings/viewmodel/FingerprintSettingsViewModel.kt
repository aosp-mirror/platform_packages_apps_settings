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
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.FingerprintManagerInteractor
import com.android.settings.biometrics.fingerprint2.lib.model.FingerprintAuthAttemptModel
import com.android.settings.biometrics.fingerprint2.lib.model.FingerprintData
import com.android.systemui.biometrics.shared.model.FingerprintSensorType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.transform
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
  private val _enrolledFingerprints: MutableStateFlow<List<FingerprintData>?> =
    MutableStateFlow(null)

  /** Represents the stream of enrolled fingerprints. */
  val enrolledFingerprints: Flow<List<FingerprintData>> =
    _enrolledFingerprints.asStateFlow().filterNotNull().filterOnlyWhenSettingsIsShown()

  /** Represents the stream of the information of "Add Fingerprint" preference. */
  val addFingerprintPrefInfo: Flow<Pair<Boolean, Int>> =
    _enrolledFingerprints.filterOnlyWhenSettingsIsShown().transform {
      emit(
        Pair(
          fingerprintManagerInteractor.canEnrollFingerprints.first(),
          fingerprintManagerInteractor.maxEnrollableFingerprints.first(),
        )
      )
    }

  /** Represents the stream of visibility of sfps preference. */
  val isSfpsPrefVisible: Flow<Boolean> =
    _enrolledFingerprints.filterOnlyWhenSettingsIsShown().transform {
      emit(fingerprintManagerInteractor.hasSideFps() && !it.isNullOrEmpty())
    }

  private val _isShowingDialog: MutableStateFlow<PreferenceViewModel?> = MutableStateFlow(null)
  val isShowingDialog =
    _isShowingDialog.combine(navigationViewModel.nextStep) { dialogFlow, nextStep ->
      if (nextStep is ShowSettings) {
        return@combine dialogFlow
      } else {
        return@combine null
      }
    }

  private val _consumerShouldAuthenticate: MutableStateFlow<Boolean> = MutableStateFlow(false)

  private val _fingerprintSensorType: Flow<FingerprintSensorType> =
    fingerprintManagerInteractor.sensorPropertiesInternal.filterNotNull().map { it.sensorType }

  private val _sensorNullOrEmpty: Flow<Boolean> =
    fingerprintManagerInteractor.sensorPropertiesInternal.map { it == null }

  private val _isLockedOut: MutableStateFlow<FingerprintAuthAttemptModel.Error?> =
    MutableStateFlow(null)

  private val _authSucceeded: MutableSharedFlow<FingerprintAuthAttemptModel.Success?> =
    MutableSharedFlow()

  private val _attemptsSoFar: MutableStateFlow<Int> = MutableStateFlow(0)
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
        _enrolledFingerprints,
        _isLockedOut,
        _attemptsSoFar,
        _fingerprintSensorType,
        _sensorNullOrEmpty,
      ) {
        dialogShowing,
        step,
        resume,
        fingerprints,
        isLockedOut,
        attempts,
        sensorType,
        sensorNullOrEmpty ->
        if (DEBUG) {
          Log.d(
            TAG,
            "canAuthenticate(isShowingDialog=${dialogShowing != null}," +
              "nextStep=${step}," +
              "resumed=${resume}," +
              "fingerprints=${fingerprints}," +
              "lockedOut=${isLockedOut}," +
              "attempts=${attempts}," +
              "sensorType=${sensorType}" +
              "sensorNullOrEmpty=${sensorNullOrEmpty}",
          )
        }
        if (sensorNullOrEmpty) {
          return@combine false
        }
        if (
          listOf(FingerprintSensorType.UDFPS_ULTRASONIC, FingerprintSensorType.UDFPS_OPTICAL)
            .contains(sensorType)
        ) {
          return@combine false
        }

        if (step != null && step is ShowSettings) {
          if (fingerprints?.isNotEmpty() == true) {
            return@combine dialogShowing == null && isLockedOut == null && resume && attempts < 15
          }
        }
        false
      }
      .sample(400)
      .distinctUntilChanged()

  /** Represents a consistent stream of authentication attempts. */
  val authFlow: Flow<FingerprintAuthAttemptModel> =
    canAuthenticate
      .transformLatest {
        try {
          Log.d(TAG, "canAuthenticate $it")
          while (it && navigationViewModel.nextStep.value is ShowSettings) {
            Log.d(TAG, "canAuthenticate authing")
            attemptingAuth()
            when (val authAttempt = fingerprintManagerInteractor.authenticate()) {
              is FingerprintAuthAttemptModel.Success -> {
                onAuthSuccess(authAttempt)
                emit(authAttempt)
              }
              is FingerprintAuthAttemptModel.Error -> {
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

  init {
    viewModelScope.launch {
      navigationViewModel.nextStep.filterNotNull().collect {
        _isShowingDialog.update { null }
        if (it is ShowSettings) {
          // reset state
          updateEnrolledFingerprints()
        }
      }
    }
  }

  /** The rename dialog has finished */
  fun onRenameDialogFinished() {
    _isShowingDialog.update { null }
  }

  /** The delete dialog has finished */
  fun onDeleteDialogFinished() {
    _isShowingDialog.update { null }
  }

  override fun toString(): String {
    return "userId: $userId\n" + "enrolledFingerprints: ${_enrolledFingerprints.value}\n"
  }

  /** The fingerprint delete button has been clicked. */
  fun onDeleteClicked(fingerprintViewModel: FingerprintData) {
    viewModelScope.launch {
      if (_isShowingDialog.value == null || navigationViewModel.nextStep.value != ShowSettings) {
        _isShowingDialog.tryEmit(PreferenceViewModel.DeleteDialog(fingerprintViewModel))
      } else {
        Log.d(TAG, "Ignoring onDeleteClicked due to dialog showing ${_isShowingDialog.value}")
      }
    }
  }

  /** The rename fingerprint dialog has been clicked. */
  fun onPrefClicked(fingerprintViewModel: FingerprintData) {
    viewModelScope.launch {
      if (_isShowingDialog.value == null || navigationViewModel.nextStep.value != ShowSettings) {
        _isShowingDialog.tryEmit(PreferenceViewModel.RenameDialog(fingerprintViewModel))
      } else {
        Log.d(TAG, "Ignoring onPrefClicked due to dialog showing ${_isShowingDialog.value}")
      }
    }
  }

  /** A request to delete a fingerprint */
  fun deleteFingerprint(fp: FingerprintData) {
    viewModelScope.launch(backgroundDispatcher) {
      if (fingerprintManagerInteractor.removeFingerprint(fp)) {
        updateEnrolledFingerprints()
      }
    }
  }

  /** A request to rename a fingerprint */
  fun renameFingerprint(fp: FingerprintData, newName: String) {
    viewModelScope.launch {
      fingerprintManagerInteractor.renameFingerprint(fp, newName)
      updateEnrolledFingerprints()
    }
  }

  private fun attemptingAuth() {
    _attemptsSoFar.update { it + 1 }
  }

  private suspend fun onAuthSuccess(success: FingerprintAuthAttemptModel.Success) {
    _authSucceeded.emit(success)
    _attemptsSoFar.update { 0 }
  }

  private fun lockout(attemptViewModel: FingerprintAuthAttemptModel.Error) {
    _isLockedOut.update { attemptViewModel }
  }

  private suspend fun updateEnrolledFingerprints() {
    _enrolledFingerprints.update { fingerprintManagerInteractor.enrolledFingerprints.first() }
  }

  /** Used to indicate whether the consumer of the view model is ready for authentication. */
  fun shouldAuthenticate(authenticate: Boolean) {
    _consumerShouldAuthenticate.update { authenticate }
  }

  private fun <T> Flow<T>.filterOnlyWhenSettingsIsShown() =
    combineTransform(navigationViewModel.nextStep) { value, currStep ->
      if (currStep != null && currStep is ShowSettings) {
        emit(value)
      }
    }

  class FingerprintSettingsViewModelFactory(
    private val userId: Int,
    private val interactor: FingerprintManagerInteractor,
    private val backgroundDispatcher: CoroutineDispatcher,
    private val navigationViewModel: FingerprintSettingsNavigationViewModel,
  ) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

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

private inline fun <T1, T2, T3, T4, T5, T6, T7, T8, R> combine(
  flow: Flow<T1>,
  flow2: Flow<T2>,
  flow3: Flow<T3>,
  flow4: Flow<T4>,
  flow5: Flow<T5>,
  flow6: Flow<T6>,
  flow7: Flow<T7>,
  flow8: Flow<T8>,
  crossinline transform: suspend (T1, T2, T3, T4, T5, T6, T7, T8) -> R,
): Flow<R> {
  return combine(flow, flow2, flow3, flow4, flow5, flow6, flow7, flow8) { args: Array<*> ->
    @Suppress("UNCHECKED_CAST")
    transform(
      args[0] as T1,
      args[1] as T2,
      args[2] as T3,
      args[3] as T4,
      args[4] as T5,
      args[5] as T6,
      args[6] as T7,
      args[7] as T8,
    )
  }
}
