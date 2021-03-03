/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.applications.specialaccess.notificationaccess;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.companion.ICompanionDeviceManager;
import android.content.ComponentName;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class HeaderPreferenceControllerTest {

    private Context mContext;
    private HeaderPreferenceController mController;
    @Mock
    LocalBluetoothManager mBm;
    @Mock
    ICompanionDeviceManager mCdm;
    @Mock
    CachedBluetoothDeviceManager mCbm;
    ComponentName mCn = new ComponentName("a", "b");

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();

        when(mBm.getCachedDeviceManager()).thenReturn(mCbm);

        mController = new HeaderPreferenceController(mContext, "key");
        mController.setCn(mCn);
        mController.setUserId(0);
        mController.setBluetoothManager(mBm);
        mController.setCdm(mCdm);
    }

    @Test
    public void getDeviceList_noAssociations() throws Exception {
        when(mCdm.getAssociations(mCn.getPackageName(), 0)).thenReturn(null);

        Collection<CachedBluetoothDevice> cachedDevices = new ArrayList<>();
        CachedBluetoothDevice cbd1 = mock(CachedBluetoothDevice.class);
        when(cbd1.getAddress()).thenReturn("00:00:00:00:00:10");
        when(cbd1.getName()).thenReturn("Device 1");
        cachedDevices.add(cbd1);
        when(mCbm.getCachedDevicesCopy()).thenReturn(cachedDevices);

        BluetoothAdapter.getDefaultAdapter().enable();

        assertThat(mController.getDeviceList().toString()).isEmpty();
    }

    @Test
    public void getDeviceList_associationsButNoDevice() throws Exception {
        List<String> macs = ImmutableList.of("00:00:00:00:00:10", "00:00:00:00:00:20");
        when(mCdm.getAssociations(mCn.getPackageName(), 0)).thenReturn(macs);

        when(mCbm.getCachedDevicesCopy()).thenReturn(new ArrayList<>());

        assertThat(mController.getDeviceList().toString()).isEmpty();
    }

    @Test
    public void getDeviceList_singleDevice() throws Exception {
        List<String> macs = ImmutableList.of("00:00:00:00:00:10", "00:00:00:00:00:20");
        when(mCdm.getAssociations(mCn.getPackageName(), 0)).thenReturn(macs);

        Collection<CachedBluetoothDevice> cachedDevices = new ArrayList<>();
        CachedBluetoothDevice cbd1 = mock(CachedBluetoothDevice.class);
        when(cbd1.getAddress()).thenReturn(macs.get(0));
        when(cbd1.getName()).thenReturn("Device 1");
        cachedDevices.add(cbd1);
        when(mCbm.getCachedDevicesCopy()).thenReturn(cachedDevices);

        assertThat(mController.getDeviceList().toString()).isEqualTo("Device 1");
    }

    @Test
    public void getDeviceList_multipleDevices() throws Exception {
        List<String> macs = ImmutableList.of("00:00:00:00:00:10", "00:00:00:00:00:20");
        when(mCdm.getAssociations(mCn.getPackageName(), 0)).thenReturn(macs);

        Collection<CachedBluetoothDevice> cachedDevices = new ArrayList<>();
        CachedBluetoothDevice cbd1 = mock(CachedBluetoothDevice.class);
        when(cbd1.getAddress()).thenReturn(macs.get(0));
        when(cbd1.getName()).thenReturn("Device 1");
        cachedDevices.add(cbd1);

        CachedBluetoothDevice cbd2 = mock(CachedBluetoothDevice.class);
        when(cbd2.getAddress()).thenReturn(macs.get(1));
        when(cbd2.getName()).thenReturn("Device 2");
        cachedDevices.add(cbd2);
        when(mCbm.getCachedDevicesCopy()).thenReturn(cachedDevices);

        assertThat(mController.getDeviceList().toString()).isEqualTo("Device 1, Device 2");
    }
}
