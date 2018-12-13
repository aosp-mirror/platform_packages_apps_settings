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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.UserHandle;
import android.os.UserManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);

        MockitoAnnotations.initMocks(this);
        doReturn(mConnectivityManager)
                .when(mContext).getSystemService(Context.CONNECTIVITY_SERVICE);
        doReturn(mUserManager)
                .when(mContext).getSystemService(Context.USER_SERVICE);

        setupIsTetherAvailable(true);

        when(mConnectivityManager.getTetherableUsbRegexs()).thenReturn(new String[0]);
        when(mConnectivityManager.getTetherableBluetoothRegexs()).thenReturn(new String[0]);
    }

    @Test
    public void testTetherNonIndexableKeys_tetherAvailable_keysNotReturned() {
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
    public void testTetherNonIndexableKeys_usbNotAvailable_usbKeyReturned() {
        when(mConnectivityManager.getTetherableUsbRegexs()).thenReturn(new String[0]);

        final List<String> niks =
            TetherSettings.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(mContext);

        assertThat(niks).contains(TetherSettings.KEY_USB_TETHER_SETTINGS);
    }

    @Test
    public void testTetherNonIndexableKeys_usbAvailable_usbKeyNotReturned() {
        // We can ignore the condition of Utils.isMonkeyRunning()
        // In normal case, monkey and robotest should not execute at the same time
        when(mConnectivityManager.getTetherableUsbRegexs()).thenReturn(new String[]{"dummyRegex"});

        final List<String> niks =
            TetherSettings.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(mContext);

        assertThat(niks).doesNotContain(TetherSettings.KEY_USB_TETHER_SETTINGS);
    }

    @Test
    public void testTetherNonIndexableKeys_bluetoothNotAvailable_bluetoothKeyReturned() {
        when(mConnectivityManager.getTetherableBluetoothRegexs()).thenReturn(new String[0]);

        final List<String> niks =
            TetherSettings.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(mContext);

        assertThat(niks).contains(TetherSettings.KEY_ENABLE_BLUETOOTH_TETHERING);
    }

    @Test
    public void testTetherNonIndexableKeys_bluetoothAvailable_bluetoothKeyNotReturned() {
        when(mConnectivityManager.getTetherableBluetoothRegexs())
                .thenReturn(new String[]{"dummyRegex"});

        final List<String> niks =
            TetherSettings.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(mContext);

        assertThat(niks).doesNotContain(TetherSettings.KEY_ENABLE_BLUETOOTH_TETHERING);
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
}
