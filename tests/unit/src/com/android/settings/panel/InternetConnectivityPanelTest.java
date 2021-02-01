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

import static com.android.settings.network.InternetUpdater.INTERNET_APM;
import static com.android.settings.network.InternetUpdater.INTERNET_APM_NETWORKS;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.testutils.ResourcesUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class InternetConnectivityPanelTest {

    public static final String TITLE_INTERNET = ResourcesUtils.getResourcesString(
            ApplicationProvider.getApplicationContext(), "provider_internet_settings");
    public static final String TITLE_APM_NETWORKS = ResourcesUtils.getResourcesString(
            ApplicationProvider.getApplicationContext(), "airplane_mode_network_panel_title");

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
    public void getTitle_internetTypeChangedFromApmToApmNetworks_verifyTitleChanged() {
        mPanel.onInternetTypeChanged(INTERNET_APM);

        assertThat(mPanel.getTitle()).isEqualTo(TITLE_INTERNET);

        clearInvocations(mPanelContentCallback);

        mPanel.onInternetTypeChanged(INTERNET_APM_NETWORKS);

        assertThat(mPanel.getTitle()).isEqualTo(TITLE_APM_NETWORKS);
        verify(mPanelContentCallback).onTitleChanged();
    }

    @Test
    public void getTitle_internetTypeChangedFromApmNetworksToApm_verifyTitleChanged() {
        mPanel.onInternetTypeChanged(INTERNET_APM_NETWORKS);

        assertThat(mPanel.getTitle()).isEqualTo(TITLE_APM_NETWORKS);

        clearInvocations(mPanelContentCallback);

        mPanel.onInternetTypeChanged(INTERNET_APM);

        assertThat(mPanel.getTitle()).isEqualTo(TITLE_INTERNET);
        verify(mPanelContentCallback).onTitleChanged();
    }
}
