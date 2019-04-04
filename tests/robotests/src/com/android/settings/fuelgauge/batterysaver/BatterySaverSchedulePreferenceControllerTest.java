/*
 * Copyright (C) 2018 The Android Open Source Project
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
 *
 */
package com.android.settings.fuelgauge.batterysaver;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.os.PowerManager;
import android.provider.Settings;
import android.provider.Settings.Global;

import androidx.preference.Preference;

import com.android.settings.testutils.shadow.SettingsShadowResources;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = SettingsShadowResources.class)
public class BatterySaverSchedulePreferenceControllerTest {

    private static final int TRIGGER_LEVEL = 20;
    private static final int DEFAULT_LEVEL = 15;

    private BatterySaverSchedulePreferenceController mController;
    private Context mContext;
    private Preference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        SettingsShadowResources.overrideResource(
                com.android.internal.R.integer.config_lowBatteryWarningLevel, DEFAULT_LEVEL);
        mContext = RuntimeEnvironment.application;
        mController = new BatterySaverSchedulePreferenceController(mContext);
        mPreference = new Preference(mContext);
        mController.mBatterySaverSchedulePreference = mPreference;
    }

    @Test
    public void testPreference_lowPowerLevelZero_percentageMode_summaryNoSchedule() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.LOW_POWER_MODE_TRIGGER_LEVEL, 0);
        Settings.Global.putInt(mContext.getContentResolver(),
                Global.AUTOMATIC_POWER_SAVE_MODE, PowerManager.POWER_SAVE_MODE_TRIGGER_PERCENTAGE);

        mController.updateState(mPreference);

        assertThat(mPreference.getSummary()).isEqualTo("No schedule");
    }

    @Test
    public void testPreference_lowPowerLevelNonZero_percentageMode_summaryPercentage() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.LOW_POWER_MODE_TRIGGER_LEVEL, TRIGGER_LEVEL);
        Settings.Global.putInt(mContext.getContentResolver(),
                Global.AUTOMATIC_POWER_SAVE_MODE, PowerManager.POWER_SAVE_MODE_TRIGGER_PERCENTAGE);

        mController.updateState(mPreference);

        assertThat(mPreference.getSummary()).isEqualTo("Will turn on at 20%");
    }

    @Test
    public void testPreference_percentageRoutine_summaryRoutine() {
        // It doesn't matter what this is set to for routine mode
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.LOW_POWER_MODE_TRIGGER_LEVEL, TRIGGER_LEVEL);
        Settings.Global.putInt(mContext.getContentResolver(),
                Global.AUTOMATIC_POWER_SAVE_MODE, PowerManager.POWER_SAVE_MODE_TRIGGER_DYNAMIC);

        mController.updateState(mPreference);

        assertThat(mPreference.getSummary()).isEqualTo("Based on your routine");
    }
}
