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

package com.android.settings.homepage.contextualcards.slices;

import static android.app.slice.Slice.HINT_LIST_ITEM;
import static android.app.slice.SliceItem.FORMAT_SLICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceProvider;
import androidx.slice.core.SliceQuery;
import androidx.slice.widget.SliceLiveData;

import com.android.settings.R;
import com.android.settings.testutils.SliceTester;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class BluetoothDevicesSliceTest {

    private static final String BLUETOOTH_MOCK_ADDRESS = "00:11:00:11:00:11";
    private static final String BLUETOOTH_MOCK_SUMMARY = "BluetoothSummary";
    private static final String BLUETOOTH_MOCK_TITLE = "BluetoothTitle";

    @Mock
    private CachedBluetoothDevice mCachedBluetoothDevice;

    private List<CachedBluetoothDevice> mBluetoothDeviceList;
    private BluetoothDevicesSlice mBluetoothDevicesSlice;
    private Context mContext;
    private IconCompat mIcon;
    private PendingIntent mDetailIntent;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;

        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);

        mBluetoothDevicesSlice = spy(new BluetoothDevicesSlice(mContext));

        // Mock the icon and detail intent of Bluetooth.
        mIcon = IconCompat.createWithResource(mContext,
                com.android.internal.R.drawable.ic_settings_bluetooth);
        mDetailIntent = PendingIntent.getActivity(mContext, 0, new Intent("test action"), 0);
        doReturn(mIcon).when(mBluetoothDevicesSlice).getBluetoothDeviceIcon(any());
        doReturn(mDetailIntent).when(mBluetoothDevicesSlice).getBluetoothDetailIntent(any());

        // Initial Bluetooth device list.
        mBluetoothDeviceList = new ArrayList<>();
    }

    @After
    public void tearDown() {
        if (!mBluetoothDeviceList.isEmpty()) {
            mBluetoothDeviceList.clear();
        }
    }

    @Test
    public void getSlice_hasBluetoothDevices_shouldHaveBluetoothDevicesTitle() {
        mockBluetoothDeviceList(1);
        doReturn(mBluetoothDeviceList).when(mBluetoothDevicesSlice).getConnectedBluetoothDevices();

        final Slice slice = mBluetoothDevicesSlice.getSlice();

        final SliceMetadata metadata = SliceMetadata.from(mContext, slice);
        assertThat(metadata.getTitle()).isEqualTo(mContext.getString(R.string.bluetooth_devices));
    }

    @Test
    public void getSlice_hasBluetoothDevices_shouldMatchBluetoothMockTitle() {
        mockBluetoothDeviceList(1);
        doReturn(mBluetoothDeviceList).when(mBluetoothDevicesSlice).getConnectedBluetoothDevices();

        final Slice slice = mBluetoothDevicesSlice.getSlice();

        final List<SliceItem> sliceItems = slice.getItems();
        SliceTester.assertAnySliceItemContainsTitle(sliceItems, BLUETOOTH_MOCK_TITLE);
    }

    @Test
    public void getSlice_hasMediaBluetoothDevice_shouldBuildMediaBluetoothAction() {
        mockBluetoothDeviceList(1 /* deviceCount */);
        doReturn(true).when(mBluetoothDeviceList.get(0)).isConnectedA2dpDevice();
        doReturn(mBluetoothDeviceList).when(mBluetoothDevicesSlice).getConnectedBluetoothDevices();

        mBluetoothDevicesSlice.getSlice();

        verify(mBluetoothDevicesSlice).buildMediaBluetoothAction(any());
    }

    @Test
    public void getSlice_noMediaBluetoothDevice_shouldNotBuildMediaBluetoothAction() {
        mockBluetoothDeviceList(1 /* deviceCount */);
        doReturn(mBluetoothDeviceList).when(mBluetoothDevicesSlice).getConnectedBluetoothDevices();

        mBluetoothDevicesSlice.getSlice();

        verify(mBluetoothDevicesSlice, never()).buildMediaBluetoothAction(any());
    }

    @Test
    public void getSlice_noBluetoothDevices_shouldHaveNoBluetoothDevicesTitle() {
        doReturn(mBluetoothDeviceList).when(mBluetoothDevicesSlice).getConnectedBluetoothDevices();

        final Slice slice = mBluetoothDevicesSlice.getSlice();

        final SliceMetadata metadata = SliceMetadata.from(mContext, slice);
        assertThat(metadata.getTitle()).isEqualTo(
                mContext.getString(R.string.no_bluetooth_devices));
    }

    @Test
    public void getSlice_exceedDefaultRowCount_shouldOnlyShowDefaultRows() {
        mockBluetoothDeviceList(BluetoothDevicesSlice.DEFAULT_EXPANDED_ROW_COUNT + 1);
        doReturn(mBluetoothDeviceList).when(mBluetoothDevicesSlice).getConnectedBluetoothDevices();

        final Slice slice = mBluetoothDevicesSlice.getSlice();

        // Get the number of RowBuilders from Slice.
        final int rows = SliceQuery.findAll(slice, FORMAT_SLICE, HINT_LIST_ITEM, null).size();
        assertThat(rows).isEqualTo(BluetoothDevicesSlice.DEFAULT_EXPANDED_ROW_COUNT);
    }

    @Test
    public void getSlice_exceedDefaultRowCount_shouldContainDefaultCountInSubTitle() {
        mockBluetoothDeviceList(BluetoothDevicesSlice.DEFAULT_EXPANDED_ROW_COUNT + 1);
        doReturn(mBluetoothDeviceList).when(mBluetoothDevicesSlice).getConnectedBluetoothDevices();

        final Slice slice = mBluetoothDevicesSlice.getSlice();

        final SliceMetadata metadata = SliceMetadata.from(mContext, slice);
        assertThat(metadata.getSubtitle()).isEqualTo(
                mContext.getResources().getQuantityString(R.plurals.show_bluetooth_devices,
                        BluetoothDevicesSlice.DEFAULT_EXPANDED_ROW_COUNT,
                        BluetoothDevicesSlice.DEFAULT_EXPANDED_ROW_COUNT));
    }

    @Test
    public void onNotifyChange_mediaDevice_shouldActivateDevice() {
        mockBluetoothDeviceList(1);
        doReturn(mBluetoothDeviceList).when(mBluetoothDevicesSlice).getConnectedBluetoothDevices();
        final Intent intent = new Intent().putExtra(
                BluetoothDevicesSlice.BLUETOOTH_DEVICE_HASH_CODE,
                mCachedBluetoothDevice.hashCode());

        mBluetoothDevicesSlice.onNotifyChange(intent);

        verify(mCachedBluetoothDevice).setActive();
    }

    private void mockBluetoothDeviceList(int deviceCount) {
        doReturn(BLUETOOTH_MOCK_TITLE).when(mCachedBluetoothDevice).getName();
        doReturn(BLUETOOTH_MOCK_SUMMARY).when(mCachedBluetoothDevice).getConnectionSummary();
        doReturn(BLUETOOTH_MOCK_ADDRESS).when(mCachedBluetoothDevice).getAddress();
        for (int i = 0; i < deviceCount; i++) {
            mBluetoothDeviceList.add(mCachedBluetoothDevice);
        }
    }
}
