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
package com.android.settings.biometrics2.ui.model

import android.content.Intent.EXTRA_USER_ID
import android.os.Bundle
import android.os.UserHandle
import androidx.annotation.VisibleForTesting
import com.android.settings.biometrics.BiometricEnrollBase.EXTRA_KEY_CHALLENGE
import com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN
import com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE
import java.time.Clock

/**
 * Secret credential data including
 * 1. userId
 * 2. challenge
 * 3. token
 * 4. gkPwHandle
 */
class CredentialModel(bundle: Bundle?, private val clock: Clock) {

    private val mInitMillis = clock.millis()

    /** userId for this credential */
    val userId: Int = (bundle ?: Bundle()).getInt(EXTRA_USER_ID, UserHandle.myUserId())

    private var clearGkPwHandleMillis: Long? = null

    /** Gatekeeper password handle */
    var gkPwHandle: Long = (bundle ?: Bundle()).getLong(EXTRA_KEY_GK_PW_HANDLE, INVALID_GK_PW_HANDLE)
        private set

    val isValidGkPwHandle: Boolean
        get() = gkPwHandle != INVALID_GK_PW_HANDLE

    /** Clear gatekeeper password handle data */
    fun clearGkPwHandle() {
        clearGkPwHandleMillis = clock.millis()
        gkPwHandle = INVALID_GK_PW_HANDLE
    }

    /** Check user id is valid or not */
    val isValidUserId: Boolean
        get() = userId != UserHandle.USER_NULL

    private var updateChallengeMillis: Long? = null

    var challenge: Long = (bundle ?: Bundle()).getLong(EXTRA_KEY_CHALLENGE, INVALID_CHALLENGE)
        set(value) {
            updateChallengeMillis = clock.millis()
            field = value
        }

    val isValidChallenge: Boolean
        get() = challenge != INVALID_CHALLENGE

    private var updateTokenMillis: Long? = null

    /** Challenge token */
    var token: ByteArray? = (bundle ?: Bundle()).getByteArray(EXTRA_KEY_CHALLENGE_TOKEN)
        set(value) {
            updateTokenMillis = clock.millis()
            field = value
        }

    val isValidToken: Boolean
        get() = token != null

    /** Returns a string representation of the object */
    override fun toString(): String {
        val gkPwHandleLen = "$gkPwHandle".length
        val tokenLen = token?.size ?: 0
        val challengeLen = "$challenge".length
        return (javaClass.simpleName + ":{initMillis:$mInitMillis"
                + ", userId:$userId"
                + ", challenge:{len:$challengeLen"
                + ", updateMillis:$updateChallengeMillis}"
                + ", token:{len:$tokenLen, isValid:$isValidToken"
                + ", updateMillis:$updateTokenMillis}"
                + ", gkPwHandle:{len:$gkPwHandleLen, isValid:$isValidGkPwHandle"
                + ", clearMillis:$clearGkPwHandleMillis}"
                + " }")
    }

    companion object {
        /** Default value for an invalid challenge */
        @VisibleForTesting
        const val INVALID_CHALLENGE = -1L

        /** Default value if GkPwHandle is invalid */
        @VisibleForTesting
        const val INVALID_GK_PW_HANDLE = 0L
    }
}
