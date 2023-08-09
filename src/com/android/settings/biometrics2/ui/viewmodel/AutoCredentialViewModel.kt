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
import android.app.Application
import android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.lifecycle.AndroidViewModel
import com.android.internal.widget.LockPatternUtils
import com.android.settings.biometrics.BiometricEnrollBase
import com.android.settings.biometrics.BiometricUtils
import com.android.settings.biometrics.BiometricUtils.GatekeeperCredentialNotMatchException
import com.android.settings.biometrics2.data.repository.FingerprintRepository
import com.android.settings.biometrics2.ui.model.CredentialModel
import com.android.settings.password.ChooseLockGeneric
import com.android.settings.password.ChooseLockPattern
import com.android.settings.password.ChooseLockSettingsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * AutoCredentialViewModel which uses CredentialModel to determine next actions for activity, like
 * start ChooseLockActivity, start ConfirmLockActivity, GenerateCredential, or do nothing.
 */
class AutoCredentialViewModel(
    application: Application,
    private val lockPatternUtils: LockPatternUtils,
    private val challengeGenerator: ChallengeGenerator,
    private val credentialModel: CredentialModel
) : AndroidViewModel(application) {

    /**
     * Generic callback for FingerprintManager#generateChallenge or FaceManager#generateChallenge
     */
    interface GenerateChallengeCallback {
        /** Generic generateChallenge method for FingerprintManager or FaceManager */
        fun onChallengeGenerated(sensorId: Int, userId: Int, challenge: Long)
    }

    /**
     * A generic interface class for calling different generateChallenge from FingerprintManager or
     * FaceManager
     */
    interface ChallengeGenerator {

        /** Callback that will be called later after challenge generated */
        var callback: GenerateChallengeCallback?

        /** Method for generating challenge from FingerprintManager or FaceManager */
        fun generateChallenge(userId: Int)
    }

    /** Used to generate challenge through FingerprintRepository */
    class FingerprintChallengeGenerator(
        private val fingerprintRepository: FingerprintRepository
    ) : ChallengeGenerator {

        override var callback: GenerateChallengeCallback? = null

        override fun generateChallenge(userId: Int) {
            callback?.let {
                fingerprintRepository.generateChallenge(userId) {
                        sensorId: Int, uid: Int, challenge: Long ->
                    it.onChallengeGenerated(sensorId, uid, challenge)
                }
            } ?:run {
                Log.e(TAG, "generateChallenge, null callback")
            }
        }

        companion object {
            private const val TAG = "FingerprintChallengeGenerator"
        }
    }

    private val _generateChallengeFailedFlow = MutableSharedFlow<Boolean>()
    val generateChallengeFailedFlow: SharedFlow<Boolean>
        get() = _generateChallengeFailedFlow.asSharedFlow()


    // flag if token is generating through checkCredential()'s generateChallenge()
    private var isGeneratingChallengeDuringCheckingCredential = false

    /** Get bundle which passing back to FingerprintSettings for late generateChallenge() */
    fun createGeneratingChallengeExtras(): Bundle? {
        if (!isGeneratingChallengeDuringCheckingCredential
            || !credentialModel.isValidToken
            || !credentialModel.isValidChallenge
        ) {
            return null
        }
        val bundle = Bundle()
        bundle.putByteArray(
            ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN,
            credentialModel.token
        )
        bundle.putLong(BiometricEnrollBase.EXTRA_KEY_CHALLENGE, credentialModel.challenge)
        return bundle
    }

    /** Check credential status for biometric enrollment. */
    fun checkCredential(scope: CoroutineScope): CredentialAction {
        return if (isValidCredential) {
            CredentialAction.CREDENTIAL_VALID
        } else if (isUnspecifiedPassword) {
            CredentialAction.FAIL_NEED_TO_CHOOSE_LOCK
        } else if (credentialModel.isValidGkPwHandle) {
            val gkPwHandle = credentialModel.gkPwHandle
            credentialModel.clearGkPwHandle()
            // GkPwHandle is got through caller activity, we shall not revoke it after
            // generateChallenge(). Let caller activity to make decision.
            generateChallenge(gkPwHandle, false, scope)
            isGeneratingChallengeDuringCheckingCredential = true
            CredentialAction.IS_GENERATING_CHALLENGE
        } else {
            CredentialAction.FAIL_NEED_TO_CONFIRM_LOCK
        }
    }

    private fun generateChallenge(
        gkPwHandle: Long,
        revokeGkPwHandle: Boolean,
        scope: CoroutineScope
    ) {
        challengeGenerator.callback = object : GenerateChallengeCallback {
            override fun onChallengeGenerated(sensorId: Int, userId: Int, challenge: Long) {
                var illegalStateExceptionCaught = false
                try {
                    val newToken = requestGatekeeperHat(gkPwHandle, challenge, userId)
                    credentialModel.challenge = challenge
                    credentialModel.token = newToken
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "generateChallenge, IllegalStateException", e)
                    illegalStateExceptionCaught = true
                } finally {
                    if (revokeGkPwHandle) {
                        lockPatternUtils.removeGatekeeperPasswordHandle(gkPwHandle)
                    }
                    Log.d(
                        TAG,
                        "generateChallenge(), model:$credentialModel"
                                + ", revokeGkPwHandle:$revokeGkPwHandle"
                    )
                    // Check credential again
                    if (!isValidCredential || illegalStateExceptionCaught) {
                        Log.w(TAG, "generateChallenge, invalid Credential or IllegalStateException")
                        scope.launch {
                            _generateChallengeFailedFlow.emit(true)
                        }
                    }
                }
            }
        }
        challengeGenerator.generateChallenge(userId)
    }

    private val isValidCredential: Boolean
        get() = !isUnspecifiedPassword && credentialModel.isValidToken

    private val isUnspecifiedPassword: Boolean
        get() = lockPatternUtils.getActivePasswordQuality(userId) == PASSWORD_QUALITY_UNSPECIFIED

    /**
     * Handle activity result from ChooseLockGeneric, ConfirmLockPassword, or ConfirmLockPattern
     * @param isChooseLock true if result is coming from ChooseLockGeneric. False if result is
     * coming from ConfirmLockPassword or ConfirmLockPattern
     * @param result activity result
     * @return if it is a valid result and viewModel is generating challenge
     */
    fun generateChallengeAsCredentialActivityResult(
        isChooseLock: Boolean,
        result: ActivityResult,
        scope: CoroutineScope
    ): Boolean {
        if ((isChooseLock && result.resultCode == ChooseLockPattern.RESULT_FINISHED) ||
            (!isChooseLock && result.resultCode == Activity.RESULT_OK)) {
            result.data?.let {
                val gkPwHandle = it.getLongExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE,
                    CredentialModel.INVALID_GK_PW_HANDLE
                )
                // Revoke self requested GkPwHandle because it shall only used once inside this
                // activity lifecycle.
                generateChallenge(gkPwHandle, true, scope)
                return true
            }
        }
        return false
    }

    val userId: Int
        get() = credentialModel.userId

    val token: ByteArray?
        get() = credentialModel.token

    @Throws(IllegalStateException::class)
    private fun requestGatekeeperHat(gkPwHandle: Long, challenge: Long, userId: Int): ByteArray? {
        val response = lockPatternUtils
            .verifyGatekeeperPasswordHandle(gkPwHandle, challenge, userId)
        if (!response.isMatched) {
            throw GatekeeperCredentialNotMatchException("Unable to request Gatekeeper HAT")
        }
        return response.gatekeeperHAT
    }

    /** Create Intent for choosing lock */
    fun createChooseLockIntent(
        context: Context, isSuw: Boolean,
        suwExtras: Bundle
    ): Intent {
        val intent = BiometricUtils.getChooseLockIntent(
            context, isSuw,
            suwExtras
        )
        intent.putExtra(
            ChooseLockGeneric.ChooseLockGenericFragment.HIDE_INSECURE_OPTIONS,
            true
        )
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_REQUEST_GK_PW_HANDLE, true)
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_FOR_FINGERPRINT, true)
        if (credentialModel.isValidUserId) {
            intent.putExtra(Intent.EXTRA_USER_ID, credentialModel.userId)
        }
        return intent
    }

    /** Create ConfirmLockLauncher */
    fun createConfirmLockLauncher(
        activity: Activity,
        requestCode: Int, title: String
    ): ChooseLockSettingsHelper {
        val builder = ChooseLockSettingsHelper.Builder(activity)
        builder.setRequestCode(requestCode)
            .setTitle(title)
            .setRequestGatekeeperPasswordHandle(true)
            .setForegroundOnly(true)
            .setReturnCredentials(true)
        if (credentialModel.isValidUserId) {
            builder.setUserId(credentialModel.userId)
        }
        return builder.build()
    }

    companion object {
        private const val TAG = "AutoCredentialViewModel"
    }
}

enum class CredentialAction {

    CREDENTIAL_VALID,

    /** Valid credential, activity does nothing. */
    IS_GENERATING_CHALLENGE,

    /** This credential looks good, but still need to run generateChallenge(). */
    FAIL_NEED_TO_CHOOSE_LOCK,

    /** Need activity to run confirm lock */
    FAIL_NEED_TO_CONFIRM_LOCK
}
