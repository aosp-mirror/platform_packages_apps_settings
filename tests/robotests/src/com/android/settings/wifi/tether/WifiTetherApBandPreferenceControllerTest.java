/*
 * Copyright (C) 2017 The Android Open Source Project
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
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class WifiTetherApBandPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private ConnectivityManager mConnectivityManager;
    @Mock
    private WifiManager mWifiManager;
    @Mock
    private WifiTetherBasePreferenceController.OnTetherConfigUpdateListener mListener;
    @Mock
    private PreferenceScreen mScreen;

    private WifiTetherApBandPreferenceController mController;
    private ListPreference mListPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mListPreference = new ListPreference(RuntimeEnvironment.application);
        when(mContext.getSystemService(Context.WIFI_SERVICE)).thenReturn(mWifiManager);
        when(mContext.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenReturn(mConnectivityManager);
        when(mConnectivityManager.getTetherableWifiRegexs()).thenReturn(new String[]{"1", "2"});
        when(mContext.getResources()).thenReturn(RuntimeEnvironment.application.getResources());
        when(mScreen.findPreference(anyString())).thenReturn(mListPreference);

        mController = new WifiTetherApBandPreferenceController(mContext, mListener);
    }

    @Test
    public void display_5GhzSupported_shouldDisplayFullList() {
        when(mWifiManager.getCountryCode()).thenReturn("US");
        when(mWifiManager.isDualBandSupported()).thenReturn(true);

        mController.displayPreference(mScreen);

        assertThat(mListPreference.getEntries().length).isEqualTo(2);
    }

    @Test
    public void display_noCountryCode_shouldDisable() {
        when(mWifiManager.getCountryCode()).thenReturn(null);
        when(mWifiManager.isDualBandSupported()).thenReturn(true);

        mController.displayPreference(mScreen);

        assertThat(mListPreference.getEntries()).isNull();
        assertThat(mListPreference.isEnabled()).isFalse();
        assertThat(mListPreference.getSummary())
                .isEqualTo(RuntimeEnvironment.application.getString(R.string.wifi_ap_choose_2G));
    }

    @Test
    public void display_5GhzNotSupported_shouldDisable() {
        when(mWifiManager.isDualBandSupported()).thenReturn(false);

        mController.displayPreference(mScreen);

        assertThat(mListPreference.getEntries()).isNull();
        assertThat(mListPreference.isEnabled()).isFalse();
        assertThat(mListPreference.getSummary())
                .isEqualTo(RuntimeEnvironment.application.getString(R.string.wifi_ap_choose_2G));
    }

    @Test
    public void changePreference_shouldUpdateValue() {
        when(mWifiManager.is5GHzBandSupported()).thenReturn(true);

        mController.displayPreference(mScreen);
        mController.onPreferenceChange(mListPreference, "1");
        assertThat(mController.getBandIndex()).isEqualTo(1);

        mController.onPreferenceChange(mListPreference, "0");
        assertThat(mController.getBandIndex()).isEqualTo(0);

        verify(mListener, times(2)).onTetherConfigUpdated();
    }
}
