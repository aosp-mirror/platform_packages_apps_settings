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

package com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.rfps.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.update

private const val touchesToShowDialog = 3
/**
 * This class is responsible for counting the number of touches on the fingerprint icon, and if this
 * number reaches a threshold it will produce an action via [shouldShowDialog] to indicate the ui
 * should show a dialog.
 */
class RFPSIconTouchViewModel : ViewModel() {

  /** Keeps the number of times a user has touches the fingerprint icon. */
  private val _touches: MutableStateFlow<Int> = MutableStateFlow(0)

  /**
   * Whether or not the UI should be showing the dialog. By making this SharingStarted.Eagerly the
   * first event 0 % 3 == 0 will fire as soon as this view model is created, so it should be ignored
   * and work as intended.
   */
  val shouldShowDialog: Flow<Boolean> =
    _touches
      .transform { numTouches -> emit((numTouches % touchesToShowDialog) == 0) }
      .shareIn(viewModelScope, SharingStarted.Eagerly, 0)

  /** Indicates a user has tapped on the fingerprint icon. */
  fun userTouchedFingerprintIcon() {
    _touches.update { _touches.value + 1 }
  }

  class RFPSIconTouchViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return RFPSIconTouchViewModel() as T
    }
  }
}
