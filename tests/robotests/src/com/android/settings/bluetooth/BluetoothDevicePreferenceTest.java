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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.UserManager;
import android.view.ContextThemeWrapper;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowAlertDialogCompat.class})
public class BluetoothDevicePreferenceTest {
    private static final boolean SHOW_DEVICES_WITHOUT_NAMES = true;
    private static final String MAC_ADDRESS = "04:52:C7:0B:D8:3C";
    private static final String MAC_ADDRESS_2 = "05:52:C7:0B:D8:3C";
    private static final String MAC_ADDRESS_3 = "06:52:C7:0B:D8:3C";
    private static final String MAC_ADDRESS_4 = "07:52:C7:0B:D8:3C";
    private static final Comparator<BluetoothDevicePreference> COMPARATOR =
            Comparator.naturalOrder();

    private Context mContext;
    @Mock
    private CachedBluetoothDevice mCachedBluetoothDevice;
    @Mock
    private CachedBluetoothDevice mCachedDevice1;
    @Mock
    private CachedBluetoothDevice mCachedDevice2;
    @Mock
    private CachedBluetoothDevice mCachedDevice3;

    private FakeFeatureFactory mFakeFeatureFactory;
    private MetricsFeatureProvider mMetricsFeatureProvider;
    private BluetoothDevicePreference mPreference;
    private List<BluetoothDevicePreference> mPreferenceList = new ArrayList<>();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Context context = spy(RuntimeEnvironment.application.getApplicationContext());
        mContext = new ContextThemeWrapper(context, R.style.Theme_Settings);
        mFakeFeatureFactory = FakeFeatureFactory.setupForTest();
        mMetricsFeatureProvider = mFakeFeatureFactory.getMetricsFeatureProvider();
        when(mCachedBluetoothDevice.getAddress()).thenReturn(MAC_ADDRESS);
        when(mCachedDevice1.getAddress()).thenReturn(MAC_ADDRESS_2);
        when(mCachedDevice2.getAddress()).thenReturn(MAC_ADDRESS_3);
        when(mCachedDevice3.getAddress()).thenReturn(MAC_ADDRESS_4);
        mPreference = new BluetoothDevicePreference(mContext, mCachedBluetoothDevice,
                SHOW_DEVICES_WITHOUT_NAMES, BluetoothDevicePreference.SortType.TYPE_DEFAULT);
    }

    @Test
    public void onClicked_deviceConnected_shouldLogBluetoothDisconnectEvent() {
        when(mCachedBluetoothDevice.isConnected()).thenReturn(true);

        mPreference.onClicked();

        verify(mMetricsFeatureProvider)
                .action(mContext, MetricsEvent.ACTION_SETTINGS_BLUETOOTH_DISCONNECT);
    }

    @Test
    public void onClicked_deviceBonded_shouldLogBluetoothConnectEvent() {
        when(mCachedBluetoothDevice.isConnected()).thenReturn(false);
        when(mCachedBluetoothDevice.getBondState()).thenReturn(BluetoothDevice.BOND_BONDED);

        mPreference.onClicked();

        verify(mMetricsFeatureProvider)
                .action(mContext, MetricsEvent.ACTION_SETTINGS_BLUETOOTH_CONNECT);
    }

    @Test
    public void onClicked_deviceNotBonded_shouldLogBluetoothPairEvent() {
        when(mCachedBluetoothDevice.isConnected()).thenReturn(false);
        when(mCachedBluetoothDevice.getBondState()).thenReturn(BluetoothDevice.BOND_NONE);
        when(mCachedBluetoothDevice.startPairing()).thenReturn(true);
        when(mCachedBluetoothDevice.hasHumanReadableName()).thenReturn(true);

        mPreference.onClicked();

        verify(mMetricsFeatureProvider)
                .action(mContext, MetricsEvent.ACTION_SETTINGS_BLUETOOTH_PAIR);
        verify(mMetricsFeatureProvider, never())
                .action(mContext,
                        MetricsEvent.ACTION_SETTINGS_BLUETOOTH_PAIR_DEVICES_WITHOUT_NAMES);
    }

    @Test
    public void onClicked_deviceNotBonded_shouldLogBluetoothPairEventAndPairWithoutNameEvent() {
        when(mCachedBluetoothDevice.isConnected()).thenReturn(false);
        when(mCachedBluetoothDevice.getBondState()).thenReturn(BluetoothDevice.BOND_NONE);
        when(mCachedBluetoothDevice.startPairing()).thenReturn(true);
        when(mCachedBluetoothDevice.hasHumanReadableName()).thenReturn(false);

        mPreference.onClicked();

        verify(mMetricsFeatureProvider)
                .action(mContext, MetricsEvent.ACTION_SETTINGS_BLUETOOTH_PAIR);
        verify(mMetricsFeatureProvider)
                .action(mContext,
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
        when(um.hasUserRestriction(UserManager.DISALLOW_CONFIG_BLUETOOTH)).thenReturn(true);

        assertThat(mPreference.shouldHideSecondTarget()).isTrue();
    }

    @Test
    public void shouldHideSecondTarget_hasBoundDeviceAndNoRestriction_shouldReturnFalse() {
        when(mCachedBluetoothDevice.getBondState()).thenReturn(BluetoothDevice.BOND_BONDED);
        final UserManager um = mock(UserManager.class);
        ReflectionHelpers.setField(mPreference, "mUserManager", um);
        when(um.hasUserRestriction(UserManager.DISALLOW_CONFIG_BLUETOOTH)).thenReturn(false);

        assertThat(mPreference.shouldHideSecondTarget()).isFalse();
    }

    @Test
    public void isVisible_showDeviceWithoutNames_visible() {
        doReturn(false).when(mCachedBluetoothDevice).hasHumanReadableName();
        BluetoothDevicePreference preference =
                new BluetoothDevicePreference(mContext, mCachedBluetoothDevice,
                        SHOW_DEVICES_WITHOUT_NAMES,
                        BluetoothDevicePreference.SortType.TYPE_DEFAULT);

        assertThat(preference.isVisible()).isTrue();
    }

    @Test
    public void isVisible_hideDeviceWithoutNames_invisible() {
        doReturn(false).when(mCachedBluetoothDevice).hasHumanReadableName();
        BluetoothDevicePreference preference =
                new BluetoothDevicePreference(mContext, mCachedBluetoothDevice,
                        false, BluetoothDevicePreference.SortType.TYPE_DEFAULT);

        assertThat(preference.isVisible()).isFalse();
    }

    @Test
    public void setNeedNotifyHierarchyChanged_updateValue() {
        mPreference.setNeedNotifyHierarchyChanged(true);

        assertThat(mPreference.mNeedNotifyHierarchyChanged).isTrue();
    }

    @Test
    public void compareTo_sortTypeFIFO() {
        final BluetoothDevicePreference preference3 = new BluetoothDevicePreference(mContext,
                mCachedDevice3, SHOW_DEVICES_WITHOUT_NAMES,
                BluetoothDevicePreference.SortType.TYPE_FIFO);
        final BluetoothDevicePreference preference2 = new BluetoothDevicePreference(mContext,
                mCachedDevice2, SHOW_DEVICES_WITHOUT_NAMES,
                BluetoothDevicePreference.SortType.TYPE_FIFO);
        final BluetoothDevicePreference preference1 = new BluetoothDevicePreference(mContext,
                mCachedDevice1, SHOW_DEVICES_WITHOUT_NAMES,
                BluetoothDevicePreference.SortType.TYPE_FIFO);

        mPreferenceList.add(preference1);
        mPreferenceList.add(preference2);
        mPreferenceList.add(preference3);
        Collections.sort(mPreferenceList, COMPARATOR);

        assertThat(mPreferenceList.get(0).getCachedDevice().getAddress())
                .isEqualTo(preference3.getCachedDevice().getAddress());
        assertThat(mPreferenceList.get(1).getCachedDevice().getAddress())
                .isEqualTo(preference2.getCachedDevice().getAddress());
        assertThat(mPreferenceList.get(2).getCachedDevice().getAddress())
                .isEqualTo(preference1.getCachedDevice().getAddress());
    }

    @Test
    public void compareTo_sortTypeDefault() {
        final BluetoothDevicePreference preference3 = new BluetoothDevicePreference(mContext,
                mCachedDevice3, SHOW_DEVICES_WITHOUT_NAMES,
                BluetoothDevicePreference.SortType.TYPE_DEFAULT);
        final BluetoothDevicePreference preference2 = new BluetoothDevicePreference(mContext,
                mCachedDevice2, SHOW_DEVICES_WITHOUT_NAMES,
                BluetoothDevicePreference.SortType.TYPE_DEFAULT);
        final BluetoothDevicePreference preference1 = new BluetoothDevicePreference(mContext,
                mCachedDevice1, SHOW_DEVICES_WITHOUT_NAMES,
                BluetoothDevicePreference.SortType.TYPE_DEFAULT);

        mPreferenceList.add(preference1);
        mPreferenceList.add(preference2);
        mPreferenceList.add(preference3);
        Collections.sort(mPreferenceList, COMPARATOR);

        assertThat(mPreferenceList.get(0).getCachedDevice().getAddress())
                .isEqualTo(preference1.getCachedDevice().getAddress());
        assertThat(mPreferenceList.get(1).getCachedDevice().getAddress())
                .isEqualTo(preference2.getCachedDevice().getAddress());
        assertThat(mPreferenceList.get(2).getCachedDevice().getAddress())
                .isEqualTo(preference3.getCachedDevice().getAddress());
    }

    @Test
    public void onAttached_callbackNotRemoved_doNotRegisterCallback() {
        mPreference.onAttached();

        verify(mCachedBluetoothDevice, never()).unregisterCallback(any());
    }

    @Test
    public void onAttached_callbackRemoved_registerCallback() {
        mPreference.onPrepareForRemoval();
        mPreference.onAttached();

        verify(mCachedBluetoothDevice, times(2)).registerCallback(any());
    }
}
