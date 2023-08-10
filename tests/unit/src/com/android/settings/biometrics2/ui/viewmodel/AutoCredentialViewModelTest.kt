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
package com.android.settings.biometrics2.ui.viewmodel

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.os.UserHandle
import androidx.activity.result.ActivityResult
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.internal.widget.LockPatternUtils
import com.android.internal.widget.VerifyCredentialResponse
import com.android.settings.biometrics.BiometricEnrollBase
import com.android.settings.biometrics2.ui.model.CredentialModel
import com.android.settings.biometrics2.ui.model.CredentialModelTest.Companion.newGkPwHandleCredentialIntentExtras
import com.android.settings.biometrics2.ui.model.CredentialModelTest.Companion.newOnlySensorValidCredentialIntentExtras
import com.android.settings.biometrics2.ui.model.CredentialModelTest.Companion.newValidTokenCredentialIntentExtras
import com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel.ChallengeGenerator
import com.android.settings.password.ChooseLockPattern
import com.android.settings.password.ChooseLockSettingsHelper
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.Mockito.`when` as whenever

@RunWith(AndroidJUnit4::class)
class AutoCredentialViewModelTest {

    @get:Rule val mockito: MockitoRule = MockitoJUnit.rule()

    @Mock private lateinit var lockPatternUtils: LockPatternUtils

    private var challengeGenerator: TestChallengeGenerator = TestChallengeGenerator()

    private lateinit var viewModel: AutoCredentialViewModel
    private fun newAutoCredentialViewModel(bundle: Bundle?): AutoCredentialViewModel {
        return AutoCredentialViewModel(
            ApplicationProvider.getApplicationContext(),
            lockPatternUtils,
            challengeGenerator,
            CredentialModel(bundle, SystemClock.elapsedRealtimeClock())
        )
    }

    @Before
    fun setUp() {
        challengeGenerator = TestChallengeGenerator()
    }

    private fun setupGenerateChallenge(userId: Int, newSensorId: Int, newChallenge: Long) {
        whenever(lockPatternUtils.getActivePasswordQuality(userId)).thenReturn(
            DevicePolicyManager.PASSWORD_QUALITY_SOMETHING
        )
        challengeGenerator.userId = userId
        challengeGenerator.sensorId = newSensorId
        challengeGenerator.challenge = newChallenge
    }

    @Test
    fun testCheckCredential_validCredentialCase() = runTest {
        val userId = 99
        viewModel = newAutoCredentialViewModel(newValidTokenCredentialIntentExtras(userId))
        whenever(lockPatternUtils.getActivePasswordQuality(userId)).thenReturn(
            DevicePolicyManager.PASSWORD_QUALITY_SOMETHING
        )

        val generateFails = listOfGenerateChallengeFailedFlow()

        // Run credential check
        val action = viewModel.checkCredential(backgroundScope)
        runCurrent()

        // Check viewModel behavior
        assertThat(action).isEqualTo(CredentialAction.CREDENTIAL_VALID)
        assertThat(generateFails.size).isEqualTo(0)

        // Check createGeneratingChallengeExtras()
        assertThat(viewModel.createGeneratingChallengeExtras()).isNull()
    }

    @Test
    fun testCheckCredential_needToChooseLock() = runTest {
        val userId = 100
        viewModel = newAutoCredentialViewModel(newOnlySensorValidCredentialIntentExtras(userId))
        whenever(lockPatternUtils.getActivePasswordQuality(userId)).thenReturn(
            DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED
        )

        val generateFails = listOfGenerateChallengeFailedFlow()

        // Run credential check
        val action = viewModel.checkCredential(backgroundScope)
        runCurrent()

        // Check viewModel behavior
        assertThat(action).isEqualTo(CredentialAction.FAIL_NEED_TO_CHOOSE_LOCK)
        assertThat(generateFails.size).isEqualTo(0)

        // Check createGeneratingChallengeExtras()
        assertThat(viewModel.createGeneratingChallengeExtras()).isNull()
    }

    @Test
    fun testCheckCredential_needToConfirmLockForSomething() = runTest {
        val userId = 101
        viewModel =
            newAutoCredentialViewModel(newOnlySensorValidCredentialIntentExtras(userId))
        whenever(lockPatternUtils.getActivePasswordQuality(userId)).thenReturn(
            DevicePolicyManager.PASSWORD_QUALITY_SOMETHING
        )

        val generateFails = listOfGenerateChallengeFailedFlow()

        // Run credential check
        val action = viewModel.checkCredential(backgroundScope)
        runCurrent()

        // Check viewModel behavior
        assertThat(action).isEqualTo(CredentialAction.FAIL_NEED_TO_CONFIRM_LOCK)
        assertThat(generateFails.size).isEqualTo(0)

        // Check createGeneratingChallengeExtras()
        assertThat(viewModel.createGeneratingChallengeExtras()).isNull()
    }

    @Test
    fun testCheckCredential_needToConfirmLockForNumeric() = runTest {
        val userId = 102
        viewModel =
            newAutoCredentialViewModel(newOnlySensorValidCredentialIntentExtras(userId))
        whenever(lockPatternUtils.getActivePasswordQuality(userId)).thenReturn(
            DevicePolicyManager.PASSWORD_QUALITY_NUMERIC
        )

        val generateFails = listOfGenerateChallengeFailedFlow()

        // Run credential check
        val action = viewModel.checkCredential(backgroundScope)
        runCurrent()

        // Check viewModel behavior
        assertThat(action).isEqualTo(CredentialAction.FAIL_NEED_TO_CONFIRM_LOCK)
        assertThat(generateFails.size).isEqualTo(0)

        // Check createGeneratingChallengeExtras()
        assertThat(viewModel.createGeneratingChallengeExtras()).isNull()
    }

    @Test
    fun testCheckCredential_needToConfirmLockForAlphabetic() = runTest {
        val userId = 103
        viewModel =
            newAutoCredentialViewModel(newOnlySensorValidCredentialIntentExtras(userId))
        whenever(lockPatternUtils.getActivePasswordQuality(userId)).thenReturn(
            DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC
        )

        val generateFails = listOfGenerateChallengeFailedFlow()

        // Run credential check
        val action = viewModel.checkCredential(this)
        runCurrent()

        // Check viewModel behavior
        assertThat(action).isEqualTo(CredentialAction.FAIL_NEED_TO_CONFIRM_LOCK)
        assertThat(generateFails.size).isEqualTo(0)

        // Check createGeneratingChallengeExtras()
        assertThat(viewModel.createGeneratingChallengeExtras()).isNull()
    }

    @Test
    fun testCheckCredential_generateChallenge() = runTest {
        val userId = 104
        val gkPwHandle = 1111L
        viewModel =
            newAutoCredentialViewModel(newGkPwHandleCredentialIntentExtras(userId, gkPwHandle))
        whenever(lockPatternUtils.getActivePasswordQuality(userId)).thenReturn(
            DevicePolicyManager.PASSWORD_QUALITY_SOMETHING
        )
        val newSensorId = 10
        val newChallenge = 20L
        setupGenerateChallenge(userId, newSensorId, newChallenge)
        whenever(
            lockPatternUtils.verifyGatekeeperPasswordHandle(
                gkPwHandle,
                newChallenge,
                userId
            )
        )
            .thenReturn(newGoodCredential(gkPwHandle, byteArrayOf(1)))
        val hasCalledRemoveGkPwHandle = AtomicBoolean()
        Mockito.doAnswer {
            hasCalledRemoveGkPwHandle.set(true)
            null
        }.`when`(lockPatternUtils).removeGatekeeperPasswordHandle(gkPwHandle)

        val generateFails = listOfGenerateChallengeFailedFlow()

        // Run credential check
        val action = viewModel.checkCredential(backgroundScope)
        runCurrent()

        // Check viewModel behavior
        assertThat(action).isEqualTo(CredentialAction.IS_GENERATING_CHALLENGE)
        assertThat(generateFails.size).isEqualTo(0)

        // Check data inside CredentialModel
        assertThat(viewModel.token).isNotNull()
        assertThat(challengeGenerator.callbackRunCount).isEqualTo(1)
        assertThat(hasCalledRemoveGkPwHandle.get()).isFalse()

        // Check createGeneratingChallengeExtras()
        val generatingChallengeExtras = viewModel.createGeneratingChallengeExtras()
        assertThat(generatingChallengeExtras).isNotNull()
        assertThat(generatingChallengeExtras!!.getLong(BiometricEnrollBase.EXTRA_KEY_CHALLENGE))
            .isEqualTo(newChallenge)
        val tokens =
            generatingChallengeExtras.getByteArray(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN)
        assertThat(tokens).isNotNull()
        assertThat(tokens!!.size).isEqualTo(1)
        assertThat(tokens[0]).isEqualTo(1)
    }

    @Test
    fun testCheckCredential_generateChallengeFail() = runTest {
        backgroundScope.launch {
            val userId = 104
            val gkPwHandle = 1111L
            viewModel =
                newAutoCredentialViewModel(newGkPwHandleCredentialIntentExtras(userId, gkPwHandle))
            whenever(lockPatternUtils.getActivePasswordQuality(userId)).thenReturn(
                DevicePolicyManager.PASSWORD_QUALITY_SOMETHING
            )
            val newSensorId = 10
            val newChallenge = 20L
            setupGenerateChallenge(userId, newSensorId, newChallenge)
            whenever(
                lockPatternUtils.verifyGatekeeperPasswordHandle(
                    gkPwHandle,
                    newChallenge,
                    userId
                )
            )
                .thenReturn(newBadCredential(0))

            val generateFails = listOfGenerateChallengeFailedFlow()

            // Run credential check
            val action = viewModel.checkCredential(this)
            runCurrent()

            assertThat(action).isEqualTo(CredentialAction.IS_GENERATING_CHALLENGE)
            assertThat(generateFails.size).isEqualTo(1)
            assertThat(generateFails[0]).isEqualTo(true)
            assertThat(challengeGenerator.callbackRunCount).isEqualTo(1)

            // Check createGeneratingChallengeExtras()
            assertThat(viewModel.createGeneratingChallengeExtras()).isNull()
        }
    }

    @Test
    fun testGetUserId_fromIntent() {
        val userId = 106
        viewModel = newAutoCredentialViewModel(newOnlySensorValidCredentialIntentExtras(userId))

        // Get userId
        assertThat(viewModel.userId).isEqualTo(userId)
    }

    @Test
    fun testGenerateChallengeAsCredentialActivityResult_invalidChooseLock() = runTest {
        backgroundScope.launch {
            val userId = 107
            val gkPwHandle = 3333L
            viewModel =
                newAutoCredentialViewModel(newGkPwHandleCredentialIntentExtras(userId, gkPwHandle))
            val intent = Intent()
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE, gkPwHandle)

            val generateFails = listOfGenerateChallengeFailedFlow()

            // Run generateChallengeAsCredentialActivityResult()
            val ret = viewModel.generateChallengeAsCredentialActivityResult(
                true,
                ActivityResult(ChooseLockPattern.RESULT_FINISHED + 1, intent),
                backgroundScope
            )
            runCurrent()

            assertThat(ret).isFalse()
            assertThat(generateFails.size).isEqualTo(0)
        }
    }

    @Test
    fun testGenerateChallengeAsCredentialActivityResult_invalidConfirmLock() = runTest {
        backgroundScope.launch {
            val userId = 107
            val gkPwHandle = 3333L
            viewModel =
                newAutoCredentialViewModel(newGkPwHandleCredentialIntentExtras(userId, gkPwHandle))
            val intent = Intent()
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE, gkPwHandle)

            val generateFails = listOfGenerateChallengeFailedFlow()

            // Run generateChallengeAsCredentialActivityResult()
            val ret = viewModel.generateChallengeAsCredentialActivityResult(
                false,
                ActivityResult(Activity.RESULT_OK + 1, intent),
                backgroundScope
            )
            runCurrent()

            assertThat(ret).isFalse()
            assertThat(generateFails.size).isEqualTo(0)
        }
    }

    @Test
    fun testGenerateChallengeAsCredentialActivityResult_nullDataChooseLock() = runTest {
        val userId = 108
        val gkPwHandle = 4444L
        viewModel =
            newAutoCredentialViewModel(newGkPwHandleCredentialIntentExtras(userId, gkPwHandle))

        val generateFails = listOfGenerateChallengeFailedFlow()

        // Run generateChallengeAsCredentialActivityResult()
        val ret = viewModel.generateChallengeAsCredentialActivityResult(
            true,
            ActivityResult(ChooseLockPattern.RESULT_FINISHED, null),
            backgroundScope
        )
        runCurrent()

        assertThat(ret).isFalse()
        assertThat(generateFails.size).isEqualTo(0)
    }

    @Test
    fun testGenerateChallengeAsCredentialActivityResult_nullDataConfirmLock() = runTest {
        val userId = 109
        viewModel =
            newAutoCredentialViewModel(newOnlySensorValidCredentialIntentExtras(userId))

        val generateFails = listOfGenerateChallengeFailedFlow()

        // Run generateChallengeAsCredentialActivityResult()
        val ret = viewModel.generateChallengeAsCredentialActivityResult(
            false,
            ActivityResult(Activity.RESULT_OK, null),
            backgroundScope
        )
        runCurrent()

        assertThat(ret).isFalse()
        assertThat(generateFails.size).isEqualTo(0)
    }

    @Test
    fun testGenerateChallengeAsCredentialActivityResult_validChooseLock() = runTest {
        val userId = 108
        viewModel =
            newAutoCredentialViewModel(newOnlySensorValidCredentialIntentExtras(userId))
        whenever(lockPatternUtils.getActivePasswordQuality(userId)).thenReturn(
            DevicePolicyManager.PASSWORD_QUALITY_SOMETHING
        )
        val gkPwHandle = 6666L
        val newSensorId = 50
        val newChallenge = 60L
        setupGenerateChallenge(userId, newSensorId, newChallenge)
        whenever(
            lockPatternUtils.verifyGatekeeperPasswordHandle(
                gkPwHandle,
                newChallenge,
                userId
            )
        )
            .thenReturn(newGoodCredential(gkPwHandle, byteArrayOf(1)))
        val hasCalledRemoveGkPwHandle = AtomicBoolean()
        Mockito.doAnswer {
            hasCalledRemoveGkPwHandle.set(true)
            null
        }.`when`(lockPatternUtils).removeGatekeeperPasswordHandle(gkPwHandle)

        val generateFails = listOfGenerateChallengeFailedFlow()

        // Run generateChallengeAsCredentialActivityResult()
        val intent =
            Intent().putExtra(ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE, gkPwHandle)
        val ret = viewModel.generateChallengeAsCredentialActivityResult(
            true,
            ActivityResult(ChooseLockPattern.RESULT_FINISHED, intent),
            backgroundScope
        )
        runCurrent()

        assertThat(ret).isTrue()
        assertThat(generateFails.size).isEqualTo(0)
        assertThat(viewModel.token).isNotNull()
        assertThat(challengeGenerator.callbackRunCount).isEqualTo(1)
        assertThat(hasCalledRemoveGkPwHandle.get()).isTrue()
    }

    @Test
    fun testGenerateChallengeAsCredentialActivityResult_validConfirmLock() = runTest {
        val userId = 109
        viewModel =
            newAutoCredentialViewModel(newOnlySensorValidCredentialIntentExtras(userId))
        whenever(lockPatternUtils.getActivePasswordQuality(userId)).thenReturn(
            DevicePolicyManager.PASSWORD_QUALITY_SOMETHING
        )
        val gkPwHandle = 5555L
        val newSensorId = 80
        val newChallenge = 90L
        setupGenerateChallenge(userId, newSensorId, newChallenge)
        whenever(
            lockPatternUtils.verifyGatekeeperPasswordHandle(
                gkPwHandle,
                newChallenge,
                userId
            )
        )
            .thenReturn(newGoodCredential(gkPwHandle, byteArrayOf(1)))
        val hasCalledRemoveGkPwHandle = AtomicBoolean()
        Mockito.doAnswer {
            hasCalledRemoveGkPwHandle.set(true)
            null
        }.`when`(lockPatternUtils).removeGatekeeperPasswordHandle(gkPwHandle)

        val generateFails = listOfGenerateChallengeFailedFlow()

        // Run generateChallengeAsCredentialActivityResult()
        val intent =
            Intent().putExtra(ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE, gkPwHandle)
        val ret = viewModel.generateChallengeAsCredentialActivityResult(
            false,
            ActivityResult(Activity.RESULT_OK, intent),
            backgroundScope
        )
        runCurrent()

        assertThat(ret).isTrue()
        assertThat(generateFails.size).isEqualTo(0)
        assertThat(viewModel.token).isNotNull()
        assertThat(challengeGenerator.callbackRunCount).isEqualTo(1)
        assertThat(hasCalledRemoveGkPwHandle.get()).isTrue()
    }

    private fun TestScope.listOfGenerateChallengeFailedFlow(): List<Boolean> =
        mutableListOf<Boolean>().also {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.generateChallengeFailedFlow.toList(it)
            }
        }

    class TestChallengeGenerator : ChallengeGenerator {
        var sensorId = -1
        var userId = UserHandle.myUserId()
        var challenge = CredentialModel.INVALID_CHALLENGE
        var callbackRunCount = 0

        override var callback: AutoCredentialViewModel.GenerateChallengeCallback? = null

        override fun generateChallenge(userId: Int) {
            callback?.let {
                it.onChallengeGenerated(sensorId, this.userId, challenge)
                ++callbackRunCount
            }
        }
    }

    private fun newGoodCredential(gkPwHandle: Long, hat: ByteArray): VerifyCredentialResponse {
        return VerifyCredentialResponse.Builder()
            .setGatekeeperPasswordHandle(gkPwHandle)
            .setGatekeeperHAT(hat)
            .build()
    }

    private fun newBadCredential(timeout: Int): VerifyCredentialResponse {
        return if (timeout > 0) {
            VerifyCredentialResponse.fromTimeout(timeout)
        } else {
            VerifyCredentialResponse.fromError()
        }
    }
}
