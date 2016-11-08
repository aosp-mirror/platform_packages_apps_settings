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

package com.android.settings.gestures;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.TwoStatePreference;

import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;
import java.util.List;

import static android.provider.Settings.Secure.CAMERA_DOUBLE_TWIST_TO_FLIP_ENABLED;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DoubleTwistPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceScreen mScreen;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SensorManager mSensorManager;
    private DoubleTwistPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new DoubleTwistPreferenceController(mContext);
    }

    @Test
    public void display_hasSensor_shouldDisplay() {
        // Mock sensors
        final List<Sensor> sensorList = new ArrayList<>();
        sensorList.add(mock(Sensor.class));
        when(mContext.getResources().getString(anyInt())).thenReturn("test");
        when(mContext.getSystemService(Context.SENSOR_SERVICE)).thenReturn(mSensorManager);
        when(mSensorManager.getSensorList(anyInt())).thenReturn(sensorList);
        when(sensorList.get(0).getName()).thenReturn("test");
        when(sensorList.get(0).getVendor()).thenReturn("test");
        when(mScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(mock(Preference.class));

        // Run through display
        mController.displayPreference(mScreen);

        // Verify preference is not removed
        verify(mScreen, never()).removePreference(any(Preference.class));
    }

    @Test
    public void display_noSensor_shouldNotDisplay() {
        when(mScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(mock(Preference.class));

        mController.displayPreference(mScreen);

        verify(mScreen).removePreference(any(Preference.class));
    }

    @Test
    public void display_differentSensor_shouldNotDisplay() {
        // Mock sensors
        final List<Sensor> sensorList = new ArrayList<>();
        sensorList.add(mock(Sensor.class));
        when(mContext.getResources().getString(anyInt())).thenReturn("test");
        when(mContext.getSystemService(Context.SENSOR_SERVICE)).thenReturn(mSensorManager);
        when(mSensorManager.getSensorList(anyInt())).thenReturn(sensorList);
        when(sensorList.get(0).getName()).thenReturn("not_test");
        when(mScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(mock(Preference.class));
        when(mScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(mock(Preference.class));

        // Run through display
        mController.displayPreference(mScreen);

        // Verify preference is not removed
        verify(mScreen).removePreference(any(Preference.class));
    }


    @Test
    public void updateState_preferenceSetCheckedWhenSettingIsOn() {
        // Mock a TwoStatePreference
        final TwoStatePreference preference = mock(TwoStatePreference.class);
        // Set the setting to be enabled.
        final Context context = ShadowApplication.getInstance().getApplicationContext();
        Settings.System.putInt(context.getContentResolver(),
                CAMERA_DOUBLE_TWIST_TO_FLIP_ENABLED, 1);

        // Run through updateState
        mController = new DoubleTwistPreferenceController(context);
        mController.updateState(preference);

        // Verify pref is checked (as setting is enabled).
        verify(preference).setChecked(true);
    }

    @Test
    public void updateState_preferenceSetUncheckedWhenSettingIsOff() {
        // Mock a TwoStatePreference
        final TwoStatePreference preference = mock(TwoStatePreference.class);
        // Set the setting to be disabled.
        final Context context = ShadowApplication.getInstance().getApplicationContext();
        Settings.System.putInt(context.getContentResolver(),
                CAMERA_DOUBLE_TWIST_TO_FLIP_ENABLED, 0);

        // Run through updateState
        mController = new DoubleTwistPreferenceController(context);
        mController.updateState(preference);

        // Verify pref is unchecked (as setting is disabled).
        verify(preference).setChecked(false);
    }

    @Test
    public void updateState_notTwoStatePreference_setSummary() {
        // Mock a regular preference
        final Preference preference = mock(Preference.class);
        // Set the setting to be disabled.
        final Context context = ShadowApplication.getInstance().getApplicationContext();
        Settings.System.putInt(context.getContentResolver(),
                CAMERA_DOUBLE_TWIST_TO_FLIP_ENABLED, 0);

        // Run through updateState
        mController = new DoubleTwistPreferenceController(context);
        mController.updateState(preference);

        // Verify summary is set to off (as setting is disabled).
        verify(preference).setSummary(com.android.settings.R.string.gesture_setting_off);
    }

}
