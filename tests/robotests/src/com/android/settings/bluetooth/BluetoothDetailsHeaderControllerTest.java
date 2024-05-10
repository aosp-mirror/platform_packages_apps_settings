/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothDevice;
import android.graphics.drawable.Drawable;

import com.android.settings.core.SettingsUIDeviceConfig;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowDeviceConfig;
import com.android.settings.testutils.shadow.ShadowEntityHeaderController;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.widget.LayoutPreference;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Ignore
@Config(shadows = {ShadowEntityHeaderController.class, ShadowDeviceConfig.class})
public class BluetoothDetailsHeaderControllerTest extends BluetoothDetailsControllerTestBase {

    private BluetoothDetailsHeaderController mController;
    private LayoutPreference mPreference;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private EntityHeaderController mHeaderController;
    @Mock
    private LocalBluetoothManager mBluetoothManager;
    @Mock
    private CachedBluetoothDeviceManager mCachedDeviceManager;
    @Mock
    private BluetoothDevice mBluetoothDevice;

    @Override
    public void setUp() {
        super.setUp();
        FakeFeatureFactory.setupForTest();
        ShadowEntityHeaderController.setUseMock(mHeaderController);
        when(mBluetoothManager.getCachedDeviceManager()).thenReturn(mCachedDeviceManager);
        when(mCachedDeviceManager.getSubDeviceSummary(mCachedDevice)).thenReturn("abc");
        mController =
            new BluetoothDetailsHeaderController(mContext, mFragment, mCachedDevice, mLifecycle);
        mPreference = new LayoutPreference(
                mContext, com.android.settingslib.widget.preference.layout.R.layout.settings_entity_header);
        mPreference.setKey(mController.getPreferenceKey());
        mScreen.addPreference(mPreference);
        setupDevice(mDeviceConfig);
        when(mCachedDevice.getDevice()).thenReturn(mBluetoothDevice);
    }

    @After
    public void tearDown() {
        ShadowEntityHeaderController.reset();
    }

    /**
     * Test to verify the current test context object works so that we are not checking null
     * against null
     */
    @Test
    public void testContextMock() {
        assertThat(mContext.getString(com.android.settingslib.R.string.bluetooth_connected))
                .isNotNull();
    }

    @Test
    public void header() {
        showScreen(mController);

        verify(mHeaderController).setLabel(mDeviceConfig.getName());
        verify(mHeaderController).setIcon(any(Drawable.class));
        verify(mHeaderController).setIconContentDescription(any(String.class));
        verify(mHeaderController).setSummary(any(String.class));
        verify(mHeaderController).setSecondSummary(any(String.class));
        verify(mHeaderController).done(true);
    }

    @Test
    public void connectionStatusChangesWhileScreenOpen() {
        InOrder inOrder = inOrder(mHeaderController);
        when(mCachedDevice.getConnectionSummary())
            .thenReturn(mContext.getString(com.android.settingslib.R.string.bluetooth_connected));
        showScreen(mController);
        inOrder.verify(mHeaderController)
            .setSummary(mContext.getString(com.android.settingslib.R.string.bluetooth_connected));

        when(mCachedDevice.getConnectionSummary()).thenReturn(null);
        mController.onDeviceAttributesChanged();
        inOrder.verify(mHeaderController).setSummary((CharSequence) null);

        when(mCachedDevice.getConnectionSummary())
            .thenReturn(mContext.getString(com.android.settingslib.R.string.bluetooth_connecting));
        mController.onDeviceAttributesChanged();
        inOrder.verify(mHeaderController)
            .setSummary(mContext.getString(com.android.settingslib.R.string.bluetooth_connecting));
    }

    @Test
    public void isAvailable_untetheredHeadsetWithConfigOn_returnFalse() {
        android.provider.DeviceConfig.setProperty(
                android.provider.DeviceConfig.NAMESPACE_SETTINGS_UI,
                SettingsUIDeviceConfig.BT_ADVANCED_HEADER_ENABLED, "true", true);
        when(mBluetoothDevice.getMetadata(
                BluetoothDevice.METADATA_IS_UNTETHERED_HEADSET)).thenReturn("true".getBytes());

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_untetheredHeadsetWithConfigOff_returnTrue() {
        android.provider.DeviceConfig.setProperty(
                android.provider.DeviceConfig.NAMESPACE_SETTINGS_UI,
                SettingsUIDeviceConfig.BT_ADVANCED_HEADER_ENABLED, "false", true);
        when(mBluetoothDevice.getMetadata(
                BluetoothDevice.METADATA_IS_UNTETHERED_HEADSET)).thenReturn("true".getBytes());

        assertThat(mController.isAvailable()).isTrue();
    }
}
