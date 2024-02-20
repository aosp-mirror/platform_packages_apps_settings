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

package com.android.settings.bluetooth;

import static com.android.settings.bluetooth.BluetoothAutoOnPreferenceController.DISABLED;
import static com.android.settings.bluetooth.BluetoothAutoOnPreferenceController.ENABLED;
import static com.android.settings.bluetooth.BluetoothAutoOnPreferenceController.PREF_KEY;
import static com.android.settings.bluetooth.BluetoothAutoOnPreferenceController.SETTING_NAME;
import static com.android.settings.bluetooth.BluetoothAutoOnPreferenceController.UNSET;
import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;
import static com.android.settingslib.flags.Flags.FLAG_BLUETOOTH_QS_TILE_DIALOG_AUTO_ON_TOGGLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;

import android.content.ContentResolver;
import android.content.Context;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class BluetoothAutoOnPreferenceControllerTest {
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    private Context mContext;
    private ContentResolver mContentResolver;
    private BluetoothAutoOnPreferenceController mController;

    @Before
    public void setUp() {
        mSetFlagsRule.enableFlags(FLAG_BLUETOOTH_QS_TILE_DIALOG_AUTO_ON_TOGGLE);
        mContext = spy(ApplicationProvider.getApplicationContext());
        mContentResolver = mContext.getContentResolver();
        mController = new BluetoothAutoOnPreferenceController(mContext, PREF_KEY);
    }

    @Test
    public void getAvailability_valueUnset_returnUnsupported() {
        Settings.Secure.putInt(mContentResolver, SETTING_NAME, UNSET);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailability_valueSet_returnAvailable() {
        Settings.Secure.putInt(mContentResolver, SETTING_NAME, DISABLED);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void isChecked_valueEnabled_returnTrue() {
        Settings.Secure.putInt(mContentResolver, SETTING_NAME, ENABLED);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
        assertThat(mController.isChecked()).isEqualTo(true);
    }

    @Test
    public void setChecked_returnTrue() {
        Settings.Secure.putInt(mContentResolver, SETTING_NAME, DISABLED);

        mController.setChecked(true);
        assertThat(mController.isChecked()).isEqualTo(true);
    }

    @Test
    public void setChecked_returnFalse() {
        Settings.Secure.putInt(mContentResolver, SETTING_NAME, ENABLED);

        mController.setChecked(false);
        assertThat(mController.isChecked()).isEqualTo(false);
    }
}
