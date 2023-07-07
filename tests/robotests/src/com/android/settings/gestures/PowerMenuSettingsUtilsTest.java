/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.gestures;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class PowerMenuSettingsUtilsTest {

    private Context mContext;
    private Resources mResources;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        mResources = mock(Resources.class);
        when(mContext.getResources()).thenReturn(mResources);

        when(mResources.getBoolean(
                        com.android.internal.R.bool
                                .config_longPressOnPowerForAssistantSettingAvailable))
                .thenReturn(true);
    }

    @Test
    public void isLongPressPowerForAssistantEnabled_valueSetToAssistant_returnsTrue() {
        Settings.Global.putInt(
                mContext.getContentResolver(), Settings.Global.POWER_BUTTON_LONG_PRESS, 5);
        assertThat(PowerMenuSettingsUtils.isLongPressPowerForAssistantEnabled(mContext)).isTrue();
    }

    @Test
    public void isLongPressPowerForAssistantEnabled_valueNotSetToAssistant_returnsFalse() {
        Settings.Global.putInt(
                mContext.getContentResolver(), Settings.Global.POWER_BUTTON_LONG_PRESS, 3);
        assertThat(PowerMenuSettingsUtils.isLongPressPowerForAssistantEnabled(mContext)).isFalse();
    }

    @Test
    public void isLongPressPowerForAssistantEnabled_valueNotSet_defaultToAssistant_returnsTrue() {
        when(mResources.getInteger(com.android.internal.R.integer.config_longPressOnPowerBehavior))
                .thenReturn(5);

        assertThat(PowerMenuSettingsUtils.isLongPressPowerForAssistantEnabled(mContext)).isTrue();
    }

    @Test
    public void isLongPressPowerForAssistantEnabled_valueNotSet_defaultToPowerMenu_returnsFalse() {
        when(mResources.getInteger(com.android.internal.R.integer.config_longPressOnPowerBehavior))
                .thenReturn(1);

        assertThat(PowerMenuSettingsUtils.isLongPressPowerForAssistantEnabled(mContext)).isFalse();
    }

    @Test
    public void isLongPressPowerSettingAvailable_defaultToAssistant_returnsTrue() {
        when(mResources.getInteger(com.android.internal.R.integer.config_longPressOnPowerBehavior))
                .thenReturn(5);
        assertThat(PowerMenuSettingsUtils.isLongPressPowerSettingAvailable(mContext)).isTrue();
    }

    @Test
    public void isLongPressPowerSettingAvailable_defaultToPowerMenu_returnsTrue() {
        when(mResources.getInteger(com.android.internal.R.integer.config_longPressOnPowerBehavior))
                .thenReturn(1);
        assertThat(PowerMenuSettingsUtils.isLongPressPowerSettingAvailable(mContext)).isTrue();
    }

    @Test
    public void isLongPressPowerSettingAvailable_defaultToPowerOff_returnsFalse() {
        // Power off is the unsupported option in long press power settings
        when(mResources.getInteger(com.android.internal.R.integer.config_longPressOnPowerBehavior))
                .thenReturn(3);
        assertThat(PowerMenuSettingsUtils.isLongPressPowerSettingAvailable(mContext)).isFalse();
    }

    @Test
    public void isLongPressPowerSettingAvailable_settingDisabled_returnsFalse() {
        // Disable the setting
        when(mResources.getBoolean(
                        com.android.internal.R.bool
                                .config_longPressOnPowerForAssistantSettingAvailable))
                .thenReturn(false);
        when(mResources.getInteger(com.android.internal.R.integer.config_longPressOnPowerBehavior))
                .thenReturn(1);

        assertThat(PowerMenuSettingsUtils.isLongPressPowerSettingAvailable(mContext)).isFalse();
    }

    @Test
    public void setLongPressPowerForAssistant_updatesValue() throws Exception {
        boolean result = PowerMenuSettingsUtils.setLongPressPowerForAssistant(mContext);

        assertThat(result).isTrue();
        assertThat(
                        Settings.Global.getInt(
                                mContext.getContentResolver(),
                                Settings.Global.POWER_BUTTON_LONG_PRESS))
                .isEqualTo(5);
    }

    @Test
    public void setLongPressPowerForAssistant_updatesKeyChordValueToPowerMenu() throws Exception {
        PowerMenuSettingsUtils.setLongPressPowerForAssistant(mContext);
        assertThat(
                        Settings.Global.getInt(
                                mContext.getContentResolver(),
                                Settings.Global.KEY_CHORD_POWER_VOLUME_UP))
                .isEqualTo(2);
    }

    @Test
    public void setLongPressPowerForPowerMenu_updatesValue() throws Exception {
        boolean result = PowerMenuSettingsUtils.setLongPressPowerForPowerMenu(mContext);

        assertThat(result).isTrue();
        assertThat(
                        Settings.Global.getInt(
                                mContext.getContentResolver(),
                                Settings.Global.POWER_BUTTON_LONG_PRESS))
                .isEqualTo(1);
    }

    @Test
    public void setLongPressPowerForPowerMenu_updatesKeyChordValueToDefault() throws Exception {
        when(mResources.getInteger(com.android.internal.R.integer.config_keyChordPowerVolumeUp))
                .thenReturn(1);

        PowerMenuSettingsUtils.setLongPressPowerForPowerMenu(mContext);

        assertThat(
                        Settings.Global.getInt(
                                mContext.getContentResolver(),
                                Settings.Global.KEY_CHORD_POWER_VOLUME_UP))
                .isEqualTo(1);
    }
}
