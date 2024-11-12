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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.flags.Flags;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowDeviceConfig;
import com.android.settings.testutils.shadow.ShadowEntityHeaderController;
import com.android.settingslib.bluetooth.LeAudioProfile;
import com.android.settingslib.widget.LayoutPreference;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowDeviceConfig.class})
public class GeneralBluetoothDetailsHeaderControllerTest
        extends BluetoothDetailsControllerTestBase {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private GeneralBluetoothDetailsHeaderController mController;
    private LayoutPreference mPreference;

    @Mock private BluetoothDevice mBluetoothDevice;
    @Mock private LeAudioProfile mLeAudioProfile;

    @Override
    public void setUp() {
        super.setUp();
        FakeFeatureFactory.setupForTest();
        mController =
                new GeneralBluetoothDetailsHeaderController(
                        mContext, mFragment, mCachedDevice, mLifecycle);
        mPreference = new LayoutPreference(mContext, R.layout.general_bt_entity_header);
        mPreference.setKey(mController.getPreferenceKey());
        mScreen.addPreference(mPreference);
        setupDevice(mDeviceConfig);
        when(mCachedDevice.getDevice()).thenReturn(mBluetoothDevice);
        when(mLeAudioProfile.getProfileId()).thenReturn(BluetoothProfile.LE_AUDIO);
    }

    @After
    public void tearDown() {
        ShadowEntityHeaderController.reset();
    }

    /**
     * Test to verify the current test context object works so that we are not checking null against
     * null
     */
    @Test
    public void testContextMock() {
        assertThat(mContext.getString(com.android.settingslib.R.string.bluetooth_connected))
                .isNotNull();
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_BLUETOOTH_DEVICE_DETAILS_POLISH)
    public void header() {
        when(mCachedDevice.getName()).thenReturn("device name");
        when(mCachedDevice.getConnectionSummary()).thenReturn("Active");

        showScreen(mController);

        TextView deviceName = mPreference.findViewById(R.id.bt_header_device_name);
        TextView summary = mPreference.findViewById(R.id.bt_header_connection_summary);
        assertThat(deviceName.getText().toString()).isEqualTo("device name");
        assertThat(summary.getText().toString()).isEqualTo("Active");
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_BLUETOOTH_DEVICE_DETAILS_POLISH)
    public void connectionStatusChangesWhileScreenOpen() {
        TextView summary = mPreference.findViewById(R.id.bt_header_connection_summary);
        when(mCachedDevice.getConnectionSummary())
                .thenReturn(
                        mContext.getString(com.android.settingslib.R.string.bluetooth_connected));

        showScreen(mController);
        String summaryText1 = summary.getText().toString();
        when(mCachedDevice.getConnectionSummary()).thenReturn(null);
        mController.onDeviceAttributesChanged();
        String summaryText2 = summary.getText().toString();
        when(mCachedDevice.getConnectionSummary())
                .thenReturn(
                        mContext.getString(com.android.settingslib.R.string.bluetooth_connecting));
        mController.onDeviceAttributesChanged();
        String summaryText3 = summary.getText().toString();

        assertThat(summaryText1)
                .isEqualTo(
                        mContext.getString(com.android.settingslib.R.string.bluetooth_connected));
        assertThat(summaryText2).isEqualTo("");
        assertThat(summaryText3)
                .isEqualTo(
                        mContext.getString(com.android.settingslib.R.string.bluetooth_connecting));
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_BLUETOOTH_DEVICE_DETAILS_POLISH)
    public void isAvailable_untetheredHeadset_returnFalse() {
        when(mBluetoothDevice.getMetadata(BluetoothDevice.METADATA_IS_UNTETHERED_HEADSET))
                .thenReturn("true".getBytes());

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_BLUETOOTH_DEVICE_DETAILS_POLISH)
    public void isAvailable_notUntetheredHeadset_returnTrue() {
        when(mBluetoothDevice.getMetadata(BluetoothDevice.METADATA_IS_UNTETHERED_HEADSET))
                .thenReturn("false".getBytes());

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_BLUETOOTH_DEVICE_DETAILS_POLISH)
    public void isAvailable_leAudioDevice_returnFalse() {
        when(mCachedDevice.getUiAccessibleProfiles())
                .thenReturn(List.of(mLeAudioProfile));

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_BLUETOOTH_DEVICE_DETAILS_POLISH)
    public void isAvailable_flagEnabled_returnTrue() {
        when(mBluetoothDevice.getMetadata(BluetoothDevice.METADATA_IS_UNTETHERED_HEADSET))
                .thenReturn("false".getBytes());

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_BLUETOOTH_DEVICE_DETAILS_POLISH)
    public void iaAvailable_flagDisabled_returnFalse() {
        assertThat(mController.isAvailable()).isFalse();
    }
}
