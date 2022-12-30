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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;

import com.android.settingslib.bluetooth.CachedBluetoothDevice;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link KeyboardSettingsPreferenceController} */
@RunWith(RobolectricTestRunner.class)
public class KeyboardSettingsPreferenceControllerTest {

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    private static final String PREFERENCE_KEY = "keyboard_settings";

    @Mock
    private Activity mActivity;
    @Mock
    private CachedBluetoothDevice mCachedBluetoothDevice;
    @Captor
    private ArgumentCaptor<Intent> mIntentArgumentCaptor;
    private Context mContext;
    private KeyboardSettingsPreferenceController mController;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        mController = new KeyboardSettingsPreferenceController(mContext, PREFERENCE_KEY);
        mController.init(mCachedBluetoothDevice, mActivity);
    }

    @Test
    public void handlePreferenceTreeClick_expected() {
        Preference mKeyboardPreference = new Preference(mContext);
        mKeyboardPreference.setKey(PREFERENCE_KEY);

        mController.handlePreferenceTreeClick(mKeyboardPreference);

        verify(mActivity).startActivityForResult(mIntentArgumentCaptor.capture(), eq(0));
        Intent expectedIntent = mIntentArgumentCaptor.getValue();
        assertThat(expectedIntent.getAction()).isEqualTo(Settings.ACTION_HARD_KEYBOARD_SETTINGS);
    }

    @Test
    public void handlePreferenceTreeClick_notExpected() {
        Preference mOtherPreference = new Preference(mContext);
        mOtherPreference.setKey("not_keyboard_settings");

        assertThat(mController.handlePreferenceTreeClick(mOtherPreference)).isFalse();
    }
}
