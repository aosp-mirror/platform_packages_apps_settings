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

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowDeviceConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowDeviceConfig.class})
public class VibrateForCallsPreferenceControllerTest {

    private static final int OFF = 0;
    private static final int ON = 1;
    private Context mContext;
    private ContentResolver mContentResolver;
    @Mock
    private TelephonyManager mTelephonyManager;
    private VibrateForCallsPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mContentResolver = mContext.getContentResolver();
        when(mContext.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(mTelephonyManager);
        mController = new VibrateForCallsPreferenceController(
            mContext, VibrateForCallsPreferenceController.RAMPING_RINGER_ENABLED);
    }

    @Test
    public void getAvailabilityStatus_notVoiceCapable_returnUnsupportedOnDevice() {
        when(mTelephonyManager.isVoiceCapable()).thenReturn(false);
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_TELEPHONY,
                VibrateForCallsPreferenceController.RAMPING_RINGER_ENABLED, "false", false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_rampingRingerEnabled_returnUnsupportedOnDevice() {
        when(mTelephonyManager.isVoiceCapable()).thenReturn(true);
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_TELEPHONY,
                VibrateForCallsPreferenceController.RAMPING_RINGER_ENABLED, "true", false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_voiceCapableAndRampingRingerDisabled_returnAvailable() {
        when(mTelephonyManager.isVoiceCapable()).thenReturn(true);
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_TELEPHONY,
                VibrateForCallsPreferenceController.RAMPING_RINGER_ENABLED, "false", false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getSummary_applyRampingRinger_rampingRingerSummary() {
        Settings.System.putInt(mContentResolver, Settings.System.VIBRATE_WHEN_RINGING, OFF);
        Settings.Global.putInt(mContentResolver, Settings.Global.APPLY_RAMPING_RINGER, ON);

        assertThat(mController.getSummary()).isEqualTo(
                mContext.getText(R.string.vibrate_when_ringing_option_ramping_ringer));
    }

    @Test
    public void getSummary_enableVibrateWhenRinging_alwaysVibrateSummary() {
        Settings.System.putInt(mContentResolver, Settings.System.VIBRATE_WHEN_RINGING, ON);
        Settings.Global.putInt(mContentResolver, Settings.Global.APPLY_RAMPING_RINGER, OFF);

        assertThat(mController.getSummary()).isEqualTo(
                mContext.getText(R.string.vibrate_when_ringing_option_always_vibrate));
    }

    @Test
    public void getSummary_notApplyRampingRingerDisableVibrateWhenRinging_neverVibrateSummary() {
        Settings.System.putInt(mContentResolver, Settings.System.VIBRATE_WHEN_RINGING, OFF);
        Settings.Global.putInt(mContentResolver, Settings.Global.APPLY_RAMPING_RINGER, OFF);

        assertThat(mController.getSummary()).isEqualTo(
                mContext.getText(R.string.vibrate_when_ringing_option_never_vibrate));
    }
}
