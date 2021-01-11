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

package com.android.settings.network;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Looper;
import android.view.View;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.testutils.AirplaneModeRule;
import com.android.settings.testutils.ResourcesUtils;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.LayoutPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class ViewAirplaneModeNetworksLayoutPreferenceControllerTest {

    private static final String KEY = ViewAirplaneModeNetworksLayoutPreferenceController.KEY;
    private static final String RES_ID_AIRPLANE_MODE_IS_ON = "condition_airplane_title";
    private static final String RES_ID_VIEWING_AIRPLANE_MODE_NETWORKS =
            "viewing_airplane_mode_networks";

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule
    public AirplaneModeRule mAirplaneModeRule = new AirplaneModeRule();
    @Mock
    private WifiManager mWifiManager;

    private Context mContext;
    private PreferenceScreen mScreen;
    private LayoutPreference mPreference;
    private ViewAirplaneModeNetworksLayoutPreferenceController mController;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        doReturn(mWifiManager).when(mContext).getSystemService(Context.WIFI_SERVICE);

        mController = new ViewAirplaneModeNetworksLayoutPreferenceController(mContext,
                mock(Lifecycle.class));
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        mScreen = preferenceManager.createPreferenceScreen(mContext);
        mPreference = new LayoutPreference(mContext,
                ResourcesUtils.getResourcesId(
                        mContext, "layout", "view_airplane_mode_networks_button"));
        mPreference.setKey(KEY);
        mScreen.addPreference(mPreference);
    }

    @Test
    public void isAvailable_airplaneModeOff_returnFalse() {
        mAirplaneModeRule.setAirplaneMode(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_airplaneModeOn_returnTrue() {
        mAirplaneModeRule.setAirplaneMode(true);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void displayPreference_wifiDisabled_showAirplaneModeIsOnButtonVisible() {
        mAirplaneModeRule.setAirplaneMode(true);
        doReturn(false).when(mWifiManager).isWifiEnabled();

        mController.displayPreference(mScreen);

        assertThat(mController.mTextView.getText())
                .isEqualTo(ResourcesUtils.getResourcesString(mContext, RES_ID_AIRPLANE_MODE_IS_ON));
        assertThat(mController.mButton.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void displayPreference_wifiEnabled_showViewingAirplaneModeNetworksButtonGone() {
        mAirplaneModeRule.setAirplaneMode(true);
        doReturn(true).when(mWifiManager).isWifiEnabled();

        mController.displayPreference(mScreen);

        assertThat(mController.mTextView.getText()).isEqualTo(
                ResourcesUtils.getResourcesString(mContext, RES_ID_VIEWING_AIRPLANE_MODE_NETWORKS));
        assertThat(mController.mButton.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void refreshLayout_wifiEnabledThenDisabled_showAirplaneModeIsOnButtonVisible() {
        mAirplaneModeRule.setAirplaneMode(true);
        // Wi-Fi enabled
        doReturn(true).when(mWifiManager).isWifiEnabled();
        // Display preference
        mController.displayPreference(mScreen);
        // Then Wi-Fi disabled
        doReturn(false).when(mWifiManager).isWifiEnabled();

        // Refresh layout
        mController.refreshLayout();

        assertThat(mController.mTextView.getText())
                .isEqualTo(ResourcesUtils.getResourcesString(mContext, RES_ID_AIRPLANE_MODE_IS_ON));
        assertThat(mController.mButton.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void refreshLayout_wifiDisabledThenEnabled_showViewingAirplaneModeNetworksButtonGone() {
        mAirplaneModeRule.setAirplaneMode(true);
        // Wi-Fi disabled
        doReturn(false).when(mWifiManager).isWifiEnabled();
        // Display preference
        mController.displayPreference(mScreen);
        // Then Wi-Fi enabled
        doReturn(true).when(mWifiManager).isWifiEnabled();

        // Refresh layout
        mController.refreshLayout();

        assertThat(mController.mTextView.getText()).isEqualTo(
                ResourcesUtils.getResourcesString(mContext, RES_ID_VIEWING_AIRPLANE_MODE_NETWORKS));
        assertThat(mController.mButton.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void onClick_shouldSetWifiEnabled() {
        mAirplaneModeRule.setAirplaneMode(true);
        doReturn(false).when(mWifiManager).isWifiEnabled();

        mController.onClick(mock(View.class));

        verify(mWifiManager).setWifiEnabled(true);
    }
}
