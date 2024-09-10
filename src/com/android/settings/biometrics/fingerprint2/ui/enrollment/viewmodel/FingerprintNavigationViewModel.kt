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

package com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.android.settings.SettingsApplication
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.SensorInteractor
import com.android.settings.biometrics.fingerprint2.lib.model.FingerprintFlow
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintNavigationStep.Finish
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintNavigationStep.TransitionStep
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintNavigationStep.UiStep
import java.lang.NullPointerException
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * This class is essentially a wrapper around [FingerprintNavigationStep] that will be used by
 * fragments/viewmodels that want to consume these events. It should provide no additional
 * functionality beyond what is available in [FingerprintNavigationStep].
 */
class FingerprintNavigationViewModel(sensorInteractor: SensorInteractor) :
  ViewModel() {

  private val _flowInternal: MutableStateFlow<FingerprintFlow?> = MutableStateFlow(null)
  private val _hasConfirmedDeviceCredential: MutableStateFlow<Boolean> = MutableStateFlow(false)
  private val _navStateInternal: StateFlow<NavigationState?> =
    combine(
        _flowInternal,
        _hasConfirmedDeviceCredential,
        sensorInteractor.sensorPropertiesInternal,
      ) { flow, hasConfirmed, sensorType ->
        if (flow == null || sensorType == null) {
          return@combine null
        }
        return@combine NavigationState(flow, hasConfirmed, sensorType)
      }
      .stateIn(viewModelScope, SharingStarted.Eagerly, null)

  private var _currentStep =
    MutableStateFlow<FingerprintNavigationStep?>(FingerprintNavigationStep.Init)

  private var _navigateTo: MutableStateFlow<UiStep?> = MutableStateFlow(null)
  val navigateTo: Flow<UiStep?> = _navigateTo.asStateFlow()

  /**
   * This indicates a navigation event should occur. Navigation depends on navStateInternal being
   * present.
   */
  val currentStep: Flow<FingerprintNavigationStep> =
    _currentStep.filterNotNull().combineTransform(_navStateInternal.filterNotNull()) { navigation, _
      ->
      emit(navigation)
    }

  private var _finishState = MutableStateFlow<Finish?>(null)

  /** This indicates the activity should finish. */
  val shouldFinish: Flow<Finish?> = _finishState.asStateFlow()

  private var _currentScreen = MutableStateFlow<UiStep?>(null)

  /** This indicates what screen should currently be presenting to the user. */
  val currentScreen: Flow<UiStep?> = _currentScreen.asStateFlow()

  /** Updates the type of flow the navigation should begin */
  fun updateFingerprintFlow(flow: FingerprintFlow) {
    _flowInternal.update { flow }
  }

  /** Indicates if we have confirmed device credential */
  fun hasConfirmedDeviceCredential(hasConfirmedDeviceCredential: Boolean) {
    _hasConfirmedDeviceCredential.update { hasConfirmedDeviceCredential }
  }

  /** See [updateInternal] for more details */
  fun update(action: FingerprintAction, caller: KClass<*>, debugStr: String) {
    Log.d(TAG, "$caller.update($action) $debugStr")
    val currentStep = _currentStep.value
    val isUiStep = currentStep is UiStep && caller is UiStep
    if (currentStep == null) {
      throw NullPointerException("current step is null")
    }
    if (isUiStep && currentStep::class != caller) {
      throw IllegalAccessError(
        "Error $currentStep != $caller, $caller should not be sending any events at this time"
      )
    }
    val navState = _navStateInternal.value
    if (navState == null) {
      throw NullPointerException("nav state is null")
    }
    val nextStep = currentStep.update(navState, action) ?: return
    Log.d(TAG, "nextStep=$nextStep")
    // Whenever an state update occurs, everything should be cleared.
    _currentStep.update { nextStep }
    _finishState.update { null }
    _currentScreen.update { null }

    when (nextStep) {
      is TransitionStep -> {
        _navigateTo.update { nextStep.nextUiStep }
      }
      is Finish -> {
        _finishState.update { nextStep }
      }
      is UiStep -> {
        _currentScreen.update { nextStep }
      }
    }
  }

  companion object {
    private const val TAG = "FingerprintNavigationViewModel"
    val Factory: ViewModelProvider.Factory = viewModelFactory {
      initializer {
        val settingsApplication =
          this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as SettingsApplication
        val biometricEnvironment = settingsApplication.biometricEnvironment
        FingerprintNavigationViewModel(biometricEnvironment!!.createSensorPropertiesInteractor())
      }
    }
  }
}
