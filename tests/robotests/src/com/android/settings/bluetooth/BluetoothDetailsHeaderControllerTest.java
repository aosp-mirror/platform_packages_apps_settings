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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.drawable.Drawable;

import com.android.settings.R;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.SettingsShadowBluetoothDevice;
import com.android.settings.testutils.shadow.ShadowEntityHeaderController;
import com.android.settings.widget.EntityHeaderController;

import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)

@Config(shadows = {SettingsShadowBluetoothDevice.class, ShadowEntityHeaderController.class})
public class BluetoothDetailsHeaderControllerTest extends BluetoothDetailsControllerTestBase {

    private BluetoothDetailsHeaderController mController;
    private LayoutPreference mPreference;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private EntityHeaderController mHeaderController;
    @Mock
    private LocalBluetoothManager mBluetoothManager;
    @Mock
    private CachedBluetoothDeviceManager mCachedDeviceManager;

    @Override
    public void setUp() {
        super.setUp();
        FakeFeatureFactory.setupForTest();
        ShadowEntityHeaderController.setUseMock(mHeaderController);
        when(mBluetoothManager.getCachedDeviceManager()).thenReturn(mCachedDeviceManager);
        when(mCachedDeviceManager.getHearingAidPairDeviceSummary(mCachedDevice)).thenReturn("abc");
        mController =
            new BluetoothDetailsHeaderController(mContext, mFragment, mCachedDevice, mLifecycle,
                mBluetoothManager);
        mPreference = new LayoutPreference(mContext, R.layout.settings_entity_header);
        mPreference.setKey(mController.getPreferenceKey());
        mScreen.addPreference(mPreference);
        setupDevice(mDeviceConfig);
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
        assertThat(mContext.getString(R.string.bluetooth_connected)).isNotNull();
    }

    @Test
    public void header() {
        showScreen(mController);

        verify(mHeaderController).setLabel(mDeviceConfig.getName());
        verify(mHeaderController).setIcon(any(Drawable.class));
        verify(mHeaderController).setIconContentDescription(any(String.class));
        verify(mHeaderController).setSummary(any(String.class));
        verify(mHeaderController, never()).setSecondSummary(any(String.class));
        verify(mHeaderController).done(mActivity, true);
    }

    @Test
    public void connectionStatusChangesWhileScreenOpen() {
        InOrder inOrder = inOrder(mHeaderController);
        when(mCachedDevice.getConnectionSummary())
            .thenReturn(mContext.getString(R.string.bluetooth_connected));
        showScreen(mController);
        inOrder.verify(mHeaderController)
            .setSummary(mContext.getString(R.string.bluetooth_connected));

        when(mCachedDevice.getConnectionSummary()).thenReturn(null);
        mController.onDeviceAttributesChanged();
        inOrder.verify(mHeaderController).setSummary((CharSequence) null);

        when(mCachedDevice.getConnectionSummary())
            .thenReturn(mContext.getString(R.string.bluetooth_connecting));
        mController.onDeviceAttributesChanged();
        inOrder.verify(mHeaderController)
            .setSummary(mContext.getString(R.string.bluetooth_connecting));
    }

    @Test
    public void testSecondSummary_isHearingAidDevice_showSecondSummary() {
        when(mCachedDevice.isHearingAidDevice()).thenReturn(true);
        showScreen(mController);

        verify(mHeaderController).setSecondSummary(any(String.class));
    }
}
