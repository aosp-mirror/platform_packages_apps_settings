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

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.os.Looper;

import androidx.lifecycle.Lifecycle;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.wifi.WifiPickerTrackerHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class CarrierWifiTogglePreferenceControllerTest {

    private static final int SUB_ID = 2;
    private static final String SSID = "ssid";

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock
    public WifiPickerTrackerHelper mWifiPickerTrackerHelper;

    private Context mContext;
    private CarrierWifiTogglePreferenceController mController;
    private PreferenceScreen mScreen;
    private Preference mTogglePreference;
    private Preference mNetworkPreference;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());

        mController = new CarrierWifiTogglePreferenceController(mContext,
                CarrierWifiTogglePreferenceController.CARRIER_WIFI_TOGGLE_PREF_KEY);
        mController.init(mock(Lifecycle.class), SUB_ID);
        mController.mIsCarrierProvisionWifiEnabled = true;
        doReturn(true).when(mWifiPickerTrackerHelper).isCarrierNetworkActive();
        doReturn(SSID).when(mWifiPickerTrackerHelper).getCarrierNetworkSsid();
        mController.mWifiPickerTrackerHelper = mWifiPickerTrackerHelper;

        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        mScreen = preferenceManager.createPreferenceScreen(mContext);
        mTogglePreference = new Preference(mContext);
        mTogglePreference.setKey(
                CarrierWifiTogglePreferenceController.CARRIER_WIFI_TOGGLE_PREF_KEY);
        mScreen.addPreference(mTogglePreference);
        mNetworkPreference = new Preference(mContext);
        mNetworkPreference.setKey(
                CarrierWifiTogglePreferenceController.CARRIER_WIFI_NETWORK_PREF_KEY);
        mScreen.addPreference(mNetworkPreference);
        mController.mCarrierNetworkPreference = mNetworkPreference;
    }

    @Test
    public void getAvailabilityStatus_carrierProvisionWifiEnabled_returnAvailable() {
        mController.mIsCarrierProvisionWifiEnabled = true;

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_carrierProvisionWifiDisabled_returnUnavailable() {
        mController.mIsCarrierProvisionWifiEnabled = false;

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void isChecked_carrierNetworkEnabled_returnTrue() {
        doReturn(true).when(mWifiPickerTrackerHelper).isCarrierNetworkEnabled();

        assertThat(mController.isChecked()).isEqualTo(true);
    }

    @Test
    public void isChecked_carrierNetworkDisabled_returnFalse() {
        doReturn(false).when(mWifiPickerTrackerHelper).isCarrierNetworkEnabled();

        assertThat(mController.isChecked()).isEqualTo(false);
    }

    @Test
    public void setChecked_checked_shouldSetCarrierNetworkEnabled() {
        mController.setChecked(true);

        verify(mWifiPickerTrackerHelper).setCarrierNetworkEnabled(true);
    }

    @Test
    public void setChecked_unchecked_shouldSetCarrierNetworkDisabled() {
        mController.setChecked(false);

        verify(mWifiPickerTrackerHelper).setCarrierNetworkEnabled(false);
    }

    @Test
    public void displayPreference_carrierNetworkActive_showCarrierNetwork() {
        doReturn(true).when(mWifiPickerTrackerHelper).isCarrierNetworkActive();
        doReturn(SSID).when(mWifiPickerTrackerHelper).getCarrierNetworkSsid();

        mController.displayPreference(mScreen);

        assertThat(mController.mCarrierNetworkPreference).isEqualTo(mNetworkPreference);
        assertThat(mNetworkPreference.isVisible()).isTrue();
        assertThat(mNetworkPreference.getSummary()).isEqualTo(SSID);
    }

    @Test
    public void displayPreference_carrierNetworkInactive_hideCarrierNetwork() {
        doReturn(false).when(mWifiPickerTrackerHelper).isCarrierNetworkActive();

        mController.displayPreference(mScreen);

        assertThat(mController.mCarrierNetworkPreference).isEqualTo(mNetworkPreference);
        assertThat(mNetworkPreference.isVisible()).isFalse();
    }

    @Test
    public void onWifiStateChanged_carrierNetworkActive_shouldSetSummary() {
        doReturn(true).when(mWifiPickerTrackerHelper).isCarrierNetworkActive();
        doReturn(SSID).when(mWifiPickerTrackerHelper).getCarrierNetworkSsid();
        mNetworkPreference.setVisible(false);
        mNetworkPreference.setSummary(null);

        mController.onWifiEntriesChanged();

        assertThat(mNetworkPreference.isVisible()).isEqualTo(true);
        assertThat(mNetworkPreference.getSummary()).isEqualTo(SSID);
    }

    @Test
    public void onWifiStateChanged_carrierNetworkInactive_shouldHideNetwork() {
        doReturn(false).when(mWifiPickerTrackerHelper).isCarrierNetworkActive();
        mNetworkPreference.setVisible(true);

        mController.onWifiEntriesChanged();

        assertThat(mNetworkPreference.isVisible()).isEqualTo(false);
    }

    @Test
    public void onWifiEntriesChanged_carrierNetworkActive_shouldSetSummary() {
        doReturn(true).when(mWifiPickerTrackerHelper).isCarrierNetworkActive();
        doReturn(SSID).when(mWifiPickerTrackerHelper).getCarrierNetworkSsid();
        mNetworkPreference.setVisible(false);
        mNetworkPreference.setSummary(null);

        mController.onWifiEntriesChanged();

        assertThat(mNetworkPreference.isVisible()).isEqualTo(true);
        assertThat(mNetworkPreference.getSummary()).isEqualTo(SSID);
    }

    @Test
    public void onWifiEntriesChanged_carrierNetworkInactive_shouldHideNetwork() {
        doReturn(false).when(mWifiPickerTrackerHelper).isCarrierNetworkActive();
        mNetworkPreference.setVisible(true);

        mController.onWifiEntriesChanged();

        assertThat(mNetworkPreference.isVisible()).isEqualTo(false);
    }
}
