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

package com.android.settings.wifi.tether;

import static org.mockito.ArgumentMatchers.anyString;;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.widget.FooterPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class WifiTetherFooterPreferenceControllerTest {
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private ConnectivityManager mConnectivityManager;
    @Mock
    private WifiManager mWifiManager;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private FooterPreference mPreference;

    private WifiTetherFooterPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(mContext.getSystemService(Context.WIFI_SERVICE)).thenReturn(mWifiManager);
        when(mContext.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenReturn(mConnectivityManager);
        when(mConnectivityManager.getTetherableWifiRegexs()).thenReturn(new String[]{"1", "2"});
        mController = new WifiTetherFooterPreferenceController(mContext);
        when(mScreen.findPreference(anyString())).thenReturn(mPreference);
    }

    @Test
    public void displayPreference_supportStaAp_showConcurrentInfo() {
        when(mWifiManager.isStaApConcurrencySupported()).thenReturn(true);

        mController.displayPreference(mScreen);

        verify(mPreference, never()).setTitle(R.string.tethering_footer_info);
        verify(mPreference).setTitle(R.string.tethering_footer_info_sta_ap_concurrency);
    }

    @Test
    public void displayPreference_notSupportStaAp_showNotConcurrentInfo() {
        when(mWifiManager.isStaApConcurrencySupported()).thenReturn(false);

        mController.displayPreference(mScreen);

        verify(mPreference).setTitle(R.string.tethering_footer_info);
        verify(mPreference, never()).setTitle(R.string.tethering_footer_info_sta_ap_concurrency);
    }
}
