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

package com.android.settings.connecteddevice.stylus;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.Intent;
import android.hardware.BatteryState;
import android.hardware.input.InputManager;
import android.os.SystemClock;
import android.view.InputDevice;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothAdapter.class})
public class StylusDeviceUpdaterTest {

    private Context mContext;
    private StylusDeviceUpdater mStylusDeviceUpdater;
    private InputDevice mStylusDevice;
    private InputDevice mOtherDevice;

    @Mock
    private SettingsActivity mSettingsActivity;
    @Mock
    private DashboardFragment mDashboardFragment;
    @Mock
    private DevicePreferenceCallback mDevicePreferenceCallback;
    @Mock
    private InputManager mInputManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);

        doReturn(mContext).when(mDashboardFragment).getContext();
        doReturn(mInputManager).when(mContext).getSystemService(InputManager.class);
        doReturn(new int[]{}).when(mInputManager).getInputDeviceIds();

        mStylusDeviceUpdater = spy(
                new StylusDeviceUpdater(mContext, mDashboardFragment, mDevicePreferenceCallback));
        mStylusDeviceUpdater.setPreferenceContext(mContext);

        doReturn(new int[]{0, 1}).when(mInputManager).getInputDeviceIds();
        mOtherDevice = new InputDevice.Builder().setId(0).setName("other").setSources(
                InputDevice.SOURCE_DPAD).build();
        doReturn(mOtherDevice).when(mInputManager).getInputDevice(0);
        mStylusDevice = new InputDevice.Builder().setId(1).setName("Pen").setExternal(
                false).setSources(
                InputDevice.SOURCE_STYLUS).build();
        doReturn(mStylusDevice).when(mInputManager).getInputDevice(1);
    }

    @Test
    public void registerCallback_registersBatteryListener() {
        mStylusDeviceUpdater.registerCallback();

        verify(mInputManager, times(1)).addInputDeviceBatteryListener(eq(1), any(),
                any());
    }

    @Test
    public void registerCallback_registersInputDeviceListener() {
        mStylusDeviceUpdater.registerCallback();

        verify(mInputManager, times(1)).registerInputDeviceListener(eq(mStylusDeviceUpdater),
                any());
    }

    @Test
    public void onInputDeviceAdded_internalStylus_registersBatteryListener() {
        mStylusDeviceUpdater.onInputDeviceAdded(1);

        verify(mInputManager, times(1)).addInputDeviceBatteryListener(eq(1), any(),
                any());
    }

    @Test
    public void onInputDeviceAdded_nonStylus_doesNotRegisterBatteryListener() {
        mStylusDeviceUpdater.onInputDeviceAdded(0);

        verify(mInputManager, never()).addInputDeviceBatteryListener(eq(1), any(),
                any());
    }

    @Test
    public void click_usiPreference_launchUsiDetailsPage() {
        doReturn(mSettingsActivity).when(mDashboardFragment).getContext();
        doReturn(true).when(mStylusDeviceUpdater).isUsiBatteryValid();
        doReturn(false).when(mStylusDeviceUpdater).hasConnectedBluetoothStylusDevice();
        mStylusDeviceUpdater.forceUpdate();
        mStylusDeviceUpdater.mLastDetectedUsiId = 1;
        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);

        mStylusDeviceUpdater.mUsiPreference.performClick();

        assertThat(mStylusDeviceUpdater.mUsiPreference.getTitle().toString()).isEqualTo(
                mContext.getString(R.string.stylus_connected_devices_title));
        verify(mSettingsActivity).startActivity(intentCaptor.capture());
        assertThat(intentCaptor.getValue().getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo(StylusUsiDetailsFragment.class.getName());
    }

    @Test
    public void forceUpdate_addsUsiPreference_validUsiDevice() {
        doReturn(true).when(mStylusDeviceUpdater).isUsiBatteryValid();
        doReturn(false).when(mStylusDeviceUpdater).hasConnectedBluetoothStylusDevice();

        mStylusDeviceUpdater.forceUpdate();

        assertThat(mStylusDeviceUpdater.mUsiPreference).isNotNull();
    }

    @Test
    public void forceUpdate_doesNotAddPreference_invalidUsiDevice() {
        doReturn(false).when(mStylusDeviceUpdater).isUsiBatteryValid();
        doReturn(false).when(mStylusDeviceUpdater).hasConnectedBluetoothStylusDevice();

        mStylusDeviceUpdater.forceUpdate();

        assertThat(mStylusDeviceUpdater.mUsiPreference).isNull();
    }

    @Test
    public void forceUpdate_removesUsiPreference_existingPreference_invalidUsiDevice() {
        doReturn(true).when(mStylusDeviceUpdater).isUsiBatteryValid();
        doReturn(false).when(mStylusDeviceUpdater).hasConnectedBluetoothStylusDevice();

        mStylusDeviceUpdater.forceUpdate();

        doReturn(false).when(mStylusDeviceUpdater).isUsiBatteryValid();
        mStylusDeviceUpdater.forceUpdate();

        assertThat(mStylusDeviceUpdater.mUsiPreference).isNull();
    }

    @Test
    public void forceUpdate_doesNotAddUsiPreference_bluetoothStylusConnected() {
        doReturn(true).when(mStylusDeviceUpdater).isUsiBatteryValid();
        doReturn(true).when(mStylusDeviceUpdater).hasConnectedBluetoothStylusDevice();

        mStylusDeviceUpdater.forceUpdate();

        assertThat(mStylusDeviceUpdater.mUsiPreference).isNull();
    }

    @Test
    public void forceUpdate_addsUsiPreference_bluetoothStylusDisconnected() {
        doReturn(true).when(mStylusDeviceUpdater).isUsiBatteryValid();
        doReturn(true).when(mStylusDeviceUpdater).hasConnectedBluetoothStylusDevice();
        mStylusDeviceUpdater.forceUpdate();

        doReturn(false).when(mStylusDeviceUpdater).hasConnectedBluetoothStylusDevice();
        mStylusDeviceUpdater.forceUpdate();

        assertThat(mStylusDeviceUpdater.mUsiPreference).isNotNull();
    }

    @Test
    public void forceUpdate_removesUsiPreference_existingPreference_bluetoothStylusConnected() {
        doReturn(true).when(mStylusDeviceUpdater).isUsiBatteryValid();
        doReturn(false).when(mStylusDeviceUpdater).hasConnectedBluetoothStylusDevice();
        mStylusDeviceUpdater.forceUpdate();
        doReturn(true).when(mStylusDeviceUpdater).hasConnectedBluetoothStylusDevice();

        mStylusDeviceUpdater.forceUpdate();

        assertThat(mStylusDeviceUpdater.mUsiPreference).isNull();
    }

    @Test
    public void onBatteryStateChanged_detectsValidUsi() {
        BatteryState batteryState = mock(BatteryState.class);
        doReturn(true).when(batteryState).isPresent();
        doReturn(0.5f).when(batteryState).getCapacity();

        mStylusDeviceUpdater.onBatteryStateChanged(1, SystemClock.uptimeMillis(),
                batteryState);

        assertThat(mStylusDeviceUpdater.isUsiBatteryValid()).isTrue();
    }

    @Test
    public void onBatteryStateChanged_detectsInvalidUsi_batteryNotPresent() {
        doReturn(false).when(mStylusDeviceUpdater).hasConnectedBluetoothStylusDevice();
        BatteryState batteryState = mock(BatteryState.class);
        doReturn(false).when(batteryState).isPresent();

        mStylusDeviceUpdater.onBatteryStateChanged(1, SystemClock.uptimeMillis(),
                batteryState);

        assertThat(mStylusDeviceUpdater.isUsiBatteryValid()).isFalse();
    }

    @Test
    public void detectsConnectedBluetoothStylus() {
        InputDevice stylusDevice = new InputDevice.Builder().setId(1).setName("Pen").setSources(
                        InputDevice.SOURCE_STYLUS)
                .build();
        doReturn(stylusDevice).when(mInputManager).getInputDevice(1);
        doReturn("04:52:C7:0B:D8:3C").when(mInputManager).getInputDeviceBluetoothAddress(1);

        assertThat(mStylusDeviceUpdater.hasConnectedBluetoothStylusDevice()).isTrue();
    }

    @Test
    public void detectsDisconnectedBluetoothStylus() {
        InputDevice stylusDevice = new InputDevice.Builder().setId(1).setName("Pen").setSources(
                InputDevice.SOURCE_STYLUS).build();
        doReturn(stylusDevice).when(mInputManager).getInputDevice(1);
        doReturn(null).when(mInputManager).getInputDeviceBluetoothAddress(1);

        assertThat(mStylusDeviceUpdater.hasConnectedBluetoothStylusDevice()).isFalse();
    }
}
