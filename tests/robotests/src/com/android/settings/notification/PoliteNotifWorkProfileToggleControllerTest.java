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

package com.android.settings.notification;

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.UserHandle;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.server.notification.Flags;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.testutils.shadow.ShadowSystemSettings;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public class PoliteNotifWorkProfileToggleControllerTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final String PREFERENCE_KEY = "preference_key";

    private Context mContext;
    PoliteNotifWorkProfileToggleController mController;
    private Preference mPreference;
    @Mock
    private AudioHelper mAudioHelper;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        when(mAudioHelper.getManagedProfileId(any())).thenReturn(UserHandle.MIN_SECONDARY_USER_ID);
        setCoolDownEnabled(true);
        assertThat(isCoolDownEnabled()).isTrue();
        mController = new PoliteNotifWorkProfileToggleController(mContext, PREFERENCE_KEY,
                mAudioHelper);
        mPreference = new Preference(RuntimeEnvironment.application);
        mPreference.setKey(mController.getPreferenceKey());
    }

    @Test
    public void isAvailable_flagEnabled_coolDownEnabled_workProfileExists_shouldReturnTrue() {
        // TODO: b/291907312 - remove feature flags
        mSetFlagsRule.enableFlags(Flags.FLAG_POLITE_NOTIFICATIONS);
        assertThat(mController.isAvailable()).isTrue();
        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    public void isAvailable_flagEnabled_coolDownDisabled_workProfileExists_shouldReturnFalse() {
        // TODO: b/291907312 - remove feature flags
        mSetFlagsRule.enableFlags(Flags.FLAG_POLITE_NOTIFICATIONS);
        setCoolDownEnabled(false);
        assertThat(isCoolDownEnabled()).isFalse();
        assertThat(mController.isAvailable()).isFalse();
        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void isAvailable_flagEnabled_workProfileMissing_shouldReturnFalse() {
        // TODO: b/291907312 - remove feature flags
        mSetFlagsRule.enableFlags(Flags.FLAG_POLITE_NOTIFICATIONS);
        when(mAudioHelper.getManagedProfileId(any())).thenReturn(UserHandle.USER_NULL);
        mController = new PoliteNotifWorkProfileToggleController(mContext, PREFERENCE_KEY,
                mAudioHelper);
        assertThat(mController.isAvailable()).isFalse();
        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.DISABLED_FOR_USER);
    }

    @Test
    public void isAvailable_coolDownEnabled_flagDisabled_shouldReturnFalse() {
        // TODO: b/291907312 - remove feature flags
        mSetFlagsRule.disableFlags(Flags.FLAG_POLITE_NOTIFICATIONS);
        assertThat(mController.isAvailable()).isFalse();
        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    @Config(shadows = ShadowSystemSettings.class)
    public void isChecked_coolDownEnabled_enabledForWorkProfile_shouldReturnTrue() {
        Settings.System.putIntForUser(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_COOLDOWN_ENABLED, ON,
                UserHandle.MIN_SECONDARY_USER_ID);
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    @Config(shadows = ShadowSystemSettings.class)
    public void isChecked_coolDownDisabled_enabledForWorkProfile_shouldReturnFalse() {
        setCoolDownEnabled(false);
        assertThat(isCoolDownEnabled()).isFalse();
        Settings.System.putIntForUser(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_COOLDOWN_ENABLED, ON,
                UserHandle.MIN_SECONDARY_USER_ID);
        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    @Config(shadows = ShadowSystemSettings.class)
    public void isChecked_coolDownEnabled_disabledForWorkProfile_shouldReturnFalse() {
        Settings.System.putIntForUser(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_COOLDOWN_ENABLED, OFF,
                UserHandle.MIN_SECONDARY_USER_ID);
        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    @Config(shadows = ShadowSystemSettings.class)
    public void setChecked_coolDownEnabled_setTrue_shouldEnablePoliteNotifForWorkProfile() {
        Settings.System.putIntForUser(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_COOLDOWN_ENABLED, OFF,
                UserHandle.MIN_SECONDARY_USER_ID);
        mController.setChecked(true);
        assertThat(Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_COOLDOWN_ENABLED, ON,
                UserHandle.MIN_SECONDARY_USER_ID)).isEqualTo(ON);
    }

    @Test
    @Config(shadows = ShadowSystemSettings.class)
    public void setChecked_coolDownEnabled_setFalse_shouldDisablePoliteNotifForWorkProfile() {
        Settings.System.putIntForUser(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_COOLDOWN_ENABLED, ON,
                UserHandle.MIN_SECONDARY_USER_ID);
        mController.setChecked(false);
        assertThat(Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_COOLDOWN_ENABLED, ON,
                UserHandle.MIN_SECONDARY_USER_ID)).isEqualTo(OFF);
    }

    @Test
    public void isVisible_coolDownSetToBeDisabled_shouldReturnFalse() {
        assertThat(mPreference.isVisible()).isTrue();
        setCoolDownEnabled(false);
        assertThat(isCoolDownEnabled()).isFalse();
        mController.updateState(mPreference);
        assertThat(mPreference.isVisible()).isFalse();
    }
    @Test
    public void isVisible_coolDownSetToBeEnabled_shouldReturnTrue() {
        setCoolDownEnabled(false);
        assertThat(isCoolDownEnabled()).isFalse();
        mController.updateState(mPreference);
        assertThat(mPreference.isVisible()).isFalse();

        setCoolDownEnabled(true);
        assertThat(isCoolDownEnabled()).isTrue();
        mController.updateState(mPreference);
        assertThat(mPreference.isVisible()).isTrue();
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
