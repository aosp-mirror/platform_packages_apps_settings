/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.biometrics2.ui.model;

import static com.android.settings.biometrics.BiometricEnrollBase.EXTRA_KEY_CHALLENGE;
import static com.android.settings.biometrics.BiometricEnrollBase.EXTRA_KEY_SENSOR_ID;
import static com.android.settings.biometrics2.ui.model.CredentialModel.INVALID_CHALLENGE;
import static com.android.settings.biometrics2.ui.model.CredentialModel.INVALID_GK_PW_HANDLE;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import android.annotation.NonNull;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;

import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.password.ChooseLockSettingsHelper;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Clock;
import java.util.Arrays;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class CredentialModelTest {

    private final Clock mClock = SystemClock.elapsedRealtimeClock();

    public static Bundle newCredentialModelIntentExtras(int userId, long challenge, int sensorId,
            @Nullable byte[] token, long gkPwHandle) {
        final Bundle bundle = new Bundle();
        bundle.putInt(Intent.EXTRA_USER_ID, userId);
        bundle.putInt(EXTRA_KEY_SENSOR_ID, sensorId);
        bundle.putLong(EXTRA_KEY_CHALLENGE, challenge);
        bundle.putByteArray(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, token);
        bundle.putLong(ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE, gkPwHandle);
        return bundle;
    }

    public static Bundle newValidTokenCredentialIntentExtras(int userId) {
        return newCredentialModelIntentExtras(userId, 1L, 1, new byte[] { 0, 1, 2 },
                INVALID_GK_PW_HANDLE);
    }

    public static Bundle newOnlySensorValidCredentialIntentExtras(int userId) {
        return newCredentialModelIntentExtras(userId, INVALID_CHALLENGE, 1, null,
                INVALID_GK_PW_HANDLE);
    }

    public static Bundle newGkPwHandleCredentialIntentExtras(int userId, long gkPwHandle) {
        return newCredentialModelIntentExtras(userId, INVALID_CHALLENGE, 1, null, gkPwHandle);
    }

    private static void checkBundleLongValue(@NonNull Bundle bundle1, @NonNull Bundle bundle2,
            @NonNull String key) {
        if (!bundle1.containsKey(key)) {
            return;
        }
        final int value1 = bundle1.getInt(key);
        final int value2 = bundle2.getInt(key);
        assertWithMessage("bundle not match, key:" + key + ", value1:" + value1 + ", value2:"
                + value2).that(value1).isEqualTo(value2);
    }

    private static void checkBundleIntValue(@NonNull Bundle bundle1, @NonNull Bundle bundle2,
            @NonNull String key) {
        if (!bundle1.containsKey(key)) {
            return;
        }
        final long value1 = bundle1.getLong(key);
        final long value2 = bundle2.getLong(key);
        assertWithMessage("bundle not match, key:" + key + ", value1:" + value1 + ", value2:"
                + value2).that(value1).isEqualTo(value2);
    }

    private static void checkBundleByteArrayValue(@NonNull Bundle bundle1, @NonNull Bundle bundle2,
            @NonNull String key) {
        if (!bundle1.containsKey(key)) {
            return;
        }
        final byte[] value1 = bundle1.getByteArray(key);
        final byte[] value2 = bundle2.getByteArray(key);
        final String errMsg = "bundle not match, key:" + key + ", value1:" + Arrays.toString(value1)
                + ", value2:" + Arrays.toString(value2);
        if (value1 == null) {
            assertWithMessage(errMsg).that(value2).isNull();
        } else {
            assertWithMessage(errMsg).that(value1.length).isEqualTo(value2.length);
            for (int i = 0; i < value1.length; ++i) {
                assertWithMessage(errMsg).that(value1[i]).isEqualTo(value2[i]);
            }
        }
    }

    public static void verifySameCredentialModels(@NonNull CredentialModel model1,
            @NonNull CredentialModel model2) {

        assertThat(model1.getUserId()).isEqualTo(model2.getUserId());
        assertThat(model1.getSensorId()).isEqualTo(model2.getSensorId());
        assertThat(model1.getChallenge()).isEqualTo(model2.getChallenge());
        assertThat(model1.getGkPwHandle()).isEqualTo(model2.getGkPwHandle());

        final byte[] token1 = model1.getToken();
        final byte[] token2 = model2.getToken();
        if (token1 == null) {
            assertThat(token2).isNull();
        } else {
            assertThat(token2).isNotNull();
            assertThat(token1.length).isEqualTo(token2.length);
            for (int i = 0; i < token1.length; ++i) {
                assertThat(token1[i]).isEqualTo(token2[i]);
            }
        }

        final Bundle bundle1 = model1.getBundle();
        final Bundle bundle2 = model2.getBundle();
        final Set<String> keySet1 = bundle1.keySet();
        assertThat(keySet1.equals(bundle2.keySet())).isTrue();
        checkBundleIntValue(bundle1, bundle2, Intent.EXTRA_USER_ID);
        checkBundleIntValue(bundle1, bundle2, EXTRA_KEY_SENSOR_ID);
        checkBundleLongValue(bundle1, bundle2, EXTRA_KEY_CHALLENGE);
        checkBundleByteArrayValue(bundle1, bundle2, EXTRA_KEY_CHALLENGE);
        checkBundleLongValue(bundle1, bundle2, EXTRA_KEY_GK_PW_HANDLE);
    }

    @Test
    public void sameValueFromBundle() {
        final Bundle bundle = newCredentialModelIntentExtras(1234, 6677L, 1,
                new byte[] { 33, 44, 55 }, 987654321);

        final CredentialModel model1 = new CredentialModel(bundle, mClock);
        final CredentialModel model2 = new CredentialModel(model1.getBundle(), mClock);

        verifySameCredentialModels(model1, model2);
    }

    @Test
    public void sameValueFromBundle_nullToken() {
        final Bundle bundle = newCredentialModelIntentExtras(22, 33L, 1, null, 21L);

        final CredentialModel model1 = new CredentialModel(bundle, mClock);
        final CredentialModel model2 = new CredentialModel(model1.getBundle(), mClock);

        verifySameCredentialModels(model1, model2);
    }
}
