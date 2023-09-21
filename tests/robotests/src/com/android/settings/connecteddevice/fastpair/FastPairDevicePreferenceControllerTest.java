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
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;

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
public class FastPairDevicePreferenceControllerTest {

    private static final String KEY = "test_key";

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Mock private DashboardFragment mDashboardFragment;
    @Mock private FastPairDeviceUpdater mFastPairDeviceUpdater;
    @Mock private PackageManager mPackageManager;
    @Mock private PreferenceManager mPreferenceManager;
    private Context mContext;
    private FastPairDevicePreferenceController mFastPairDevicePrefController;
    private PreferenceGroup mPreferenceGroup;
    private Preference mSeeAllPreference;
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;
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
        mFastPairDevicePrefController = new FastPairDevicePreferenceController(mContext, KEY);
        mShadowBluetoothAdapter = Shadow.extract(BluetoothAdapter.getDefaultAdapter());

        mPreferenceGroup = spy(new PreferenceCategory(mContext));
        doReturn(mPreferenceManager).when(mPreferenceGroup).getPreferenceManager();
        mSeeAllPreference = spy(new Preference(mContext));
        mPreferenceGroup.setVisible(false);
        mSeeAllPreference.setVisible(false);
        mFastPairDevicePrefController.setPreferenceGroup(mPreferenceGroup);
        mFastPairDevicePrefController.mSeeAllPreference = mSeeAllPreference;
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SUBSEQUENT_PAIR_SETTINGS_INTEGRATION)
    public void onStart_registerCallback() {
        // register the callback in onStart()
        mFastPairDevicePrefController.onStart(mLifecycleOwner);
        verify(mFastPairDeviceUpdater).registerCallback();
        verify(mContext)
                .registerReceiver(
                        mFastPairDevicePrefController.mReceiver,
                        mFastPairDevicePrefController.mIntentFilter,
                        Context.RECEIVER_EXPORTED_UNAUDITED);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SUBSEQUENT_PAIR_SETTINGS_INTEGRATION)
    public void onStop_unregisterCallback() {
        // register broadcast first
        mContext.registerReceiver(
                mFastPairDevicePrefController.mReceiver, null, Context.RECEIVER_EXPORTED_UNAUDITED);

        // unregister the callback in onStop()
        mFastPairDevicePrefController.onStop(mLifecycleOwner);
        verify(mFastPairDeviceUpdater).unregisterCallback();
        verify(mContext).unregisterReceiver(mFastPairDevicePrefController.mReceiver);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SUBSEQUENT_PAIR_SETTINGS_INTEGRATION)
    public void getAvailabilityStatus_noBluetoothFeature_returnUnsupported() {
        doReturn(false).when(mPackageManager).hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);

        assertThat(mFastPairDevicePrefController.getAvailabilityStatus())
                .isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_SUBSEQUENT_PAIR_SETTINGS_INTEGRATION)
    public void getAvailabilityStatus_noFastPairFeature_returnUnsupported() {
        doReturn(true).when(mPackageManager).hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);

        assertThat(mFastPairDevicePrefController.getAvailabilityStatus())
                .isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SUBSEQUENT_PAIR_SETTINGS_INTEGRATION)
    public void getAvailabilityStatus_bothBluetoothFastPairFeature_returnSupported() {
        doReturn(true).when(mPackageManager).hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);

        assertThat(mFastPairDevicePrefController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SUBSEQUENT_PAIR_SETTINGS_INTEGRATION)
    public void onDeviceAdded_addThreeFastPairDevicePreference_displayThreeNoSeeAll() {
        mShadowBluetoothAdapter.setEnabled(true);
        final GearPreference preference1 = new GearPreference(mContext, null /* AttributeSet */);
        final GearPreference preference2 = new GearPreference(mContext, null /* AttributeSet */);
        final GearPreference preference3 = new GearPreference(mContext, null /* AttributeSet */);

        mFastPairDevicePrefController.onDeviceAdded(preference1);
        mFastPairDevicePrefController.onDeviceAdded(preference2);
        mFastPairDevicePrefController.onDeviceAdded(preference3);

        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(3);
        assertThat(mPreferenceGroup.isVisible()).isTrue();
        assertThat(mSeeAllPreference.isVisible()).isFalse();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SUBSEQUENT_PAIR_SETTINGS_INTEGRATION)
    public void onDeviceAdded_addFourDevicePreference_onlyDisplayThreeWithSeeAll() {
        mShadowBluetoothAdapter.setEnabled(true);
        final GearPreference preference1 = new GearPreference(mContext, null /* AttributeSet */);
        preference1.setOrder(4);
        final GearPreference preference2 = new GearPreference(mContext, null /* AttributeSet */);
        preference2.setOrder(3);
        final GearPreference preference3 = new GearPreference(mContext, null /* AttributeSet */);
        preference3.setOrder(1);
        final GearPreference preference4 = new GearPreference(mContext, null /* AttributeSet */);
        preference4.setOrder(2);
        final GearPreference preference5 = new GearPreference(mContext, null /* AttributeSet */);
        preference5.setOrder(5);

        mFastPairDevicePrefController.onDeviceAdded(preference1);
        mFastPairDevicePrefController.onDeviceAdded(preference2);
        mFastPairDevicePrefController.onDeviceAdded(preference3);
        mFastPairDevicePrefController.onDeviceAdded(preference4);
        mFastPairDevicePrefController.onDeviceAdded(preference5);

        // 3 GearPreference and 1 see all preference
        assertThat(mPreferenceGroup.getPreference(0)).isEqualTo(preference3);
        assertThat(mPreferenceGroup.getPreference(1)).isEqualTo(preference4);
        assertThat(mPreferenceGroup.getPreference(2)).isEqualTo(preference2);
        assertThat(mPreferenceGroup.isVisible()).isTrue();
        assertThat(mSeeAllPreference.isVisible()).isTrue();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SUBSEQUENT_PAIR_SETTINGS_INTEGRATION)
    public void onDeviceRemoved_removeFourthDevice_hideSeeAll() {
        mShadowBluetoothAdapter.setEnabled(true);
        final GearPreference preference1 = new GearPreference(mContext, null /* AttributeSet */);
        preference1.setOrder(1);
        final GearPreference preference2 = new GearPreference(mContext, null /* AttributeSet */);
        preference2.setOrder(2);
        final GearPreference preference3 = new GearPreference(mContext, null /* AttributeSet */);
        preference3.setOrder(3);
        final GearPreference preference4 = new GearPreference(mContext, null /* AttributeSet */);
        preference4.setOrder(4);
        final GearPreference preference5 = new GearPreference(mContext, null /* AttributeSet */);
        preference5.setOrder(5);

        mFastPairDevicePrefController.onDeviceAdded(preference1);
        mFastPairDevicePrefController.onDeviceAdded(preference2);
        mFastPairDevicePrefController.onDeviceAdded(preference3);
        mFastPairDevicePrefController.onDeviceAdded(preference4);
        mFastPairDevicePrefController.onDeviceAdded(preference5);

        mFastPairDevicePrefController.onDeviceRemoved(preference4);
        mFastPairDevicePrefController.onDeviceRemoved(preference2);

        // 3 GearPreference and no see all preference
        assertThat(mPreferenceGroup.getPreference(0)).isEqualTo(preference1);
        assertThat(mPreferenceGroup.getPreference(1)).isEqualTo(preference3);
        assertThat(mPreferenceGroup.getPreference(2)).isEqualTo(preference5);
        assertThat(mPreferenceGroup.isVisible()).isTrue();
        assertThat(mSeeAllPreference.isVisible()).isFalse();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SUBSEQUENT_PAIR_SETTINGS_INTEGRATION)
    public void updatePreferenceVisibility_bluetoothIsDisable_shouldHidePreference() {
        mShadowBluetoothAdapter.setEnabled(true);
        final GearPreference preference1 = new GearPreference(mContext, null /* AttributeSet */);
        mFastPairDevicePrefController.onDeviceAdded(preference1);
        assertThat(mPreferenceGroup.isVisible()).isTrue();

        mShadowBluetoothAdapter.setEnabled(false);
        // register broadcast first
        mContext.registerReceiver(
                mFastPairDevicePrefController.mReceiver,
                mFastPairDevicePrefController.mIntentFilter,
                Context.RECEIVER_EXPORTED_UNAUDITED);
        Intent intent = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.sendBroadcast(intent);

        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mPreferenceGroup.isVisible()).isFalse();
    }
}
