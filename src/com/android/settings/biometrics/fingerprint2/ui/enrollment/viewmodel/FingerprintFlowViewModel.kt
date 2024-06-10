/*
 * Copyright (C) 2024 The Android Open Source Project
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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.android.settings.biometrics.fingerprint2.lib.model.FingerprintFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update

class FingerprintFlowViewModel() : ViewModel() {
  val _mutableFingerprintFlow: MutableStateFlow<FingerprintFlow?> = MutableStateFlow(null)
  val fingerprintFlow: Flow<FingerprintFlow?> =
    _mutableFingerprintFlow.shareIn(viewModelScope, SharingStarted.Eagerly, 1)

  /** Used to set the fingerprint flow type */
  fun updateFlowType(fingerprintFlowType: FingerprintFlow) {
    _mutableFingerprintFlow.update { fingerprintFlowType }
  }

  companion object {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
      initializer { FingerprintFlowViewModel() }
    }
  }
}
