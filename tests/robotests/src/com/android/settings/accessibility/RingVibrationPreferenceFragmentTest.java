/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.provider.Settings;

import com.android.settings.testutils.shadow.ShadowDeviceConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public class RingVibrationPreferenceFragmentTest {

    private Context mContext;
    private RingVibrationPreferenceFragment mFragment;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mFragment = spy(new RingVibrationPreferenceFragment());
        doReturn(mContext).when(mFragment).getContext();
    }

    @Test
    @Config(shadows = {ShadowDeviceConfig.class})
    public void getVibrationEnabledSetting_rampingRingerEnabled_returnApplyRampingRinger() {
        // Turn on both flags to enable ramping ringer.
        Settings.Global.putInt(
                mContext.getContentResolver(), Settings.Global.APPLY_RAMPING_RINGER, 1 /* ON */);
        assertThat(mFragment.getVibrationEnabledSetting()).isEqualTo(
            Settings.Global.APPLY_RAMPING_RINGER);
    }

    @Test
    public void getVibrationEnabledSetting_rampingRingerDisabled_returnVibrationWhenRinging() {
        // Turn off Settings.Global.APPLY_RAMPING_RINGER to disable ramping ringer.
        Settings.Global.putInt(
                mContext.getContentResolver(), Settings.Global.APPLY_RAMPING_RINGER, 0 /* OFF */);
        assertThat(mFragment.getVibrationEnabledSetting()).isEqualTo(
            Settings.System.VIBRATE_WHEN_RINGING);
    }
}
