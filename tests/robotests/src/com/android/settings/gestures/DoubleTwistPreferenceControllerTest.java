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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.UserManager;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.testutils.shadow.ShadowDoubleTwistPreferenceController;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = SettingsShadowResources.class)
public class DoubleTwistPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SensorManager mSensorManager;
    private DoubleTwistPreferenceController mController;
    private static final String KEY_DOUBLE_TWIST = "gesture_double_twist";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mock(UserManager.class));
        mController = new DoubleTwistPreferenceController(mContext, KEY_DOUBLE_TWIST);
    }

    @After
    public void tearDown() {
        SettingsShadowResources.reset();
    }

    @Test
    public void isAvailable_hasSensor_shouldReturnTrue() {
        // Mock sensors
        final List<Sensor> sensorList = new ArrayList<>();
        sensorList.add(mock(Sensor.class));
        when(mContext.getResources().getString(anyInt())).thenReturn("test");
        when(mContext.getSystemService(Context.SENSOR_SERVICE)).thenReturn(mSensorManager);
        when(mSensorManager.getSensorList(anyInt())).thenReturn(sensorList);
        when(sensorList.get(0).getName()).thenReturn("test");
        when(sensorList.get(0).getVendor()).thenReturn("test");

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_noSensor_shouldReturnFalse() {
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_differentSensor_shouldReturnFalse() {
        // Mock sensors
        final List<Sensor> sensorList = new ArrayList<>();
        sensorList.add(mock(Sensor.class));
        when(mContext.getResources().getString(anyInt())).thenReturn("test");
        when(mContext.getSystemService(Context.SENSOR_SERVICE)).thenReturn(mSensorManager);
        when(mSensorManager.getSensorList(anyInt())).thenReturn(sensorList);
        when(sensorList.get(0).getName()).thenReturn("not_test");

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isSuggestionCompleted_doubleTwist_trueWhenNotAvailable() {
        SettingsShadowResources.overrideResource(
                R.string.gesture_double_twist_sensor_name, "nonexistant name");
        SettingsShadowResources.overrideResource(
                R.string.gesture_double_twist_sensor_vendor, "nonexistant vendor");

        assertThat(DoubleTwistPreferenceController.isSuggestionComplete(
                RuntimeEnvironment.application, null /* prefs */))
                .isTrue();
    }

    @Test
    @Config(shadows = ShadowDoubleTwistPreferenceController.class)
    public void onPreferenceChange_hasWorkProfile_shouldUpdateSettingForWorkProfileUser() {
        final int managedId = 2;
        Settings.Secure.putIntForUser(
                null, Settings.Secure.CAMERA_DOUBLE_TWIST_TO_FLIP_ENABLED, 0, managedId);
        DoubleTwistPreferenceController controller =
                spy(new DoubleTwistPreferenceController(mContext, KEY_DOUBLE_TWIST));
        ShadowDoubleTwistPreferenceController.setManagedProfileId(managedId);

        // enable the gesture
        controller.onPreferenceChange(null, true);
        assertThat(Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.CAMERA_DOUBLE_TWIST_TO_FLIP_ENABLED, 0, managedId)).isEqualTo(1);

        // disable the gesture
        controller.onPreferenceChange(null, false);
        assertThat(Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.CAMERA_DOUBLE_TWIST_TO_FLIP_ENABLED, 1, managedId)).isEqualTo(0);
    }

    @Test
    public void testIsChecked_configIsSet_shouldReturnTrue() {
        // Set the setting to be enabled.
        final Context context = RuntimeEnvironment.application;
        Settings.Secure.putInt(context.getContentResolver(),
                Settings.Secure.CAMERA_DOUBLE_TWIST_TO_FLIP_ENABLED, 1);
        mController = new DoubleTwistPreferenceController(context, KEY_DOUBLE_TWIST);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void testIsChecked_configIsNotSet_shouldReturnFalse() {
        // Set the setting to be disabled.
        final Context context = RuntimeEnvironment.application;
        Settings.Secure.putInt(context.getContentResolver(),
                Settings.Secure.CAMERA_DOUBLE_TWIST_TO_FLIP_ENABLED, 0);
        mController = new DoubleTwistPreferenceController(context, KEY_DOUBLE_TWIST);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isSliceableCorrectKey_returnsTrue() {
        final DoubleTwistPreferenceController controller =
        new DoubleTwistPreferenceController(mContext,"gesture_double_twist");
        assertThat(controller.isSliceable()).isTrue();
    }

    @Test
    public void isSliceableIncorrectKey_returnsFalse() {
        final DoubleTwistPreferenceController controller =
        new DoubleTwistPreferenceController(mContext, "bad_key");
        assertThat(controller.isSliceable()).isFalse();
    }

    @Test
    public void isPublicSlice_returnTrue() {
        assertThat(mController.isPublicSlice()).isTrue();
    }
}
