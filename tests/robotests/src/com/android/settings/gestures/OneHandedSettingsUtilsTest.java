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
import android.os.UserHandle;
import android.provider.Settings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class OneHandedSettingsUtilsTest {

    private static final int OFF = 0;
    private static final int ON = 1;

    private Context mContext;

    private int mCurrentUserId;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mCurrentUserId = UserHandle.myUserId();
        OneHandedSettingsUtils.setUserId(mCurrentUserId);
    }

    @Test
    public void setOneHandedModeEnabled_setEnable_shouldReturnEnabled() {
        OneHandedSettingsUtils.setOneHandedModeEnabled(mContext, true);

        assertThat(Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.ONE_HANDED_MODE_ENABLED, OFF, mCurrentUserId)).isEqualTo(ON);
    }

    @Test
    public void setOneHandedModeEnabled_setDisable_shouldReturnDisabled() {
        OneHandedSettingsUtils.setOneHandedModeEnabled(mContext, false);

        assertThat(Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.ONE_HANDED_MODE_ENABLED, OFF, mCurrentUserId)).isEqualTo(OFF);
    }

    @Test
    public void setTapsAppToExitEnabled_setEnable_shouldReturnEnabled() {
        OneHandedSettingsUtils.setTapsAppToExitEnabled(mContext, true);

        assertThat(Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.TAPS_APP_TO_EXIT, OFF, mCurrentUserId)).isEqualTo(ON);
    }

    @Test
    public void setTapsAppToExitEnabled_setDisable_shouldReturnDisabled() {
        OneHandedSettingsUtils.setTapsAppToExitEnabled(mContext, false);

        assertThat(Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.TAPS_APP_TO_EXIT, OFF, mCurrentUserId)).isEqualTo(OFF);
    }

    @Test
    public void setTimeout_setNever_shouldReturnNeverValue() {
        OneHandedSettingsUtils.setTimeoutValue(mContext,
                OneHandedSettingsUtils.OneHandedTimeout.NEVER.getValue());

        assertThat(Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.ONE_HANDED_MODE_TIMEOUT,
                OneHandedSettingsUtils.OneHandedTimeout.NEVER.getValue(), mCurrentUserId))
                .isEqualTo(0);
    }

    @Test
    public void setTimeout_setShort_shouldReturnShortValue() {
        OneHandedSettingsUtils.setTimeoutValue(mContext,
                OneHandedSettingsUtils.OneHandedTimeout.SHORT.getValue());

        assertThat(Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.ONE_HANDED_MODE_TIMEOUT,
                OneHandedSettingsUtils.OneHandedTimeout.SHORT.getValue(), mCurrentUserId))
                .isEqualTo(4);
    }

    @Test
    public void setTimeout_setMedium_shouldReturnMediumValue() {
        OneHandedSettingsUtils.setTimeoutValue(mContext,
                OneHandedSettingsUtils.OneHandedTimeout.MEDIUM.getValue());

        assertThat(Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.ONE_HANDED_MODE_TIMEOUT,
                OneHandedSettingsUtils.OneHandedTimeout.MEDIUM.getValue(), mCurrentUserId))
                .isEqualTo(8);
    }

    @Test
    public void setTimeout_setLong_shouldReturnLongValue() {
        OneHandedSettingsUtils.setTimeoutValue(mContext,
                OneHandedSettingsUtils.OneHandedTimeout.LONG.getValue());

        assertThat(Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.ONE_HANDED_MODE_TIMEOUT,
                OneHandedSettingsUtils.OneHandedTimeout.LONG.getValue(), mCurrentUserId))
                .isEqualTo(12);
    }
}
