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

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.util.Pair;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;

import com.android.settings.bluetooth.BluetoothDevicePreference;
import com.android.settings.bluetooth.BluetoothDeviceUpdater;
import com.android.settings.connecteddevice.dock.DockUpdater;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.flags.Flags;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class SavedDeviceGroupControllerTest {

    private static final String FAKE_ADDRESS_1 = "AA:AA:AA:AA:AA:01";
    private static final String FAKE_ADDRESS_2 = "AA:AA:AA:AA:AA:02";
    private static final String FAKE_ADDRESS_3 = "AA:AA:AA:AA:AA:03";

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Mock
    private DashboardFragment mDashboardFragment;
    @Mock
    private BluetoothDeviceUpdater mBluetoothDeviceUpdater;
    @Mock
    private DockUpdater mSavedDockUpdater;
    @Mock
    private PackageManager mPackageManager;
    @Mock private BluetoothManager mBluetoothManager;
    @Mock private BluetoothAdapter mBluetoothAdapter;
    @Mock private CachedBluetoothDevice mCachedDevice1;
    @Mock private CachedBluetoothDevice mCachedDevice2;
    @Mock private CachedBluetoothDevice mCachedDevice3;
    @Mock private BluetoothDevice mBluetoothDevice1;
    @Mock private BluetoothDevice mBluetoothDevice2;
    @Mock private BluetoothDevice mBluetoothDevice3;
    @Mock private Drawable mDrawable;
    @Mock private PreferenceManager mPreferenceManager;

    private Context mContext;
    private SavedDeviceGroupController mSavedDeviceGroupController;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;
    private PreferenceGroup mPreferenceGroup;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Pair<Drawable, String> pairs = new Pair<>(mDrawable, "fake_device");
        mContext = spy(RuntimeEnvironment.application);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        doReturn(mContext).when(mDashboardFragment).getContext();
        doReturn(mPackageManager).when(mContext).getPackageManager();

        when(mCachedDevice1.getDevice()).thenReturn(mBluetoothDevice1);
        when(mCachedDevice1.getAddress()).thenReturn(FAKE_ADDRESS_1);
        when(mCachedDevice1.getDrawableWithDescription()).thenReturn(pairs);
        when(mCachedDevice2.getDevice()).thenReturn(mBluetoothDevice2);
        when(mCachedDevice2.getAddress()).thenReturn(FAKE_ADDRESS_2);
        when(mCachedDevice2.getDrawableWithDescription()).thenReturn(pairs);
        when(mCachedDevice3.getDevice()).thenReturn(mBluetoothDevice3);
        when(mCachedDevice3.getAddress()).thenReturn(FAKE_ADDRESS_3);
        when(mCachedDevice3.getDrawableWithDescription()).thenReturn(pairs);
        final List<BluetoothDevice> mMostRecentlyConnectedDevices = new ArrayList<>();
        mMostRecentlyConnectedDevices.add(mBluetoothDevice1);
        mMostRecentlyConnectedDevices.add(mBluetoothDevice2);
        mMostRecentlyConnectedDevices.add(mBluetoothDevice3);
        when(mContext.getSystemService(BluetoothManager.class)).thenReturn(mBluetoothManager);
        when(mBluetoothManager.getAdapter()).thenReturn(mBluetoothAdapter);
        when(mBluetoothAdapter.getMostRecentlyConnectedDevices())
                .thenReturn(mMostRecentlyConnectedDevices);

        mPreferenceGroup = spy(new PreferenceCategory(mContext));
        when(mPreferenceGroup.getPreferenceManager()).thenReturn(mPreferenceManager);
        mSavedDeviceGroupController = new SavedDeviceGroupController(mContext);
        mSavedDeviceGroupController.setBluetoothDeviceUpdater(mBluetoothDeviceUpdater);
        mSavedDeviceGroupController.setSavedDockUpdater(mSavedDockUpdater);
        mSavedDeviceGroupController.setPreferenceGroup(mPreferenceGroup);
    }

    @Test
    public void testRegister() {
        // register the callback in onStart()
        mSavedDeviceGroupController.onStart();

        verify(mBluetoothDeviceUpdater).registerCallback();
        verify(mSavedDockUpdater).registerCallback();
        verify(mBluetoothDeviceUpdater).refreshPreference();
    }

    @Test
    public void testUnregister() {
        // unregister the callback in onStop()
        mSavedDeviceGroupController.onStop();
        verify(mBluetoothDeviceUpdater).unregisterCallback();
        verify(mSavedDockUpdater).unregisterCallback();
    }

    @Test
    public void testGetAvailabilityStatus_noBluetoothDockFeature_returnUnSupported() {
        doReturn(false).when(mPackageManager).hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
        mSavedDeviceGroupController.setSavedDockUpdater(null);

        assertThat(mSavedDeviceGroupController.getAvailabilityStatus()).isEqualTo(
                UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void testGetAvailabilityStatus_BluetoothFeature_returnSupported() {
        doReturn(true).when(mPackageManager).hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
        mSavedDeviceGroupController.setSavedDockUpdater(null);

        assertThat(mSavedDeviceGroupController.getAvailabilityStatus()).isEqualTo(
                AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_haveDockFeature_returnSupported() {
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)).thenReturn(false);

        assertThat(mSavedDeviceGroupController.getAvailabilityStatus()).isEqualTo(
            AVAILABLE);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SAVED_DEVICES_ORDER_BY_RECENCY)
    public void updatePreferenceGroup_bluetoothIsEnable_shouldOrderByMostRecentlyConnected() {
        when(mBluetoothAdapter.isEnabled()).thenReturn(true);
        final BluetoothDevicePreference preference3 =
                new BluetoothDevicePreference(
                        mContext,
                        mCachedDevice3,
                        true,
                        BluetoothDevicePreference.SortType.TYPE_NO_SORT);
        final BluetoothDevicePreference preference2 =
                new BluetoothDevicePreference(
                        mContext,
                        mCachedDevice2,
                        true,
                        BluetoothDevicePreference.SortType.TYPE_NO_SORT);
        final BluetoothDevicePreference preference1 =
                new BluetoothDevicePreference(
                        mContext,
                        mCachedDevice1,
                        true,
                        BluetoothDevicePreference.SortType.TYPE_NO_SORT);
        mSavedDeviceGroupController.onDeviceAdded(preference3);
        mSavedDeviceGroupController.onDeviceAdded(preference2);
        mSavedDeviceGroupController.onDeviceAdded(preference1);

        mSavedDeviceGroupController.updatePreferenceGroup();

        // Refer to the order of {@link #mMostRecentlyConnectedDevices}
        assertThat(mPreferenceGroup.isVisible()).isTrue();
        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(3);
        assertThat(preference1.getOrder()).isEqualTo(0);
        assertThat(preference2.getOrder()).isEqualTo(1);
        assertThat(preference3.getOrder()).isEqualTo(2);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SAVED_DEVICES_ORDER_BY_RECENCY)
    public void updatePreferenceGroup_bluetoothIsDisable_shouldShowNoPreference() {
        when(mBluetoothAdapter.isEnabled()).thenReturn(false);
        final BluetoothDevicePreference preference3 =
                new BluetoothDevicePreference(
                        mContext,
                        mCachedDevice3,
                        true,
                        BluetoothDevicePreference.SortType.TYPE_NO_SORT);
        final BluetoothDevicePreference preference2 =
                new BluetoothDevicePreference(
                        mContext,
                        mCachedDevice2,
                        true,
                        BluetoothDevicePreference.SortType.TYPE_NO_SORT);
        final BluetoothDevicePreference preference1 =
                new BluetoothDevicePreference(
                        mContext,
                        mCachedDevice2,
                        true,
                        BluetoothDevicePreference.SortType.TYPE_NO_SORT);
        mSavedDeviceGroupController.onDeviceAdded(preference3);
        mSavedDeviceGroupController.onDeviceAdded(preference2);
        mSavedDeviceGroupController.onDeviceAdded(preference1);

        mSavedDeviceGroupController.updatePreferenceGroup();

        assertThat(mPreferenceGroup.isVisible()).isFalse();
    }
}
