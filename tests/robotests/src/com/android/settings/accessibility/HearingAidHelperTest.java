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

import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.bluetooth.Utils;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.HapClientProfile;
import com.android.settingslib.bluetooth.HearingAidProfile;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;

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

import java.util.ArrayList;
import java.util.Collections;

/** Tests for {@link HearingAidHelper}. */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothAdapter.class, ShadowBluetoothUtils.class})
public class HearingAidHelperTest {
    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private static final String TEST_DEVICE_ADDRESS = "00:A1:A1:A1:A1:A1";

    @Mock
    private LocalBluetoothManager mLocalBluetoothManager;
    @Mock
    private LocalBluetoothProfileManager mLocalBluetoothProfileManager;
    @Mock
    private CachedBluetoothDeviceManager mCachedDeviceManager;
    @Mock
    private CachedBluetoothDevice mCachedBluetoothDevice;
    @Mock
    private HearingAidProfile mHearingAidProfile;
    @Mock
    private HapClientProfile mHapClientProfile;
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;
    private HearingAidHelper mHelper;

    @Before
    public void setUp() {
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBluetoothManager;
        mLocalBluetoothManager = Utils.getLocalBtManager(mContext);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mShadowBluetoothAdapter = Shadow.extract(mBluetoothAdapter);
        mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(TEST_DEVICE_ADDRESS);
        when(mCachedBluetoothDevice.getAddress()).thenReturn(TEST_DEVICE_ADDRESS);
        when(mLocalBluetoothManager.getCachedDeviceManager()).thenReturn(mCachedDeviceManager);
        when(mCachedDeviceManager.findDevice(mBluetoothDevice)).thenReturn(mCachedBluetoothDevice);
        when(mLocalBluetoothManager.getProfileManager()).thenReturn(mLocalBluetoothProfileManager);
        when(mLocalBluetoothProfileManager.getHearingAidProfile()).thenReturn(mHearingAidProfile);
        when(mLocalBluetoothProfileManager.getHapClientProfile()).thenReturn(mHapClientProfile);

        mHelper = new HearingAidHelper(mContext);
    }

    @Test
    public void isHearingAidSupported_ashaSupported_returnTrue() {
        mShadowBluetoothAdapter.clearSupportedProfiles();
        mShadowBluetoothAdapter.addSupportedProfiles(BluetoothProfile.HEARING_AID);

        assertThat(mHelper.isHearingAidSupported()).isTrue();
    }

    @Test
    public void isHearingAidSupported_hapSupported_returnTrue() {
        mShadowBluetoothAdapter.clearSupportedProfiles();
        mShadowBluetoothAdapter.addSupportedProfiles(BluetoothProfile.HAP_CLIENT);

        assertThat(mHelper.isHearingAidSupported()).isTrue();
    }

    @Test
    public void isHearingAidSupported_unsupported_returnFalse() {
        mShadowBluetoothAdapter.clearSupportedProfiles();

        assertThat(mHelper.isHearingAidSupported()).isFalse();
    }

    @Test
    public void isAllHearingAidRelatedProfilesReady_allReady_returnTrue() {
        when(mHearingAidProfile.isProfileReady()).thenReturn(true);
        when(mHapClientProfile.isProfileReady()).thenReturn(true);

        assertThat(mHelper.isAllHearingAidRelatedProfilesReady()).isTrue();
    }

    @Test
    public void isAllHearingAidRelatedProfilesReady_notFullReady_returnFalse() {
        when(mHearingAidProfile.isProfileReady()).thenReturn(false);
        when(mHapClientProfile.isProfileReady()).thenReturn(true);

        assertThat(mHelper.isAllHearingAidRelatedProfilesReady()).isFalse();
    }

    @Test
    public void getConnectedHearingAidDeviceList_oneDeviceAdded_getOneDevice() {
        mBluetoothAdapter.enable();
        mShadowBluetoothAdapter.clearSupportedProfiles();
        mShadowBluetoothAdapter.addSupportedProfiles(BluetoothProfile.HEARING_AID);
        when(mHearingAidProfile.getConnectedDevices()).thenReturn(new ArrayList<>(
                Collections.singletonList(mBluetoothDevice)));

        assertThat(mHelper.getConnectedHearingAidDeviceList().size()).isEqualTo(1);
    }

    @Test
    public void getConnectedHearingAidDeviceList_oneSubDeviceAdded_getZeroDevice() {
        mBluetoothAdapter.enable();
        mShadowBluetoothAdapter.clearSupportedProfiles();
        mShadowBluetoothAdapter.addSupportedProfiles(BluetoothProfile.HEARING_AID);
        when(mHearingAidProfile.getConnectedDevices()).thenReturn(new ArrayList<>(
                Collections.singletonList(mBluetoothDevice)));
        when(mLocalBluetoothManager.getCachedDeviceManager().isSubDevice(
                mBluetoothDevice)).thenReturn(true);

        assertThat(mHelper.getConnectedHearingAidDeviceList().size()).isEqualTo(0);
    }

    @Test
    public void getConnectedHearingAidDevice_getExpectedCachedBluetoothDevice() {
        mBluetoothAdapter.enable();
        mShadowBluetoothAdapter.clearSupportedProfiles();
        mShadowBluetoothAdapter.addSupportedProfiles(BluetoothProfile.HEARING_AID);
        when(mHearingAidProfile.getConnectedDevices()).thenReturn(new ArrayList<>(
                Collections.singletonList(mBluetoothDevice)));

        assertThat(mHelper.getConnectedHearingAidDevice()).isEqualTo(mCachedBluetoothDevice);
        assertThat(mCachedBluetoothDevice.getAddress()).isEqualTo(mBluetoothDevice.getAddress());
    }
}
