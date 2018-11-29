/*
 * Copyright 2018 The Android Open Source Project
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
 * limitations under the License
 */
package com.android.settings.connecteddevice;

import android.content.Context;
import android.content.pm.PackageManager;

import android.support.v7.preference.Preference;

import com.android.settings.bluetooth.BluetoothDeviceUpdater;
import com.android.settings.connecteddevice.dock.DockUpdater;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.*;

@RunWith(SettingsRobolectricTestRunner.class)
public class PreviouslyConnectedDevicePreferenceControllerTest {

    private final String KEY = "test_key";

    @Mock
    private DashboardFragment mDashboardFragment;
    @Mock
    private BluetoothDeviceUpdater mBluetoothDeviceUpdater;
    @Mock
    private DockUpdater mDockUpdater;
    @Mock
    private PackageManager mPackageManager;

    private Context mContext;
    private PreviouslyConnectedDevicePreferenceController mPreConnectedDeviceController;
    private Preference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mContext).when(mDashboardFragment).getContext();
        doReturn(mPackageManager).when(mContext).getPackageManager();
        mPreConnectedDeviceController =
                new PreviouslyConnectedDevicePreferenceController(mContext, KEY);
        mPreConnectedDeviceController.setBluetoothDeviceUpdater(mBluetoothDeviceUpdater);
        mPreConnectedDeviceController.setSavedDockUpdater(mDockUpdater);

        mPreference = new Preference(mContext);
        mPreConnectedDeviceController.setPreference(mPreference);
    }

    @Test
    public void callbackCanRegisterAndUnregister() {
        // register the callback in onStart()
        mPreConnectedDeviceController.onStart();
        verify(mBluetoothDeviceUpdater).registerCallback();
        verify(mDockUpdater).registerCallback();

        // unregister the callback in onStop()
        mPreConnectedDeviceController.onStop();
        verify(mBluetoothDeviceUpdater).unregisterCallback();
        verify(mDockUpdater).unregisterCallback();
    }

    @Test
    public void getAvailabilityStatus_noBluetoothFeature_returnUnSupported() {
        doReturn(false).when(mPackageManager).hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
        assertThat(mPreConnectedDeviceController.getAvailabilityStatus()).isEqualTo(
                CONDITIONALLY_UNAVAILABLE);
    }
    @Test
    public void getAvailabilityStatus_hasBluetoothFeature_returnSupported() {
        doReturn(true).when(mPackageManager).hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
        assertThat(mPreConnectedDeviceController.getAvailabilityStatus()).isEqualTo(
                AVAILABLE);
    }

    @Test
    public void onDeviceAdded_addFirstDevice_preferenceIsEnable() {
        doReturn(true).when(mPackageManager).hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
        mPreConnectedDeviceController.setPreferenceSize(0);
        mPreConnectedDeviceController.onDeviceAdded(mPreference);

        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void onDeviceRemoved_removeLastDevice_preferenceIsDisable() {
        doReturn(true).when(mPackageManager).hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
        mPreConnectedDeviceController.setPreferenceSize(1);
        mPreConnectedDeviceController.onDeviceRemoved(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
    }
}
