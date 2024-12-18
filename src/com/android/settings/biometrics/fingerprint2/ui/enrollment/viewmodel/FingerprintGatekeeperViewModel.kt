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

import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.android.settings.SettingsApplication
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.GenerateChallengeInteractor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface GatekeeperInfo {
  object Invalid : GatekeeperInfo

  object Timeout : GatekeeperInfo

  data class GatekeeperPasswordInfo(val token: ByteArray?, val passwordHandle: Long?) :
    GatekeeperInfo
}

/**
 * This class is responsible for maintaining the gatekeeper information including things like
 * timeouts.
 *
 * Please note, that this class can't fully support timeouts of the gatekeeper password handle due
 * to the fact that a handle may have been generated earlier in the settings enrollment and passed
 * in as a parameter to this class.
 */
class FingerprintGatekeeperViewModel(
  private val generateChallengeInteractor: GenerateChallengeInteractor
) : ViewModel() {

  private var _gatekeeperInfo: MutableStateFlow<GatekeeperInfo?> = MutableStateFlow(null)

  /** The gatekeeper info for fingerprint enrollment. */
  val gatekeeperInfo: Flow<GatekeeperInfo?> = _gatekeeperInfo.asStateFlow()

  /** Indicates if the gatekeeper info is valid. */
  val hasValidGatekeeperInfo: Flow<Boolean> =
    gatekeeperInfo.map { it is GatekeeperInfo.GatekeeperPasswordInfo }

  private var countDownTimer: CountDownTimer? = null

  /** Timeout of 15 minutes for a generated challenge */
  private val TIMEOUT: Long = 15 * 60 * 1000

  /** Called after a confirm device credential attempt has been made. */
  fun onConfirmDevice(
    wasSuccessful: Boolean,
    theGatekeeperPasswordHandle: Long?,
    shouldStartTimer: Boolean = true,
  ) {
    if (!wasSuccessful) {
      Log.d(TAG, "confirmDevice failed")
      _gatekeeperInfo.update { GatekeeperInfo.Invalid }
    } else {
      viewModelScope.launch {
        val res = generateChallengeInteractor.generateChallenge(theGatekeeperPasswordHandle!!)
        _gatekeeperInfo.update { GatekeeperInfo.GatekeeperPasswordInfo(res.second, res.first) }
        if (shouldStartTimer) {
          startTimeout()
        }
      }
    }
  }

  private fun startTimeout() {
    countDownTimer?.cancel()
    countDownTimer =
      object : CountDownTimer(TIMEOUT, 1000) {
        override fun onFinish() {
          _gatekeeperInfo.update { GatekeeperInfo.Timeout }
        }

        override fun onTick(millisUntilFinished: Long) {}
      }
  }

  companion object {
    private const val TAG = "FingerprintGatekeeperViewModel"

    /**
     * A function that checks if the challenge and token are valid, in which case a
     * [GatekeeperInfo.GatekeeperPasswordInfo] is provided, else [GatekeeperInfo.Invalid]
     */
    fun toGateKeeperInfo(challenge: Long?, token: ByteArray?): GatekeeperInfo {
      Log.d(TAG, "toGateKeeperInfo(${challenge == null}, ${token == null})")
      if (challenge == null || token == null) {
        return GatekeeperInfo.Invalid
      }
      return GatekeeperInfo.GatekeeperPasswordInfo(token, challenge)
    }

    val Factory: ViewModelProvider.Factory = viewModelFactory {
      initializer {
        val settingsApplication =
          this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as SettingsApplication
        val biometricEnvironment = settingsApplication.biometricEnvironment
        FingerprintGatekeeperViewModel(biometricEnvironment!!.createGenerateChallengeInteractor())
      }
    }
  }
}
