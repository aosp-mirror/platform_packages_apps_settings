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

package com.android.settings.gestures;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.provider.Settings;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class OneHandedSettingsUtilsTest {

    private static final int TIMEOUT_INDEX_NEVER = 0;
    private static final int TIMEOUT_INDEX_SHORT = 1;
    private static final int TIMEOUT_INDEX_MEDIUM = 2;
    private static final int TIMEOUT_INDEX_LONG = 3;

    private Context mContext;
    private String[] mConfigTimeout;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mConfigTimeout = mContext.getResources().getStringArray(R.array.one_handed_timeout_values);
    }

    @Test
    public void setSettingsOneHandedModeEnabled_setEnable_shouldReturnEnabled() {
        OneHandedSettingsUtils.setSettingsOneHandedModeEnabled(mContext, true);

        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ONE_HANDED_MODE_ENABLED, 0)).isEqualTo(1);
    }

    @Test
    public void setSettingsOneHandedModeEnabled_setDisable_shouldReturnDisabled() {
        OneHandedSettingsUtils.setSettingsOneHandedModeEnabled(mContext, false);

        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ONE_HANDED_MODE_ENABLED, 0)).isEqualTo(0);
    }

    @Test
    public void setSettingsTapsAppToExitEnabled_setEnable_shouldReturnEnabled() {
        OneHandedSettingsUtils.setSettingsTapsAppToExit(mContext, true);

        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.TAPS_APP_TO_EXIT, 1)).isEqualTo(1);
    }

    @Test
    public void setSettingsTapsAppToExitEnabled_setDisable_shouldReturnDisabled() {
        OneHandedSettingsUtils.setSettingsTapsAppToExit(mContext, false);

        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.TAPS_APP_TO_EXIT, 1)).isEqualTo(0);
    }

    @Test
    public void setSettingsTimeout_setNever_shouldReturnNeverValue() {
        OneHandedSettingsUtils.setSettingsOneHandedModeTimeout(mContext,
                OneHandedSettingsUtils.OneHandedTimeout.NEVER.getValue());

        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ONE_HANDED_MODE_TIMEOUT,
                OneHandedSettingsUtils.OneHandedTimeout.NEVER.getValue()))
                .isEqualTo(Integer.parseInt(mConfigTimeout[TIMEOUT_INDEX_NEVER]));
    }

    @Test
    public void setSettingsTimeout_setShort_shouldReturnShortValue() {
        OneHandedSettingsUtils.setSettingsOneHandedModeTimeout(mContext,
                OneHandedSettingsUtils.OneHandedTimeout.SHORT.getValue());

        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ONE_HANDED_MODE_TIMEOUT,
                OneHandedSettingsUtils.OneHandedTimeout.SHORT.getValue()))
                .isEqualTo(Integer.parseInt(mConfigTimeout[TIMEOUT_INDEX_SHORT]));
    }

    @Test
    public void setSettingsTimeout_setMedium_shouldReturnMediumValue() {
        OneHandedSettingsUtils.setSettingsOneHandedModeTimeout(mContext,
                OneHandedSettingsUtils.OneHandedTimeout.MEDIUM.getValue());

        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ONE_HANDED_MODE_TIMEOUT,
                OneHandedSettingsUtils.OneHandedTimeout.MEDIUM.getValue()))
                .isEqualTo(Integer.parseInt(mConfigTimeout[TIMEOUT_INDEX_MEDIUM]));
    }

    @Test
    public void setSettingsTimeout_setLong_shouldReturnLongValue() {
        OneHandedSettingsUtils.setSettingsOneHandedModeTimeout(mContext,
                OneHandedSettingsUtils.OneHandedTimeout.LONG.getValue());

        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ONE_HANDED_MODE_TIMEOUT,
                OneHandedSettingsUtils.OneHandedTimeout.LONG.getValue()))
                .isEqualTo(Integer.parseInt(mConfigTimeout[TIMEOUT_INDEX_LONG]));
    }
}
