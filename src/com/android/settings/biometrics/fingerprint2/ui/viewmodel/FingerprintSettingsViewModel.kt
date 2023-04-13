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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Models the UI state for fingerprint settings.
 */
class FingerprintSettingsViewModel(
    private val userId: Int,
    gateKeeperPassword: Long?,
    theChallenge: Long?,
    theChallengeToken: ByteArray?,
    private val fingerprintManager: FingerprintManager
) : ViewModel() {

    private val _nextStep: MutableStateFlow<NextStepViewModel?> = MutableStateFlow(null)
    /**
     *  This flow represents the high level state for the FingerprintSettingsV2Fragment. The
     *  consumer of this flow should call [onUiCommandExecuted] which will set the state to null,
     *  confirming that the UI has consumed the last command and is ready to consume another
     *  command.
     */
    val nextStep = _nextStep.asStateFlow()


    private var gateKeeperPasswordHandle: Long? = gateKeeperPassword
    private var challenge: Long? = theChallenge
    private var challengeToken: ByteArray? = theChallengeToken

    /**
     * Indicates to the view model that a confirm device credential action has been completed
     * with a [theGateKeeperPasswordHandle] which will be used for [FingerprintManager]
     * operations such as [FingerprintManager.enroll].
     */
    fun onConfirmDevice(wasSuccessful: Boolean, theGateKeeperPasswordHandle: Long?) {

        if (!wasSuccessful) {
            launchFinishSettings("ConfirmDeviceCredential was unsuccessful")
            return
        }
        if (theGateKeeperPasswordHandle == null) {
            launchFinishSettings("ConfirmDeviceCredential gatekeeper password was null")
            return
        }

        gateKeeperPasswordHandle = theGateKeeperPasswordHandle
        launchEnrollNextStep()
    }

    /**
     * Notifies that enrollment was successful.
     */
    fun onEnrollSuccess() {
        _nextStep.update {
            ShowSettings(userId)
        }
    }

    /**
     * Notifies that an additional enrollment failed.
     */
    fun onEnrollAdditionalFailure() {
        launchFinishSettings("Failed to enroll additional fingerprint")
    }

    /**
     * Notifies that the first enrollment failed.
     */
    fun onEnrollFirstFailure(reason: String) {
        launchFinishSettings(reason)
    }

    /**
     * Notifies that first enrollment failed (with resultCode)
     */
    fun onEnrollFirstFailure(reason: String, resultCode: Int) {
        launchFinishSettings(reason, resultCode)
    }

    /**
     * Notifies that a users first enrollment succeeded.
     */
    fun onEnrollFirst(token: ByteArray?, keyChallenge: Long?) {
        if (token == null) {
            launchFinishSettings("Error, empty token")
            return
        }
        if (keyChallenge == null) {
            launchFinishSettings("Error, empty keyChallenge")
            return
        }
        challengeToken = token
        challenge = keyChallenge

        _nextStep.update {
            ShowSettings(userId)
        }
    }


    /**
     * Indicates if this settings activity has been called with correct token and challenge
     * and that we do not need to launch confirm device credential.
     */
    fun updateTokenAndChallenge(token: ByteArray?, theChallenge: Long?) {
        challengeToken = token
        challenge = theChallenge
        if (challengeToken == null) {
            _nextStep.update {
                LaunchConfirmDeviceCredential(userId)
            }
        } else {
            launchEnrollNextStep()
        }
    }

    /**
     * Indicates a UI command has been consumed by the UI, and the logic can send another
     * UI command.
     */
    fun onUiCommandExecuted() {
        _nextStep.update {
            null
        }
    }

    private fun launchEnrollNextStep() {
        if (fingerprintManager.getEnrolledFingerprints(userId).isEmpty()) {
            _nextStep.update {
                EnrollFirstFingerprint(userId, gateKeeperPasswordHandle, challenge, challengeToken)
            }
        } else {
            _nextStep.update {
                ShowSettings(userId)
            }
        }
    }

    private fun launchFinishSettings(reason: String) {
        _nextStep.update {
            FinishSettings(reason)
        }
    }

    private fun launchFinishSettings(reason: String, errorCode: Int) {
        _nextStep.update {
            FinishSettingsWithResult(errorCode, reason)
        }
    }

    class FingerprintSettingsViewModelFactory(
        private val userId: Int,
        private val fingerprintManager: FingerprintManager,
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            modelClass: Class<T>,
        ): T {

            return FingerprintSettingsViewModel(
                userId, null, null, null, fingerprintManager
            ) as T
        }
    }
}
