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

package com.android.settings.wifi.savedaccesspoints;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.wifi.WifiConfiguration;

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
public class SavedAccessPointsPreferenceControllerTest {

    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private PreferenceCategory mPreferenceCategory;

    private Context mContext;
    private WifiManager mWifiManager;
    private SavedAccessPointsWifiSettings mSettings;
    private SavedAccessPointsPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mWifiManager = mContext.getSystemService(WifiManager.class);
        mSettings = spy(new SavedAccessPointsWifiSettings());
        mController = spy(new SavedAccessPointsPreferenceController(mContext, "test_key"));
        mController.setHost(mSettings);

        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(mPreferenceCategory);
        when(mPreferenceCategory.getContext()).thenReturn(mContext);
    }

    @Test
    public void getAvailability_alwaysAvailable() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void onStart_shouldRefreshApList() {
        doNothing().when(mController).refreshSavedAccessPoints();

        mController.onStart();

        verify(mController).refreshSavedAccessPoints();
    }

    @Test
    public void postRefresh_shouldRefreshApList() {
        doNothing().when(mController).refreshSavedAccessPoints();

        mController.postRefreshSavedAccessPoints();

        verify(mController).refreshSavedAccessPoints();
    }

    @Test
    public void forget_onSuccess_shouldRefreshApList() {
        doNothing().when(mController).refreshSavedAccessPoints();

        mController.onSuccess();

        verify(mController).refreshSavedAccessPoints();
    }

    @Test
    public void forget_onFailure_shouldRefreshApList() {
        doNothing().when(mController).refreshSavedAccessPoints();

        mController.onFailure(0 /* reason */);

        verify(mController).refreshSavedAccessPoints();
    }

    @Test
    @Config(shadows = ShadowAccessPoint.class)
    public void refreshSavedAccessPoints_shouldListNonSubscribedAPs() {
        final WifiConfiguration config = new WifiConfiguration();
        config.SSID = "SSID";
        config.BSSID = "BSSID";
        config.networkId = 2;
        mWifiManager.addNetwork(config);

        final ArgumentCaptor<AccessPointPreference> captor =
                ArgumentCaptor.forClass(AccessPointPreference.class);
        mController.displayPreference(mPreferenceScreen);
        mController.refreshSavedAccessPoints();

        verify(mPreferenceCategory).addPreference(captor.capture());

        final AccessPointPreference pref = captor.getValue();
        assertThat(pref.getTitle()).isEqualTo(config.SSID);
    }

    @Test
    @Config(shadows = ShadowAccessPoint.class)
    public void refreshSavedAccessPoints_shouldNotListSubscribedAPs() {
        FeatureFlagPersistent.setEnabled(mContext, FeatureFlags.NETWORK_INTERNET_V2, true);

        mWifiManager.addOrUpdatePasspointConfiguration(
                SubscribedAccessPointsPreferenceControllerTest.createMockPasspointConfiguration());

        mController.displayPreference(mPreferenceScreen);
        mController.refreshSavedAccessPoints();

        verify(mPreferenceCategory, never()).addPreference(any(AccessPointPreference.class));
    }
}
