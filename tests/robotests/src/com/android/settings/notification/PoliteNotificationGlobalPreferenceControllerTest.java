/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.notification;

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import com.android.server.notification.Flags;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.testutils.shadow.ShadowSystemSettings;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public class PoliteNotificationGlobalPreferenceControllerTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final String PREFERENCE_KEY = "preference_key";

    private Context mContext;
    PoliteNotificationGlobalPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        setCoolDownEnabled(true);
        assertThat(isCoolDownEnabled()).isTrue();
        // TODO: b/291907312 - remove feature flags
        mSetFlagsRule.enableFlags(Flags.FLAG_POLITE_NOTIFICATIONS);
        mController = new PoliteNotificationGlobalPreferenceController(mContext, PREFERENCE_KEY);
    }

    @Test
    public void isAvailable_flagEnabled_shouldReturnTrue() {
        assertThat(mController.isAvailable()).isTrue();
        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    public void isAvailable_flagDisabled_shouldReturnFalse() {
        // TODO: b/291907312 - remove feature flags
        mSetFlagsRule.disableFlags(Flags.FLAG_POLITE_NOTIFICATIONS);
        assertThat(mController.isAvailable()).isFalse();
        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    @Config(shadows = ShadowSystemSettings.class)
    public void isChecked_coolDownEnabled_shouldReturnTrue() {
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    @Config(shadows = ShadowSystemSettings.class)
    public void isChecked_coolDownDisabled_shouldReturnFalse() {
        setCoolDownEnabled(false);
        assertThat(isCoolDownEnabled()).isFalse();
        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    @Config(shadows = ShadowSystemSettings.class)
    public void setChecked_setTrue_shouldEnableCoolDown() {
        setCoolDownEnabled(false);
        assertThat(isCoolDownEnabled()).isFalse();

        mController.setChecked(true);
        assertThat(isCoolDownEnabled()).isTrue();
    }

    @Test
    @Config(shadows = ShadowSystemSettings.class)
    public void setChecked_setFalse_shouldDisableCoolDown() {
        assertThat(isCoolDownEnabled()).isTrue();

        mController.setChecked(false);
        assertThat(isCoolDownEnabled()).isFalse();
    }

    private void setCoolDownEnabled(boolean enabled) {
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_COOLDOWN_ENABLED, (enabled ? ON : OFF));
    }

    private boolean isCoolDownEnabled() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_COOLDOWN_ENABLED, ON) == ON;
    }
}
