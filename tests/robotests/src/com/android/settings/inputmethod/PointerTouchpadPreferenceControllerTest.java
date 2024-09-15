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

import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

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

/** Tests for {@link PointerTouchpadPreferenceController}. */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowInputDevice.class,
})
public final class PointerTouchpadPreferenceControllerTest {
    @Rule public MockitoRule rule = MockitoJUnit.rule();

    private PointerTouchpadPreferenceController mController;

    @Before
    public void initObjects() {
        Context context = ApplicationProvider.getApplicationContext();
        mController = new PointerTouchpadPreferenceController(context, "pointer_touchpad");
        ShadowInputDevice.reset();
    }

    @Test
    public void getAvailableStatus_noTouchpadOrMouseConditionallyUnavailable() {
        int deviceId = 1;
        ShadowInputDevice.sDeviceIds = new int[]{deviceId};
        InputDevice device = ShadowInputDevice.makeInputDevicebyIdWithSources(deviceId,
                InputDevice.SOURCE_BLUETOOTH_STYLUS);
        ShadowInputDevice.addDevice(deviceId, device);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
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
        int deviceId = 1;
        ShadowInputDevice.sDeviceIds = new int[]{deviceId};
        InputDevice device = ShadowInputDevice.makeInputDevicebyIdWithSources(deviceId,
                InputDevice.SOURCE_MOUSE);
        ShadowInputDevice.addDevice(deviceId, device);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }
}
