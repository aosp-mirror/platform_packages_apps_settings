/*
 * Copyright 2024 The Android Open Source Project
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

package com.android.settings.inputmethod;

import static android.platform.test.flag.junit.SetFlagsRule.DefaultInitValueType.DEVICE_DEFAULT;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.UserHandle;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;
import android.view.InputDevice;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowInputDevice;
import com.android.settings.testutils.shadow.ShadowSystemSettings;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Tests for {@link TouchpadThreeFingerTapPreferenceController} */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowSystemSettings.class,
        ShadowInputDevice.class,
})
public class TouchpadThreeFingerTapPreferenceControllerTest {
    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule(DEVICE_DEFAULT);
    @Mock
    LifecycleOwner mLifecycleOwner;

    private Context mContext;
    private TouchpadThreeFingerTapPreferenceController mController;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mController = new TouchpadThreeFingerTapPreferenceController(mContext, "three_finger_tap");
        ShadowInputDevice.reset();
    }

    @Test
    @EnableFlags(com.android.hardware.input.Flags.FLAG_TOUCHPAD_THREE_FINGER_TAP_SHORTCUT)
    public void getAvailabilityStatus_flagEnabledHasTouchPad() {
        int deviceId = 1;
        ShadowInputDevice.sDeviceIds = new int[]{deviceId};
        InputDevice device = ShadowInputDevice.makeInputDevicebyIdWithSources(deviceId,
                InputDevice.SOURCE_TOUCHPAD);
        ShadowInputDevice.addDevice(deviceId, device);

        assertEquals(mController.getAvailabilityStatus(), AVAILABLE);
    }

    @Test
    @EnableFlags(com.android.hardware.input.Flags.FLAG_TOUCHPAD_THREE_FINGER_TAP_SHORTCUT)
    public void getAvailabilityStatus_flagEnabledNoTouchPad() {
        int deviceId = 1;
        ShadowInputDevice.sDeviceIds = new int[]{deviceId};
        InputDevice device = ShadowInputDevice.makeInputDevicebyIdWithSources(deviceId,
                InputDevice.SOURCE_BLUETOOTH_STYLUS);
        ShadowInputDevice.addDevice(deviceId, device);

        assertEquals(mController.getAvailabilityStatus(), CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    @DisableFlags(com.android.hardware.input.Flags.FLAG_TOUCHPAD_THREE_FINGER_TAP_SHORTCUT)
    public void getAvailabilityStatus_flagDisabled() {
        int deviceId = 1;
        ShadowInputDevice.sDeviceIds = new int[]{deviceId};
        InputDevice device = ShadowInputDevice.makeInputDevicebyIdWithSources(deviceId,
                InputDevice.SOURCE_TOUCHPAD);
        ShadowInputDevice.addDevice(deviceId, device);

        assertEquals(mController.getAvailabilityStatus(), CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void onPause_logCurrentFillValue() {
        int customizationValue = 1;
        Settings.System.putIntForUser(mContext.getContentResolver(),
                Settings.System.TOUCHPAD_THREE_FINGER_TAP_CUSTOMIZATION, customizationValue,
                UserHandle.USER_CURRENT);

        mController.onStateChanged(mLifecycleOwner, Lifecycle.Event.ON_PAUSE);

        verify(mFeatureFactory.metricsFeatureProvider).action(
                    any(), eq(SettingsEnums.ACTION_TOUCHPAD_THREE_FINGER_TAP_CUSTOMIZATION_CHANGED),
                    eq(customizationValue));
    }
}
