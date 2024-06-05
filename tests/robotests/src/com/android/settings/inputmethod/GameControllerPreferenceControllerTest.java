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

package com.android.settings.inputmethod;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.hardware.input.InputManager;
import android.view.InputDevice;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public class GameControllerPreferenceControllerTest {

    @Mock
    private InputManager mInputManager;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private InputDevice mInputDevice;

    private Context mContext;
    private GameControllerPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(Context.INPUT_SERVICE)).thenReturn(mInputManager);
        mController = new GameControllerPreferenceController(mContext, "test_key");
    }

    @Test
    public void testLifecycle_shouldRegisterInputManager() {
        mController.onResume();

        // register is called, but unregister should not be called.
        verify(mInputManager).registerInputDeviceListener(mController, null);
        verify(mInputManager, never()).unregisterInputDeviceListener(mController);

        mController.onPause();
        // register is not called any more times, but unregister should be called once.
        verify(mInputManager).registerInputDeviceListener(mController, null);
        verify(mInputManager).unregisterInputDeviceListener(mController);
    }

    @Test
    public void getAvailabilityStatus_hasDeviceWithVibrator_shouldReturnAvailable() {
        when(mInputManager.getInputDeviceIds()).thenReturn(new int[] {1});
        when(mInputManager.getInputDevice(1)).thenReturn(mInputDevice);
        when(mInputDevice.isVirtual()).thenReturn(false);
        when(mInputDevice.getVibrator().hasVibrator()).thenReturn(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_hasNoVibratingDevice_shouldReturnDisabled() {
        when(mInputManager.getInputDeviceIds()).thenReturn(new int[] {1});
        when(mInputManager.getInputDevice(1)).thenReturn(mInputDevice);
        when(mInputDevice.isVirtual()).thenReturn(false);
        when(mInputDevice.getVibrator().hasVibrator()).thenReturn(false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_hasNoPhysicalDevice_shouldReturnDisabled() {
        when(mInputManager.getInputDeviceIds()).thenReturn(new int[] {1});
        when(mInputManager.getInputDevice(1)).thenReturn(mInputDevice);
        when(mInputDevice.isVirtual()).thenReturn(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_hasNoDevice_shouldReturnDisabled() {
        when(mInputManager.getInputDeviceIds()).thenReturn(new int[] {});

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void getAvailabilityStatus_ifDisabled_shouldReturnDisabled() {
        mController = new GameControllerPreferenceController(mContext, "testkey");

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void setChecked_toEnabled_shouldSetToSettingsProvider() {
        mController.setChecked(true);
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void setChecked_toDisabled_shouldSetToSettingsProvider() {
        mController.setChecked(true);
        mController.setChecked(false);
        assertThat(mController.isChecked()).isFalse();
    }
}
