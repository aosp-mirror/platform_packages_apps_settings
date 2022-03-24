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
    }

    @Test
    public void longPressBehaviourValuePresent_returnsValue() {
        when(mResources.getInteger(
                com.android.internal.R.integer.config_longPressOnPowerBehavior))
                .thenReturn(0);
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.POWER_BUTTON_LONG_PRESS, 1);

        assertThat(PowerMenuSettingsUtils.getPowerButtonSettingValue(mContext)).isEqualTo(1);
    }

    @Test
    public void longPressBehaviourValueNotPresent_returnsDefault() {
        when(mResources.getInteger(
                com.android.internal.R.integer.config_longPressOnPowerBehavior))
                .thenReturn(2);

        assertThat(PowerMenuSettingsUtils.getPowerButtonSettingValue(mContext)).isEqualTo(2);
    }

    @Test
    public void longPressBehaviourValueSetToAssistant_isAssistEnabledReturnsTrue() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.POWER_BUTTON_LONG_PRESS, 5);
        assertThat(PowerMenuSettingsUtils.isLongPressPowerForAssistEnabled(mContext)).isTrue();
    }

    @Test
    public void longPressBehaviourValueNotSetToAssistant_isAssistEnabledReturnsFalse() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.POWER_BUTTON_LONG_PRESS, 3);
        assertThat(PowerMenuSettingsUtils.isLongPressPowerForAssistEnabled(mContext)).isFalse();
    }

    @Test
    public void longPressBehaviourDefaultSetToAssistant_isAssistEnabledReturnsFalse() {
        when(mResources.getInteger(
                com.android.internal.R.integer.config_longPressOnPowerBehavior))
                .thenReturn(3);

        assertThat(PowerMenuSettingsUtils.isLongPressPowerForAssistEnabled(mContext)).isFalse();
    }
}
