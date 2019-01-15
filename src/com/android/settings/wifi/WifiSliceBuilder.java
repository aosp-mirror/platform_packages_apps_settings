/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static android.app.slice.Slice.EXTRA_TOGGLE_STATE;
import static android.provider.SettingsSlicesContract.KEY_WIFI;

import static androidx.slice.builders.ListBuilder.ICON_IMAGE;

import android.annotation.ColorInt;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiSsid;
import android.provider.SettingsSlicesContract;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SubSettings;
import com.android.settings.Utils;
import com.android.settings.search.DatabaseIndexingUtils;
import com.android.settings.slices.SliceBroadcastReceiver;
import com.android.settings.slices.SliceBuilderUtils;

import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;

import androidx.core.graphics.drawable.IconCompat;
import android.text.TextUtils;

/**
 * Utility class to build a Wifi Slice, and handle all associated actions.
 */
public class WifiSliceBuilder {

    /**
     * Backing Uri for the Wifi Slice.
     */
    public static final Uri WIFI_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSlicesContract.AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath(KEY_WIFI)
            .build();

    /**
     * Action notifying a change on the Wifi Slice.
     */
    public static final String ACTION_WIFI_SLICE_CHANGED =
            "com.android.settings.wifi.action.WIFI_CHANGED";

    public static final IntentFilter INTENT_FILTER = new IntentFilter();

    static {
        INTENT_FILTER.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        INTENT_FILTER.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
    }

    private WifiSliceBuilder() {
    }

    /**
     * Return a Wifi Slice bound to {@link #WIFI_URI}.
     * <p>
     * Note that you should register a listener for {@link #INTENT_FILTER} to get changes for Wifi.
     */
    public static Slice getSlice(Context context) {
        final boolean isWifiEnabled = isWifiEnabled(context);
        final IconCompat icon = IconCompat.createWithResource(context,
                R.drawable.ic_settings_wireless);
        final String title = context.getString(R.string.wifi_settings);
        final CharSequence summary = getSummary(context);
        @ColorInt final int color = Utils.getColorAccent(context);
        final PendingIntent toggleAction = getBroadcastIntent(context);
        final PendingIntent primaryAction = getPrimaryAction(context);
        final SliceAction primarySliceAction = new SliceAction(primaryAction, icon, title);
        final SliceAction toggleSliceAction = new SliceAction(toggleAction, null /* actionTitle */,
                isWifiEnabled);

        return new ListBuilder(context, WIFI_URI, ListBuilder.INFINITY)
                .setAccentColor(color)
                .addRow(b -> b
                        .setTitle(title)
                        .setSubtitle(summary)
                        .addEndItem(toggleSliceAction)
                        .setPrimaryAction(primarySliceAction))
                .build();
    }

    /**
     * Update the current wifi status to the boolean value keyed by
     * {@link android.app.slice.Slice#EXTRA_TOGGLE_STATE} on {@param intent}.
     */
    public static void handleUriChange(Context context, Intent intent) {
        final WifiManager wifiManager = context.getSystemService(WifiManager.class);
        final boolean newState = intent.getBooleanExtra(EXTRA_TOGGLE_STATE,
                wifiManager.isWifiEnabled());
        wifiManager.setWifiEnabled(newState);
        // Do not notifyChange on Uri. The service takes longer to update the current value than it
        // does for the Slice to check the current value again. Let {@link SliceBroadcastRelay}
        // handle it.
    }

    public static Intent getIntent(Context context) {
        final String screenTitle = context.getText(R.string.wifi_settings).toString();
        final Uri contentUri = new Uri.Builder().appendPath(KEY_WIFI).build();
        final Intent intent = DatabaseIndexingUtils.buildSearchResultPageIntent(context,
                WifiSettings.class.getName(), KEY_WIFI, screenTitle,
                MetricsEvent.DIALOG_WIFI_AP_EDIT)
                .setClassName(context.getPackageName(), SubSettings.class.getName())
                .setData(contentUri);

        return intent;
    }

    private static boolean isWifiEnabled(Context context) {
        final WifiManager wifiManager = context.getSystemService(WifiManager.class);

        switch (wifiManager.getWifiState()) {
            case WifiManager.WIFI_STATE_ENABLED:
            case WifiManager.WIFI_STATE_ENABLING:
                return true;
            case WifiManager.WIFI_STATE_DISABLED:
            case WifiManager.WIFI_STATE_DISABLING:
            case WifiManager.WIFI_STATE_UNKNOWN:
            default:
                return false;
        }
    }

    private static CharSequence getSummary(Context context) {
        final WifiManager wifiManager = context.getSystemService(WifiManager.class);

        switch (wifiManager.getWifiState()) {
            case WifiManager.WIFI_STATE_ENABLED:
                final String ssid = WifiInfo.removeDoubleQuotes(wifiManager.getConnectionInfo()
                        .getSSID());
                if (TextUtils.equals(ssid, WifiSsid.NONE)) {
                    return context.getText(R.string.disconnected);
                }
                return ssid;
            case WifiManager.WIFI_STATE_ENABLING:
                return context.getText(R.string.disconnected);
            case WifiManager.WIFI_STATE_DISABLED:
            case WifiManager.WIFI_STATE_DISABLING:
                return context.getText(R.string.switch_off_text);
            case WifiManager.WIFI_STATE_UNKNOWN:
            default:
                return "";
        }
    }

    private static PendingIntent getPrimaryAction(Context context) {
        final Intent intent = getIntent(context);
        return PendingIntent.getActivity(context, 0 /* requestCode */,
                intent, 0 /* flags */);
    }

    private static PendingIntent getBroadcastIntent(Context context) {
        final Intent intent = new Intent(ACTION_WIFI_SLICE_CHANGED);
        intent.setClass(context, SliceBroadcastReceiver.class);
        return PendingIntent.getBroadcast(context, 0 /* requestCode */, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }
}
