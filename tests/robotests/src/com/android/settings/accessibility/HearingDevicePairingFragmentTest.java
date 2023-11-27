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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothUuid;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Pair;

import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.bluetooth.BluetoothDevicePreference;
import com.android.settings.bluetooth.BluetoothProgressCategory;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.HearingAidInfo;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

/** Tests for {@link HearingDevicePairingFragment}. */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothAdapter.class})
public class HearingDevicePairingFragmentTest {

    private static final String TEST_DEVICE_ADDRESS = "00:A1:A1:A1:A1:A1";

    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    @Spy
    private final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    @Spy
    private final HearingDevicePairingFragment mFragment = new TestHearingDevicePairingFragment();

    @Mock
    private LocalBluetoothManager mLocalManager;
    @Mock
    private CachedBluetoothDeviceManager mCachedDeviceManager;
    @Mock
    private CachedBluetoothDevice mCachedDevice;
    @Mock
    private BluetoothProgressCategory mAvailableHearingDeviceGroup;

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private BluetoothDevice mDevice;
    private BluetoothDevicePreference mDevicePreference;


    @Before
    public void setUp() {
        mFragment.mLocalManager = mLocalManager;
        mFragment.mCachedDeviceManager = mCachedDeviceManager;
        mFragment.mBluetoothAdapter = mBluetoothAdapter;
        doReturn(mContext).when(mFragment).getContext();
        doReturn(mAvailableHearingDeviceGroup).when(mFragment).findPreference(
                "available_hearing_devices");
        mFragment.initPreferencesFromPreferenceScreen();


        mDevice = mBluetoothAdapter.getRemoteDevice(TEST_DEVICE_ADDRESS);
        doReturn(mDevice).when(mCachedDevice).getDevice();
        final Pair<Drawable, String> pair = new Pair<>(mock(Drawable.class), "test_device");
        doReturn(pair).when(mCachedDevice).getDrawableWithDescription();

        mDevicePreference = new BluetoothDevicePreference(mContext, mCachedDevice, true,
                BluetoothDevicePreference.SortType.TYPE_DEFAULT);
    }

    @Test
    public void startAndStopScanning_stateIsCorrect() {
        mFragment.startScanning();

        verify(mFragment).startLeScanning();

        mFragment.stopScanning();

        verify(mFragment).stopLeScanning();
    }

    @Test
    public void onDeviceDeleted_stateIsCorrect() {
        mFragment.mDevicePreferenceMap.put(mCachedDevice, mDevicePreference);

        assertThat(mFragment.mDevicePreferenceMap).isNotEmpty();

        mFragment.onDeviceDeleted(mCachedDevice);

        assertThat(mFragment.mDevicePreferenceMap).isEmpty();
        verify(mAvailableHearingDeviceGroup).removePreference(mDevicePreference);
    }

    @Test
    public void addDevice_bluetoothOff_doNothing() {
        doReturn(BluetoothAdapter.STATE_OFF).when(mBluetoothAdapter).getState();

        assertThat(mFragment.mDevicePreferenceMap.size()).isEqualTo(0);

        mFragment.addDevice(mCachedDevice);

        verify(mAvailableHearingDeviceGroup, never()).addPreference(mDevicePreference);
        assertThat(mFragment.mDevicePreferenceMap.size()).isEqualTo(0);
    }

    @Test
    public void addDevice_addToAvailableHearingDeviceGroup() {
        doReturn(BluetoothAdapter.STATE_ON).when(mBluetoothAdapter).getState();

        assertThat(mFragment.mDevicePreferenceMap.size()).isEqualTo(0);

        mFragment.addDevice(mCachedDevice);

        verify(mAvailableHearingDeviceGroup).addPreference(mDevicePreference);
        assertThat(mFragment.mDevicePreferenceMap.size()).isEqualTo(1);
    }

    @Test
    public void handleLeScanResult_markDeviceAsHearingAid() {
        ScanResult scanResult = mock(ScanResult.class);
        doReturn(mDevice).when(scanResult).getDevice();
        doReturn(mCachedDevice).when(mCachedDeviceManager).findDevice(mDevice);

        mFragment.handleLeScanResult(scanResult);

        verify(mCachedDevice).setHearingAidInfo(new HearingAidInfo.Builder().build());
    }

    @Test
    public void handleLeScanResult_isAndroidCompatible_addDevice() {
        ScanResult scanResult = mock(ScanResult.class);
        doReturn(mDevice).when(scanResult).getDevice();
        doReturn(mCachedDevice).when(mCachedDeviceManager).findDevice(mDevice);
        doReturn(true).when(mFragment).isAndroidCompatibleHearingAid(scanResult);

        mFragment.handleLeScanResult(scanResult);

        verify(mFragment).addDevice(mCachedDevice);
    }

    @Test
    public void handleLeScanResult_isNotAndroidCompatible_() {
        ScanResult scanResult = mock(ScanResult.class);
        doReturn(mDevice).when(scanResult).getDevice();
        doReturn(mCachedDevice).when(mCachedDeviceManager).findDevice(mDevice);
        doReturn(false).when(mFragment).isAndroidCompatibleHearingAid(scanResult);

        mFragment.handleLeScanResult(scanResult);

        verify(mFragment).discoverServices(mCachedDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_deviceConnected_inSelectedList_finish() {
        doReturn(true).when(mCachedDevice).isConnected();
        mFragment.mSelectedDeviceList.add(mDevice);

        mFragment.onProfileConnectionStateChanged(mCachedDevice, BluetoothAdapter.STATE_CONNECTED,
                BluetoothProfile.A2DP);

        verify(mFragment).finish();
    }

    @Test
    public void onProfileConnectionStateChanged_deviceConnected_notInSelectedList_deleteDevice() {
        doReturn(true).when(mCachedDevice).isConnected();

        mFragment.onProfileConnectionStateChanged(mCachedDevice, BluetoothAdapter.STATE_CONNECTED,
                BluetoothProfile.A2DP);

        verify(mFragment).removeDevice(mCachedDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_deviceNotConnected_doNothing() {
        doReturn(false).when(mCachedDevice).isConnected();

        mFragment.onProfileConnectionStateChanged(mCachedDevice, BluetoothAdapter.STATE_CONNECTED,
                BluetoothProfile.A2DP);

        verify(mFragment, never()).finish();
        verify(mFragment, never()).removeDevice(mCachedDevice);
    }

    @Test
    public void onBluetoothStateChanged_stateOn_startScanningAndShowToast() {
        mFragment.onBluetoothStateChanged(BluetoothAdapter.STATE_ON);

        verify(mFragment).startScanning();
        verify(mFragment).showBluetoothTurnedOnToast();
    }

    @Test
    public void onBluetoothStateChanged_stateOff_finish() {
        mFragment.onBluetoothStateChanged(BluetoothAdapter.STATE_OFF);

        verify(mFragment).finish();
    }

    @Test
    public void onDeviceBondStateChanged_bonded_finish() {
        mFragment.onDeviceBondStateChanged(mCachedDevice, BluetoothDevice.BOND_BONDED);

        verify(mFragment).finish();
    }

    @Test
    public void onDeviceBondStateChanged_selectedDeviceNotBonded_startScanning() {
        mFragment.mSelectedDevice = mDevice;

        mFragment.onDeviceBondStateChanged(mCachedDevice, BluetoothDevice.BOND_NONE);

        verify(mFragment).startScanning();
    }

    @Test
    public void isAndroidCompatibleHearingAid_asha_returnTrue() {
        ScanResult scanResult = createAshaScanResult();

        boolean isCompatible = mFragment.isAndroidCompatibleHearingAid(scanResult);

        assertThat(isCompatible).isTrue();
    }

    @Test
    public void isAndroidCompatibleHearingAid_has_returnTrue() {
        ScanResult scanResult = createHasScanResult();

        boolean isCompatible = mFragment.isAndroidCompatibleHearingAid(scanResult);

        assertThat(isCompatible).isTrue();
    }

    @Test
    public void isAndroidCompatibleHearingAid_mfiHas_returnFalse() {
        ScanResult scanResult = createMfiHasScanResult();

        boolean isCompatible = mFragment.isAndroidCompatibleHearingAid(scanResult);

        assertThat(isCompatible).isFalse();
    }

    private ScanResult createAshaScanResult() {
        ScanResult scanResult = mock(ScanResult.class);
        ScanRecord scanRecord = mock(ScanRecord.class);
        byte[] fakeAshaServiceData = new byte[] {
                0x09, 0x16, (byte) 0xf0, (byte) 0xfd, 0x01, 0x00, 0x01, 0x02, 0x03, 0x04};
        doReturn(scanRecord).when(scanResult).getScanRecord();
        doReturn(fakeAshaServiceData).when(scanRecord).getServiceData(BluetoothUuid.HEARING_AID);
        return scanResult;
    }

    private ScanResult createHasScanResult() {
        ScanResult scanResult = mock(ScanResult.class);
        ScanRecord scanRecord = mock(ScanRecord.class);
        doReturn(scanRecord).when(scanResult).getScanRecord();
        doReturn(List.of(BluetoothUuid.HAS)).when(scanRecord).getServiceUuids();
        return scanResult;
    }

    private ScanResult createMfiHasScanResult() {
        ScanResult scanResult = mock(ScanResult.class);
        ScanRecord scanRecord = mock(ScanRecord.class);
        byte[] fakeMfiServiceData = new byte[] {0x00, 0x00, 0x00, 0x00};
        doReturn(scanRecord).when(scanResult).getScanRecord();
        doReturn(fakeMfiServiceData).when(scanRecord).getServiceData(BluetoothUuid.MFI_HAS);
        return scanResult;
    }

    private class TestHearingDevicePairingFragment extends HearingDevicePairingFragment {
        @Override
        protected Preference getCachedPreference(String key) {
            if (key.equals(TEST_DEVICE_ADDRESS)) {
                return mDevicePreference;
            }
            return super.getCachedPreference(key);
        }
    }
}
