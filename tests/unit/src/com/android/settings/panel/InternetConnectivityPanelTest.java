/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.panel;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.network.AirplaneModePreferenceController;
import com.android.settings.slices.CustomSliceRegistry;
import com.android.settings.testutils.ResourcesUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class InternetConnectivityPanelTest {

    public static final String TITLE_INTERNET = ResourcesUtils.getResourcesString(
            ApplicationProvider.getApplicationContext(), "provider_internet_settings");
    public static final String TITLE_APM_NETWORKS = ResourcesUtils.getResourcesString(
            ApplicationProvider.getApplicationContext(), "airplane_mode_network_panel_title");
    public static final String SUBTITLE_APM_IS_ON = ResourcesUtils.getResourcesString(
            ApplicationProvider.getApplicationContext(), "condition_airplane_title");
    public static final String BUTTON_SETTINGS = ResourcesUtils.getResourcesString(
            ApplicationProvider.getApplicationContext(), "settings_button");

    @Rule
    public final MockitoRule mMocks = MockitoJUnit.rule();
    @Mock
    PanelContentCallback mPanelContentCallback;

    private Context mContext;
    private InternetConnectivityPanel mPanel;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());

        mPanel = InternetConnectivityPanel.create(mContext);
        mPanel.registerCallback(mPanelContentCallback);
        mPanel.mIsProviderModelEnabled = true;
    }

    @Test
    public void getTitle_apmOnApmNetworksOff_shouldBeInternet() {
        mPanel.onAirplaneModeChanged(true);
        mPanel.onAirplaneModeNetworksChanged(false);

        assertThat(mPanel.getTitle()).isEqualTo(TITLE_INTERNET);
    }

    @Test
    public void getTitle_apmOnApmNetworksOn_shouldBeApmNetworks() {
        mPanel.onAirplaneModeChanged(true);
        mPanel.onAirplaneModeNetworksChanged(true);

        assertThat(mPanel.getTitle()).isEqualTo(TITLE_APM_NETWORKS);
    }

    @Test
    public void getTitle_notInternetApmNetworks_shouldBeInternet() {
        mPanel.onAirplaneModeNetworksChanged(false);

        assertThat(mPanel.getTitle()).isEqualTo(TITLE_INTERNET);
    }

    @Test
    public void getSubTitle_apmOnApmNetworksOff_shouldBeApmIsOn() {
        mPanel.onAirplaneModeChanged(true);
        mPanel.onAirplaneModeNetworksChanged(false);

        assertThat(mPanel.getSubTitle()).isEqualTo(SUBTITLE_APM_IS_ON);
    }

    @Test
    public void getSubTitle_apmOnApmNetworksOn_shouldBeNull() {
        mPanel.onAirplaneModeChanged(true);
        mPanel.onAirplaneModeNetworksChanged(true);

        assertThat(mPanel.getSubTitle()).isNull();
    }

    @Test
    public void getSubTitle_apmOff_shouldBeNull() {
        mPanel.onAirplaneModeChanged(false);

        assertThat(mPanel.getSubTitle()).isNull();
    }

    @Test
    public void getCustomizedButtonTitle_apmOnApmNetworksOff_shouldBeNull() {
        mPanel.onAirplaneModeChanged(true);
        mPanel.onAirplaneModeNetworksChanged(false);

        assertThat(mPanel.getCustomizedButtonTitle()).isNull();
    }

    @Test
    public void getCustomizedButtonTitle_apmOnApmNetworksOn_shouldBeSettings() {
        mPanel.onAirplaneModeChanged(true);
        mPanel.onAirplaneModeNetworksChanged(true);

        assertThat(mPanel.getCustomizedButtonTitle()).isEqualTo(BUTTON_SETTINGS);
    }

    @Test
    public void getCustomizedButtonTitle_apmOff_shouldBeSettings() {
        mPanel.onAirplaneModeChanged(false);

        assertThat(mPanel.getCustomizedButtonTitle()).isEqualTo(BUTTON_SETTINGS);
    }

    @Test
    public void getSlices_providerModelDisabled_containsNecessarySlices() {
        mPanel.mIsProviderModelEnabled = false;
        final List<Uri> uris = mPanel.getSlices();

        assertThat(uris).containsExactly(
                AirplaneModePreferenceController.SLICE_URI,
                CustomSliceRegistry.MOBILE_DATA_SLICE_URI,
                CustomSliceRegistry.WIFI_SLICE_URI);
    }

    @Test
    public void getSlices_providerModelEnabled_containsNecessarySlices() {
        final List<Uri> uris = mPanel.getSlices();

        assertThat(uris).containsExactly(
                CustomSliceRegistry.PROVIDER_MODEL_SLICE_URI,
                CustomSliceRegistry.TURN_ON_WIFI_SLICE_URI);
    }

    @Test
    public void getSeeMoreIntent_notNull() {
        assertThat(mPanel.getSeeMoreIntent()).isNotNull();
    }

    @Test
    public void onAirplaneModeOn_apmNetworksOff_changeHeaderAndHideSettings() {
        mPanel.onAirplaneModeNetworksChanged(false);
        clearInvocations(mPanelContentCallback);

        mPanel.onAirplaneModeChanged(true);

        verify(mPanelContentCallback).onHeaderChanged();
        verify(mPanelContentCallback).onCustomizedButtonStateChanged();
    }

    @Test
    public void onAirplaneModeOn_apmNetworksOn_changeTitleAndShowSettings() {
        mPanel.onAirplaneModeNetworksChanged(true);
        clearInvocations(mPanelContentCallback);

        mPanel.onAirplaneModeChanged(true);

        verify(mPanelContentCallback).onTitleChanged();
        verify(mPanelContentCallback).onCustomizedButtonStateChanged();
    }

    @Test
    public void onAirplaneModeNetworksOn_apmOff_changeTitleAndShowSettings() {
        mPanel.onAirplaneModeChanged(false);
        clearInvocations(mPanelContentCallback);

        mPanel.onAirplaneModeNetworksChanged(true);

        verify(mPanelContentCallback).onTitleChanged();
        verify(mPanelContentCallback).onCustomizedButtonStateChanged();
    }

    @Test
    public void onAirplaneModeNetworksOff_apmOff_changeTitleAndShowSettings() {
        mPanel.onAirplaneModeChanged(false);
        clearInvocations(mPanelContentCallback);

        mPanel.onAirplaneModeNetworksChanged(false);

        verify(mPanelContentCallback).onTitleChanged();
        verify(mPanelContentCallback).onCustomizedButtonStateChanged();
    }
}
