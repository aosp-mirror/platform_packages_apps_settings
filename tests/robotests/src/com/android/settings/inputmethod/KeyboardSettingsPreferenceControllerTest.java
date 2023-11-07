/*
 * Copyright (C) 2022 The Android Open Source Project
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.hardware.input.InputDeviceIdentifier;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.inputmethod.PhysicalKeyboardFragment.HardKeyboardDeviceInfo;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

/** Tests for {@link KeyboardSettingsPreferenceController} */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowInputManager.class,
})
public class KeyboardSettingsPreferenceControllerTest {

    private static final int VENDOR_ID = 123;
    private static final int PRODUCT_ID = 456;

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    private static final String PREFERENCE_KEY = "keyboard_settings";

    @Mock
    private CachedBluetoothDevice mCachedBluetoothDevice;
    @Mock
    private InputDeviceIdentifier mInputDeviceIdentifier;

    private Context mContext;
    private KeyboardSettingsPreferenceController mController;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        doNothing().when(mContext).startActivity(any());
        mController = spy(new KeyboardSettingsPreferenceController(mContext, PREFERENCE_KEY));
        mController.init(mCachedBluetoothDevice);
    }

    @Test
    public void handlePreferenceTreeClick_expected() {
        Preference mKeyboardPreference = new Preference(mContext);
        mKeyboardPreference.setKey(PREFERENCE_KEY);
        final ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        String address = "BT_ADDRESS";
        HardKeyboardDeviceInfo deviceInfo =
                new HardKeyboardDeviceInfo(
                        "TEST_DEVICE",
                        mInputDeviceIdentifier,
                        "TEST_DEVICE_LABEL",
                        address,
                        VENDOR_ID,
                        PRODUCT_ID);
        List<HardKeyboardDeviceInfo> keyboards = new ArrayList<>();
        keyboards.add(deviceInfo);
        when(mController.getHardKeyboardList()).thenReturn(keyboards);
        when(mCachedBluetoothDevice.getAddress()).thenReturn(address);

        mController.handlePreferenceTreeClick(mKeyboardPreference);

        verify(mContext).startActivity(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(Settings.ACTION_HARD_KEYBOARD_SETTINGS);
    }

    @Test
    public void handlePreferenceTreeClick_notExpected() {
        Preference mOtherPreference = new Preference(mContext);
        mOtherPreference.setKey("not_keyboard_settings");

        assertThat(mController.handlePreferenceTreeClick(mOtherPreference)).isFalse();
    }
}
