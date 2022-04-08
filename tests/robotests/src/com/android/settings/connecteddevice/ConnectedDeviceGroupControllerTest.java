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
 * limitations under the License
 */
package com.android.settings.connecteddevice;

import static com.android.settings.core.BasePreferenceController.AVAILABLE_UNSEARCHABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.bluetooth.ConnectedBluetoothDeviceUpdater;
import com.android.settings.connecteddevice.dock.DockUpdater;
import com.android.settings.connecteddevice.usb.ConnectedUsbDeviceUpdater;
import com.android.settings.dashboard.DashboardFragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplicationPackageManager;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowApplicationPackageManager.class)
public class ConnectedDeviceGroupControllerTest {

    private static final String PREFERENCE_KEY_1 = "pref_key_1";

    @Mock
    private DashboardFragment mDashboardFragment;
    @Mock
    private ConnectedBluetoothDeviceUpdater mConnectedBluetoothDeviceUpdater;
    @Mock
    private ConnectedUsbDeviceUpdater mConnectedUsbDeviceUpdater;
    @Mock
    private DockUpdater mConnectedDockUpdater;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceManager mPreferenceManager;

    private ShadowApplicationPackageManager mPackageManager;
    private PreferenceGroup mPreferenceGroup;
    private Context mContext;
    private Preference mPreference;
    private ConnectedDeviceGroupController mConnectedDeviceGroupController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mPreference = new Preference(mContext);
        mPreference.setKey(PREFERENCE_KEY_1);
        mPackageManager = (ShadowApplicationPackageManager) Shadows.shadowOf(
                mContext.getPackageManager());
        mPreferenceGroup = spy(new PreferenceScreen(mContext, null));
        when(mPreferenceGroup.getPreferenceManager()).thenReturn(mPreferenceManager);
        doReturn(mContext).when(mDashboardFragment).getContext();
        mPackageManager.setSystemFeature(PackageManager.FEATURE_BLUETOOTH, true);

        mConnectedDeviceGroupController = new ConnectedDeviceGroupController(mContext);
        mConnectedDeviceGroupController.init(mConnectedBluetoothDeviceUpdater,
                mConnectedUsbDeviceUpdater, mConnectedDockUpdater);
        mConnectedDeviceGroupController.mPreferenceGroup = mPreferenceGroup;
    }

    @Test
    public void onDeviceAdded_firstAdd_becomeVisibleAndPreferenceAdded() {
        mConnectedDeviceGroupController.onDeviceAdded(mPreference);

        assertThat(mPreferenceGroup.isVisible()).isTrue();
        assertThat((Preference) mPreferenceGroup.findPreference(PREFERENCE_KEY_1))
                .isEqualTo(mPreference);
    }

    @Test
    public void onDeviceRemoved_lastRemove_becomeInvisibleAndPreferenceRemoved() {
        mPreferenceGroup.addPreference(mPreference);

        mConnectedDeviceGroupController.onDeviceRemoved(mPreference);

        assertThat(mPreferenceGroup.isVisible()).isFalse();
        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    public void onDeviceRemoved_notLastRemove_stillVisible() {
        mPreferenceGroup.setVisible(true);
        mPreferenceGroup.addPreference(mPreference);
        mPreferenceGroup.addPreference(new Preference(mContext));

        mConnectedDeviceGroupController.onDeviceRemoved(mPreference);

        assertThat(mPreferenceGroup.isVisible()).isTrue();
    }

    @Test
    public void displayPreference_becomeInvisible() {
        doReturn(mPreferenceGroup).when(mPreferenceScreen).findPreference(anyString());

        mConnectedDeviceGroupController.displayPreference(mPreferenceScreen);

        assertThat(mPreferenceGroup.isVisible()).isFalse();
    }

    @Test
    public void onStart_shouldRegisterUpdaters() {
        // register the callback in onStart()
        mConnectedDeviceGroupController.onStart();
        verify(mConnectedBluetoothDeviceUpdater).registerCallback();
        verify(mConnectedUsbDeviceUpdater).registerCallback();
        verify(mConnectedDockUpdater).registerCallback();
    }

    @Test
    public void onStop_shouldUnregisterUpdaters() {
        // unregister the callback in onStop()
        mConnectedDeviceGroupController.onStop();
        verify(mConnectedBluetoothDeviceUpdater).unregisterCallback();
        verify(mConnectedUsbDeviceUpdater).unregisterCallback();
        verify(mConnectedDockUpdater).unregisterCallback();
    }

    @Test
    public void getAvailabilityStatus_noBluetoothUsbDockFeature_returnUnSupported() {
        mPackageManager.setSystemFeature(PackageManager.FEATURE_BLUETOOTH, false);
        mPackageManager.setSystemFeature(PackageManager.FEATURE_USB_ACCESSORY, false);
        mPackageManager.setSystemFeature(PackageManager.FEATURE_USB_HOST, false);
        mConnectedDeviceGroupController.init(mConnectedBluetoothDeviceUpdater,
                mConnectedUsbDeviceUpdater, null);

        assertThat(mConnectedDeviceGroupController.getAvailabilityStatus()).isEqualTo(
                UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_BluetoothFeature_returnSupported() {
        mPackageManager.setSystemFeature(PackageManager.FEATURE_BLUETOOTH, true);
        mPackageManager.setSystemFeature(PackageManager.FEATURE_USB_ACCESSORY, false);
        mPackageManager.setSystemFeature(PackageManager.FEATURE_USB_HOST, false);
        mConnectedDeviceGroupController.init(mConnectedBluetoothDeviceUpdater,
                mConnectedUsbDeviceUpdater, null);

        assertThat(mConnectedDeviceGroupController.getAvailabilityStatus()).isEqualTo(
                AVAILABLE_UNSEARCHABLE);
    }

    @Test
    public void getAvailabilityStatus_haveUsbFeature_returnSupported() {
        mPackageManager.setSystemFeature(PackageManager.FEATURE_BLUETOOTH, false);
        mPackageManager.setSystemFeature(PackageManager.FEATURE_USB_ACCESSORY, false);
        mPackageManager.setSystemFeature(PackageManager.FEATURE_USB_HOST, true);
        mConnectedDeviceGroupController.init(mConnectedBluetoothDeviceUpdater,
                mConnectedUsbDeviceUpdater, null);

        assertThat(mConnectedDeviceGroupController.getAvailabilityStatus()).isEqualTo(
                AVAILABLE_UNSEARCHABLE);
    }

    @Test
    public void getAvailabilityStatus_haveDockFeature_returnSupported() {
        mPackageManager.setSystemFeature(PackageManager.FEATURE_BLUETOOTH, false);
        mPackageManager.setSystemFeature(PackageManager.FEATURE_USB_ACCESSORY, false);
        mPackageManager.setSystemFeature(PackageManager.FEATURE_USB_HOST, false);
        mConnectedDeviceGroupController.init(mConnectedBluetoothDeviceUpdater,
                mConnectedUsbDeviceUpdater, mConnectedDockUpdater);

        assertThat(mConnectedDeviceGroupController.getAvailabilityStatus()).isEqualTo(
                AVAILABLE_UNSEARCHABLE);
    }

}
