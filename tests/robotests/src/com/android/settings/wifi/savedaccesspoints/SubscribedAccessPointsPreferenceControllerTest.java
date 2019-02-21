/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.wifi.savedaccesspoints;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.net.wifi.WifiConfiguration;

import android.net.wifi.hotspot2.PasspointConfiguration;
import android.net.wifi.hotspot2.pps.HomeSp;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiManager;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.FeatureFlags;
import com.android.settings.development.featureflags.FeatureFlagPersistent;
import com.android.settings.testutils.shadow.ShadowAccessPoint;
import com.android.settings.testutils.shadow.ShadowThreadUtils;
import com.android.settings.testutils.shadow.ShadowWifiManager;
import com.android.settingslib.wifi.AccessPointPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowThreadUtils.class, ShadowWifiManager.class})
public class SubscribedAccessPointsPreferenceControllerTest {

    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private PreferenceCategory mPreferenceCategory;

    private Context mContext;
    private WifiManager mWifiManager;
    private SavedAccessPointsWifiSettings mSettings;
    private SubscribedAccessPointsPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mWifiManager = mContext.getSystemService(WifiManager.class);
        mSettings = spy(new SavedAccessPointsWifiSettings());
        mController = spy(new SubscribedAccessPointsPreferenceController(mContext, "test_key"));
        mController.setHost(mSettings);

        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(mPreferenceCategory);
        when(mPreferenceCategory.getContext()).thenReturn(mContext);

        FeatureFlagPersistent.setEnabled(mContext, FeatureFlags.NETWORK_INTERNET_V2, true);
    }

    @Test
    public void getAvailability_alwaysAvailable() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void onStart_shouldRefreshApList() {
        doNothing().when(mController).refreshSubscribedAccessPoints();

        mController.onStart();

        verify(mController).refreshSubscribedAccessPoints();
    }

    @Test
    public void postRefresh_shouldRefreshApList() {
        doNothing().when(mController).refreshSubscribedAccessPoints();

        mController.postRefreshSubscribedAccessPoints();

        verify(mController).refreshSubscribedAccessPoints();
    }

    @Test
    public void forget_onSuccess_shouldRefreshApList() {
        doNothing().when(mController).refreshSubscribedAccessPoints();

        mController.onSuccess();

        verify(mController).refreshSubscribedAccessPoints();
    }

    @Test
    public void forget_onFailure_shouldRefreshApList() {
        doNothing().when(mController).refreshSubscribedAccessPoints();

        mController.onFailure(0 /* reason */);

        verify(mController).refreshSubscribedAccessPoints();
    }

    @Test
    @Config(shadows = ShadowAccessPoint.class)
    public void refreshSubscribedAccessPoints_shouldNotListNonSubscribedAPs() {
        final WifiConfiguration config = new WifiConfiguration();
        config.SSID = "SSID";
        config.BSSID = "BSSID";
        config.networkId = 2;
        mWifiManager.addNetwork(config);

        mController.displayPreference(mPreferenceScreen);
        mController.refreshSubscribedAccessPoints();

        verify(mPreferenceCategory, never()).addPreference(any(AccessPointPreference.class));
    }

    @Test
    @Config(shadows = ShadowAccessPoint.class)
    public void refreshSubscribedAccessPoints_shouldListSubscribedAPs() {
        mWifiManager.addOrUpdatePasspointConfiguration(createMockPasspointConfiguration());

        mController.displayPreference(mPreferenceScreen);
        mController.refreshSubscribedAccessPoints();

        final ArgumentCaptor<AccessPointPreference> captor =
                ArgumentCaptor.forClass(AccessPointPreference.class);
        verify(mPreferenceCategory).addPreference(captor.capture());

        final AccessPointPreference pref = captor.getValue();
        assertThat(pref.getTitle()).isEqualTo("TESTPASSPOINT");
    }

    public static PasspointConfiguration createMockPasspointConfiguration() {
        final PasspointConfiguration config = new PasspointConfiguration();
        final HomeSp homeSp = new HomeSp();
        homeSp.setFqdn("FQDN");
        homeSp.setFriendlyName("TESTPASSPOINT");
        config.setHomeSp(homeSp);
        return config;
    }
}
