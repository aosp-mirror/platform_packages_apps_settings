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
import android.os.UserHandle;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.password.ChooseLockPassword.ChooseLockPasswordFragment;
import com.android.settings.password.ChooseLockPassword.IntentBuilder;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.testutils.shadow.ShadowEventLogWriter;
import com.android.settings.testutils.shadow.ShadowUtils;
import com.android.setupwizardlib.GlifLayout;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDrawable;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(
        manifest = TestConfig.MANIFEST_PATH,
        sdk = TestConfig.SDK_VERSION,
        shadows = {
                SettingsShadowResources.class,
                SettingsShadowResources.SettingsShadowTheme.class,
                ShadowEventLogWriter.class,
                ShadowUtils.class
        })
public class ChooseLockPasswordTest {

    @Before
    public void setUp() throws Exception {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.string.config_headlineFontFamily, "");
    }

    @After
    public void tearDown() {
        SettingsShadowResources.reset();
    }

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

    @Test
    public void assertThat_chooseLockIconChanged_WhenFingerprintExtraSet() {
        ShadowDrawable drawable = setActivityAndGetIconDrawable(true);
        assertThat(drawable.getCreatedFromResId()).isEqualTo(R.drawable.ic_fingerprint_header);
    }

    @Test
    public void assertThat_chooseLockIconNotChanged_WhenFingerprintExtraSet() {
        ShadowDrawable drawable = setActivityAndGetIconDrawable(false);
        assertThat(drawable.getCreatedFromResId()).isNotEqualTo(R.drawable.ic_fingerprint_header);
    }

    private ShadowDrawable setActivityAndGetIconDrawable(boolean addFingerprintExtra) {
        ChooseLockPassword passwordActivity =
                Robolectric.buildActivity(
                        ChooseLockPassword.class,
                        new IntentBuilder(application)
                                .setUserId(UserHandle.myUserId())
                                .setForFingerprint(addFingerprintExtra)
                                .build())
                        .setup().get();
        ChooseLockPasswordFragment fragment = (ChooseLockPasswordFragment)
                passwordActivity.getFragmentManager().findFragmentById(R.id.main_content);
        return Shadows.shadowOf(((GlifLayout) fragment.getView()).getIcon());
    }
}
