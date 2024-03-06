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

import android.content.Context
import android.content.res.Configuration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.android.systemui.unfold.compat.ScreenSizeFoldProvider
import com.android.systemui.unfold.updates.FoldProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Represents all of the information on fold state. */
class FoldStateViewModel(context: Context) : ViewModel() {

  private val screenSizeFoldProvider = ScreenSizeFoldProvider(context)

  /** A flow that contains the fold state info */
  val isFolded: Flow<Boolean> = callbackFlow {
    val foldStateListener =
      object : FoldProvider.FoldCallback {
        override fun onFoldUpdated(isFolded: Boolean) {
          trySend(isFolded)
        }
      }
    screenSizeFoldProvider.registerCallback(foldStateListener, context.mainExecutor)
    awaitClose { screenSizeFoldProvider.unregisterCallback(foldStateListener) }
  }

  fun onConfigurationChange(newConfig: Configuration) {
    screenSizeFoldProvider.onConfigurationChange(newConfig)
  }

  class FoldStateViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
      modelClass: Class<T>,
    ): T {
      return FoldStateViewModel(context) as T
    }
  }
}
