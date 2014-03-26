/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.quicksettings;

import static com.android.internal.util.cm.QSConstants.TILES_DEFAULT;
import static com.android.internal.util.cm.QSConstants.TILE_AIRPLANE;
import static com.android.internal.util.cm.QSConstants.TILE_AUTOROTATE;
import static com.android.internal.util.cm.QSConstants.TILE_BATTERY;
import static com.android.internal.util.cm.QSConstants.TILE_BLUETOOTH;
import static com.android.internal.util.cm.QSConstants.TILE_BRIGHTNESS;
import static com.android.internal.util.cm.QSConstants.TILE_CAMERA;
import static com.android.internal.util.cm.QSConstants.TILE_DELIMITER;
import static com.android.internal.util.cm.QSConstants.TILE_EXPANDEDDESKTOP;
import static com.android.internal.util.cm.QSConstants.TILE_GPS;
import static com.android.internal.util.cm.QSConstants.TILE_LOCKSCREEN;
import static com.android.internal.util.cm.QSConstants.TILE_LTE;
import static com.android.internal.util.cm.QSConstants.TILE_MOBILEDATA;
import static com.android.internal.util.cm.QSConstants.TILE_NETWORKMODE;
import static com.android.internal.util.cm.QSConstants.TILE_NFC;
import static com.android.internal.util.cm.QSConstants.TILE_RINGER;
import static com.android.internal.util.cm.QSConstants.TILE_SCREENTIMEOUT;
import static com.android.internal.util.cm.QSConstants.TILE_SETTINGS;
import static com.android.internal.util.cm.QSConstants.TILE_SLEEP;
import static com.android.internal.util.cm.QSConstants.TILE_SYNC;
import static com.android.internal.util.cm.QSConstants.TILE_TORCH;
import static com.android.internal.util.cm.QSConstants.TILE_USER;
import static com.android.internal.util.cm.QSConstants.TILE_VOLUME;
import static com.android.internal.util.cm.QSConstants.TILE_WIFI;
import static com.android.internal.util.cm.QSConstants.TILE_WIFIAP;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.telephony.Phone;
import com.android.internal.util.cm.QSUtils;
import com.android.settings.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class QuickSettingsUtil {
    private static final String TAG = "QuickSettingsUtil";

    public static final Map<String, TileInfo> TILES;

    private static final Map<String, TileInfo> ENABLED_TILES = new HashMap<String, TileInfo>();
    private static final Map<String, TileInfo> DISABLED_TILES = new HashMap<String, TileInfo>();

    static {
        TILES = Collections.unmodifiableMap(ENABLED_TILES);
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_AIRPLANE, R.string.title_tile_airplane,
                "com.android.systemui:drawable/ic_qs_airplane_on"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_BATTERY, R.string.title_tile_battery,
                "com.android.systemui:drawable/ic_qs_battery_neutral"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_BLUETOOTH, R.string.title_tile_bluetooth,
                "com.android.systemui:drawable/ic_qs_bluetooth_on"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_BRIGHTNESS, R.string.title_tile_brightness,
                "com.android.systemui:drawable/ic_qs_brightness_auto_off"));
        registerTile(new QuickSettingsUtil.TileInfo(
                 TILE_CAMERA, R.string.title_tile_camera,
                "com.android.systemui:drawable/ic_qs_camera"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_EXPANDEDDESKTOP, R.string.title_tile_expanded_desktop,
                "com.android.systemui:drawable/ic_qs_expanded_desktop_neutral"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_SLEEP, R.string.title_tile_power,
                "com.android.systemui:drawable/ic_qs_power"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_GPS, R.string.title_tile_gps,
                "com.android.systemui:drawable/ic_qs_location_on"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_LOCKSCREEN, R.string.title_tile_lockscreen,
                "com.android.systemui:drawable/ic_qs_lock_screen_on"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_LTE, R.string.title_tile_lte,
                "com.android.systemui:drawable/ic_qs_lte_on"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_MOBILEDATA, R.string.title_tile_mobiledata,
                "com.android.systemui:drawable/ic_qs_signal_full_4"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_NETWORKMODE, R.string.title_tile_networkmode,
                "com.android.systemui:drawable/ic_qs_2g3g_on"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_NFC, R.string.title_tile_nfc,
                "com.android.systemui:drawable/ic_qs_nfc_on"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_AUTOROTATE, R.string.title_tile_autorotate,
                "com.android.systemui:drawable/ic_qs_auto_rotate"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_SCREENTIMEOUT, R.string.title_tile_screen_timeout,
                "com.android.systemui:drawable/ic_qs_screen_timeout_on"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_SETTINGS, R.string.title_tile_settings,
                "com.android.systemui:drawable/ic_qs_settings"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_RINGER, R.string.title_tile_sound,
                "com.android.systemui:drawable/ic_qs_ring_on"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_SYNC, R.string.title_tile_sync,
                "com.android.systemui:drawable/ic_qs_sync_on"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_TORCH, R.string.title_tile_torch,
                "com.android.systemui:drawable/ic_qs_torch_on"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_USER, R.string.title_tile_user,
                "com.android.systemui:drawable/ic_qs_default_user"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_VOLUME, R.string.title_tile_volume,
                "com.android.systemui:drawable/ic_qs_volume"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_WIFI, R.string.title_tile_wifi,
                "com.android.systemui:drawable/ic_qs_wifi_full_4"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_WIFIAP, R.string.title_tile_wifiap,
                "com.android.systemui:drawable/ic_qs_wifi_ap_on"));
    }

    private static void registerTile(QuickSettingsUtil.TileInfo info) {
        ENABLED_TILES.put(info.getId(), info);
    }

    private static void removeTile(String id) {
        ENABLED_TILES.remove(id);
        DISABLED_TILES.remove(id);
        TILES_DEFAULT.remove(id);
    }

    private static void disableTile(String id) {
        if (ENABLED_TILES.containsKey(id)) {
            DISABLED_TILES.put(id, ENABLED_TILES.remove(id));
        }
    }

    private static void enableTile(String id) {
        if (DISABLED_TILES.containsKey(id)) {
            ENABLED_TILES.put(id, DISABLED_TILES.remove(id));
        }
    }

    private static synchronized void removeUnsupportedTiles(Context context) {
        // Don't show mobile data options if not supported
        if (!QSUtils.deviceSupportsMobileData(context)) {
            removeTile(TILE_MOBILEDATA);
            removeTile(TILE_WIFIAP);
            removeTile(TILE_NETWORKMODE);
        }

        // Don't show the bluetooth options if not supported
        if (!QSUtils.deviceSupportsBluetooth()) {
            removeTile(TILE_BLUETOOTH);
        }

        // Don't show the NFC tile if not supported
        if (!QSUtils.deviceSupportsNfc(context)) {
            removeTile(TILE_NFC);
        }

        // Don't show the LTE tile if not supported
        if (!QSUtils.deviceSupportsLte(context)) {
            removeTile(TILE_LTE);
        }

        // Don't show the Torch tile if not supported
        if (!QSUtils.deviceSupportsTorch(context)) {
            removeTile(TILE_TORCH);
        }

        // Don't show the Camera tile if the device has no cameras
        if (!QSUtils.deviceSupportsCamera()) {
            removeTile(TILE_CAMERA);
        }
    }

    private static synchronized void refreshAvailableTiles(Context context) {
        ContentResolver resolver = context.getContentResolver();

        // Some phones run on networks not supported by the networkmode tile,
        // so make it available only where supported
        int networkState = -99;
        try {
            networkState = Settings.Global.getInt(resolver,
                    Settings.Global.PREFERRED_NETWORK_MODE);
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "Unable to retrieve PREFERRED_NETWORK_MODE", e);
        }

        switch (networkState) {
            // list of supported network modes
            case Phone.NT_MODE_WCDMA_PREF:
            case Phone.NT_MODE_WCDMA_ONLY:
            case Phone.NT_MODE_GSM_UMTS:
            case Phone.NT_MODE_GSM_ONLY:
                enableTile(TILE_NETWORKMODE);
                break;
            default:
                disableTile(TILE_NETWORKMODE);
                break;
        }

        // Don't show the Expanded desktop tile if expanded desktop is disabled
        if (QSUtils.expandedDesktopEnabled(resolver)) {
            enableTile(TILE_EXPANDEDDESKTOP);
        } else {
            disableTile(TILE_EXPANDEDDESKTOP);
        }

    }

    public static synchronized void updateAvailableTiles(Context context) {
        removeUnsupportedTiles(context);
        refreshAvailableTiles(context);
    }

    public static boolean isTileAvailable(String id) {
        return ENABLED_TILES.containsKey(id);
    }

    public static String getCurrentTiles(Context context, boolean isRibbon) {
        String tiles = Settings.System.getString(context.getContentResolver(),
                isRibbon ? Settings.System.QUICK_SETTINGS_RIBBON_TILES
                         : Settings.System.QUICK_SETTINGS_TILES);
        if (tiles == null) {
            tiles = getDefaultTiles(context);
        }
        return tiles;
    }

    public static void saveCurrentTiles(Context context, String tiles, boolean isRibbon) {
        Settings.System.putString(context.getContentResolver(),
                isRibbon ? Settings.System.QUICK_SETTINGS_RIBBON_TILES
                         : Settings.System.QUICK_SETTINGS_TILES, tiles);
    }

    public static void resetTiles(Context context, boolean isRibbon) {
        String defaultTiles = getDefaultTiles(context);
        Settings.System.putString(context.getContentResolver(),
                isRibbon ? Settings.System.QUICK_SETTINGS_RIBBON_TILES
                         : Settings.System.QUICK_SETTINGS_TILES, defaultTiles);
    }

    public static String mergeInNewTileString(String oldString, String newString) {
        ArrayList<String> oldList = getTileListFromString(oldString);
        ArrayList<String> newList = getTileListFromString(newString);
        ArrayList<String> mergedList = new ArrayList<String>();

        // add any items from oldlist that are in new list
        for (String tile : oldList) {
            if (newList.contains(tile)) {
                mergedList.add(tile);
            }
        }

        // append anything in newlist that isn't already in the merged list to
        // the end of the list
        for (String tile : newList) {
            if (!mergedList.contains(tile)) {
                mergedList.add(tile);
            }
        }

        // return merged list
        return getTileStringFromList(mergedList);
    }

    public static ArrayList<String> getTileListFromString(String tiles) {
        return new ArrayList<String>(Arrays.asList(tiles.split("\\|")));
    }

    public static String getTileStringFromList(ArrayList<String> tiles) {
        if (tiles == null || tiles.size() <= 0) {
            return "";
        } else {
            String s = tiles.get(0);
            for (int i = 1; i < tiles.size(); i++) {
                s += TILE_DELIMITER + tiles.get(i);
            }
            return s;
        }
    }

    public static String getDefaultTiles(Context context) {
        removeUnsupportedTiles(context);
        return TextUtils.join(TILE_DELIMITER, TILES_DEFAULT);
    }

    public static class TileInfo {
        private String mId;
        private int mTitleResId;
        private String mIcon;

        public TileInfo(String id, int titleResId, String icon) {
            mId = id;
            mTitleResId = titleResId;
            mIcon = icon;
        }

        public String getId() {
            return mId;
        }

        public int getTitleResId() {
            return mTitleResId;
        }

        public String getIcon() {
            return mIcon;
        }
    }
}
