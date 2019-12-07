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

package com.android.settings.homepage.contextualcards.conditional;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiManager;

import com.android.settings.homepage.contextualcards.ContextualCard;
import com.android.settings.testutils.shadow.ShadowWifiManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowWifiManager.class})
public class HotspotConditionControllerTest {

    private static final String WIFI_AP_SSID = "Test Hotspot";

    @Mock
    private ConditionManager mConditionManager;

    private Context mContext;
    private HotspotConditionController mController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mController = new HotspotConditionController(mContext, mConditionManager);
    }

    @Test
    public void buildContextualCard_hasWifiAp_shouldHaveWifiApSsid() {
        setupSoftApConfiguration();

        final ContextualCard card = mController.buildContextualCard();

        assertThat(card.getSummaryText()).isEqualTo(WIFI_AP_SSID);
    }

    @Test
    public void buildContextualCard_noWifiAp_shouldHaveEmptySsid() {
        final ContextualCard card = mController.buildContextualCard();

        assertThat(card.getSummaryText()).isEqualTo("");
    }

    private void setupSoftApConfiguration() {
        final SoftApConfiguration wifiApConfig = new SoftApConfiguration.Builder()
                .setSsid(WIFI_AP_SSID).build();
        mContext.getSystemService(WifiManager.class).setSoftApConfiguration(wifiApConfig);
    }
}
