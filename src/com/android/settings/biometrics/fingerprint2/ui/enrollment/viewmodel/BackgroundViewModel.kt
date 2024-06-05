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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** A class for determining if the application is in the background or not. */
class BackgroundViewModel : ViewModel() {

  private val _background = MutableStateFlow(false)
  /** When true, the application is in background, else false */
  val background = _background.asStateFlow()

  /** Indicates that the application has been put in the background. */
  fun wentToBackground() {
    _background.update { true }
  }

  /** Indicates that the application has been brought to the foreground. */
  fun inForeground() {
    _background.update { false }
  }

  class BackgroundViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return BackgroundViewModel() as T
    }
  }
}
