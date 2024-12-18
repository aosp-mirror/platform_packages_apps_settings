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

package com.android.settings.biometrics.fingerprint2.domain.interactor

import android.content.Context
import com.android.settingslib.display.DisplayDensityUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update

/**
 * This class is responsible for handling updates to fontScale and displayDensity and forwarding
 * these events to classes that need them
 */
interface DisplayDensityInteractor {
  /** Indicates the display density has been updated. */
  fun updateDisplayDensity(density: Int)

  /** Indicates the font scale has been updates. */
  fun updateFontScale(fontScale: Float)

  /** A flow that propagates fontscale. */
  val fontScale: Flow<Float>

  /** A flow that propagates displayDensity. */
  val displayDensity: Flow<Int>

  /** A flow that propagates the default display density. */
  val defaultDisplayDensity: Flow<Int>
}

/**
 * Implementation of the [DisplayDensityInteractor]. This interactor is used to forward activity
 * information to the rest of the application.
 */
class DisplayDensityInteractorImpl(context: Context, scope: CoroutineScope) :
  DisplayDensityInteractor {

  val displayDensityUtils = DisplayDensityUtils(context)

  override fun updateDisplayDensity(density: Int) {
    _displayDensity.update { density }
  }

  override fun updateFontScale(fontScale: Float) {
    _fontScale.update { fontScale }
  }

  private val _fontScale = MutableStateFlow(context.resources.configuration.fontScale)
  private val _displayDensity =
    MutableStateFlow(
      displayDensityUtils.defaultDisplayDensityValues[
          displayDensityUtils.currentIndexForDefaultDisplay]
    )

  override val fontScale: Flow<Float> = _fontScale.asStateFlow()

  override val displayDensity: Flow<Int> = _displayDensity.asStateFlow()

  override val defaultDisplayDensity: Flow<Int> =
    flowOf(displayDensityUtils.defaultDensityForDefaultDisplay)
      .shareIn(scope, SharingStarted.Eagerly, 1)
}
