/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static android.provider.Settings.System.VIBRATE_WHEN_RINGING;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.settings.testutils.shadow.ShadowDeviceConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowContentResolver;

@RunWith(RobolectricTestRunner.class)
@Config(shadows={ShadowDeviceConfig.class})
public class VibrateWhenRingPreferenceControllerTest {

    private static final String KEY_VIBRATE_WHEN_RINGING = "vibrate_when_ringing";
    private final int DEFAULT_VALUE = 0;
    private final int NOTIFICATION_VIBRATE_WHEN_RINGING = 1;
    private Context mContext;
    private ContentResolver mContentResolver;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private TelephonyManager mTelephonyManager;
    private VibrateWhenRingPreferenceController mController;
    private Preference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mContentResolver = mContext.getContentResolver();
        when(mContext.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(mTelephonyManager);
        mController = new VibrateWhenRingPreferenceController(mContext, KEY_VIBRATE_WHEN_RINGING);
        mPreference = new Preference(mContext);
        mPreference.setKey(mController.getPreferenceKey());
        when(mScreen.findPreference(mPreference.getKey())).thenReturn(mPreference);
    }

    @Test
    public void display_shouldDisplay() {
        when(mTelephonyManager.isVoiceCapable()).thenReturn(true);
        DeviceConfig.setProperty("telephony", "ramping_ringer_enabled", "false", false);
        mController.displayPreference(mScreen);
        assertThat(mPreference.isVisible()).isTrue();
    }

    @Test
    public void display_shouldNotDisplay_notVoiceCapable() {
        when(mTelephonyManager.isVoiceCapable()).thenReturn(false);
        DeviceConfig.setProperty("telephony", "ramping_ringer_enabled", "false", false);
        mController.displayPreference(mScreen);
        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void display_shouldNotDisplay_RampingRingerEnabled() {
        when(mTelephonyManager.isVoiceCapable()).thenReturn(true);
        DeviceConfig.setProperty("telephony", "ramping_ringer_enabled", "true", false);
        mController.displayPreference(mScreen);
        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void display_shouldNotDisplay_VoiceEnabled_RampingRingerEnabled() {
        when(mTelephonyManager.isVoiceCapable()).thenReturn(true);
        DeviceConfig.setProperty("telephony", "ramping_ringer_enabled", "true", false);
        mController.displayPreference(mScreen);
        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void display_shouldNotDisplay_VoiceDisabled_RampingRingerEnabled() {
        when(mTelephonyManager.isVoiceCapable()).thenReturn(false);
        DeviceConfig.setProperty("telephony", "ramping_ringer_enabled", "true", false);
        mController.displayPreference(mScreen);
        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void testOnPreferenceChange_turnOn_returnOn() {
        mController.onPreferenceChange(null, true);
        final int mode = Settings.System.getInt(mContext.getContentResolver(),
                VIBRATE_WHEN_RINGING, DEFAULT_VALUE);

        assertThat(mode).isEqualTo(NOTIFICATION_VIBRATE_WHEN_RINGING);
    }

    @Test
    public void testOnPreferenceChange_turnOff_returnOff() {
        mController.onPreferenceChange(null, false);
        final int mode = Settings.System.getInt(mContext.getContentResolver(),
                VIBRATE_WHEN_RINGING, DEFAULT_VALUE);

        assertThat(mode).isEqualTo(DEFAULT_VALUE);
    }

    @Test
    public void voiceCapable_availabled() {
        when(mTelephonyManager.isVoiceCapable()).thenReturn(true);
        DeviceConfig.setProperty("telephony", "ramping_ringer_enabled", "false", false);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void voiceCapable_notAvailabled() {
        when(mTelephonyManager.isVoiceCapable()).thenReturn(false);
        DeviceConfig.setProperty("telephony", "ramping_ringer_enabled", "false", false);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void updateState_settingIsOn_preferenceShouldBeChecked() {
        final TwoStatePreference preference = mock(TwoStatePreference.class);
        Settings.System.putInt(mContext.getContentResolver(), VIBRATE_WHEN_RINGING, 1);

        mController.updateState(preference);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void updateState_settingIsOff_preferenceShouldNotBeChecked() {
        final TwoStatePreference preference = mock(TwoStatePreference.class);
        Settings.System.putInt(mContext.getContentResolver(), VIBRATE_WHEN_RINGING, 0);

        mController.updateState(preference);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void setChecked_settingsIsOn() {
        mController.setChecked(true);
        final int mode = Settings.System.getInt(mContext.getContentResolver(), VIBRATE_WHEN_RINGING,
                -1);

        assertThat(mode).isEqualTo(NOTIFICATION_VIBRATE_WHEN_RINGING);
    }

    @Test
    public void setChecked_settingsIsOff() {
        mController.setChecked(false);
        final int mode = Settings.System.getInt(mContext.getContentResolver(), VIBRATE_WHEN_RINGING,
                -1);

        assertThat(mode).isEqualTo(DEFAULT_VALUE);
    }

    @Test
    public void testObserver_onResume_shouldRegisterObserver() {
        final ShadowContentResolver shadowContentResolver = Shadow.extract(mContentResolver);
        mController.displayPreference(mScreen);

        mController.onResume();

        assertThat(shadowContentResolver.getContentObservers(
                Settings.System.getUriFor(VIBRATE_WHEN_RINGING))).isNotEmpty();
    }

    @Test
    public void testObserver_onPause_shouldUnregisterObserver() {
        final ShadowContentResolver shadowContentResolver = Shadow.extract(mContentResolver);
        mController.displayPreference(mScreen);

        mController.onResume();
        mController.onPause();

        assertThat(shadowContentResolver.getContentObservers(
                Settings.System.getUriFor(VIBRATE_WHEN_RINGING))).isEmpty();
    }

    @Test
    public void isSliceableCorrectKey_returnsTrue() {
        final VibrateWhenRingPreferenceController controller =
                new VibrateWhenRingPreferenceController(mContext, "vibrate_when_ringing");
        assertThat(controller.isSliceable()).isTrue();
    }

    @Test
    public void isSliceableIncorrectKey_returnsFalse() {
        final VibrateWhenRingPreferenceController controller =
                new VibrateWhenRingPreferenceController(mContext, "bad_key");
        assertThat(controller.isSliceable()).isFalse();
    }

}
