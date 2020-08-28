/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static com.android.settings.AllInOneTetherSettings.BLUETOOTH_TETHER_KEY;
import static com.android.settings.AllInOneTetherSettings.ETHERNET_TETHER_KEY;
import static com.android.settings.AllInOneTetherSettings.EXPANDED_CHILD_COUNT_DEFAULT;
import static com.android.settings.AllInOneTetherSettings.EXPANDED_CHILD_COUNT_MAX;
import static com.android.settings.AllInOneTetherSettings.EXPANDED_CHILD_COUNT_WITH_SECURITY_NON;
import static com.android.settings.AllInOneTetherSettings.USB_TETHER_KEY;
import static com.android.settings.AllInOneTetherSettings.WIFI_TETHER_DISABLE_KEY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.SoftApConfiguration;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.FeatureFlagUtils;

import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.FeatureFlags;
import com.android.settings.testutils.shadow.ShadowWifiManager;
import com.android.settings.wifi.tether.WifiTetherAutoOffPreferenceController;
import com.android.settings.wifi.tether.WifiTetherSecurityPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

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
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowWifiManager.class})
public class AllInOneTetherSettingsTest {
    private static final String[] WIFI_REGEXS = {"wifi_regexs"};
    private static final String[] USB_REGEXS = {"usb_regexs"};
    private static final String[] BT_REGEXS = {"bt_regexs"};
    private static final String[] ETHERNET_REGEXS = {"ethernet_regexs"};

    private Context mContext;
    private AllInOneTetherSettings mAllInOneTetherSettings;

    @Mock
    private ConnectivityManager mConnectivityManager;
    @Mock
    private UserManager mUserManager;
    @Mock
    private WifiTetherSecurityPreferenceController mSecurityPreferenceController;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private PreferenceGroup mWifiTetherGroup;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);

        MockitoAnnotations.initMocks(this);
        doReturn(mConnectivityManager)
                .when(mContext).getSystemService(Context.CONNECTIVITY_SERVICE);
        doReturn(WIFI_REGEXS).when(mConnectivityManager).getTetherableWifiRegexs();
        doReturn(USB_REGEXS).when(mConnectivityManager).getTetherableUsbRegexs();
        doReturn(BT_REGEXS).when(mConnectivityManager).getTetherableBluetoothRegexs();
        doReturn(ETHERNET_REGEXS).when(mConnectivityManager).getTetherableIfaces();
        doReturn(mUserManager).when(mContext).getSystemService(Context.USER_SERVICE);
        // Assume the feature is enabled for most test cases.
        FeatureFlagUtils.setEnabled(mContext, FeatureFlags.TETHER_ALL_IN_ONE, true);
        mAllInOneTetherSettings = spy(new AllInOneTetherSettings());
        doReturn(mPreferenceScreen).when(mAllInOneTetherSettings).getPreferenceScreen();
        ReflectionHelpers.setField(mAllInOneTetherSettings, "mLifecycle", mock(Lifecycle.class));
        ReflectionHelpers.setField(mAllInOneTetherSettings, "mSecurityPreferenceController",
                mSecurityPreferenceController);
        ReflectionHelpers.setField(mAllInOneTetherSettings, "mWifiTetherGroup", mWifiTetherGroup);
    }

    @Test
    public void getNonIndexableKeys_tetherAvailable_featureEnabled_keysReturnedCorrectly() {
        // To let TetherUtil.isTetherAvailable return true, select one of the combinations
        setupIsTetherAvailable(true);

        FeatureFlagUtils.setEnabled(mContext, FeatureFlags.TETHER_ALL_IN_ONE, true);
        final List<String> niks =
                AllInOneTetherSettings.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(mContext);

        assertThat(niks).doesNotContain(AllInOneTetherSettings.KEY_WIFI_TETHER_NETWORK_NAME);
        assertThat(niks).doesNotContain(
                AllInOneTetherSettings.KEY_WIFI_TETHER_NETWORK_PASSWORD);
        assertThat(niks).doesNotContain(AllInOneTetherSettings.KEY_WIFI_TETHER_AUTO_OFF);
        assertThat(niks).doesNotContain(AllInOneTetherSettings.KEY_WIFI_TETHER_NETWORK_AP_BAND);
        assertThat(niks).doesNotContain(AllInOneTetherSettings.KEY_WIFI_TETHER_SECURITY);
        assertThat(niks).doesNotContain(BLUETOOTH_TETHER_KEY);
        assertThat(niks).doesNotContain(USB_TETHER_KEY);
        assertThat(niks).doesNotContain(ETHERNET_TETHER_KEY);

        // This key should be returned because it's not visible by default.
        assertThat(niks).contains(WIFI_TETHER_DISABLE_KEY);
    }

    @Test
    public void getNonIndexableKeys_tetherAvailable_featureDisabled_keysReturned() {
        setupIsTetherAvailable(true);
        FeatureFlagUtils.setEnabled(mContext, FeatureFlags.TETHER_ALL_IN_ONE, false);

        final List<String> niks =
                AllInOneTetherSettings.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(mContext);

        assertThat(niks).contains(AllInOneTetherSettings.KEY_WIFI_TETHER_NETWORK_NAME);
        assertThat(niks).contains(AllInOneTetherSettings.KEY_WIFI_TETHER_NETWORK_PASSWORD);
        assertThat(niks).contains(AllInOneTetherSettings.KEY_WIFI_TETHER_AUTO_OFF);
        assertThat(niks).contains(AllInOneTetherSettings.KEY_WIFI_TETHER_NETWORK_AP_BAND);
        assertThat(niks).contains(AllInOneTetherSettings.KEY_WIFI_TETHER_SECURITY);
        assertThat(niks).contains(WIFI_TETHER_DISABLE_KEY);
        assertThat(niks).contains(BLUETOOTH_TETHER_KEY);
        assertThat(niks).contains(USB_TETHER_KEY);
        assertThat(niks).contains(ETHERNET_TETHER_KEY);
    }

    @Test
    public void getNonIndexableKeys_tetherNotAvailable_keysReturned() {
        // To let TetherUtil.isTetherAvailable return false, select one of the combinations
        setupIsTetherAvailable(false);

        final List<String> niks =
                AllInOneTetherSettings.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(mContext);

        assertThat(niks).contains(AllInOneTetherSettings.KEY_WIFI_TETHER_NETWORK_NAME);
        assertThat(niks).contains(AllInOneTetherSettings.KEY_WIFI_TETHER_NETWORK_PASSWORD);
        assertThat(niks).contains(AllInOneTetherSettings.KEY_WIFI_TETHER_AUTO_OFF);
        assertThat(niks).contains(AllInOneTetherSettings.KEY_WIFI_TETHER_NETWORK_AP_BAND);
        assertThat(niks).contains(AllInOneTetherSettings.KEY_WIFI_TETHER_SECURITY);
        assertThat(niks).contains(WIFI_TETHER_DISABLE_KEY);
        assertThat(niks).doesNotContain(BLUETOOTH_TETHER_KEY);
        assertThat(niks).doesNotContain(USB_TETHER_KEY);
        assertThat(niks).doesNotContain(ETHERNET_TETHER_KEY);
    }

    @Test
    public void getPreferenceControllers_notEmpty() {
        assertThat(AllInOneTetherSettings.SEARCH_INDEX_DATA_PROVIDER
                .getPreferenceControllers(mContext)).isNotEmpty();
    }

    @Test
    public void createPreferenceControllers_hasAutoOffPreference() {
        assertThat(mAllInOneTetherSettings.createPreferenceControllers(mContext)
                .stream()
                .filter(controller -> controller instanceof WifiTetherAutoOffPreferenceController)
                .count())
                .isEqualTo(1);
    }

    @Test
    public void getInitialChildCount_withSecurity() {
        when(mSecurityPreferenceController.getSecurityType())
                .thenReturn(SoftApConfiguration.SECURITY_TYPE_WPA2_PSK);
        assertThat(mAllInOneTetherSettings.getInitialExpandedChildCount()).isEqualTo(
                EXPANDED_CHILD_COUNT_DEFAULT);
    }

    @Test
    public void getInitialChildCount_withoutSecurity() {
        when(mSecurityPreferenceController.getSecurityType())
                .thenReturn(SoftApConfiguration.SECURITY_TYPE_OPEN);
        assertThat(mAllInOneTetherSettings.getInitialExpandedChildCount()).isEqualTo(
                EXPANDED_CHILD_COUNT_WITH_SECURITY_NON);
    }

    @Test
    public void getInitialExpandedChildCount_expandAllChild() {
        assertThat(mAllInOneTetherSettings.getInitialExpandedChildCount())
                .isNotEqualTo(EXPANDED_CHILD_COUNT_MAX);
        ReflectionHelpers.setField(mAllInOneTetherSettings, "mShouldShowWifiConfig", false);
        assertThat(mAllInOneTetherSettings.getInitialExpandedChildCount())
                .isEqualTo(EXPANDED_CHILD_COUNT_MAX);
        ReflectionHelpers.setField(mAllInOneTetherSettings, "mShouldShowWifiConfig", true);
        assertThat(mAllInOneTetherSettings.getInitialExpandedChildCount())
                .isEqualTo(EXPANDED_CHILD_COUNT_MAX);
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
