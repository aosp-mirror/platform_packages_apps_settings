/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.password;

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.RuntimeEnvironment.application;

import android.app.admin.DevicePolicyManager;
import android.content.Intent;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.password.ChooseLockPassword.IntentBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(
        manifest = TestConfig.MANIFEST_PATH,
        sdk = TestConfig.SDK_VERSION)
public class ChooseLockPasswordTest {

    @Test
    public void intentBuilder_setPassword_shouldAddExtras() {
        Intent intent = new IntentBuilder(application)
                .setPassword("password")
                .setPasswordQuality(DevicePolicyManager.PASSWORD_QUALITY_NUMERIC)
                .setPasswordLengthRange(123, 456)
                .setUserId(123)
                .build();

        assertThat(intent.getBooleanExtra(ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, true))
                .named("EXTRA_KEY_HAS_CHALLENGE")
                .isFalse();
        assertThat(intent.getStringExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD))
                .named("EXTRA_KEY_PASSWORD")
                .isEqualTo("password");
        assertThat(intent.getIntExtra(ChooseLockPassword.PASSWORD_MIN_KEY, 0))
                .named("PASSWORD_MIN_KEY")
                .isEqualTo(123);
        assertThat(intent.getIntExtra(ChooseLockPassword.PASSWORD_MAX_KEY, 0))
                .named("PASSWORD_MAX_KEY")
                .isEqualTo(456);
        assertThat(intent.getIntExtra(LockPatternUtils.PASSWORD_TYPE_KEY, 0))
                .named("PASSWORD_TYPE_KEY")
                .isEqualTo(DevicePolicyManager.PASSWORD_QUALITY_NUMERIC);
        assertThat(intent.getIntExtra(Intent.EXTRA_USER_ID, 0))
                .named("EXTRA_USER_ID")
                .isEqualTo(123);
    }

    @Test
    public void intentBuilder_setChallenge_shouldAddExtras() {
        Intent intent = new IntentBuilder(application)
                .setChallenge(12345L)
                .setPasswordQuality(DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC)
                .setPasswordLengthRange(123, 456)
                .setUserId(123)
                .build();

        assertThat(intent.getBooleanExtra(ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, false))
                .named("EXTRA_KEY_HAS_CHALLENGE")
                .isTrue();
        assertThat(intent.getLongExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, 0L))
                .named("EXTRA_KEY_CHALLENGE")
                .isEqualTo(12345L);
        assertThat(intent.getIntExtra(ChooseLockPassword.PASSWORD_MIN_KEY, 0))
                .named("PASSWORD_MIN_KEY")
                .isEqualTo(123);
        assertThat(intent.getIntExtra(ChooseLockPassword.PASSWORD_MAX_KEY, 0))
                .named("PASSWORD_MAX_KEY")
                .isEqualTo(456);
        assertThat(intent.getIntExtra(LockPatternUtils.PASSWORD_TYPE_KEY, 0))
                .named("PASSWORD_TYPE_KEY")
                .isEqualTo(DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC);
        assertThat(intent.getIntExtra(Intent.EXTRA_USER_ID, 0))
                .named("EXTRA_USER_ID")
                .isEqualTo(123);
    }
}
