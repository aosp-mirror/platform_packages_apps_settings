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
 * limitations under the License
 */

package com.android.settings.wifi.slice;

import android.content.Context;
import android.net.Uri;
import android.os.SystemClock;

import com.android.settings.slices.SliceBackgroundWorker;

/**
 * {@link SliceBackgroundWorker} for Wi-Fi, used by {@link ContextualWifiSlice}.
 */
public class ContextualWifiScanWorker extends WifiScanWorker {

    private static long sVisibleUiSessionToken;
    private static long sActiveSession;

    public ContextualWifiScanWorker(Context context, Uri uri) {
        super(context, uri);
    }

    /**
     * Starts a new visible UI session for the purpose of automatically starting captive portal.
     *
     * A visible UI session is defined as a duration of time when a UI screen is visible to user.
     * Going to a sub-page and coming out breaks the continuation, leaving the page and coming back
     * breaks it too.
     */
    public static void newVisibleUiSession() {
        sVisibleUiSessionToken = SystemClock.elapsedRealtime();
    }

    static void saveSession() {
        sActiveSession = sVisibleUiSessionToken;
    }

    @Override
    protected void clearClickedWifiOnSliceUnpinned() {
        // Do nothing for contextual Wi-Fi slice
    }

    @Override
    protected boolean isSessionValid() {
        if (sVisibleUiSessionToken != sActiveSession) {
            clearClickedWifi();
            return false;
        }
        return true;
    }

    @Override
    protected int getApRowCount() {
        return ContextualWifiSlice.getApRowCount();
    }
}