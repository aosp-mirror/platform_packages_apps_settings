/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.settings.fuelgauge.batterysaver;

import static com.android.settingslib.fuelgauge.BatterySaverUtils.KEY_PERCENTAGE;

import static com.google.common.truth.Truth.assertThat;

import android.content.ContentResolver;
import android.content.Context;
import android.os.PowerManager;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class BatterySaverScheduleRadioButtonsControllerTest {
    private Context mContext;
    private ContentResolver mResolver;
    private BatterySaverScheduleRadioButtonsController mController;
    private BatterySaverScheduleSeekBarController mSeekBarController;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mSeekBarController = new BatterySaverScheduleSeekBarController(mContext);
        mController = new BatterySaverScheduleRadioButtonsController(
                mContext, mSeekBarController);
        mResolver = mContext.getContentResolver();
    }

    @Test
    public void setDefaultKey_percentage_shouldSuppressNotification() {
        Secure.putInt(
                mContext.getContentResolver(), Secure.LOW_POWER_WARNING_ACKNOWLEDGED, 1);
        Settings.Global.putInt(mResolver, Global.AUTOMATIC_POWER_SAVE_MODE,
                PowerManager.POWER_SAVE_MODE_TRIGGER_PERCENTAGE);
        Settings.Global.putInt(mResolver, Global.LOW_POWER_MODE_TRIGGER_LEVEL, 5);
        mController.setDefaultKey(KEY_PERCENTAGE);

        final int result = Settings.Secure.getInt(mResolver,
                Secure.SUPPRESS_AUTO_BATTERY_SAVER_SUGGESTION, 0);
        assertThat(result).isEqualTo(1);
    }
}
