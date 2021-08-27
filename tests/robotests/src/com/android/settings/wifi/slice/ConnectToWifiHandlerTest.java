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

package com.android.settings.wifi.slice;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;

import com.android.settings.wifi.WifiDialogActivity;
import com.android.wifitrackerlib.WifiEntry;
import com.android.wifitrackerlib.WifiEntry.ConnectCallback;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ConnectToWifiHandlerTest {

    private static final String AP_SSID = "\"ap\"";
    private Context mContext;
    private ConnectToWifiHandler mHandler;
    @Mock
    private WifiScanWorker mWifiScanWorker;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        mHandler = spy(new ConnectToWifiHandler());
        doReturn(mWifiScanWorker).when(mHandler).getWifiScanWorker(any());
    }

    @Test
    public void onReceive_nonNullKeyAndUri_shouldConnectWifintry() {
        final Intent intent = new Intent();
        final String key = "key";
        intent.putExtra(ConnectToWifiHandler.KEY_CHOSEN_WIFIENTRY_KEY, key);
        intent.putExtra(ConnectToWifiHandler.KEY_WIFI_SLICE_URI,
                com.android.settings.slices.CustomSliceRegistry.WIFI_SLICE_URI);
        final WifiEntry wifiEntry = mock(WifiEntry.class);
        when(mWifiScanWorker.getWifiEntry(key)).thenReturn(wifiEntry);

        mHandler.onReceive(mContext, intent);

        verify(wifiEntry).connect(any());
    }

    @Test
    public void onConnectResult_failNoConfig_shouldStartActivity() {
        final String key = "key";
        final WifiEntry wifiEntry = mock(WifiEntry.class);
        when(wifiEntry.getKey()).thenReturn(key);
        final ConnectToWifiHandler.WifiEntryConnectCallback callback =
                spy(new ConnectToWifiHandler.WifiEntryConnectCallback(mContext, wifiEntry));

        callback.onConnectResult(ConnectCallback.CONNECT_STATUS_FAILURE_NO_CONFIG);

        final ArgumentCaptor<Intent> argument = ArgumentCaptor.forClass(Intent.class);
        verify(mContext).startActivity(argument.capture());
        assertThat(argument.getValue().getStringExtra(WifiDialogActivity.KEY_CHOSEN_WIFIENTRY_KEY))
                .isEqualTo(key);
        assertThat(argument.getValue().getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK)
                .isEqualTo(Intent.FLAG_ACTIVITY_NEW_TASK);
    }
}
