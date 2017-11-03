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
 * limitations under the License
 */

package com.android.settings;

import android.app.PendingIntent;
import android.app.slice.Slice;
import android.app.slice.SliceProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

public class SettingsSliceProvider extends SliceProvider {
    public static final String SLICE_AUTHORITY = "com.android.settings.slices";

    public static final String PATH_WIFI = "wifi";
    public static final String ACTION_WIFI_CHANGED =
            "com.android.settings.slice.action.WIFI_CHANGED";
    // TODO -- Associate slice URI with search result instead of separate hardcoded thing
    public static final String[] WIFI_SEARCH_TERMS = {"wi-fi", "wifi", "internet"};

    public static Uri getUri(String path) {
        return new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(SLICE_AUTHORITY)
                .appendPath(path).build();
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Slice onBindSlice(Uri sliceUri) {
        String path = sliceUri.getPath();
        switch (path) {
            case "/" + PATH_WIFI:
                return createWifi(sliceUri);

        }
        throw new IllegalArgumentException("Unrecognized slice uri: " + sliceUri);
    }

    private Slice createWifi(Uri uri) {
        // Get wifi state
        String[] toggleHints;
        WifiManager wifiManager = (WifiManager) getContext().getSystemService(Context.WIFI_SERVICE);
        int wifiState = wifiManager.getWifiState();
        boolean wifiEnabled = false;
        String state;
        switch (wifiState) {
            case WifiManager.WIFI_STATE_DISABLED:
            case WifiManager.WIFI_STATE_DISABLING:
                state = getContext().getString(R.string.disconnected);
                break;
            case WifiManager.WIFI_STATE_ENABLED:
            case WifiManager.WIFI_STATE_ENABLING:
                state = wifiManager.getConnectionInfo().getSSID();
                WifiInfo.removeDoubleQuotes(state);
                wifiEnabled = true;
                break;
            case WifiManager.WIFI_STATE_UNKNOWN:
            default:
                state = ""; // just don't show anything?
                break;
        }
        if (wifiEnabled) {
            toggleHints = new String[] {Slice.HINT_TOGGLE, Slice.HINT_SELECTED};
        } else {
            toggleHints = new String[] {Slice.HINT_TOGGLE};
        }
        // Construct the slice
        Slice.Builder b = new Slice.Builder(uri);
        b.addSubSlice(new Slice.Builder(b)
                .addAction(getIntent("android.settings.WIFI_SETTINGS"),
                        new Slice.Builder(b)
                                .addText(getContext().getString(R.string.wifi_settings), null)
                                .addText(state, null)
                                .addIcon(Icon.createWithResource(getContext(),
                                        R.drawable.ic_settings_wireless), null, Slice.HINT_HIDDEN)
                                .addHints(Slice.HINT_TITLE)
                                .build())
                .addAction(getBroadcastIntent(ACTION_WIFI_CHANGED),
                        new Slice.Builder(b)
                                .addHints(toggleHints)
                                .build())
                .build());
        return b.build();
    }

    private PendingIntent getIntent(String action) {
        Intent intent = new Intent(action);
        PendingIntent pi = PendingIntent.getActivity(getContext(), 0, intent, 0);
        return pi;
    }

    private PendingIntent getBroadcastIntent(String action) {
        Intent intent = new Intent(action);
        intent.setClass(getContext(), SliceBroadcastReceiver.class);
        return PendingIntent.getBroadcast(getContext(), 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }
}
