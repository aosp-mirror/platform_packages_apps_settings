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
import static com.android.settings.flags.Flags.FLAG_RESOLUTION_AND_ENABLE_CONNECTED_DISPLAY_SETTING;
import static com.android.settings.flags.Flags.FLAG_ROTATION_CONNECTED_DISPLAY_SETTING;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.hardware.input.InputManager;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.util.FeatureFlagUtils;
import android.view.InputDevice;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.bluetooth.ConnectedBluetoothDeviceUpdater;
import com.android.settings.bluetooth.Utils;
import com.android.settings.connecteddevice.display.ExternalDisplayUpdater;
import com.android.settings.connecteddevice.dock.DockUpdater;
import com.android.settings.connecteddevice.stylus.StylusDeviceUpdater;
import com.android.settings.connecteddevice.usb.ConnectedUsbDeviceUpdater;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.flags.FakeFeatureFlagsImpl;
import com.android.settings.flags.Flags;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
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
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplicationPackageManager;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowApplicationPackageManager.class, ShadowBluetoothUtils.class,
        ShadowBluetoothAdapter.class})
public class ConnectedDeviceGroupControllerTest {

    private static final String PREFERENCE_KEY_1 = "pref_key_1";
    private static final String DEVICE_NAME = "device";

    @Mock
    private DashboardFragment mDashboardFragment;
    @Mock
    private ExternalDisplayUpdater mExternalDisplayUpdater;
    @Mock
    private ConnectedBluetoothDeviceUpdater mConnectedBluetoothDeviceUpdater;
    @Mock
    private ConnectedUsbDeviceUpdater mConnectedUsbDeviceUpdater;
    @Mock
    private DockUpdater mConnectedDockUpdater;
    @Mock
    private StylusDeviceUpdater mStylusDeviceUpdater;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceManager mPreferenceManager;
    @Mock
    private InputManager mInputManager;
    @Mock
    private CachedBluetoothDeviceManager mCachedDeviceManager;
    @Mock
    private LocalBluetoothManager mLocalBluetoothManager;
    @Mock
    private CachedBluetoothDevice mCachedDevice;
    @Mock
    private BluetoothDevice mDevice;
    @Mock
    private Resources mResources;
    private final FakeFeatureFlagsImpl mFakeFeatureFlags = new FakeFeatureFlagsImpl();

    private ShadowApplicationPackageManager mPackageManager;
    private PreferenceGroup mPreferenceGroup;
    private Context mContext;
    private Preference mPreference;
    private ConnectedDeviceGroupController mConnectedDeviceGroupController;

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFakeFeatureFlags.setFlag(FLAG_ROTATION_CONNECTED_DISPLAY_SETTING, true);
        mFakeFeatureFlags.setFlag(FLAG_RESOLUTION_AND_ENABLE_CONNECTED_DISPLAY_SETTING, true);

        mContext = spy(ApplicationProvider.getApplicationContext());
        mPreference = new Preference(mContext);
        mPreference.setKey(PREFERENCE_KEY_1);
        mPackageManager = (ShadowApplicationPackageManager) Shadows.shadowOf(
                mContext.getPackageManager());
        mPreferenceGroup = spy(new PreferenceScreen(mContext, null));
        when(mPreferenceGroup.getPreferenceManager()).thenReturn(mPreferenceManager);
        doReturn(mContext).when(mDashboardFragment).getContext();
        mPackageManager.setSystemFeature(PackageManager.FEATURE_BLUETOOTH, true);
        when(mContext.getSystemService(InputManager.class)).thenReturn(mInputManager);
        when(mContext.getResources()).thenReturn(mResources);
        when(mInputManager.getInputDeviceIds()).thenReturn(new int[]{});

        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBluetoothManager;
        mLocalBluetoothManager = Utils.getLocalBtManager(mContext);
        when(mLocalBluetoothManager.getCachedDeviceManager()).thenReturn(mCachedDeviceManager);

        mConnectedDeviceGroupController = spy(new ConnectedDeviceGroupController(mContext));
        when(mConnectedDeviceGroupController.getFeatureFlags()).thenReturn(mFakeFeatureFlags);

        mConnectedDeviceGroupController.init(mExternalDisplayUpdater,
                mConnectedBluetoothDeviceUpdater, mConnectedUsbDeviceUpdater, mConnectedDockUpdater,
                mStylusDeviceUpdater);
        mConnectedDeviceGroupController.mPreferenceGroup = mPreferenceGroup;

        when(mCachedDevice.getName()).thenReturn(DEVICE_NAME);
        when(mCachedDevice.getDevice()).thenReturn(mDevice);
        when(mCachedDeviceManager.getCachedDevicesCopy()).thenReturn(
                ImmutableList.of(mCachedDevice));

        FeatureFlagUtils.setEnabled(mContext, FeatureFlagUtils.SETTINGS_SHOW_STYLUS_PREFERENCES,
                true);
        when(mPreferenceScreen.getContext()).thenReturn(mContext);
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

        verify(mExternalDisplayUpdater).registerCallback();
        verify(mConnectedBluetoothDeviceUpdater).registerCallback();
        verify(mConnectedUsbDeviceUpdater).registerCallback();
        verify(mConnectedDockUpdater).registerCallback();
        verify(mConnectedBluetoothDeviceUpdater).refreshPreference();
        verify(mStylusDeviceUpdater).registerCallback();
    }

    @Test
    public void onStop_shouldUnregisterUpdaters() {
        // unregister the callback in onStop()
        mConnectedDeviceGroupController.onStop();
        verify(mExternalDisplayUpdater).unregisterCallback();
        verify(mConnectedBluetoothDeviceUpdater).unregisterCallback();
        verify(mConnectedUsbDeviceUpdater).unregisterCallback();
        verify(mConnectedDockUpdater).unregisterCallback();
        verify(mStylusDeviceUpdater).unregisterCallback();
    }

    @Test
    public void getAvailabilityStatus_noBluetoothUsbDockFeature_returnUnSupported() {
        mFakeFeatureFlags.setFlag(FLAG_ROTATION_CONNECTED_DISPLAY_SETTING, false);
        mFakeFeatureFlags.setFlag(FLAG_RESOLUTION_AND_ENABLE_CONNECTED_DISPLAY_SETTING, false);
        mPackageManager.setSystemFeature(PackageManager.FEATURE_BLUETOOTH, false);
        mPackageManager.setSystemFeature(PackageManager.FEATURE_USB_ACCESSORY, false);
        mPackageManager.setSystemFeature(PackageManager.FEATURE_USB_HOST, false);
        mConnectedDeviceGroupController.init(null, mConnectedBluetoothDeviceUpdater,
                mConnectedUsbDeviceUpdater, null, null);

        assertThat(mConnectedDeviceGroupController.getAvailabilityStatus()).isEqualTo(
                UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_connectedDisplay_returnSupported() {
        mPackageManager.setSystemFeature(PackageManager.FEATURE_BLUETOOTH, false);
        mPackageManager.setSystemFeature(PackageManager.FEATURE_USB_ACCESSORY, false);
        mPackageManager.setSystemFeature(PackageManager.FEATURE_USB_HOST, false);
        mConnectedDeviceGroupController.init(null, mConnectedBluetoothDeviceUpdater,
                mConnectedUsbDeviceUpdater, null, null);

        assertThat(mConnectedDeviceGroupController.getAvailabilityStatus()).isEqualTo(
                AVAILABLE_UNSEARCHABLE);
    }

    @Test
    public void getAvailabilityStatus_BluetoothFeature_returnSupported() {
        mPackageManager.setSystemFeature(PackageManager.FEATURE_BLUETOOTH, true);
        mPackageManager.setSystemFeature(PackageManager.FEATURE_USB_ACCESSORY, false);
        mPackageManager.setSystemFeature(PackageManager.FEATURE_USB_HOST, false);
        mConnectedDeviceGroupController.init(null, mConnectedBluetoothDeviceUpdater,
                mConnectedUsbDeviceUpdater, null, null);

        assertThat(mConnectedDeviceGroupController.getAvailabilityStatus()).isEqualTo(
                AVAILABLE_UNSEARCHABLE);
    }

    @Test
    public void getAvailabilityStatus_haveUsbFeature_returnSupported() {
        mPackageManager.setSystemFeature(PackageManager.FEATURE_BLUETOOTH, false);
        mPackageManager.setSystemFeature(PackageManager.FEATURE_USB_ACCESSORY, false);
        mPackageManager.setSystemFeature(PackageManager.FEATURE_USB_HOST, true);
        mConnectedDeviceGroupController.init(null, mConnectedBluetoothDeviceUpdater,
                mConnectedUsbDeviceUpdater, null, null);

        assertThat(mConnectedDeviceGroupController.getAvailabilityStatus()).isEqualTo(
                AVAILABLE_UNSEARCHABLE);
    }

    @Test
    public void getAvailabilityStatus_haveDockFeature_returnSupported() {
        mPackageManager.setSystemFeature(PackageManager.FEATURE_BLUETOOTH, false);
        mPackageManager.setSystemFeature(PackageManager.FEATURE_USB_ACCESSORY, false);
        mPackageManager.setSystemFeature(PackageManager.FEATURE_USB_HOST, false);
        mConnectedDeviceGroupController.init(null, mConnectedBluetoothDeviceUpdater,
                mConnectedUsbDeviceUpdater, mConnectedDockUpdater, null);

        assertThat(mConnectedDeviceGroupController.getAvailabilityStatus()).isEqualTo(
                AVAILABLE_UNSEARCHABLE);
    }


    @Test
    public void getAvailabilityStatus_noUsiStylusFeature_returnUnSupported() {
        mFakeFeatureFlags.setFlag(FLAG_ROTATION_CONNECTED_DISPLAY_SETTING, false);
        mFakeFeatureFlags.setFlag(FLAG_RESOLUTION_AND_ENABLE_CONNECTED_DISPLAY_SETTING, false);
        mPackageManager.setSystemFeature(PackageManager.FEATURE_BLUETOOTH, false);
        mPackageManager.setSystemFeature(PackageManager.FEATURE_USB_ACCESSORY, false);
        mPackageManager.setSystemFeature(PackageManager.FEATURE_USB_HOST, false);
        when(mInputManager.getInputDeviceIds()).thenReturn(new int[]{0});
        when(mInputManager.getInputDevice(0)).thenReturn(new InputDevice.Builder().setSources(
                InputDevice.SOURCE_DPAD).setExternal(false).build());

        mConnectedDeviceGroupController.init(null, mConnectedBluetoothDeviceUpdater,
                mConnectedUsbDeviceUpdater, null, mStylusDeviceUpdater);

        assertThat(mConnectedDeviceGroupController.getAvailabilityStatus()).isEqualTo(
                UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_haveUsiStylusFeature_returnSupported() {
        mPackageManager.setSystemFeature(PackageManager.FEATURE_BLUETOOTH, false);
        mPackageManager.setSystemFeature(PackageManager.FEATURE_USB_ACCESSORY, false);
        mPackageManager.setSystemFeature(PackageManager.FEATURE_USB_HOST, false);
        when(mInputManager.getInputDeviceIds()).thenReturn(new int[]{0});
        when(mInputManager.getInputDevice(0)).thenReturn(new InputDevice.Builder().setSources(
                InputDevice.SOURCE_STYLUS).setExternal(false).build());

        mConnectedDeviceGroupController.init(null, mConnectedBluetoothDeviceUpdater,
                mConnectedUsbDeviceUpdater, mConnectedDockUpdater, mStylusDeviceUpdater);

        assertThat(mConnectedDeviceGroupController.getAvailabilityStatus()).isEqualTo(
                AVAILABLE_UNSEARCHABLE);
    }

    @Test
    public void init_noBluetoothAndUsbFeature_doesNotCrash() {
        DashboardFragment fragment = mock(DashboardFragment.class);
        when(fragment.getContext()).thenReturn(mContext);
        when(mPreferenceScreen.findPreference(anyString())).thenReturn(mPreferenceGroup);
        mPackageManager.setSystemFeature(PackageManager.FEATURE_BLUETOOTH, false);
        mPackageManager.setSystemFeature(PackageManager.FEATURE_USB_ACCESSORY, false);
        mPackageManager.setSystemFeature(PackageManager.FEATURE_USB_HOST, false);

        mConnectedDeviceGroupController.init(fragment);
        mConnectedDeviceGroupController.displayPreference(mPreferenceScreen);
        mConnectedDeviceGroupController.onStart();
        mConnectedDeviceGroupController.onStop();
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_BONDED_BLUETOOTH_DEVICE_SEARCHABLE)
    public void updateDynamicRawDataToIndex_deviceNotBonded_deviceIsNotSearchable() {
        when(mDevice.getBondState()).thenReturn(BluetoothDevice.BOND_NONE);
        List<SearchIndexableRaw> searchData = new ArrayList<>();

        mConnectedDeviceGroupController.updateDynamicRawDataToIndex(searchData);

        assertThat(searchData).isEmpty();
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_BONDED_BLUETOOTH_DEVICE_SEARCHABLE)
    public void updateDynamicRawDataToIndex_deviceBonded_deviceIsSearchable() {
        when(mDevice.getBondState()).thenReturn(BluetoothDevice.BOND_BONDED);
        List<SearchIndexableRaw> searchData = new ArrayList<>();

        mConnectedDeviceGroupController.updateDynamicRawDataToIndex(searchData);

        assertThat(searchData).isNotEmpty();
        assertThat(searchData.get(0).key).contains(DEVICE_NAME);
    }
}
