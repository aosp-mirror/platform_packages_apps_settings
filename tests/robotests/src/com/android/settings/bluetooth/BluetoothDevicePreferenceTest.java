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

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.UserManager;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION,
        shadows = SettingsShadowResources.class)
public class BluetoothDevicePreferenceTest {

    private Context mContext;
    @Mock
    private CachedBluetoothDevice mCachedBluetoothDevice;
    @Mock
    private DeviceListPreferenceFragment mDeviceListPreferenceFragment;

    private FakeFeatureFactory mFakeFeatureFactory;
    private MetricsFeatureProvider mMetricsFeatureProvider;
    private BluetoothDevicePreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application.getApplicationContext());
        FakeFeatureFactory.setupForTest(mContext);
        mFakeFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);
        mMetricsFeatureProvider = mFakeFeatureFactory.getMetricsFeatureProvider();
        mPreference = new BluetoothDevicePreference(mContext, mCachedBluetoothDevice,
                mDeviceListPreferenceFragment);
    }

    @Test
    public void onClicked_deviceConnected_shouldLogBluetoothDisconnectEvent() {
        when(mCachedBluetoothDevice.isConnected()).thenReturn(true);

        mPreference.onClicked();

        verify(mMetricsFeatureProvider).action(
                mContext, MetricsEvent.ACTION_SETTINGS_BLUETOOTH_DISCONNECT);
    }

    @Test
    public void onClicked_deviceBonded_shouldLogBluetoothConnectEvent() {
        when(mCachedBluetoothDevice.isConnected()).thenReturn(false);
        when(mCachedBluetoothDevice.getBondState()).thenReturn(BluetoothDevice.BOND_BONDED);

        mPreference.onClicked();

        verify(mMetricsFeatureProvider).action(
                mContext, MetricsEvent.ACTION_SETTINGS_BLUETOOTH_CONNECT);
    }

    @Test
    public void onClicked_deviceNotBonded_shouldLogBluetoothPairEvent() {
        when(mCachedBluetoothDevice.isConnected()).thenReturn(false);
        when(mCachedBluetoothDevice.getBondState()).thenReturn(BluetoothDevice.BOND_NONE);
        when(mCachedBluetoothDevice.startPairing()).thenReturn(true);
        when(mCachedBluetoothDevice.hasHumanReadableName()).thenReturn(true);

        mPreference.onClicked();

        verify(mMetricsFeatureProvider).action(
                mContext, MetricsEvent.ACTION_SETTINGS_BLUETOOTH_PAIR);
        verify(mMetricsFeatureProvider, never()).action(mContext,
                MetricsEvent.ACTION_SETTINGS_BLUETOOTH_PAIR_DEVICES_WITHOUT_NAMES);
    }

    @Test
    public void onClicked_deviceNotBonded_shouldLogBluetoothPairEventAndPairWithoutNameEvent() {
        when(mCachedBluetoothDevice.isConnected()).thenReturn(false);
        when(mCachedBluetoothDevice.getBondState()).thenReturn(BluetoothDevice.BOND_NONE);
        when(mCachedBluetoothDevice.startPairing()).thenReturn(true);
        when(mCachedBluetoothDevice.hasHumanReadableName()).thenReturn(false);

        mPreference.onClicked();

        verify(mMetricsFeatureProvider).action(
                mContext, MetricsEvent.ACTION_SETTINGS_BLUETOOTH_PAIR);
        verify(mMetricsFeatureProvider).action(mContext,
                MetricsEvent.ACTION_SETTINGS_BLUETOOTH_PAIR_DEVICES_WITHOUT_NAMES);
    }

    @Test
    public void getSecondTargetResource_shouldBeGearIconLayout() {
        assertThat(mPreference.getSecondTargetResId()).isEqualTo(R.layout.preference_widget_gear);
    }

    @Test
    public void shouldHideSecondTarget_noDevice_shouldReturnTrue() {
        ReflectionHelpers.setField(mPreference, "mCachedDevice", null);

        assertThat(mPreference.shouldHideSecondTarget()).isTrue();
    }

    @Test
    public void shouldHideSecondTarget_notBond_shouldReturnTrue() {
        when(mCachedBluetoothDevice.getBondState()).thenReturn(BluetoothDevice.BOND_NONE);

        assertThat(mPreference.shouldHideSecondTarget()).isTrue();
    }

    @Test
    public void shouldHideSecondTarget_hasUserRestriction_shouldReturnTrue() {
        final UserManager um = mock(UserManager.class);
        ReflectionHelpers.setField(mPreference, "mUserManager", um);
        when(um.hasUserRestriction(UserManager.DISALLOW_CONFIG_BLUETOOTH))
                .thenReturn(true);

        assertThat(mPreference.shouldHideSecondTarget()).isTrue();
    }

    @Test
    public void shouldHideSecondTarget_hasBoundDeviceAndNoRestriction_shouldReturnFalse() {
        when(mCachedBluetoothDevice.getBondState()).thenReturn(BluetoothDevice.BOND_BONDED);
        final UserManager um = mock(UserManager.class);
        ReflectionHelpers.setField(mPreference, "mUserManager", um);
        when(um.hasUserRestriction(UserManager.DISALLOW_CONFIG_BLUETOOTH))
                .thenReturn(false);

        assertThat(mPreference.shouldHideSecondTarget()).isFalse();
    }

    @Test
    public void imagingDeviceIcon_isICSettingsPrint() {
        when(mCachedBluetoothDevice.getBatteryLevel()).thenReturn(
                BluetoothDevice.BATTERY_LEVEL_UNKNOWN);
        when(mCachedBluetoothDevice.getBtClass()).thenReturn(
                new BluetoothClass(BluetoothClass.Device.Major.IMAGING));

        mPreference.onDeviceAttributesChanged();
        assertThat(mPreference.getIcon()).isEqualTo(
                mContext.getDrawable(R.drawable.ic_settings_print));
    }

    @Test
    public void testVisible_notVisibleThenVisible() {
        when(mDeviceListPreferenceFragment.shouldShowDevicesWithoutNames()).thenReturn(false);
        final boolean[] humanReadableName = {false};
        doAnswer(invocation -> humanReadableName[0]).when(mCachedBluetoothDevice)
                .hasHumanReadableName();
        BluetoothDevicePreference preference =
                new BluetoothDevicePreference(mContext, mCachedBluetoothDevice,
                        mDeviceListPreferenceFragment);
        assertThat(preference.isVisible()).isFalse();
        humanReadableName[0] = true;
        preference.onDeviceAttributesChanged();
        assertThat(preference.isVisible()).isTrue();
    }

    @Test
    public void testVisible_visibleThenNotVisible() {
        when(mDeviceListPreferenceFragment.shouldShowDevicesWithoutNames()).thenReturn(false);
        final boolean[] humanReadableName = {true};
        doAnswer(invocation -> humanReadableName[0]).when(mCachedBluetoothDevice)
                .hasHumanReadableName();
        BluetoothDevicePreference preference =
                new BluetoothDevicePreference(mContext, mCachedBluetoothDevice,
                        mDeviceListPreferenceFragment);
        assertThat(preference.isVisible()).isTrue();
        humanReadableName[0] = false;
        preference.onDeviceAttributesChanged();
        assertThat(preference.isVisible()).isFalse();
    }

    @Test
    public void testVisible_alwaysVisibleWhenEnabled() {
        when(mDeviceListPreferenceFragment.shouldShowDevicesWithoutNames()).thenReturn(true);
        final boolean[] humanReadableName = {true};
        doAnswer(invocation -> humanReadableName[0]).when(mCachedBluetoothDevice)
                .hasHumanReadableName();
        BluetoothDevicePreference preference =
                new BluetoothDevicePreference(mContext, mCachedBluetoothDevice,
                        mDeviceListPreferenceFragment);
        assertThat(preference.isVisible()).isTrue();
        humanReadableName[0] = false;
        preference.onDeviceAttributesChanged();
        assertThat(preference.isVisible()).isTrue();
    }
}
