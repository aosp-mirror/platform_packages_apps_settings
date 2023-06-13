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

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.os.UserHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.biometrics.BiometricEnrollBase
import com.android.settings.password.ChooseLockSettingsHelper
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Arrays

@RunWith(AndroidJUnit4::class)
class CredentialModelTest {

    private val clock = SystemClock.elapsedRealtimeClock()

    @Test
    fun testNullBundle() {
        val credentialModel = CredentialModel(null, clock)
        Truth.assertThat(credentialModel.userId).isEqualTo(UserHandle.myUserId())
    }

    @Test
    fun testSameValueFromBundle() {
        val bundle = newCredentialModelIntentExtras(1234, 6677L, byteArrayOf(33, 44, 55), 987654321)
        val model1 = CredentialModel(bundle, clock)
        val model2 = CredentialModel(model1.bundle, clock)
        verifySameCredentialModels(model1, model2)
    }

    @Test
    fun testSameValueFromBundle_nullToken() {
        val bundle = newCredentialModelIntentExtras(22, 33L, null, 21L)
        val model1 = CredentialModel(bundle, clock)
        val model2 = CredentialModel(model1.bundle, clock)
        verifySameCredentialModels(model1, model2)
    }

    companion object {
        @JvmStatic
        fun newCredentialModelIntentExtras(
            userId: Int, challenge: Long,
            token: ByteArray?, gkPwHandle: Long
        ): Bundle {
            val bundle = Bundle()
            bundle.putInt(Intent.EXTRA_USER_ID, userId)
            bundle.putLong(BiometricEnrollBase.EXTRA_KEY_CHALLENGE, challenge)
            bundle.putByteArray(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, token)
            bundle.putLong(ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE, gkPwHandle)
            return bundle
        }

        @JvmStatic
        fun newValidTokenCredentialIntentExtras(userId: Int): Bundle {
            return newCredentialModelIntentExtras(
                userId, 1L, byteArrayOf(0, 1, 2),
                CredentialModel.INVALID_GK_PW_HANDLE
            )
        }

        @JvmStatic
        fun newOnlySensorValidCredentialIntentExtras(userId: Int): Bundle {
            return newCredentialModelIntentExtras(
                userId, CredentialModel.INVALID_CHALLENGE, null,
                CredentialModel.INVALID_GK_PW_HANDLE
            )
        }

        @JvmStatic
        fun newGkPwHandleCredentialIntentExtras(userId: Int, gkPwHandle: Long): Bundle {
            return newCredentialModelIntentExtras(
                userId,
                CredentialModel.INVALID_CHALLENGE,
                null,
                gkPwHandle
            )
        }

        private fun checkBundleLongValue(
            bundle1: Bundle, bundle2: Bundle,
            key: String
        ) {
            if (!bundle1.containsKey(key)) {
                return
            }
            val value1 = bundle1.getInt(key)
            val value2 = bundle2.getInt(key)
            Truth.assertWithMessage(
                "bundle not match, key:" + key + ", value1:" + value1 + ", value2:"
                        + value2
            ).that(value1).isEqualTo(value2)
        }

        private fun checkBundleIntValue(
            bundle1: Bundle, bundle2: Bundle,
            key: String
        ) {
            if (!bundle1.containsKey(key)) {
                return
            }
            val value1 = bundle1.getLong(key)
            val value2 = bundle2.getLong(key)
            Truth.assertWithMessage(
                "bundle not match, key:" + key + ", value1:" + value1 + ", value2:"
                        + value2
            ).that(value1).isEqualTo(value2)
        }

        private fun checkBundleByteArrayValue(
            bundle1: Bundle, bundle2: Bundle,
            key: String
        ) {
            if (!bundle1.containsKey(key)) {
                return
            }
            val value1 = bundle1.getByteArray(key)
            val value2 = bundle2.getByteArray(key)
            val errMsg = ("bundle not match, key:" + key + ", value1:" + Arrays.toString(value1)
                    + ", value2:" + Arrays.toString(value2))
            if (value1 == null) {
                Truth.assertWithMessage(errMsg).that(value2).isNull()
            } else {
                Truth.assertWithMessage(errMsg).that(value1.size).isEqualTo(
                    value2!!.size
                )
                for (i in value1.indices) {
                    Truth.assertWithMessage(errMsg).that(value1[i]).isEqualTo(
                        value2[i]
                    )
                }
            }
        }

        fun verifySameCredentialModels(
            model1: CredentialModel,
            model2: CredentialModel
        ) {
            Truth.assertThat(model1.userId).isEqualTo(model2.userId)
            Truth.assertThat(model1.challenge).isEqualTo(model2.challenge)
            Truth.assertThat(model1.gkPwHandle).isEqualTo(model2.gkPwHandle)
            val token1 = model1.token
            val token2 = model2.token
            if (token1 == null) {
                Truth.assertThat(token2).isNull()
            } else {
                Truth.assertThat(token2).isNotNull()
                Truth.assertThat(token1.size).isEqualTo(token2!!.size)
                for (i in token1.indices) {
                    Truth.assertThat(token1[i]).isEqualTo(
                        token2[i]
                    )
                }
            }
            val bundle1 = model1.bundle
            val bundle2 = model2.bundle
            val keySet1 = bundle1.keySet()
            Truth.assertThat(keySet1 == bundle2.keySet()).isTrue()
            checkBundleIntValue(bundle1, bundle2, Intent.EXTRA_USER_ID)
            checkBundleIntValue(bundle1, bundle2, BiometricEnrollBase.EXTRA_KEY_SENSOR_ID)
            checkBundleLongValue(bundle1, bundle2, BiometricEnrollBase.EXTRA_KEY_CHALLENGE)
            checkBundleByteArrayValue(bundle1, bundle2, BiometricEnrollBase.EXTRA_KEY_CHALLENGE)
            checkBundleLongValue(bundle1, bundle2, ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE)
        }
    }
}
