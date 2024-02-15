/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.connecteddevice.fastpair;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Looper;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.flags.Flags;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.widget.GearPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowBluetoothAdapter.class)
public class FastPairDeviceGroupControllerTest {

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    private static final String KEY = "fast_pair_device_list";

    @Mock private DashboardFragment mDashboardFragment;
    @Mock private FastPairDeviceUpdater mFastPairDeviceUpdater;
    @Mock private PackageManager mPackageManager;
    @Mock private PreferenceManager mPreferenceManager;
    @Mock private PreferenceScreen mScreen;
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;
    private Context mContext;
    private FastPairDeviceGroupController mFastPairDeviceGroupController;
    private PreferenceGroup mPreferenceGroup;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        doReturn(mContext).when(mDashboardFragment).getContext();
        doReturn(mPackageManager).when(mContext).getPackageManager();
        FastPairFeatureProvider provider =
                FakeFeatureFactory.setupForTest().getFastPairFeatureProvider();
        doReturn(mFastPairDeviceUpdater).when(provider).getFastPairDeviceUpdater(any(), any());
        mFastPairDeviceGroupController = new FastPairDeviceGroupController(mContext);
        mPreferenceGroup = spy(new PreferenceCategory(mContext));
        doReturn(mPreferenceManager).when(mPreferenceGroup).getPreferenceManager();
        mPreferenceGroup.setVisible(false);
        mFastPairDeviceGroupController.setPreferenceGroup(mPreferenceGroup);
        when(mScreen.findPreference(KEY)).thenReturn(mPreferenceGroup);
        when(mScreen.getContext()).thenReturn(mContext);
        mShadowBluetoothAdapter = Shadow.extract(BluetoothAdapter.getDefaultAdapter());
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SUBSEQUENT_PAIR_SETTINGS_INTEGRATION)
    public void onStart_flagOn_registerCallback() {
        // register the callback in onStart()
        mFastPairDeviceGroupController.onStart(mLifecycleOwner);
        verify(mFastPairDeviceUpdater).registerCallback();
        verify(mContext)
                .registerReceiver(
                        mFastPairDeviceGroupController.mReceiver,
                        mFastPairDeviceGroupController.mIntentFilter,
                        Context.RECEIVER_EXPORTED);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SUBSEQUENT_PAIR_SETTINGS_INTEGRATION)
    public void onStop_flagOn_unregisterCallback() {
        // register broadcast first
        mContext.registerReceiver(
                mFastPairDeviceGroupController.mReceiver, null, Context.RECEIVER_EXPORTED);

        // unregister the callback in onStop()
        mFastPairDeviceGroupController.onStop(mLifecycleOwner);
        verify(mFastPairDeviceUpdater).unregisterCallback();
        verify(mContext).unregisterReceiver(mFastPairDeviceGroupController.mReceiver);
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_SUBSEQUENT_PAIR_SETTINGS_INTEGRATION)
    public void onStart_flagOff_registerCallback() {
        // register the callback in onStart()
        mFastPairDeviceGroupController.onStart(mLifecycleOwner);
        assertThat(mFastPairDeviceUpdater).isNull();
        verify(mContext)
                .registerReceiver(
                        mFastPairDeviceGroupController.mReceiver,
                        mFastPairDeviceGroupController.mIntentFilter,
                        Context.RECEIVER_EXPORTED);
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_SUBSEQUENT_PAIR_SETTINGS_INTEGRATION)
    public void onStop_flagOff_unregisterCallback() {
        // register broadcast first
        mContext.registerReceiver(
                mFastPairDeviceGroupController.mReceiver, null, Context.RECEIVER_EXPORTED);

        // unregister the callback in onStop()
        mFastPairDeviceGroupController.onStop(mLifecycleOwner);
        assertThat(mFastPairDeviceUpdater).isNull();
        verify(mContext).unregisterReceiver(mFastPairDeviceGroupController.mReceiver);
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_SUBSEQUENT_PAIR_SETTINGS_INTEGRATION)
    public void getAvailabilityStatus_noFastPairFeature_returnUnSupported() {
        doReturn(true).when(mPackageManager).hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);

        assertThat(mFastPairDeviceGroupController.getAvailabilityStatus())
                .isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_SUBSEQUENT_PAIR_SETTINGS_INTEGRATION)
    public void getAvailabilityStatus_noBluetoothFastPairFeature_returnUnSupported() {
        doReturn(false).when(mPackageManager).hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);

        assertThat(mFastPairDeviceGroupController.getAvailabilityStatus())
                .isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SUBSEQUENT_PAIR_SETTINGS_INTEGRATION)
    public void getAvailabilityStatus_noBluetoothFeature_returnUnSupported() {
        doReturn(false).when(mPackageManager).hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);

        assertThat(mFastPairDeviceGroupController.getAvailabilityStatus())
                .isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SUBSEQUENT_PAIR_SETTINGS_INTEGRATION)
    public void getAvailabilityStatus_withBluetoothFastPairFeature_returnSupported() {
        doReturn(true).when(mPackageManager).hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);

        assertThat(mFastPairDeviceGroupController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void updatePreferenceVisibility_bluetoothIsDisable_shouldHidePreference() {
        mShadowBluetoothAdapter.setEnabled(true);
        final GearPreference preference1 = new GearPreference(mContext, null /* AttributeSet */);
        mFastPairDeviceGroupController.onDeviceAdded(preference1);
        assertThat(mPreferenceGroup.isVisible()).isTrue();

        mShadowBluetoothAdapter.setEnabled(false);
        // register broadcast first
        mContext.registerReceiver(
                mFastPairDeviceGroupController.mReceiver,
                mFastPairDeviceGroupController.mIntentFilter,
                Context.RECEIVER_EXPORTED_UNAUDITED);
        Intent intent = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.sendBroadcast(intent);

        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mPreferenceGroup.isVisible()).isFalse();
    }

    @Test
    public void onDeviceAdd_bluetoothIsDisable_shouldHidePreference() {
        mShadowBluetoothAdapter.setEnabled(true);
        final GearPreference preference1 = new GearPreference(mContext, null /* AttributeSet */);
        mFastPairDeviceGroupController.onDeviceAdded(preference1);
        assertThat(mPreferenceGroup.isVisible()).isTrue();
    }

    @Test
    public void onDeviceRemoved_bluetoothIsDisable_shouldHidePreference() {
        mShadowBluetoothAdapter.setEnabled(true);
        final GearPreference preference1 = new GearPreference(mContext, null /* AttributeSet */);
        mPreferenceGroup.addPreference(preference1);
        mFastPairDeviceGroupController.onDeviceRemoved(preference1);
        assertThat(mPreferenceGroup.isVisible()).isFalse();
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_SUBSEQUENT_PAIR_SETTINGS_INTEGRATION)
    public void displayPreference_notAvailable_doNothing() {
        mFastPairDeviceGroupController.displayPreference(mScreen);
        assertThat(mPreferenceGroup.isVisible()).isFalse();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SUBSEQUENT_PAIR_SETTINGS_INTEGRATION)
    public void displayPreference_isAvailable_fetchFastPairDevices() {
        doReturn(true).when(mPackageManager).hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);

        mFastPairDeviceGroupController.displayPreference(mScreen);
        verify(mFastPairDeviceUpdater).forceUpdate();
    }
}
