/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.hardware.input.InputDeviceIdentifier;
import android.hardware.input.InputManager;
import android.hardware.input.KeyboardLayout;
import android.view.InputDevice;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.testutils.shadow.ShadowInputDevice;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class KeyboardLayoutPickerControllerTest {

    @Mock
    private Fragment mFragment;
    @Mock
    private InputManager mInputManager;

    private Context mContext;
    private InputDeviceIdentifier mInputDeviceIdentifier;
    private KeyboardLayoutPickerController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        final ShadowApplication shadowContext = ShadowApplication.getInstance();
        shadowContext.setSystemService(Context.INPUT_SERVICE, mInputManager);

        mContext = RuntimeEnvironment.application;
        mInputDeviceIdentifier = new InputDeviceIdentifier("descriptor", 1, 1);
        mController = new KeyboardLayoutPickerController(mContext, "pref_key");

        initializeOneLayout();
    }

    @Test
    public void isAlwaysAvailable() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void testLifecycle_onStart_shouldRegisterInputManager() {
        final FragmentActivity activity = Robolectric.setupActivity(FragmentActivity.class);
        when(mFragment.getActivity()).thenReturn(activity);

        mController.onStart();

        // Register is called, but unregister should not be called.
        verify(mInputManager).registerInputDeviceListener(mController, null);
        verify(mInputManager, never()).unregisterInputDeviceListener(mController);
    }

    @Test
    public void testLifecycle_onStart_NoInputDevice_shouldReturn() {
        final FragmentActivity activity = Robolectric.setupActivity(FragmentActivity.class);
        when(mInputManager.getInputDeviceByDescriptor(anyString())).thenReturn(null);
        when(mFragment.getActivity()).thenReturn(activity);

        mController.onStart();
        verify(mInputManager, never()).getEnabledKeyboardLayoutsForInputDevice(any());
    }

    @Test
    public void testLifecycle_onStop_shouldCancelRegisterInputManager() {
        mController.onStop();

        // Unregister is called, but register should not be called.
        verify(mInputManager).unregisterInputDeviceListener(mController);
        verify(mInputManager, never()).registerInputDeviceListener(mController, null);
    }

    @Test
    public void test_createPreferenceHierarchy_shouldAddOnePreference() {
        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        final PreferenceScreen screen = preferenceManager.createPreferenceScreen(mContext);

        mController.displayPreference(screen);

        // We create a keyboard layouts in initializeOneLayout()
        assertThat(screen.getPreferenceCount()).isEqualTo(1);
    }

    @Test
    public void test_createPreferenceHierarchy_shouldAddTwoPreference() {
        initializeTwoLayouts();
        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        final PreferenceScreen screen = preferenceManager.createPreferenceScreen(mContext);

        mController.displayPreference(screen);

        // We create two keyboard layouts in initializeOneLayout()
        assertThat(screen.getPreferenceCount()).isEqualTo(2);
    }

    @Test
    @Config(shadows = ShadowInputDevice.class)
    public void testOnDeviceRemove_getSameDevice_shouldFinish() {
        final int TARGET_DEVICE_ID = 1;
        final FragmentActivity activity = Robolectric.setupActivity(FragmentActivity.class);
        final String[] enableKeyboardLayouts = {"layout1"};
        final InputDevice device = ShadowInputDevice.makeInputDevicebyId(TARGET_DEVICE_ID);

        when(mFragment.getActivity()).thenReturn(activity);
        when(mInputManager.getInputDeviceByDescriptor(anyString())).thenReturn(device);
        when(mInputManager.getEnabledKeyboardLayoutsForInputDevice(
                any(InputDeviceIdentifier.class))).thenReturn(enableKeyboardLayouts);

        mController.onStart();
        mController.onInputDeviceRemoved(TARGET_DEVICE_ID);

        assertThat(activity.isFinishing()).isTrue();
    }

    @Test
    @Config(shadows = ShadowInputDevice.class)
    public void testOnDeviceRemove_getDifferentDevice_shouldNotFinish() {
        final int TARGET_DEVICE_ID = 1;
        final int ANOTHER_DEVICE_ID = 2;
        final FragmentActivity activity = Robolectric.setupActivity(FragmentActivity.class);
        final String[] enableKeyboardLayouts = {"layout1"};
        final InputDevice device = ShadowInputDevice.makeInputDevicebyId(TARGET_DEVICE_ID);

        when(mFragment.getActivity()).thenReturn(activity);
        when(mInputManager.getInputDeviceByDescriptor(anyString())).thenReturn(device);
        when(mInputManager.getEnabledKeyboardLayoutsForInputDevice(
                any(InputDeviceIdentifier.class))).thenReturn(enableKeyboardLayouts);

        mController.onStart();
        mController.onInputDeviceRemoved(ANOTHER_DEVICE_ID);

        assertThat(activity.isFinishing()).isFalse();
    }

    private void initializeOneLayout() {
        final KeyboardLayout[] keyboardLayouts = {new KeyboardLayout("", "", "", 1, null, 0, 1, 1)};
        when(mInputManager.getKeyboardLayoutsForInputDevice(
                any(InputDeviceIdentifier.class))).thenReturn(
                keyboardLayouts);

        mController.initialize(mFragment, mInputDeviceIdentifier);
    }

    private void initializeTwoLayouts() {
        final KeyboardLayout[] keyboardLayouts = {new KeyboardLayout("", "", "", 1, null, 0, 1, 1),
                new KeyboardLayout("", "", "", 2, null, 0, 2, 2)};
        when(mInputManager.getKeyboardLayoutsForInputDevice(any(InputDeviceIdentifier.class))).
                thenReturn(keyboardLayouts);

        mController.initialize(mFragment, mInputDeviceIdentifier);
    }
}
