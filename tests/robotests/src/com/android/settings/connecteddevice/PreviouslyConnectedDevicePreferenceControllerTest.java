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
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

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
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.util.Pair;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;

import com.android.settings.R;
import com.android.settings.bluetooth.BluetoothDevicePreference;
import com.android.settings.bluetooth.BluetoothDeviceUpdater;
import com.android.settings.connecteddevice.dock.DockUpdater;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.flags.Flags;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.widget.SingleTargetGearPreference;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowBluetoothAdapter.class)
public class PreviouslyConnectedDevicePreferenceControllerTest {

    private static final String KEY = "test_key";
    private static final String FAKE_ADDRESS_1 = "AA:AA:AA:AA:AA:01";
    private static final String FAKE_ADDRESS_2 = "AA:AA:AA:AA:AA:02";
    private static final String FAKE_ADDRESS_3 = "AA:AA:AA:AA:AA:03";
    private static final String FAKE_ADDRESS_4 = "AA:AA:AA:AA:AA:04";
    private static final String FAKE_ADDRESS_5 = "AA:AA:AA:AA:AA:05";

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Mock
    private DashboardFragment mDashboardFragment;
    @Mock
    private BluetoothDeviceUpdater mBluetoothDeviceUpdater;
    @Mock
    private DockUpdater mDockUpdater;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private PreferenceManager mPreferenceManager;
    @Mock
    private Preference mSeeAllPreference;
    @Mock
    private CachedBluetoothDevice mCachedDevice1;
    @Mock
    private CachedBluetoothDevice mCachedDevice2;
    @Mock
    private CachedBluetoothDevice mCachedDevice3;
    @Mock
    private CachedBluetoothDevice mCachedDevice4;
    @Mock
    private CachedBluetoothDevice mCachedDevice5;
    @Mock
    private BluetoothDevice mBluetoothDevice1;
    @Mock
    private BluetoothDevice mBluetoothDevice2;
    @Mock
    private BluetoothDevice mBluetoothDevice3;
    @Mock
    private BluetoothDevice mBluetoothDevice4;
    @Mock
    private BluetoothDevice mBluetoothDevice5;
    @Mock
    private Drawable mDrawable;

    @Mock private BluetoothManager mBluetoothManager;
    @Mock private BluetoothAdapter mBluetoothAdapter;

    private Context mContext;
    private PreviouslyConnectedDevicePreferenceController mPreConnectedDeviceController;
    private PreferenceGroup mPreferenceGroup;
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Pair<Drawable, String> pairs = new Pair<>(mDrawable, "fake_device");
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mContext).when(mDashboardFragment).getContext();
        doReturn(mPackageManager).when(mContext).getPackageManager();
        when(mContext.getSystemService(BluetoothManager.class)).thenReturn(mBluetoothManager);
        when(mBluetoothManager.getAdapter()).thenReturn(mBluetoothAdapter);
        mShadowBluetoothAdapter = Shadow.extract(BluetoothAdapter.getDefaultAdapter());

        when(mCachedDevice1.getDevice()).thenReturn(mBluetoothDevice1);
        when(mCachedDevice1.getAddress()).thenReturn(FAKE_ADDRESS_1);
        when(mCachedDevice1.getDrawableWithDescription()).thenReturn(pairs);
        when(mCachedDevice2.getDevice()).thenReturn(mBluetoothDevice2);
        when(mCachedDevice2.getAddress()).thenReturn(FAKE_ADDRESS_2);
        when(mCachedDevice2.getDrawableWithDescription()).thenReturn(pairs);
        when(mCachedDevice3.getDevice()).thenReturn(mBluetoothDevice3);
        when(mCachedDevice3.getAddress()).thenReturn(FAKE_ADDRESS_3);
        when(mCachedDevice3.getDrawableWithDescription()).thenReturn(pairs);
        when(mCachedDevice4.getDevice()).thenReturn(mBluetoothDevice4);
        when(mCachedDevice4.getAddress()).thenReturn(FAKE_ADDRESS_4);
        when(mCachedDevice4.getDrawableWithDescription()).thenReturn(pairs);
        when(mCachedDevice5.getDevice()).thenReturn(mBluetoothDevice5);
        when(mCachedDevice5.getAddress()).thenReturn(FAKE_ADDRESS_5);
        when(mCachedDevice5.getDrawableWithDescription()).thenReturn(pairs);

        final List<BluetoothDevice> mMostRecentlyConnectedDevices = new ArrayList<>();
        mMostRecentlyConnectedDevices.add(mBluetoothDevice1);
        mMostRecentlyConnectedDevices.add(mBluetoothDevice2);
        mMostRecentlyConnectedDevices.add(mBluetoothDevice4);
        mMostRecentlyConnectedDevices.add(mBluetoothDevice3);
        mShadowBluetoothAdapter.setMostRecentlyConnectedDevices(mMostRecentlyConnectedDevices);
        when(mBluetoothAdapter.getMostRecentlyConnectedDevices())
                .thenReturn(mMostRecentlyConnectedDevices);

        mPreConnectedDeviceController =
                new PreviouslyConnectedDevicePreferenceController(mContext, KEY);
        mPreConnectedDeviceController.setBluetoothDeviceUpdater(mBluetoothDeviceUpdater);
        mPreConnectedDeviceController.setSavedDockUpdater(mDockUpdater);
        mPreferenceGroup = spy(new PreferenceCategory(mContext));
        doReturn(mPreferenceManager).when(mPreferenceGroup).getPreferenceManager();
        mPreferenceGroup.setVisible(false);
        mPreConnectedDeviceController.setPreferenceGroup(mPreferenceGroup);
        mPreConnectedDeviceController.mSeeAllPreference = mSeeAllPreference;
    }

    @Test
    public void onStart_registerCallback() {
        // register the callback in onStart()
        mPreConnectedDeviceController.onStart();

        verify(mBluetoothDeviceUpdater).registerCallback();
        verify(mDockUpdater).registerCallback();
        verify(mContext).registerReceiver(mPreConnectedDeviceController.mReceiver,
                mPreConnectedDeviceController.mIntentFilter, Context.RECEIVER_EXPORTED_UNAUDITED);
        verify(mBluetoothDeviceUpdater).refreshPreference();
    }

    @Test
    public void onStop_unregisterCallback() {
        // register it first
        mContext.registerReceiver(mPreConnectedDeviceController.mReceiver, null,
                Context.RECEIVER_EXPORTED/*UNAUDITED*/);

        // unregister the callback in onStop()
        mPreConnectedDeviceController.onStop();

        verify(mBluetoothDeviceUpdater).unregisterCallback();
        verify(mDockUpdater).unregisterCallback();
        verify(mContext).unregisterReceiver(mPreConnectedDeviceController.mReceiver);
    }

    @Test
    public void getAvailabilityStatus_noBluetoothDockFeature_returnUnSupported() {
        doReturn(false).when(mPackageManager).hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
        mPreConnectedDeviceController.setSavedDockUpdater(null);

        assertThat(mPreConnectedDeviceController.getAvailabilityStatus()).isEqualTo(
                CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_hasBluetoothFeature_returnSupported() {
        doReturn(true).when(mPackageManager).hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
        mPreConnectedDeviceController.setSavedDockUpdater(null);

        assertThat(mPreConnectedDeviceController.getAvailabilityStatus()).isEqualTo(
                AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_haveDockFeature_returnSupported() {
        doReturn(false).when(mPackageManager).hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);

        assertThat(mPreConnectedDeviceController.getAvailabilityStatus()).isEqualTo(
            AVAILABLE);
    }

    @Ignore("b/322712259")
    @Test
    public void onDeviceAdded_addDevicePreference_displayIt() {
        final BluetoothDevicePreference preference1 = new BluetoothDevicePreference(
                mContext, mCachedDevice1, true, BluetoothDevicePreference.SortType.TYPE_NO_SORT);

        mPreConnectedDeviceController.onDeviceAdded(preference1);

        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(2);
    }

    @Ignore("b/322712259")
    @Test
    public void onDeviceAdded_addDockDevicePreference_displayIt() {
        final SingleTargetGearPreference dockPreference = new SingleTargetGearPreference(
                mContext, null /* AttributeSet */);

        mPreConnectedDeviceController.onDeviceAdded(dockPreference);

        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(2);
    }

    @Ignore("b/322712259")
    @Test
    public void onDeviceAdded_addFourDevicePreference_onlyDisplayThree() {
        final BluetoothDevicePreference preference1 = new BluetoothDevicePreference(
                mContext, mCachedDevice1, true, BluetoothDevicePreference.SortType.TYPE_NO_SORT);
        final BluetoothDevicePreference preference2 = new BluetoothDevicePreference(
                mContext, mCachedDevice2, true, BluetoothDevicePreference.SortType.TYPE_NO_SORT);
        final BluetoothDevicePreference preference3 = new BluetoothDevicePreference(
                mContext, mCachedDevice3, true, BluetoothDevicePreference.SortType.TYPE_NO_SORT);
        final BluetoothDevicePreference preference4 = new BluetoothDevicePreference(
                mContext, mCachedDevice4, true, BluetoothDevicePreference.SortType.TYPE_NO_SORT);
        final SingleTargetGearPreference dockPreference = new SingleTargetGearPreference(
                mContext, null /* AttributeSet */);

        mPreConnectedDeviceController.onDeviceAdded(preference1);
        mPreConnectedDeviceController.onDeviceAdded(preference2);
        mPreConnectedDeviceController.onDeviceAdded(preference3);
        mPreConnectedDeviceController.onDeviceAdded(preference4);
        mPreConnectedDeviceController.onDeviceAdded(dockPreference);

        // 3 BluetoothDevicePreference and 1 see all preference
        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(4);
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_SAVED_DEVICES_ORDER_BY_RECENCY)
    public void onDeviceAdded_addPreferenceNotExistInRecentlyDevices_noCrash() {
        final BluetoothDevicePreference preference = new BluetoothDevicePreference(
                mContext, mCachedDevice5, true, BluetoothDevicePreference.SortType.TYPE_NO_SORT);

        mPreConnectedDeviceController.onDeviceAdded(preference);

        // 1 BluetoothDevicePreference and 1 see all preference
        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(2);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SAVED_DEVICES_ORDER_BY_RECENCY)
    public void onDeviceAdded_addPreferenceNotExistInRecentlyDevices_doNothing() {
        final BluetoothDevicePreference preference = new BluetoothDevicePreference(
                mContext, mCachedDevice5, true, BluetoothDevicePreference.SortType.TYPE_NO_SORT);

        mPreConnectedDeviceController.onDeviceAdded(preference);

        // 1 see all preference
        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(1);
    }

    @Test
    public void onDeviceRemoved_removeLastDevice_showSeeAllPreference() {
        final BluetoothDevicePreference preference1 = new BluetoothDevicePreference(
                mContext, mCachedDevice1, true, BluetoothDevicePreference.SortType.TYPE_NO_SORT);
        final SingleTargetGearPreference dockPreference = new SingleTargetGearPreference(
                mContext, null /* AttributeSet */);
        mPreferenceGroup.addPreference(preference1);
        mPreferenceGroup.addPreference(dockPreference);

        mPreConnectedDeviceController.onDeviceRemoved(preference1);
        mPreConnectedDeviceController.onDeviceRemoved(dockPreference);

        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(1);
    }

    @Test
    public void updatePreferenceVisibility_bluetoothIsEnable_shouldShowCorrectText() {
        mShadowBluetoothAdapter.setEnabled(true);
        when(mBluetoothAdapter.isEnabled()).thenReturn(true);
        mPreConnectedDeviceController.updatePreferenceVisibility();

        verify(mSeeAllPreference).setSummary("");
    }

    @Test
    public void updatePreferenceVisibility_bluetoothIsDisable_shouldShowCorrectText() {
        mShadowBluetoothAdapter.setEnabled(false);
        when(mBluetoothAdapter.isEnabled()).thenReturn(false);
        mPreConnectedDeviceController.updatePreferenceVisibility();

        verify(mSeeAllPreference).setSummary(
                mContext.getString(R.string.connected_device_see_all_summary));
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SAVED_DEVICES_ORDER_BY_RECENCY)
    public void updatePreferenceGroup_bluetoothIsEnable_shouldOrderByMostRecentlyConnected() {
        when(mBluetoothAdapter.isEnabled()).thenReturn(true);
        final BluetoothDevicePreference preference4 =
                new BluetoothDevicePreference(
                        mContext,
                        mCachedDevice4,
                        true,
                        BluetoothDevicePreference.SortType.TYPE_NO_SORT);
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
        mPreConnectedDeviceController.onDeviceAdded(preference4);
        mPreConnectedDeviceController.onDeviceAdded(preference3);
        mPreConnectedDeviceController.onDeviceAdded(preference2);

        mPreConnectedDeviceController.updatePreferenceGroup();

        // Refer to the order of {@link #mMostRecentlyConnectedDevices}, the first one is see all
        // preference
        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(4);
        assertThat(preference2.getOrder()).isEqualTo(0);
        assertThat(preference4.getOrder()).isEqualTo(1);
        assertThat(preference3.getOrder()).isEqualTo(2);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SAVED_DEVICES_ORDER_BY_RECENCY)
    public void updatePreferenceGroup_bluetoothIsDisable_shouldShowOnlySeeAllPreference() {
        when(mBluetoothAdapter.isEnabled()).thenReturn(false);
        final BluetoothDevicePreference preference4 =
                new BluetoothDevicePreference(
                        mContext,
                        mCachedDevice4,
                        true,
                        BluetoothDevicePreference.SortType.TYPE_NO_SORT);
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
        mPreConnectedDeviceController.onDeviceAdded(preference4);
        mPreConnectedDeviceController.onDeviceAdded(preference3);
        mPreConnectedDeviceController.onDeviceAdded(preference2);

        mPreConnectedDeviceController.updatePreferenceGroup();

        // 1 see all preference
        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(1);
    }
}
