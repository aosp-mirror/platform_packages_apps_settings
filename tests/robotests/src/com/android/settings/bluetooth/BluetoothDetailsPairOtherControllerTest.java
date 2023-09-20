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

package com.android.settings.bluetooth;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import com.android.settings.R;
import com.android.settings.applications.SpacePreference;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.HearingAidInfo;
import com.android.settingslib.widget.ButtonPreference;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

import java.util.HashSet;
import java.util.Set;

/** Tests for {@link BluetoothDetailsPairOtherController}. */
@RunWith(RobolectricTestRunner.class)
public class BluetoothDetailsPairOtherControllerTest extends BluetoothDetailsControllerTestBase  {
    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    @Mock
    private CachedBluetoothDevice mSubCachedDevice;
    private BluetoothDetailsPairOtherController mController;
    private ButtonPreference mPreference;
    private SpacePreference mSpacePreference;

    @Override
    public void setUp() {
        super.setUp();

        mController = new BluetoothDetailsPairOtherController(mContext, mFragment, mCachedDevice,
                mLifecycle);
        mPreference = new ButtonPreference(mContext);
        mSpacePreference = new SpacePreference(mContext, null);
        mPreference.setKey(mController.getPreferenceKey());
        mSpacePreference.setKey(BluetoothDetailsPairOtherController.KEY_SPACE);
        mScreen.addPreference(mPreference);
        mScreen.addPreference(mSpacePreference);
    }

    /** Test the pair other side button title during initialization. */
    @Test
    public void init_leftSideDevice_pairRightSideButtonTitle() {
        when(mCachedDevice.getDeviceSide()).thenReturn(HearingAidInfo.DeviceSide.SIDE_LEFT);

        mController.init(mScreen);

        assertThat(mPreference.getTitle().toString()).isEqualTo(
                mContext.getString(R.string.bluetooth_pair_right_ear_button));
    }

    /** Test the pair other side button title during initialization. */
    @Test
    public void init_rightSideDevice_pairLeftSideButtonTitle() {
        when(mCachedDevice.getDeviceSide()).thenReturn(HearingAidInfo.DeviceSide.SIDE_RIGHT);

        mController.init(mScreen);

        assertThat(mPreference.getTitle().toString()).isEqualTo(
                mContext.getString(R.string.bluetooth_pair_left_ear_button));
    }

    /** Test the pair other side button visibility during initialization. */
    @Test
    public void init_isNotConnectedHearingAidDevice_preferenceIsNotVisible() {
        when(mCachedDevice.isConnectedHearingAidDevice()).thenReturn(false);

        mController.init(mScreen);

        assertThat(mPreference.isVisible()).isFalse();
        assertThat(mSpacePreference.isVisible()).isFalse();
    }

    /**
     * Test if the controller is available.
     * Conditions:
     *      1. Hearing aids is not connected
     * Expected result:
     *      The controller is not available. No need to show pair other side hint for
     *      not connected device.
     */
    @Test
    public void isAvailable_isNotConnectedHearingAidDevice_notAvailable() {
        when(mCachedDevice.isConnectedHearingAidDevice()).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    /**
     * Test if the controller is available.
     * Conditions:
     *      1. Monaural hearing aids
     * Expected result:
     *      The controller is not available. No need to show pair other side hint for
     *      monaural device.
     */
    @Test
    public void isAvailable_isConnectedHearingAidDevice_isMonaural_notAvailable() {
        when(mCachedDevice.isConnectedHearingAidDevice()).thenReturn(true);
        when(mCachedDevice.getDeviceMode()).thenReturn(HearingAidInfo.DeviceMode.MODE_MONAURAL);

        assertThat(mController.isAvailable()).isFalse();
    }

    /**
     * Test if the controller is available.
     * Conditions:
     *      1. Binaural ASHA hearing aids
     *      2. Sub device is added
     *      3. Sub device is connected
     * Expected result:
     *      The controller is not available. Both sides are already paired and connected.
     */
    @Test
    public void isAvailable_ashaDevice_otherDeviceIsConnected_notAvailable() {
        when(mCachedDevice.isConnectedHearingAidDevice()).thenReturn(true);
        when(mCachedDevice.isConnectedAshaHearingAidDevice()).thenReturn(true);
        when(mCachedDevice.getDeviceMode()).thenReturn(HearingAidInfo.DeviceMode.MODE_BINAURAL);
        when(mSubCachedDevice.isConnectedAshaHearingAidDevice()).thenReturn(true);
        when(mCachedDevice.getSubDevice()).thenReturn(mSubCachedDevice);

        assertThat(mController.isAvailable()).isFalse();
    }

    /**
     * Test if the controller is available.
     * Conditions:
     *      1. Binaural ASHA hearing aids
     *      2. Sub device is added
     *      3. Sub device is not connected
     * Expected result:
     *      The controller is available. Need to show the hint to pair the other side.
     */
    @Test
    public void isAvailable_ashaDevice_otherDeviceIsNotConnected_available() {
        when(mCachedDevice.isConnectedHearingAidDevice()).thenReturn(true);
        when(mCachedDevice.isConnectedAshaHearingAidDevice()).thenReturn(true);
        when(mCachedDevice.getDeviceMode()).thenReturn(HearingAidInfo.DeviceMode.MODE_BINAURAL);
        when(mSubCachedDevice.isConnectedAshaHearingAidDevice()).thenReturn(false);
        when(mCachedDevice.getSubDevice()).thenReturn(mSubCachedDevice);

        assertThat(mController.isAvailable()).isTrue();
    }

    /**
     * Test if the controller is available.
     * Conditions:
     *      1. Binaural ASHA hearing aids
     *      2. No sub device added
     * Expected result:
     *      The controller is available. Need to show the hint to pair the other side.
     */
    @Test
    public void isAvailable_ashaDevice_otherDeviceIsNotExist_available() {
        when(mCachedDevice.isConnectedHearingAidDevice()).thenReturn(true);
        when(mCachedDevice.isConnectedAshaHearingAidDevice()).thenReturn(true);
        when(mCachedDevice.getDeviceMode()).thenReturn(HearingAidInfo.DeviceMode.MODE_BINAURAL);
        when(mCachedDevice.getSubDevice()).thenReturn(null);

        assertThat(mController.isAvailable()).isTrue();
    }

    /**
     * Test if the controller is available.
     * Conditions:
     *      1. Binaural LE Audio hearing aids
     *      2. Member device is added
     *      3. Member device is connected
     * Expected result:
     *      The controller is not available. Both sides are already paired and connected.
     */
    @Test
    public void isAvailable_leAudioDevice_otherDeviceIsConnected_notAvailable() {
        when(mCachedDevice.isConnectedHearingAidDevice()).thenReturn(true);
        when(mCachedDevice.isConnectedLeAudioHearingAidDevice()).thenReturn(true);
        when(mCachedDevice.getDeviceMode()).thenReturn(HearingAidInfo.DeviceMode.MODE_BINAURAL);
        when(mSubCachedDevice.isConnectedLeAudioHearingAidDevice()).thenReturn(true);
        when(mCachedDevice.getMemberDevice()).thenReturn(Set.of(mSubCachedDevice));

        assertThat(mController.isAvailable()).isFalse();
    }

    /**
     * Test if the controller is available.
     * Conditions:
     *      1. Binaural LE Audio hearing aids
     *      2. Member device is added
     *      3. Member device is not connected
     * Expected result:
     *      The controller is available. Need to show the hint to pair the other side.
     */
    @Test
    public void isAvailable_leAudioDevice_otherDeviceIsNotConnected_available() {
        when(mCachedDevice.isConnectedHearingAidDevice()).thenReturn(true);
        when(mCachedDevice.isConnectedLeAudioHearingAidDevice()).thenReturn(true);
        when(mCachedDevice.getDeviceMode()).thenReturn(HearingAidInfo.DeviceMode.MODE_BINAURAL);
        when(mSubCachedDevice.isConnectedLeAudioHearingAidDevice()).thenReturn(false);
        when(mCachedDevice.getMemberDevice()).thenReturn(Set.of(mSubCachedDevice));

        assertThat(mController.isAvailable()).isTrue();
    }

    /**
     * Test if the controller is available.
     * Conditions:
     *      1. Binaural LE Audio hearing aids
     *      2. No member device added
     * Expected result:
     *      The controller is available. Need to show the hint to pair the other side.
     */
    @Test
    public void isAvailable_leAudioDevice_otherDeviceIsNotExist_available() {
        when(mCachedDevice.isConnectedHearingAidDevice()).thenReturn(true);
        when(mCachedDevice.isConnectedLeAudioHearingAidDevice()).thenReturn(true);
        when(mCachedDevice.getDeviceMode()).thenReturn(HearingAidInfo.DeviceMode.MODE_BINAURAL);
        when(mCachedDevice.getMemberDevice()).thenReturn(new HashSet<>());

        assertThat(mController.isAvailable()).isTrue();
    }

    /** Test the pair other side button title after refreshing. */
    @Test
    public void refresh_rightSideDevice_pairLeftSideButtonTitle() {
        when(mCachedDevice.getDeviceSide()).thenReturn(HearingAidInfo.DeviceSide.SIDE_RIGHT);
        mController.init(mScreen);

        mController.refresh();

        assertThat(mPreference.getTitle().toString()).isEqualTo(
                mContext.getString(R.string.bluetooth_pair_left_ear_button));
    }

    /** Test the pair other side button visibility after refreshing. */
    @Test
    public void refresh_isNotConnectedHearingAidDevice_preferenceIsNotVisible() {
        when(mCachedDevice.isConnectedHearingAidDevice()).thenReturn(false);
        mController.init(mScreen);

        mController.refresh();

        assertThat(mPreference.isVisible()).isFalse();
        assertThat(mSpacePreference.isVisible()).isFalse();
    }
}
