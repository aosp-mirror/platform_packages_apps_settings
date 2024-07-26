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

import static android.view.flags.Flags.enableVectorCursorA11ySettings;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.view.InputDevice;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.testutils.shadow.ShadowInputDevice;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Tests for {@link TrackpadSettingsController} */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowSystemSettings.class,
        ShadowInputDevice.class,
})
public class TrackpadSettingsControllerTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private static final String PREFERENCE_KEY = "trackpad_settings";

    private Context mContext;
    private TrackpadSettingsController mController;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mController = new TrackpadSettingsController(mContext, PREFERENCE_KEY);
        ShadowInputDevice.reset();
    }

    @Test
    public void getAvailabilityStatus_isTouchpadAvailable() {
        int deviceId = 1;
        ShadowInputDevice.sDeviceIds = new int[]{deviceId};
        InputDevice device = ShadowInputDevice.makeInputDevicebyIdWithSources(deviceId,
                InputDevice.SOURCE_TOUCHPAD);
        ShadowInputDevice.addDevice(deviceId, device);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_isMouseAvailable() {
        assumeTrue(enableVectorCursorA11ySettings());

        int deviceId = 1;
        ShadowInputDevice.sDeviceIds = new int[]{deviceId};
        InputDevice device = ShadowInputDevice.makeInputDevicebyIdWithSources(deviceId,
                InputDevice.SOURCE_MOUSE);
        ShadowInputDevice.addDevice(deviceId, device);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_noMouseOrTouchpadUnavailable() {
        int deviceId = 1;
        ShadowInputDevice.sDeviceIds = new int[]{deviceId};
        InputDevice device = ShadowInputDevice.makeInputDevicebyIdWithSources(deviceId,
                InputDevice.SOURCE_UNKNOWN);
        ShadowInputDevice.addDevice(deviceId, device);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }
}
