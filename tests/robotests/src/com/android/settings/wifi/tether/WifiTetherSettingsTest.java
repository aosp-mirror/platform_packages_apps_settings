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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.FeatureFlagUtils;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.FeatureFlags;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowFragment;
import com.android.settings.testutils.shadow.ShadowWifiManager;

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
public class WifiTetherSettingsTest {
    private static final String[] WIFI_REGEXS = {"wifi_regexs"};

    private Context mContext;
    private WifiTetherSettings mWifiTetherSettings;

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

        mWifiTetherSettings = new WifiTetherSettings();
    }

    @Test
    public void wifiTetherNonIndexableKeys_tetherAvailable_keysNotReturned() {
        FeatureFlagUtils.setEnabled(mContext, FeatureFlags.TETHER_ALL_IN_ONE, false);
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

    @Test
    @Config(shadows = ShadowFragment.class)
    public void startFragment_notAdminUser_shouldRemoveAllPreferences() {
        final WifiTetherSettings settings = spy(new WifiTetherSettings());
        final FragmentActivity activity = mock(FragmentActivity.class);
        when(settings.getActivity()).thenReturn(activity);
        when(settings.getContext()).thenReturn(mContext);
        final Resources.Theme theme = mContext.getTheme();
        when(activity.getTheme()).thenReturn(theme);
        when(activity.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        doNothing().when(settings)
            .onCreatePreferences(any(Bundle.class), nullable(String.class));
        final FakeFeatureFactory fakeFeatureFactory = FakeFeatureFactory.setupForTest();
        ReflectionHelpers.setField(settings, "mDashboardFeatureProvider",
            fakeFeatureFactory.dashboardFeatureProvider);
        final TextView emptyTextView = mock(TextView.class);
        ReflectionHelpers.setField(settings, "mEmptyTextView", emptyTextView);
        final PreferenceScreen screen = mock(PreferenceScreen.class);
        doReturn(screen).when(settings).getPreferenceScreen();
        settings.onCreate(Bundle.EMPTY);

        settings.onStart();

        verify(screen).removeAll();
    }

    @Test
    public void createPreferenceControllers_hasAutoOffPreference() {
        assertThat(mWifiTetherSettings.createPreferenceControllers(mContext)
                .stream()
                .filter(controller -> controller instanceof WifiTetherAutoOffPreferenceController)
                .count())
                .isEqualTo(1);
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
