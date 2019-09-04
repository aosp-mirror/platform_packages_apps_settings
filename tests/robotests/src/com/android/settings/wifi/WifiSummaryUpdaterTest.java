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

package com.android.settings.wifi;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;

import com.android.settings.R;
import com.android.settings.widget.SummaryUpdater.OnSummaryChangeListener;
import com.android.settingslib.wifi.WifiStatusTracker;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class WifiSummaryUpdaterTest {
    @Mock private WifiStatusTracker mWifiTracker;
    @Mock private SummaryListener mListener;

    private Context mContext;
    private WifiSummaryUpdater mSummaryUpdater;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application.getApplicationContext());
        mSummaryUpdater = new WifiSummaryUpdater(mContext, mListener, mWifiTracker);
    }

    @Test
    public void register_true_shouldRegisterListenerAndTracker() {
        mSummaryUpdater.register(true);

        verify(mContext).registerReceiver(any(BroadcastReceiver.class), any(IntentFilter.class));
        verify(mWifiTracker).setListening(true);
    }

    @Test
    public void register_false_shouldUnregisterListenerAndTracker() {
        mSummaryUpdater.register(true);
        mSummaryUpdater.register(false);

        verify(mContext).unregisterReceiver(any(BroadcastReceiver.class));
        verify(mWifiTracker).setListening(false);
    }

    @Test
    public void onReceive_networkStateChanged_shouldSendSummaryChange() {
        mSummaryUpdater.register(true);
        mContext.sendBroadcast(new Intent(WifiManager.NETWORK_STATE_CHANGED_ACTION));

        verify(mListener).onSummaryChanged(anyString());
    }

    @Test
    public void onReceive_rssiChanged_shouldSendSummaryChange() {
        mSummaryUpdater.register(true);
        mContext.sendBroadcast(new Intent(WifiManager.RSSI_CHANGED_ACTION));

        verify(mListener).onSummaryChanged(anyString());
    }

    @Test
    public void getSummary_wifiDisabled_shouldReturnDisabled() {
        mWifiTracker.enabled = false;

        assertThat(mSummaryUpdater.getSummary())
            .isEqualTo(mContext.getString(R.string.switch_off_text));
    }

    @Test
    public void getSummary_wifiDisconnected_shouldReturnDisconnected() {
        mWifiTracker.enabled = true;
        mWifiTracker.connected = false;

        assertThat(mSummaryUpdater.getSummary())
            .isEqualTo(mContext.getString(R.string.disconnected));
    }

    @Test
    public void getSummary_wifiConnected_shouldReturnSsid() {
        mWifiTracker.enabled = true;
        mWifiTracker.connected = true;
        mWifiTracker.ssid = "Test Ssid";

        assertThat(mSummaryUpdater.getSummary()).isEqualTo("Test Ssid");
    }

    @Test
    public void getSummary_wifiConnected_withSpeedLabel_shouldReturnSsid_withSpeedLabel() {
        mWifiTracker.enabled = true;
        mWifiTracker.connected = true;
        mWifiTracker.ssid = "Test Ssid";
        mWifiTracker.statusLabel = "Very Fast";

        assertThat(mSummaryUpdater.getSummary()).isEqualTo("Test Ssid / Very Fast");
    }

    private class SummaryListener implements OnSummaryChangeListener {
        private String summary;

        @Override
        public void onSummaryChanged(String summary) {
            this.summary = summary;
        }
    }
}
