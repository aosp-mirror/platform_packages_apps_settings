/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.hardware.input.InputManager;
import android.view.InputDevice;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowInputDevice;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public class PhysicalKeyboardPreferenceControllerTest {

    @Mock
    private Context mContext;
    @Mock
    private InputManager mIm;
    @Mock
    private Preference mPreference;

    private PhysicalKeyboardPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(InputManager.class)).thenReturn(mIm);
        mController = new PhysicalKeyboardPreferenceController(mContext, null /* lifecycle */);
    }

    @After
    public void tearDown() {
        ShadowInputDevice.reset();
    }

    @Test
    public void testPhysicalKeyboard_byDefault_shouldBeShown() {
        final Context context = spy(RuntimeEnvironment.application.getApplicationContext());
        mController = new PhysicalKeyboardPreferenceController(context, null);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void testPhysicalKeyboard_ifDisabled_shouldNotBeShown() {
        final Context context = spy(RuntimeEnvironment.application.getApplicationContext());
        mController = new PhysicalKeyboardPreferenceController(context, null);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    @Config(shadows = ShadowInputDevice.class)
    public void updateState_noKeyboard_setDisconnectedSummary() {
        ShadowInputDevice.sDeviceIds = new int[0];
        mController.updateState(mPreference);

        verify(mPreference).setSummary(R.string.keyboard_disconnected);
    }

    @Test
    @Config(shadows = ShadowInputDevice.class)
    public void updateState_hasKeyboard_setSummaryToKeyboardName() {
        final InputDevice device = mock(InputDevice.class);
        when(device.isVirtual()).thenReturn(false);
        when(device.isFullKeyboard()).thenReturn(true);
        when(device.getName()).thenReturn("test_keyboard");
        ShadowInputDevice.sDeviceIds = new int[]{0};
        ShadowInputDevice.addDevice(0, device);

        mController.updateState(mPreference);

        verify(mPreference).setSummary(device.getName());
    }
}
