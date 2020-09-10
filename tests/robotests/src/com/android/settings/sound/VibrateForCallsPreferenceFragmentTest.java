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

package com.android.settings.sound;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class VibrateForCallsPreferenceFragmentTest {

    private static final int OFF = 0;
    private static final int ON = 1;
    private Context mContext;
    private ContentResolver mContentResolver;
    private VibrateForCallsPreferenceFragment mFragment;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);
        mContentResolver = mContext.getContentResolver();
        mFragment = spy(new VibrateForCallsPreferenceFragment());
        doReturn(mContext).when(mFragment).getContext();
        mFragment.onAttach(mContext);
    }

    @Test
    public void getDefaultKey_applyRampingRinger_keyRampingRinger() {
        Settings.System.putInt(mContentResolver, Settings.System.VIBRATE_WHEN_RINGING, OFF);
        Settings.Global.putInt(mContentResolver, Settings.Global.APPLY_RAMPING_RINGER, ON);

        assertThat(mFragment.getDefaultKey()).isEqualTo(
                VibrateForCallsPreferenceFragment.KEY_RAMPING_RINGER);
    }

    @Test
    public void getDefaultKey_enableVibrateWhenRinging_keyAlwaysVibrate() {
        Settings.System.putInt(mContentResolver, Settings.System.VIBRATE_WHEN_RINGING, ON);
        Settings.Global.putInt(mContentResolver, Settings.Global.APPLY_RAMPING_RINGER, OFF);

        assertThat(mFragment.getDefaultKey()).isEqualTo(
                VibrateForCallsPreferenceFragment.KEY_ALWAYS_VIBRATE);
    }

    @Test
    public void getDefaultKey_notApplyRampingRingerDisableVibrateWhenRinging_keyNeverVibrate() {
        Settings.System.putInt(mContentResolver, Settings.System.VIBRATE_WHEN_RINGING, OFF);
        Settings.Global.putInt(mContentResolver, Settings.Global.APPLY_RAMPING_RINGER, OFF);

        assertThat(mFragment.getDefaultKey()).isEqualTo(
                VibrateForCallsPreferenceFragment.KEY_NEVER_VIBRATE);
    }

    @Test
    public void setDefaultKey_keyRampingRinger_applyRampingRingerDisableVibrateWhenRinging() {
        mFragment.setDefaultKey(VibrateForCallsPreferenceFragment.KEY_RAMPING_RINGER);

        assertThat(Settings.Global.getInt(
            mContentResolver, Settings.Global.APPLY_RAMPING_RINGER, OFF)).isEqualTo(ON);
        assertThat(Settings.System.getInt(
            mContentResolver, Settings.System.VIBRATE_WHEN_RINGING, OFF)).isEqualTo(OFF);
    }

    @Test
    public void setDefaultKey_keyAlwaysVibrate_notApplyRampingRingerEnableVibrateWhenRinging() {
        mFragment.setDefaultKey(VibrateForCallsPreferenceFragment.KEY_ALWAYS_VIBRATE);

        assertThat(Settings.Global.getInt(
            mContentResolver, Settings.Global.APPLY_RAMPING_RINGER, OFF)).isEqualTo(OFF);
        assertThat(Settings.System.getInt(
            mContentResolver, Settings.System.VIBRATE_WHEN_RINGING, OFF)).isEqualTo(ON);
    }

    @Test
    public void setDefaultKey_keyNeverVibrate_notApplyRampingRingerDisableVibrateWhenRinging() {
        mFragment.setDefaultKey(VibrateForCallsPreferenceFragment.KEY_NEVER_VIBRATE);

        assertThat(Settings.Global.getInt(
            mContentResolver, Settings.Global.APPLY_RAMPING_RINGER, OFF)).isEqualTo(OFF);
        assertThat(Settings.System.getInt(
            mContentResolver, Settings.System.VIBRATE_WHEN_RINGING, OFF)).isEqualTo(OFF);
    }
}
