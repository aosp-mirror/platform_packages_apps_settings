/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.view.InputDevice;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.keyboard.Flags;
import com.android.settings.testutils.shadow.ShadowInputDevice;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Tests for {@link PhysicalKeyboardA11yPreferenceController} */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowInputDevice.class,
})
public class PhysicalKeyboardA11yPreferenceControllerTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();
    private static final String PREFERENCE_KEY = "physical_keyboard_a11y";
    private Context mContext;
    private PhysicalKeyboardA11yPreferenceController mController;
    @Mock
    InputDevice mInputDevice;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mController = new PhysicalKeyboardA11yPreferenceController(mContext, PREFERENCE_KEY);
    }

    @Test
    @EnableFlags(Flags.FLAG_KEYBOARD_AND_TOUCHPAD_A11Y_NEW_PAGE_ENABLED)
    public void getAvailabilityStatus_expected() {
        int deviceId = 1;
        ShadowInputDevice.sDeviceIds = new int[]{deviceId};
        when(mInputDevice.isVirtual()).thenReturn(false);
        when(mInputDevice.isFullKeyboard()).thenReturn(true);

        ShadowInputDevice.addDevice(deviceId, mInputDevice);

        assertThat(InputDevice.getDeviceIds()).isNotEmpty();
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);

    }

    @Test
    @EnableFlags(Flags.FLAG_KEYBOARD_AND_TOUCHPAD_A11Y_NEW_PAGE_ENABLED)
    public void getAvailabilityStatus_deviceIsNotAsExpected_unavailable() {
        int deviceId = 1;
        ShadowInputDevice.sDeviceIds = new int[]{deviceId};
        when(mInputDevice.isVirtual()).thenReturn(true);
        when(mInputDevice.isFullKeyboard()).thenReturn(false);

        ShadowInputDevice.addDevice(deviceId, mInputDevice);

        assertThat(InputDevice.getDeviceIds()).isNotEmpty();
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.CONDITIONALLY_UNAVAILABLE);

    }

}
