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

package com.android.settings.slices;

import android.app.PendingIntent;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.net.wifi.WifiManager;

import com.android.settings.R;

import androidx.app.slice.Slice;
import androidx.app.slice.SliceProvider;
import androidx.app.slice.builders.ListBuilder;

public class SettingsSliceProvider extends SliceProvider {
    public static final String SLICE_AUTHORITY = "com.android.settings.slices";

    public static final String PATH_WIFI = "wifi";
    public static final String ACTION_WIFI_CHANGED =
            "com.android.settings.slice.action.WIFI_CHANGED";

    // TODO -- Associate slice URI with search result instead of separate hardcoded thing
    public static Uri getUri(String path) {
        return new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(SLICE_AUTHORITY)
                .appendPath(path).build();
    }

    @Override
    public boolean onCreateSliceProvider() {
        return true;
    }

    @Override
    public Slice onBindSlice(Uri sliceUri) {
        String path = sliceUri.getPath();
        switch (path) {
            case "/" + PATH_WIFI:
                return createWifiSlice(sliceUri);
        }
        throw new IllegalArgumentException("Unrecognized slice uri: " + sliceUri);
    }


    // TODO (b/70622039) remove this when the proper wifi slice is enabled.
    private Slice createWifiSlice(Uri sliceUri) {
        // Get wifi state
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
                wifiEnabled = true;
                break;
            case WifiManager.WIFI_STATE_UNKNOWN:
            default:
                state = ""; // just don't show anything?
                break;
        }

        boolean finalWifiEnabled = wifiEnabled;
        return new ListBuilder(sliceUri)
                .setColor(R.color.material_blue_500)
                .add(b -> b
                        .setTitle(getContext().getString(R.string.wifi_settings))
                        .setTitleItem(Icon.createWithResource(getContext(), R.drawable.wifi_signal))
                        .setSubtitle(state)
                        .addToggle(getBroadcastIntent(ACTION_WIFI_CHANGED), finalWifiEnabled)
                        .setContentIntent(getIntent(Intent.ACTION_MAIN)))
                .build();
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
