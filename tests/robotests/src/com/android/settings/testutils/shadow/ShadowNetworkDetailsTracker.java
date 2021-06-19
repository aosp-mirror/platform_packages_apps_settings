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

package com.android.settings.testutils.shadow;

import static org.mockito.Mockito.mock;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkScoreManager;
import android.net.wifi.WifiManager;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;

import com.android.wifitrackerlib.NetworkDetailsTracker;
import com.android.wifitrackerlib.WifiEntry;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.time.Clock;

@Implements(NetworkDetailsTracker.class)
public class ShadowNetworkDetailsTracker {

    @Implementation
    public static NetworkDetailsTracker createNetworkDetailsTracker(@NonNull Lifecycle lifecycle,
            @NonNull Context context,
            @NonNull WifiManager wifiManager,
            @NonNull ConnectivityManager connectivityManager,
            @NonNull NetworkScoreManager networkScoreManager,
            @NonNull Handler mainHandler,
            @NonNull Handler workerHandler,
            @NonNull Clock clock,
            long maxScanAgeMillis,
            long scanIntervalMillis,
            String key) {
        return mock(NetworkDetailsTracker.class);
    }

    @Implementation
    public WifiEntry getWifiEntry() {
        return mock(WifiEntry.class);
    }
}
