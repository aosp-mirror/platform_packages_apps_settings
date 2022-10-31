/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings;

import static android.content.Intent.ACTION_MEDIA_SHARED;
import static android.content.Intent.ACTION_MEDIA_UNSHARED;
import static android.hardware.usb.UsbManager.ACTION_USB_STATE;

import static com.android.settings.wifi.WifiUtils.setCanShowWifiHotspotCached;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothPan;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.net.TetheringManager;
import android.net.wifi.WifiManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.FeatureFlagUtils;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.core.FeatureFlags;
import com.android.settingslib.RestrictedSwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class TetherSettingsTest {

    private Context mContext;

    @Mock
    private ConnectivityManager mConnectivityManager;
    @Mock
    private UserManager mUserManager;
    @Mock
    private TetheringManager mTetheringManager;

    @Before
    public void setUp() throws Exception {
        mContext = spy(RuntimeEnvironment.application);

        MockitoAnnotations.initMocks(this);
        doReturn(mConnectivityManager)
                .when(mContext).getSystemService(Context.CONNECTIVITY_SERVICE);
        doReturn(mUserManager)
                .when(mContext).getSystemService(Context.USER_SERVICE);
        doReturn(mTetheringManager)
                .when(mContext).getSystemService(Context.TETHERING_SERVICE);
        doReturn(mContext).when(mContext).createPackageContextAsUser(
                any(String.class), anyInt(), any(UserHandle.class));

        setupIsTetherAvailable(true);
        setCanShowWifiHotspotCached(true);

        when(mTetheringManager.getTetherableUsbRegexs()).thenReturn(new String[0]);
        when(mTetheringManager.getTetherableBluetoothRegexs()).thenReturn(new String[0]);
    }

    @Test
    public void testTetherNonIndexableKeys_tetherAvailable_keysNotReturned() {
        FeatureFlagUtils.setEnabled(mContext, FeatureFlags.TETHER_ALL_IN_ONE, false);
        // To let TetherUtil.isTetherAvailable return true, select one of the combinations
        setupIsTetherAvailable(true);

        final List<String> niks =
                TetherSettings.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(mContext);

        assertThat(niks).doesNotContain(TetherSettings.KEY_TETHER_PREFS_SCREEN);
        assertThat(niks).doesNotContain(TetherSettings.KEY_WIFI_TETHER);
    }

    @Test
    public void testTetherNonIndexableKeys_tetherNotAvailable_keysReturned() {
        // To let TetherUtil.isTetherAvailable return false, select one of the combinations
        setupIsTetherAvailable(false);

        final List<String> niks =
                TetherSettings.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(mContext);

        assertThat(niks).contains(TetherSettings.KEY_TETHER_PREFS_SCREEN);
        assertThat(niks).contains(TetherSettings.KEY_WIFI_TETHER);
    }

    @Test
    public void getNonIndexableKeys_canNotShowWifiHotspot_containsWifiTether() {
        setCanShowWifiHotspotCached(false);
        setupIsTetherAvailable(true);

        List<String> keys = TetherSettings.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(mContext);

        assertThat(keys).contains(TetherSettings.KEY_WIFI_TETHER);
    }

    @Test
    public void testTetherNonIndexableKeys_usbNotAvailable_usbKeyReturned() {
        when(mTetheringManager.getTetherableUsbRegexs()).thenReturn(new String[0]);

        final List<String> niks =
            TetherSettings.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(mContext);

        assertThat(niks).contains(TetherSettings.KEY_USB_TETHER_SETTINGS);
    }

    @Test
    public void testTetherNonIndexableKeys_usbAvailable_usbKeyNotReturned() {
        FeatureFlagUtils.setEnabled(mContext, FeatureFlags.TETHER_ALL_IN_ONE, false);
        // We can ignore the condition of Utils.isMonkeyRunning()
        // In normal case, monkey and robotest should not execute at the same time
        when(mTetheringManager.getTetherableUsbRegexs()).thenReturn(new String[]{"fakeRegex"});

        final List<String> niks =
            TetherSettings.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(mContext);

        assertThat(niks).doesNotContain(TetherSettings.KEY_USB_TETHER_SETTINGS);
    }

    @Test
    public void testTetherNonIndexableKeys_bluetoothNotAvailable_bluetoothKeyReturned() {
        when(mTetheringManager.getTetherableBluetoothRegexs()).thenReturn(new String[0]);

        final List<String> niks =
            TetherSettings.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(mContext);

        assertThat(niks).contains(TetherSettings.KEY_ENABLE_BLUETOOTH_TETHERING);
    }

    @Test
    public void testTetherNonIndexableKeys_bluetoothAvailable_bluetoothKeyNotReturned() {
        FeatureFlagUtils.setEnabled(mContext, FeatureFlags.TETHER_ALL_IN_ONE, false);
        when(mTetheringManager.getTetherableBluetoothRegexs())
                .thenReturn(new String[]{"fakeRegex"});

        final List<String> niks =
            TetherSettings.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(mContext);

        assertThat(niks).doesNotContain(TetherSettings.KEY_ENABLE_BLUETOOTH_TETHERING);
    }

    @Test
    public void testSetFooterPreferenceTitle_isStaApConcurrencySupported_showStaApString() {
        final TetherSettings spyTetherSettings = spy(new TetherSettings());
        spyTetherSettings.mContext = mContext;
        final Preference mockPreference = mock(Preference.class);
        when(spyTetherSettings.findPreference(TetherSettings.KEY_TETHER_PREFS_TOP_INTRO))
            .thenReturn(mockPreference);
        final WifiManager mockWifiManager = mock(WifiManager.class);
        when(mContext.getSystemService(Context.WIFI_SERVICE)).thenReturn(mockWifiManager);
        when(mockWifiManager.isStaApConcurrencySupported()).thenReturn(true);

        spyTetherSettings.setTopIntroPreferenceTitle();

        verify(mockPreference, never()).setTitle(R.string.tethering_footer_info);
        verify(mockPreference).setTitle(R.string.tethering_footer_info_sta_ap_concurrency);
    }

    @Test
    public void testBluetoothState_updateBluetoothState_bluetoothTetheringStateOn() {
        final TetherSettings spyTetherSettings = spy(new TetherSettings());
        spyTetherSettings.mContext = mContext;
        spyTetherSettings.mTm = mTetheringManager;
        final SwitchPreference mockSwitchPreference = mock(SwitchPreference.class);
        when(spyTetherSettings.findPreference(TetherSettings.KEY_ENABLE_BLUETOOTH_TETHERING))
            .thenReturn(mockSwitchPreference);
        final FragmentActivity mockActivity = mock(FragmentActivity.class);
        when(spyTetherSettings.getActivity()).thenReturn(mockActivity);
        final ArgumentCaptor<BroadcastReceiver> captor =
                ArgumentCaptor.forClass(BroadcastReceiver.class);
        when(mockActivity.registerReceiver(captor.capture(), any(IntentFilter.class)))
            .thenReturn(null);
        // Bluetooth tethering state is on
        when(spyTetherSettings.getBluetoothState()).thenReturn(BluetoothAdapter.STATE_ON);
        when(spyTetherSettings.isBluetoothTetheringOn()).thenReturn(true);

        spyTetherSettings.setupTetherPreference();
        spyTetherSettings.registerReceiver();
        updateOnlyBluetoothState(spyTetherSettings);

        // Simulate Bluetooth tethering state changed
        final BroadcastReceiver receiver = captor.getValue();
        final Intent bluetoothTetheringOn = new Intent(BluetoothPan.ACTION_TETHERING_STATE_CHANGED);
        bluetoothTetheringOn.putExtra(BluetoothPan.EXTRA_TETHERING_STATE,
                BluetoothPan.TETHERING_STATE_ON);
        receiver.onReceive(mockActivity, bluetoothTetheringOn);

        verify(mockSwitchPreference).setEnabled(true);
        verify(mockSwitchPreference).setChecked(true);
    }

    @Test
    public void testBluetoothState_updateBluetoothState_bluetoothTetheringStateOff() {
        final TetherSettings spyTetherSettings = spy(new TetherSettings());
        spyTetherSettings.mContext = mContext;
        spyTetherSettings.mTm = mTetheringManager;
        final SwitchPreference mockSwitchPreference = mock(SwitchPreference.class);
        when(spyTetherSettings.findPreference(TetherSettings.KEY_ENABLE_BLUETOOTH_TETHERING))
            .thenReturn(mockSwitchPreference);
        final FragmentActivity mockActivity = mock(FragmentActivity.class);
        when(spyTetherSettings.getActivity()).thenReturn(mockActivity);
        final ArgumentCaptor<BroadcastReceiver> captor =
                ArgumentCaptor.forClass(BroadcastReceiver.class);
        when(mockActivity.registerReceiver(captor.capture(), any(IntentFilter.class)))
            .thenReturn(null);
        // Bluetooth tethering state is off
        when(spyTetherSettings.getBluetoothState()).thenReturn(BluetoothAdapter.STATE_ON);
        when(spyTetherSettings.isBluetoothTetheringOn()).thenReturn(false);

        spyTetherSettings.setupTetherPreference();
        spyTetherSettings.registerReceiver();
        updateOnlyBluetoothState(spyTetherSettings);

        // Simulate Bluetooth tethering state changed
        final BroadcastReceiver receiver = captor.getValue();
        final Intent bluetoothTetheringOn = new Intent(BluetoothPan.ACTION_TETHERING_STATE_CHANGED);
        bluetoothTetheringOn.putExtra(BluetoothPan.EXTRA_TETHERING_STATE,
                BluetoothPan.TETHERING_STATE_ON);
        receiver.onReceive(mockActivity, bluetoothTetheringOn);

        verify(mockSwitchPreference).setEnabled(true);
        verify(mockSwitchPreference).setChecked(false);
    }

    @Test
    public void updateState_usbTetheringIsEnabled_checksUsbTethering() {
        String [] tethered = {"rndis0"};
        TetherSettings spyTetherSettings = spy(new TetherSettings());
        RestrictedSwitchPreference tetheringPreference = mock(RestrictedSwitchPreference.class);
        when(spyTetherSettings.findPreference(TetherSettings.KEY_USB_TETHER_SETTINGS))
                .thenReturn(tetheringPreference);
        spyTetherSettings.mContext = mContext;
        spyTetherSettings.mTm = mTetheringManager;
        spyTetherSettings.setupTetherPreference();
        spyTetherSettings.mUsbRegexs = tethered;

        spyTetherSettings.updateUsbState(tethered);

        verify(tetheringPreference).setEnabled(true);
        verify(tetheringPreference).setChecked(true);
    }

    @Test
    public void updateState_usbTetheringIsDisabled_unchecksUsbTethering() {
        String [] tethered = {"rndis0"};
        TetherSettings spyTetherSettings = spy(new TetherSettings());
        RestrictedSwitchPreference tetheringPreference = mock(RestrictedSwitchPreference.class);
        when(spyTetherSettings.findPreference(TetherSettings.KEY_USB_TETHER_SETTINGS))
                .thenReturn(tetheringPreference);
        spyTetherSettings.mContext = mContext;
        spyTetherSettings.mTm = mTetheringManager;
        spyTetherSettings.setupTetherPreference();
        spyTetherSettings.mUsbRegexs = tethered;

        spyTetherSettings.updateUsbState(new String[0]);

        verify(tetheringPreference).setEnabled(false);
        verify(tetheringPreference).setChecked(false);
    }

    @Test
    public void onReceive_usbIsConnected_tetheringPreferenceIsEnabled() {
        RestrictedSwitchPreference tetheringPreference = mock(RestrictedSwitchPreference.class);
        FragmentActivity mockActivity = mock(FragmentActivity.class);
        ArgumentCaptor<BroadcastReceiver> captor = ArgumentCaptor.forClass(BroadcastReceiver.class);
        setupUsbStateComponents(tetheringPreference, captor, mockActivity);

        BroadcastReceiver receiver = captor.getValue();
        Intent usbStateChanged = new Intent(ACTION_USB_STATE);
        usbStateChanged.putExtra(UsbManager.USB_CONNECTED, true);
        receiver.onReceive(mockActivity, usbStateChanged);

        verify(tetheringPreference).setEnabled(true);
    }

    @Test
    public void onReceive_usbIsDisconnected_tetheringPreferenceIsDisabled() {
        RestrictedSwitchPreference tetheringPreference = mock(RestrictedSwitchPreference.class);
        FragmentActivity mockActivity = mock(FragmentActivity.class);
        ArgumentCaptor<BroadcastReceiver> captor = ArgumentCaptor.forClass(BroadcastReceiver.class);
        setupUsbStateComponents(tetheringPreference, captor, mockActivity);

        BroadcastReceiver receiver = captor.getValue();
        Intent usbStateChanged = new Intent(ACTION_USB_STATE);
        usbStateChanged.putExtra(UsbManager.USB_CONNECTED, false);
        receiver.onReceive(mockActivity, usbStateChanged);

        verify(tetheringPreference).setEnabled(false);
    }

    @Test
    public void onReceive_mediaIsShared_tetheringPreferenceIsDisabled() {
        RestrictedSwitchPreference tetheringPreference = mock(RestrictedSwitchPreference.class);
        FragmentActivity mockActivity = mock(FragmentActivity.class);
        ArgumentCaptor<BroadcastReceiver> captor = ArgumentCaptor.forClass(BroadcastReceiver.class);
        setupUsbStateComponents(tetheringPreference, captor, mockActivity);

        BroadcastReceiver receiver = captor.getValue();
        Intent mediaIsShared = new Intent(ACTION_MEDIA_SHARED);
        receiver.onReceive(mockActivity, mediaIsShared);

        verify(tetheringPreference).setEnabled(false);
    }

    @Test
    public void onReceive_mediaIsUnshared_tetheringPreferenceIsEnabled() {
        RestrictedSwitchPreference tetheringPreference = mock(RestrictedSwitchPreference.class);
        FragmentActivity mockActivity = mock(FragmentActivity.class);
        ArgumentCaptor<BroadcastReceiver> captor = ArgumentCaptor.forClass(BroadcastReceiver.class);
        setupUsbStateComponents(tetheringPreference, captor, mockActivity);

        BroadcastReceiver receiver = captor.getValue();
        Intent mediaIsShared = new Intent(ACTION_MEDIA_UNSHARED);
        Intent usbStateChanged = new Intent(ACTION_USB_STATE);
        usbStateChanged.putExtra(UsbManager.USB_CONNECTED, true);
        receiver.onReceive(mockActivity, usbStateChanged);
        receiver.onReceive(mockActivity, mediaIsShared);

        verify(tetheringPreference, times(2)).setEnabled(true);
    }

    private void updateOnlyBluetoothState(TetherSettings tetherSettings) {
        doReturn(mTetheringManager).when(tetherSettings)
            .getSystemService(Context.TETHERING_SERVICE);
        when(mTetheringManager.getTetherableIfaces()).thenReturn(new String[0]);
        when(mTetheringManager.getTetheredIfaces()).thenReturn(new String[0]);
        when(mTetheringManager.getTetheringErroredIfaces()).thenReturn(new String[0]);
        doNothing().when(tetherSettings).updateUsbState(any(String[].class));
        doNothing().when(tetherSettings).updateEthernetState(any(String[].class),
                any(String[].class));
    }

    private void setupIsTetherAvailable(boolean returnValue) {
        when(mConnectivityManager.isTetheringSupported()).thenReturn(true);

        // For RestrictedLockUtils.checkIfRestrictionEnforced
        final int userId = UserHandle.myUserId();
        List<UserManager.EnforcingUser> enforcingUsers = new ArrayList<>();
        when(mUserManager.getUserRestrictionSources(
                UserManager.DISALLOW_CONFIG_TETHERING, UserHandle.of(userId)))
                .thenReturn(enforcingUsers);

        // For RestrictedLockUtils.hasBaseUserRestriction
        when(mUserManager.hasBaseUserRestriction(
                UserManager.DISALLOW_CONFIG_TETHERING, UserHandle.of(userId)))
                .thenReturn(!returnValue);
    }

    private void setupUsbStateComponents(RestrictedSwitchPreference preference,
            ArgumentCaptor<BroadcastReceiver> captor, FragmentActivity activity) {
        TetherSettings spyTetherSettings = spy(new TetherSettings());
        SwitchPreference mockSwitchPreference = mock(SwitchPreference.class);

        when(spyTetherSettings.findPreference(TetherSettings.KEY_USB_TETHER_SETTINGS))
                .thenReturn(preference);
        when(spyTetherSettings.findPreference(TetherSettings.KEY_ENABLE_BLUETOOTH_TETHERING))
                .thenReturn(mockSwitchPreference);
        spyTetherSettings.mContext = mContext;
        spyTetherSettings.mTm = mTetheringManager;
        when(spyTetherSettings.getActivity()).thenReturn(activity);
        when(activity.registerReceiver(captor.capture(), any(IntentFilter.class)))
                .thenReturn(null);

        spyTetherSettings.setupTetherPreference();
        spyTetherSettings.registerReceiver();
        updateOnlyBluetoothState(spyTetherSettings);
    }
}
