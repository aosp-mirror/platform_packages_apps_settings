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

package com.android.settings.biometrics.fingerprint2.ui.binder

import androidx.lifecycle.LifecycleCoroutineScope
import com.android.settings.biometrics.fingerprint2.ui.fragment.FingerprintSettingsV2Fragment
import com.android.settings.biometrics.fingerprint2.ui.viewmodel.EnrollAdditionalFingerprint
import com.android.settings.biometrics.fingerprint2.ui.viewmodel.EnrollFirstFingerprint
import com.android.settings.biometrics.fingerprint2.ui.viewmodel.FingerprintSettingsViewModel
import com.android.settings.biometrics.fingerprint2.ui.viewmodel.FinishSettings
import com.android.settings.biometrics.fingerprint2.ui.viewmodel.FinishSettingsWithResult
import com.android.settings.biometrics.fingerprint2.ui.viewmodel.LaunchConfirmDeviceCredential
import com.android.settings.biometrics.fingerprint2.ui.viewmodel.ShowSettings
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * Binds a [FingerprintSettingsViewModel] to a [FingerprintSettingsV2Fragment]
 */
object FingerprintViewBinder {

    interface Binding {
        fun onConfirmDevice(wasSuccessful: Boolean, theGateKeeperPasswordHandle: Long?)
        fun onEnrollSuccess()
        fun onEnrollAdditionalFailure()
        fun onEnrollFirstFailure(reason: String)
        fun onEnrollFirstFailure(reason: String, resultCode: Int)
        fun onEnrollFirst(token: ByteArray?, keyChallenge: Long?)
    }

    /** Initial listener for the first enrollment request */
    fun bind(
        viewModel: FingerprintSettingsViewModel,
        lifecycleScope: LifecycleCoroutineScope,
        token: ByteArray?,
        challenge: Long?,
        launchFullFingerprintEnrollment: (
            userId: Int,
            gateKeeperPasswordHandle: Long?,
            challenge: Long?,
            challengeToken: ByteArray?
        ) -> Unit,
        launchAddFingerprint: (userId: Int, challengeToken: ByteArray?) -> Unit,
        launchConfirmOrChooseLock: (userId: Int) -> Unit,
        finish: () -> Unit,
        setResultExternal: (resultCode: Int) -> Unit,
    ): Binding {

        lifecycleScope.launch {
            viewModel.nextStep.filterNotNull().collect { nextStep ->
                when (nextStep) {
                    is EnrollFirstFingerprint -> launchFullFingerprintEnrollment(
                        nextStep.userId,
                        nextStep.gateKeeperPasswordHandle,
                        nextStep.challenge,
                        nextStep.challengeToken
                    )

                    is EnrollAdditionalFingerprint -> launchAddFingerprint(
                        nextStep.userId, nextStep.challengeToken
                    )

                    is LaunchConfirmDeviceCredential -> launchConfirmOrChooseLock(nextStep.userId)

                    is FinishSettings -> {
                        println("Finishing due to ${nextStep.reason}")
                        finish()
                    }

                    is FinishSettingsWithResult -> {
                        println("Finishing with result ${nextStep.result} due to ${nextStep.reason}")
                        setResultExternal(nextStep.result)
                        finish()
                    }

                    is ShowSettings -> println("show settings")
                }

                viewModel.onUiCommandExecuted()
            }
        }

        viewModel.updateTokenAndChallenge(token, if (challenge == -1L) null else challenge)

        return object : Binding {
            override fun onConfirmDevice(
                wasSuccessful: Boolean, theGateKeeperPasswordHandle: Long?
            ) {
                viewModel.onConfirmDevice(wasSuccessful, theGateKeeperPasswordHandle)
            }

            override fun onEnrollSuccess() {
                viewModel.onEnrollSuccess()
            }

            override fun onEnrollAdditionalFailure() {
                viewModel.onEnrollAdditionalFailure()
            }

            override fun onEnrollFirstFailure(reason: String) {
                viewModel.onEnrollFirstFailure(reason)
            }

            override fun onEnrollFirstFailure(reason: String, resultCode: Int) {
                viewModel.onEnrollFirstFailure(reason, resultCode)
            }

            override fun onEnrollFirst(token: ByteArray?, keyChallenge: Long?) {
                viewModel.onEnrollFirst(token, keyChallenge)
            }
        }
    }

}