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

package com.android.settings.wifi.tether;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.UserHandle;
import android.os.UserManager;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowWifiManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowWifiManager.class})
public class WifiTetherSettingsTest {
    private static final String[] WIFI_REGEXS = {"wifi_regexs"};

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
        doReturn(WIFI_REGEXS).when(mConnectivityManager).getTetherableWifiRegexs();
        doReturn(mUserManager).when(mContext).getSystemService(Context.USER_SERVICE);
    }

    @Test
    public void wifiTetherNonIndexableKeys_tetherAvailable_keysNotReturned() {
        // To let TetherUtil.isTetherAvailable return true, select one of the combinations
        setupIsTetherAvailable(true);

        final List<String> niks =
                WifiTetherSettings.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(mContext);

        assertThat(niks).doesNotContain(WifiTetherSettings.KEY_WIFI_TETHER_NETWORK_NAME);
        assertThat(niks).doesNotContain(WifiTetherSettings.KEY_WIFI_TETHER_NETWORK_PASSWORD);
        assertThat(niks).doesNotContain(WifiTetherSettings.KEY_WIFI_TETHER_AUTO_OFF);
        assertThat(niks).doesNotContain(WifiTetherSettings.KEY_WIFI_TETHER_NETWORK_AP_BAND);
    }

    @Test
    public void wifiTetherNonIndexableKeys_tetherNotAvailable_keysReturned() {
        // To let TetherUtil.isTetherAvailable return false, select one of the combinations
        setupIsTetherAvailable(false);

        final List<String> niks =
                WifiTetherSettings.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(mContext);

        assertThat(niks).contains(WifiTetherSettings.KEY_WIFI_TETHER_NETWORK_NAME);
        assertThat(niks).contains(WifiTetherSettings.KEY_WIFI_TETHER_NETWORK_PASSWORD);
        assertThat(niks).contains(WifiTetherSettings.KEY_WIFI_TETHER_AUTO_OFF);
        assertThat(niks).contains(WifiTetherSettings.KEY_WIFI_TETHER_NETWORK_AP_BAND);
    }

    @Test
    public void createPreferenceControllers_notEmpty() {
        assertThat(WifiTetherSettings.SEARCH_INDEX_DATA_PROVIDER.getPreferenceControllers(mContext))
                .isNotEmpty();
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
