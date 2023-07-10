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

package com.android.settings.fingerprint2.viewmodel

import android.hardware.fingerprint.Fingerprint
import android.hardware.fingerprint.FingerprintManager
import com.android.settings.biometrics.fingerprint2.ui.viewmodel.EnrollFirstFingerprint
import com.android.settings.biometrics.fingerprint2.ui.viewmodel.FinishSettings
import com.android.settings.biometrics.fingerprint2.ui.viewmodel.FinishSettingsWithResult
import com.android.settings.biometrics.fingerprint2.ui.viewmodel.LaunchConfirmDeviceCredential
import com.android.settings.biometrics.fingerprint2.ui.viewmodel.NextStepViewModel
import com.android.settings.biometrics.fingerprint2.ui.viewmodel.ShowSettings
import com.android.settings.biometrics.fingerprint2.ui.viewmodel.FingerprintSettingsViewModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.anyInt
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.Mockito.`when` as whenever

@RunWith(MockitoJUnitRunner::class)
class FingerprintSettingsViewModelTest {

    @JvmField
    @Rule
    var rule = MockitoJUnit.rule()

    @Mock
    private lateinit var fingerprintManager: FingerprintManager
    private lateinit var underTest: FingerprintSettingsViewModel
    private val defaultUserId = 0

    @Before
    fun setup() {
        // @formatter:off
        underTest = FingerprintSettingsViewModel.FingerprintSettingsViewModelFactory(
            defaultUserId,
            fingerprintManager,
        ).create(FingerprintSettingsViewModel::class.java)
        // @formatter:on
    }

    @Test
    fun testNoGateKeeper_launchesConfirmDeviceCredential() = runTest {
        var nextStep: NextStepViewModel? = null
        val job = launch {
            underTest.nextStep.collect {
                nextStep = it
            }
        }

        underTest.updateTokenAndChallenge(null, null)

        runCurrent()
        assertThat(nextStep).isEqualTo(LaunchConfirmDeviceCredential(defaultUserId))
        job.cancel()
    }

    @Test
    fun testConfirmDevice_fails() = runTest {
        var nextStep: NextStepViewModel? = null
        val job = launch {
            underTest.nextStep.collect {
                nextStep = it
            }
        }

        underTest.updateTokenAndChallenge(null, null)
        underTest.onConfirmDevice(false, null)

        runCurrent()

        assertThat(nextStep).isInstanceOf(FinishSettings::class.java)
        job.cancel()
    }

    @Test
    fun confirmDeviceSuccess_noGateKeeper() = runTest {
        var nextStep: NextStepViewModel? = null
        val job = launch {
            underTest.nextStep.collect {
                nextStep = it
            }
        }

        underTest.updateTokenAndChallenge(null, null)
        underTest.onConfirmDevice(true, null)

        runCurrent()

        assertThat(nextStep).isInstanceOf(FinishSettings::class.java)
        job.cancel()
    }

    @Test
    fun confirmDeviceSuccess_launchesEnrollment_ifNoPreviousEnrollments() = runTest {
        whenever(fingerprintManager.getEnrolledFingerprints(anyInt())).thenReturn(emptyList())

        var nextStep: NextStepViewModel? = null
        val job = launch {
            underTest.nextStep.collect {
                nextStep = it
            }
        }

        underTest.updateTokenAndChallenge(null, null)
        underTest.onConfirmDevice(true, 10L)

        runCurrent()

        assertThat(nextStep).isEqualTo(EnrollFirstFingerprint(defaultUserId, 10L, null, null))
        job.cancel()
    }

    @Test
    fun firstEnrollment_fails() = runTest {
        whenever(fingerprintManager.getEnrolledFingerprints(anyInt())).thenReturn(emptyList())

        var nextStep: NextStepViewModel? = null
        val job = launch {
            underTest.nextStep.collect {
                nextStep = it
            }
        }

        underTest.updateTokenAndChallenge(null, null)
        underTest.onConfirmDevice(true, 10L)
        underTest.onEnrollFirstFailure("We failed!!")

        runCurrent()

        assertThat(nextStep).isInstanceOf(FinishSettings::class.java)
        job.cancel()
    }

    @Test
    fun firstEnrollment_failsWithReason() = runTest {
        whenever(fingerprintManager.getEnrolledFingerprints(anyInt())).thenReturn(emptyList())

        var nextStep: NextStepViewModel? = null
        val job = launch {
            underTest.nextStep.collect {
                nextStep = it
            }
        }

        val failStr = "We failed!!"
        val failReason = 101

        underTest.updateTokenAndChallenge(null, null)
        underTest.onConfirmDevice(true, 10L)
        underTest.onEnrollFirstFailure(failStr, failReason)

        runCurrent()

        assertThat(nextStep).isEqualTo(FinishSettingsWithResult(failReason, failStr))
        job.cancel()
    }

    @Test
    fun firstEnrollmentSucceeds_noToken() = runTest {
        whenever(fingerprintManager.getEnrolledFingerprints(anyInt())).thenReturn(emptyList())

        var nextStep: NextStepViewModel? = null
        val job = launch {
            underTest.nextStep.collect {
                nextStep = it
            }
        }

        underTest.updateTokenAndChallenge(null, null)
        underTest.onConfirmDevice(true, 10L)
        underTest.onEnrollFirst(null, null)

        runCurrent()

        assertThat(nextStep).isEqualTo(FinishSettings("Error, empty token"))
        job.cancel()
    }

    @Test
    fun firstEnrollmentSucceeds_noKeyChallenge() = runTest {
        whenever(fingerprintManager.getEnrolledFingerprints(anyInt())).thenReturn(emptyList())

        var nextStep: NextStepViewModel? = null
        val job = launch {
            underTest.nextStep.collect {
                nextStep = it
            }
        }

        val byteArray = ByteArray(1) {
            3
        }

        underTest.updateTokenAndChallenge(null, null)
        underTest.onConfirmDevice(true, 10L)
        underTest.onEnrollFirst(byteArray, null)

        runCurrent()

        assertThat(nextStep).isEqualTo(FinishSettings("Error, empty keyChallenge"))
        job.cancel()
    }

    @Test
    fun firstEnrollment_succeeds() = runTest {
        whenever(fingerprintManager.getEnrolledFingerprints(anyInt())).thenReturn(emptyList())

        var nextStep: NextStepViewModel? = null
        val job = launch {
            underTest.nextStep.collect {
                nextStep = it
            }
        }

        val byteArray = ByteArray(1) {
            3
        }
        val keyChallenge = 89L

        underTest.updateTokenAndChallenge(null, null)
        underTest.onConfirmDevice(true, 10L)
        underTest.onEnrollFirst(byteArray, keyChallenge)

        runCurrent()

        assertThat(nextStep).isEqualTo(ShowSettings(defaultUserId))
        job.cancel()
    }

    @Test
    fun confirmDeviceCredential_withEnrolledFingerprint_showsSettings() = runTest {
        whenever(fingerprintManager.getEnrolledFingerprints(anyInt())).thenReturn(
            listOf(
                Fingerprint(
                    "a", 1, 2, 3L
                )
            )
        )

        var nextStep: NextStepViewModel? = null
        val job = launch {
            underTest.nextStep.collect {
                nextStep = it
            }
        }

        underTest.updateTokenAndChallenge(null, null)
        underTest.onConfirmDevice(true, 10L)

        runCurrent()

        assertThat(nextStep).isEqualTo(ShowSettings(defaultUserId))
        job.cancel()
    }

    @Test
    fun enrollAdditionalFingerprints_fails() = runTest {
        whenever(fingerprintManager.getEnrolledFingerprints(anyInt())).thenReturn(
            listOf(
                Fingerprint(
                    "a", 1, 2, 3L
                )
            )
        )

        var nextStep: NextStepViewModel? = null
        val job = launch {
            underTest.nextStep.collect {
                nextStep = it
            }
        }

        underTest.updateTokenAndChallenge(null, null)
        underTest.onConfirmDevice(true, 10L)
        underTest.onEnrollAdditionalFailure()

        runCurrent()

        assertThat(nextStep).isInstanceOf(FinishSettings::class.java)
        job.cancel()
    }

    @Test
    fun enrollAdditional_success() = runTest {
        whenever(fingerprintManager.getEnrolledFingerprints(anyInt())).thenReturn(
            listOf(
                Fingerprint(
                    "a", 1, 2, 3L
                )
            )
        )

        var nextStep: NextStepViewModel? = null
        val job = launch {
            underTest.nextStep.collect {
                nextStep = it
            }
        }

        underTest.updateTokenAndChallenge(null, null)
        underTest.onConfirmDevice(true, 10L)
        underTest.onEnrollSuccess()

        runCurrent()

        assertThat(nextStep).isEqualTo(ShowSettings(defaultUserId))
        job.cancel()
    }
}