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
package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.bluetooth.Utils;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.search.SearchIndexableRaw;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

/** Tests for {@link SavedHearingDevicePreferenceController} */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothUtils.class})
public class SavedHearingDevicePreferenceControllerTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final String PREFERENCE_KEY = "preference_key";
    private static final String DEVICE_NAME = "device";

    private Context mContext;
    private SavedHearingDevicePreferenceController mSavedHearingDevicePreferenceController;
    @Mock
    private SavedHearingDeviceUpdater mSavedHearingDeviceUpdater;
    @Mock
    private CachedBluetoothDeviceManager mCachedDeviceManager;
    @Mock
    private LocalBluetoothManager mLocalBluetoothManager;
    @Mock
    private CachedBluetoothDevice mCachedDevice;
    @Mock
    private BluetoothDevice mDevice;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = ApplicationProvider.getApplicationContext();
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBluetoothManager;
        mLocalBluetoothManager = Utils.getLocalBtManager(mContext);
        when(mLocalBluetoothManager.getCachedDeviceManager()).thenReturn(mCachedDeviceManager);
        when(mCachedDevice.getDevice()).thenReturn(mDevice);
        when(mCachedDevice.getName()).thenReturn(DEVICE_NAME);
        when(mCachedDeviceManager.getCachedDevicesCopy()).thenReturn(
                ImmutableList.of(mCachedDevice));

        mSavedHearingDevicePreferenceController =
                new SavedHearingDevicePreferenceController(mContext, PREFERENCE_KEY);
        mSavedHearingDevicePreferenceController.init(mSavedHearingDeviceUpdater);
    }

    @Test
    @EnableFlags(Flags.FLAG_FIX_A11Y_SETTINGS_SEARCH)
    public void updateDynamicRawDataToIndex_isNotHearingAidDevice_deviceIsNotSearchable() {
        when(mDevice.getBondState()).thenReturn(BluetoothDevice.BOND_BONDED);
        when(mDevice.isConnected()).thenReturn(false);
        when(mCachedDevice.isHearingAidDevice()).thenReturn(false);
        List<SearchIndexableRaw> searchData = new ArrayList<>();

        mSavedHearingDevicePreferenceController.updateDynamicRawDataToIndex(searchData);

        assertThat(searchData).isEmpty();
    }

    @Test
    @EnableFlags(Flags.FLAG_FIX_A11Y_SETTINGS_SEARCH)
    public void updateDynamicRawDataToIndex_isHearingAidDevice_deviceIsSearchable() {
        when(mDevice.getBondState()).thenReturn(BluetoothDevice.BOND_BONDED);
        when(mDevice.isConnected()).thenReturn(false);
        when(mCachedDevice.isHearingAidDevice()).thenReturn(true);
        List<SearchIndexableRaw> searchData = new ArrayList<>();

        mSavedHearingDevicePreferenceController.updateDynamicRawDataToIndex(searchData);

        assertThat(searchData).isNotEmpty();
        assertThat(searchData.get(0).key).contains(DEVICE_NAME);
    }
}
