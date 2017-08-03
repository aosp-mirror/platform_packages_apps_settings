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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.hardware.input.InputManager;
import android.view.InputDevice;

import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class GameControllerPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private InputManager mInputManager;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private InputDevice mInputDevice;

    private GameControllerPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(Context.INPUT_SERVICE)).thenReturn(mInputManager);
        mController = new GameControllerPreferenceController(mContext);
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
    public void testIsAvailable_hasDeviceWithVibrator_shouldReturnTrue() {
        when(mInputManager.getInputDeviceIds()).thenReturn(new int[]{1});
        when(mInputManager.getInputDevice(1)).thenReturn(mInputDevice);
        when(mInputDevice.isVirtual()).thenReturn(false);
        when(mInputDevice.getVibrator().hasVibrator()).thenReturn(true);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void testIsAvailable_hasNoVibratingDevice_shouldReturnFalse() {
        when(mInputManager.getInputDeviceIds()).thenReturn(new int[]{1});
        when(mInputManager.getInputDevice(1)).thenReturn(mInputDevice);
        when(mInputDevice.isVirtual()).thenReturn(false);
        when(mInputDevice.getVibrator().hasVibrator()).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void testIsAvailable_hasNoPhysicalDevice_shouldReturnFalse() {
        when(mInputManager.getInputDeviceIds()).thenReturn(new int[]{1});
        when(mInputManager.getInputDevice(1)).thenReturn(mInputDevice);
        when(mInputDevice.isVirtual()).thenReturn(true);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void testIsAvailable_hasNoDevice_shouldReturnFalse() {
        when(mInputManager.getInputDeviceIds()).thenReturn(new int[]{});

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void updateNonIndexableKeys_shouldIncludeCategoryAndPrefKeys() {
        when(mInputManager.getInputDeviceIds()).thenReturn(new int[]{});


        final List<String> nonIndexables = new ArrayList<>();
        mController.updateNonIndexableKeys(nonIndexables);

        assertThat(mController.isAvailable()).isFalse();
        assertThat(nonIndexables).containsExactlyElementsIn(Arrays.asList(
                GameControllerPreferenceController.PREF_KEY,
                mController.getPreferenceKey()));
    }
}
