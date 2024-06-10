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
import android.os.Process
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator

/** Indicates the possible vibration effects for fingerprint enrollment */
sealed class FingerprintVibrationEffects {
  /** A vibration indicating an error */
  data object UdfpsError : FingerprintVibrationEffects()

  /**
   * A vibration indicating success, this usually occurs when progress on the UDFPS enrollment has
   * been made
   */
  data object UdfpsSuccess : FingerprintVibrationEffects()

  /** This vibration typically occurs when a help message is shown during UDFPS enrollment */
  data object UdfpsHelp : FingerprintVibrationEffects()
}

/** Interface for sending haptic feedback */
interface VibrationInteractor {
  /** This will send a haptic vibration */
  fun vibrate(effect: FingerprintVibrationEffects, caller: String)
}

/** Implementation of the VibrationInteractor interface */
class VibrationInteractorImpl(val applicationContext: Context) : VibrationInteractor {
  val vibrator = applicationContext.getSystemService(Vibrator::class.java)!!

  override fun vibrate(effect: FingerprintVibrationEffects, caller: String) {
    val callerString = "$caller::$effect"
    val res =
      when (effect) {
        FingerprintVibrationEffects.UdfpsHelp,
        FingerprintVibrationEffects.UdfpsError ->
          Pair(VIBRATE_EFFECT_ERROR, FINGERPRINT_ENROLLING_SONIFICATION_ATTRIBUTES)
        FingerprintVibrationEffects.UdfpsSuccess ->
          Pair(VIBRATE_EFFECT_SUCCESS, HARDWARE_FEEDBACK_VIBRATION_ATTRIBUTES)
      }
    vibrator.vibrate(
      Process.myUid(),
      applicationContext.opPackageName,
      res.first,
      callerString,
      res.second,
    )
  }

  companion object {
    private val VIBRATE_EFFECT_ERROR = VibrationEffect.createWaveform(longArrayOf(0, 5, 55, 60), -1)
    private val FINGERPRINT_ENROLLING_SONIFICATION_ATTRIBUTES =
      VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ACCESSIBILITY)
    private val HARDWARE_FEEDBACK_VIBRATION_ATTRIBUTES =
      VibrationAttributes.createForUsage(VibrationAttributes.USAGE_HARDWARE_FEEDBACK)
    private val VIBRATE_EFFECT_SUCCESS = VibrationEffect.get(VibrationEffect.EFFECT_CLICK)
  }
}
