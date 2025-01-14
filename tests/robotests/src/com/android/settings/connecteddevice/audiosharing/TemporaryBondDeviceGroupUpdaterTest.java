/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.settings.connecteddevice.audiosharing;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothStatusCodes;
import android.content.Context;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.bluetooth.Utils;
import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.flags.Flags;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

import java.util.ArrayList;
import java.util.Collection;

/** Tests for {@link TemporaryBondDeviceGroupUpdater}. */
@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
                ShadowBluetoothAdapter.class,
                ShadowBluetoothUtils.class
        })
public class TemporaryBondDeviceGroupUpdaterTest {
    private static final String TAG = "TemporaryBondDeviceGroupUpdater";
    private static final String PREF_KEY_PREFIX = "temp_bond_bt_";
    private static final String TEMP_BOND_METADATA =
            "<TEMP_BOND_TYPE>le_audio_sharing</TEMP_BOND_TYPE>";
    private static final int METADATA_FAST_PAIR_CUSTOMIZED_FIELDS = 25;

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Mock
    private DevicePreferenceCallback mDevicePreferenceCallback;
    @Mock
    private CachedBluetoothDevice mCachedBluetoothDevice;
    @Mock
    private BluetoothDevice mBluetoothDevice;
    @Mock
    private LocalBluetoothManager mLocalBtManager;
    @Mock
    private CachedBluetoothDeviceManager mCachedDeviceManager;

    private TemporaryBondDeviceGroupUpdater mDeviceUpdater;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        ShadowBluetoothAdapter shadowBluetoothAdapter = Shadow.extract(
                BluetoothAdapter.getDefaultAdapter());
        shadowBluetoothAdapter.setEnabled(true);
        shadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        shadowBluetoothAdapter.setIsLeAudioBroadcastAssistantSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        Context context = ApplicationProvider.getApplicationContext();
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBtManager;
        mLocalBtManager = Utils.getLocalBtManager(context);
        when(mLocalBtManager.getCachedDeviceManager()).thenReturn(mCachedDeviceManager);
        when(mCachedBluetoothDevice.getDevice()).thenReturn(mBluetoothDevice);
        Collection<CachedBluetoothDevice> cachedDevices = new ArrayList<>();
        cachedDevices.add(mCachedBluetoothDevice);
        when(mCachedDeviceManager.getCachedDevicesCopy()).thenReturn(cachedDevices);
        mDeviceUpdater =
                spy(
                        new TemporaryBondDeviceGroupUpdater(
                                context, mDevicePreferenceCallback, /* metricsCategory= */ 0));
        mDeviceUpdater.setPrefContext(context);
    }

    @After
    public void tearDown() {
        ShadowBluetoothUtils.reset();
    }

    @Test
    @RequiresFlagsEnabled({Flags.FLAG_ENABLE_TEMPORARY_BOND_DEVICES_UI,
            Flags.FLAG_ENABLE_LE_AUDIO_SHARING})
    public void isFilterMatched_isTemporaryBondDevice_returnsTrue() {
        when(mBluetoothDevice.isConnected()).thenReturn(true);
        when(mBluetoothDevice.getBondState()).thenReturn(BluetoothDevice.BOND_BONDED);
        when(mBluetoothDevice.getMetadata(METADATA_FAST_PAIR_CUSTOMIZED_FIELDS))
                .thenReturn(TEMP_BOND_METADATA.getBytes());

        assertThat(mDeviceUpdater.isFilterMatched(mCachedBluetoothDevice)).isTrue();
    }

    @Test
    public void getLogTag_returnsCorrectTag() {
        assertThat(mDeviceUpdater.getLogTag()).isEqualTo(TAG);
    }

    @Test
    public void getPreferenceKey_returnsCorrectKey() {
        assertThat(mDeviceUpdater.getPreferenceKeyPrefix()).isEqualTo(PREF_KEY_PREFIX);
    }
}
