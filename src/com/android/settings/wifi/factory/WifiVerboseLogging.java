/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.wifi.factory;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Wi-Fi Verbose Logging
 */
public class WifiVerboseLogging {
    private static final String TAG = "WifiVerboseLogging";

    protected final Context mAppContext;
    protected final WifiManager mWifiManager;
    protected final boolean mIsVerboseLoggingEnabled;

    public WifiVerboseLogging(@NonNull Context appContext, @NonNull WifiManager wifiManager) {
        mAppContext = appContext;
        mWifiManager = wifiManager;
        mIsVerboseLoggingEnabled = wifiManager.isVerboseLoggingEnabled();
        Log.v(TAG, "isVerboseLoggingEnabled:" + mIsVerboseLoggingEnabled);
    }

    /**
     * Send a {@link Log#VERBOSE} log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public void log(@Nullable String tag, @NonNull String msg) {
        if (mIsVerboseLoggingEnabled) {
            Log.v(tag, msg);
        }
    }
}
