/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.bluetooth.BluetoothProgressCategory;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

/** Tests for {@link HearingDevicePairingDetail}. */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothAdapter.class})
public class HearingDevicePairingDetailTest {

    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Mock
    private CachedBluetoothDevice mCachedBluetoothDevice;
    private BluetoothProgressCategory mProgressCategory;
    private TestHearingDevicePairingDetail mFragment;

    @Before
    public void setUp() {
        final BluetoothAdapter bluetoothAdapter = spy(BluetoothAdapter.getDefaultAdapter());
        final ShadowBluetoothAdapter shadowBluetoothAdapter = Shadow.extract(
                BluetoothAdapter.getDefaultAdapter());
        shadowBluetoothAdapter.setEnabled(true);

        mProgressCategory = spy(new BluetoothProgressCategory(mContext));
        mFragment = spy(new TestHearingDevicePairingDetail());
        when(mFragment.getContext()).thenReturn(mContext);
        when(mFragment.findPreference(
                HearingDevicePairingDetail.KEY_AVAILABLE_HEARING_DEVICES)).thenReturn(
                mProgressCategory);
        mFragment.setBluetoothAdapter(bluetoothAdapter);

    }

    @Test
    public void getDeviceListKey_expectedKey() {
        assertThat(mFragment.getDeviceListKey()).isEqualTo(
                HearingDevicePairingDetail.KEY_AVAILABLE_HEARING_DEVICES);
    }

    @Test
    public void onDeviceBondStateChanged_bondNone_setProgressFalse() {
        mFragment.initPreferencesFromPreferenceScreen();

        mFragment.onDeviceBondStateChanged(mCachedBluetoothDevice, BluetoothDevice.BOND_NONE);

        verify(mProgressCategory).setProgress(true);
    }

    @Test
    public void onDeviceBondStateChanged_bonding_setProgressTrue() {
        mFragment.initPreferencesFromPreferenceScreen();

        mFragment.onDeviceBondStateChanged(mCachedBluetoothDevice, BluetoothDevice.BOND_BONDING);

        verify(mProgressCategory).setProgress(false);
    }

    private static class TestHearingDevicePairingDetail extends HearingDevicePairingDetail {
        TestHearingDevicePairingDetail() {
            super();
        }

        public void setBluetoothAdapter(BluetoothAdapter bluetoothAdapter) {
            this.mBluetoothAdapter = bluetoothAdapter;
        }

        public void enableScanning() {
            super.enableScanning();
        }
    }
}
