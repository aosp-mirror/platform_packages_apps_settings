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

package com.android.settings.mahdi.quicksettings;

import static com.android.internal.util.mahdi.QSConstants.TILES_DEFAULT;
import static com.android.internal.util.mahdi.QSConstants.TILE_AIRPLANE;
import static com.android.internal.util.mahdi.QSConstants.TILE_AUTOROTATE;
import static com.android.internal.util.mahdi.QSConstants.TILE_BATTERY;
import static com.android.internal.util.mahdi.QSConstants.TILE_BLUETOOTH;
import static com.android.internal.util.mahdi.QSConstants.TILE_BRIGHTNESS;
import static com.android.internal.util.mahdi.QSConstants.TILE_CAMERA;
import static com.android.internal.util.mahdi.QSConstants.TILE_CONTACT;
import static com.android.internal.util.mahdi.QSConstants.TILE_CUSTOM;
import static com.android.internal.util.mahdi.QSConstants.TILE_CUSTOM_KEY;
import static com.android.internal.util.mahdi.QSConstants.TILE_CUSTOM_DELIMITER;
import static com.android.internal.util.mahdi.QSConstants.TILE_DELIMITER;
import static com.android.internal.util.mahdi.QSConstants.TILE_IMMERSIVEMODE;
import static com.android.internal.util.mahdi.QSConstants.TILE_LOCATION;
import static com.android.internal.util.mahdi.QSConstants.TILE_LOCKSCREEN;
import static com.android.internal.util.mahdi.QSConstants.TILE_LTE;
import static com.android.internal.util.mahdi.QSConstants.TILE_MOBILEDATA;
import static com.android.internal.util.mahdi.QSConstants.TILE_NETWORKADB;
import static com.android.internal.util.mahdi.QSConstants.TILE_NETWORKMODE;
import static com.android.internal.util.mahdi.QSConstants.TILE_NFC;
import static com.android.internal.util.mahdi.QSConstants.TILE_PROFILE;
import static com.android.internal.util.mahdi.QSConstants.TILE_QUICKRECORD;
import static com.android.internal.util.mahdi.QSConstants.TILE_QUIETHOURS;
import static com.android.internal.util.mahdi.QSConstants.TILE_RINGER;
import static com.android.internal.util.mahdi.QSConstants.TILE_SCREENTIMEOUT;
import static com.android.internal.util.mahdi.QSConstants.TILE_SETTINGS;
import static com.android.internal.util.mahdi.QSConstants.TILE_SLEEP;
import static com.android.internal.util.mahdi.QSConstants.TILE_SYNC;
import static com.android.internal.util.mahdi.QSConstants.TILE_TORCH;
import static com.android.internal.util.mahdi.QSConstants.TILE_USER;
import static com.android.internal.util.mahdi.QSConstants.TILE_VOLUME;
import static com.android.internal.util.mahdi.QSConstants.TILE_WIFI;
import static com.android.internal.util.mahdi.QSConstants.TILE_WIFIAP;
import static com.android.internal.util.mahdi.QSConstants.TILE_NAVBAR;
import static com.android.internal.util.mahdi.QSConstants.TILE_NETWORKSPEED;
import static com.android.internal.util.mahdi.QSConstants.TILE_MUSIC;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.telephony.Phone;
import com.android.internal.util.mahdi.QSUtils;
import com.android.internal.util.mahdi.DeviceUtils;
import com.android.settings.R;

import java.io.File;
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
                TILE_CONTACT, R.string.title_tile_contact,
                "com.android.systemui:drawable/ic_qs_default_user"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_CUSTOM, R.string.title_tile_custom,
                "com.android.systemui:drawable/ic_qs_settings"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_IMMERSIVEMODE, R.string.title_tile_immersive_mode,
                "com.android.systemui:drawable/ic_qs_immersive_mode_on"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_SLEEP, R.string.title_tile_power,
                "com.android.systemui:drawable/ic_qs_power"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_LOCATION, R.string.title_tile_location,
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
                TILE_PROFILE, R.string.title_tile_profile,
                "com.android.systemui:drawable/ic_qs_profiles"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_QUICKRECORD, R.string.title_tile_quick_record,
                "com.android.systemui:drawable/ic_qs_quickrecord"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_QUIETHOURS, R.string.title_tile_quiet_hours,
                "com.android.systemui:drawable/ic_qs_quiet_hours_on"));
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
                "com.android.systemui:drawable/ic_qs_wifi_ap_neutral"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_NETWORKADB, R.string.title_tile_network_adb,
                "com.android.systemui:drawable/ic_qs_network_adb_on"));
	registerTile(new QuickSettingsUtil.TileInfo(
                TILE_NAVBAR, R.string.title_tile_navbar,
                "com.android.systemui:drawable/ic_qs_navbar_on"));
	registerTile(new QuickSettingsUtil.TileInfo(
                TILE_NETWORKSPEED, R.string.title_tile_network_speed,
                "com.android.systemui:drawable/ic_qs_network_speed_on"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_MUSIC, R.string.title_tile_music,
                "com.android.systemui:drawable/ic_qs_media_play"));
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
        if (!context.getResources().getBoolean(R.bool.has_led_flash)) {
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

        // Don't show the profiles tile if profiles are disabled
        if (QSUtils.systemProfilesEnabled(resolver)) {
            enableTile(TILE_PROFILE);
        } else {
            disableTile(TILE_PROFILE);
        }

        // Don't show the Expanded desktop tile if expanded desktop is disabled
        if (QSUtils.globalImmersiveModeEnabled(resolver)) {
            enableTile(TILE_IMMERSIVEMODE);
        } else {
            disableTile(TILE_IMMERSIVEMODE);
        }

        // Don't show the Network ADB tile if adb debugging is disabled
        if (QSUtils.adbEnabled(resolver)) {
            enableTile(TILE_NETWORKADB);
        } else {
            disableTile(TILE_NETWORKADB);
        }
    }

    public static synchronized void updateAvailableTiles(Context context) {
        removeUnsupportedTiles(context);
        refreshAvailableTiles(context);
    }

    public static boolean isTileAvailable(String id) {
        return ENABLED_TILES.containsKey(id);
    }

    public static String getCurrentTiles(Context context) {
        String tiles = Settings.System.getString(context.getContentResolver(),
                Settings.System.QUICK_SETTINGS_TILES);
        if (tiles == null) {
            tiles = getDefaultTiles(context);
        }
        return tiles;
    }

    public static void saveCurrentTiles(Context context, String tiles) {
        Settings.System.putString(context.getContentResolver(),
                Settings.System.QUICK_SETTINGS_TILES, tiles);
    }

    public static void resetTiles(Context context) {
        Settings.System.putString(context.getContentResolver(),
                Settings.System.QUICK_SETTINGS_TILES, null);
        Settings.System.putString(context.getContentResolver(),
                Settings.System.CUSTOM_TOGGLE_EXTRAS, null);
        Settings.System.putString(context.getContentResolver(),
                Settings.System.CUSTOM_TOGGLE_ACTIONS, null);
        Settings.System.putString(context.getContentResolver(),
                Settings.System.TILE_CONTACT_ACTIONS, null);
    }

    public static void deleteCustomTile(Context context, String tileKey) {
        String tileExtra = getCustomExtras(context,
                Settings.System.CUSTOM_TOGGLE_EXTRAS, tileKey);
        if (tileExtra != null) {
            String[] getResolvedIcon = tileExtra.split(TILE_CUSTOM_DELIMITER);
            if (getResolvedIcon.length > 2) {
                deleteCustomIcon(getResolvedIcon[1]);
            }
        }
        deleteActions(context,
                Settings.System.CUSTOM_TOGGLE_EXTRAS, tileKey);
        for (int i = 0; i < 5; i++) {
            deleteCustomIcon(getActionsAtIndex(context, i, 2, tileKey));
        }
        deleteActions(context,
                Settings.System.CUSTOM_TOGGLE_ACTIONS, tileKey);
    }

    private static void deleteCustomIcon(String file) {
        if (file != null && !file.equals(" ")) {
            File f = new File(file);
            if (f != null && f.exists()) {
                f.delete();
            }
        }
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
        return new ArrayList<String>(Arrays.asList(tiles.split("\\" + TILE_DELIMITER)));
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

    public static String getCustomExtras(Context context, String setting, String tileKey) {
        ArrayList<String> array = getCustomArray(context, setting);
        String action = null;
        for (int i = 0; i < array.size(); i++) {
            if (array.get(i) != null && array.get(i).contains(tileKey)) {
                String[] split = array.get(i).split(TILE_CUSTOM_KEY);
                action = split[0];
            }
        }
        return action;
    }

    public static String getActionsAtIndex(
            Context context, int index, int actionIndex, String tileKey) {
        String actions = Settings.System.getString(
                context.getContentResolver(),
                Settings.System.CUSTOM_TOGGLE_ACTIONS);
        String returnAction = null;
        if (actions != null && actions.contains(tileKey)) {
            for (String action : actions.split("\\|")) {
                if (action.contains(tileKey) && action.endsWith(Integer.toString(index))) {
                    String[] split = action.split(TILE_CUSTOM_KEY);
                    String[] returned = split[0].split(TILE_CUSTOM_DELIMITER);
                    returnAction = returned[actionIndex];
                    if (returnAction.equals(" ")) {
                        returnAction = null;
                    }
                }
            }
        }
        return returnAction;
    }

    public static void deleteActions(Context context, String setting, String tileKey) {
        ArrayList<String> array = getCustomArray(context, setting);
        for (int i = 0; i < array.size(); i++) {
            if (array.get(i) != null && array.get(i).contains(tileKey)) {
                array.remove(i);
            }
        }
        Settings.System.putString(
                context.getContentResolver(), setting, getTileStringFromList(array));
    }

    public static ArrayList<String> getCustomArray(Context context, String setting) {
        String actions = Settings.System.getString(
                context.getContentResolver(), setting);
        if (actions == null) {
            ArrayList<String> list = new ArrayList<String>();
            list.add(null);
            return list;
        } else {
            return new ArrayList<String>(Arrays.asList(actions.split("\\" + TILE_DELIMITER)));
        }
    }

    public static void saveCustomExtras(
            Context context, String tileKey, String collapseMatch,
            String icon, String name, String values, String resolver, String type) {
        String setting = Settings.System.CUSTOM_TOGGLE_EXTRAS;
        String oldSetting = getCustomExtras(context, setting, tileKey);
        ArrayList<String> oldList = getCustomArray(context, setting);
        ArrayList<String> newList = new ArrayList<String>();

        String[] indexes = { " ", " ", " ", " ", " ", " " };

        if (oldSetting != null) {
            String[] returned = oldSetting.split(TILE_CUSTOM_DELIMITER);
            if (returned.length == 6) {
                indexes[0] = returned[0];
                indexes[1] = returned[1];
                indexes[2] = returned[2];
                indexes[3] = returned[3];
                indexes[4] = returned[4];
                indexes[5] = returned[5];
            } else {
                indexes[0] = returned[0];
            }
        }

        if (collapseMatch != null) {
            indexes[0] = collapseMatch;
        }
        if (icon != null) {
            deleteCustomIcon(indexes[1]);
            indexes[1] = icon;
        }
        if (name != null) {
            indexes[2] = name;
        }
        if (values != null) {
            indexes[3] = values;
        }
        if (resolver != null) {
            indexes[4] = resolver;
        }
        if (type != null) {
            indexes[5] = type;
        }

        newList.add(
                indexes[0] + TILE_CUSTOM_DELIMITER
                + indexes[1] + TILE_CUSTOM_DELIMITER
                + indexes[2] + TILE_CUSTOM_DELIMITER
                + indexes[3] + TILE_CUSTOM_DELIMITER
                + indexes[4] + TILE_CUSTOM_DELIMITER
                + indexes[5] + TILE_CUSTOM_KEY
                + tileKey);

        for (String tile : oldList) {
            if (tile != null && !tile.contains(tileKey)) {
                newList.add(tile);
            }
        }

        Settings.System.putString(
                context.getContentResolver(), setting, getTileStringFromList(newList));
    }

    public static void saveExtras(
            Context context, String action, String tilekey, String setting) {
        String oldSetting = Settings.System.getString(
                context.getContentResolver(), setting);
        ArrayList<String> oldList = getCustomArray(context, setting);
        ArrayList<String> newList = new ArrayList<String>();
        newList.add(action + TILE_CUSTOM_KEY + tilekey);

        for (String tile : oldList) {
            if (tile != null && !tile.contains(tilekey)) {
                newList.add(tile);
            }
        }

        Settings.System.putString(
                context.getContentResolver(),
                Settings.System.TILE_CONTACT_ACTIONS,
                getTileStringFromList(newList));
    }

    public static void saveCustomActions(
            Context context, int index, int actionIndex, String action, String tileKey) {
        String setting = Settings.System.CUSTOM_TOGGLE_ACTIONS;
        String oldSetting = Settings.System.getString(
                context.getContentResolver(), setting);
        ArrayList<String> oldList = getCustomArray(context, setting);
        ArrayList<String> newList = new ArrayList<String>();

        String clickAction = " ";
        String longAction = " ";
        String icon = " ";

        if (oldSetting != null && oldSetting.contains(tileKey)) {
            for (String act : oldSetting.split("\\|")) {
                if (act.contains(tileKey) && act.endsWith(Integer.toString(index))) {
                    String[] split = act.split(TILE_CUSTOM_KEY);
                    String[] returned = split[0].split(TILE_CUSTOM_DELIMITER);
                    clickAction = returned[0];
                    longAction = returned[1];
                    icon = returned[2];
                }
            }
        }

        switch (actionIndex) {
            case 0:
                clickAction = action;
                break;
            case 1:
                longAction = action;
                break;
            case 2:
                icon = action;
                break;
        }

        newList.add(clickAction
                + TILE_CUSTOM_DELIMITER + longAction
                + TILE_CUSTOM_DELIMITER + icon
                + TILE_CUSTOM_KEY + tileKey + " " + Integer.toString(index));

        for (String tile : oldList) {
            if (tile != null) {
                if (tile.contains(tileKey)) {
                    if (!tile.endsWith(Integer.toString(index))) {
                        newList.add(tile);
                    }
                } else {
                    newList.add(tile);
                }
            }
        }

        Settings.System.putString(
                context.getContentResolver(), setting, getTileStringFromList(newList));
    }

    public static void deleteCustomActions(Context context, int index, String tileKey) {
        String setting = Settings.System.CUSTOM_TOGGLE_ACTIONS;
        String oldSetting = Settings.System.getString(
                context.getContentResolver(), setting);
        ArrayList<String> oldList = getCustomArray(context, setting);
        ArrayList<String> newList = new ArrayList<String>();

        for (int i = 0; i < oldList.size(); i++) {
            if (oldList.get(i).contains(tileKey)
                    && oldList.get(i).endsWith(Integer.toString(index))) {
                String[] split = oldList.get(i).split(TILE_CUSTOM_KEY);
                String[] returned = split[0].split(TILE_CUSTOM_DELIMITER);
                deleteCustomIcon(returned[2]);
            } else {
                newList.add(oldList.get(i));
            }
        }

        Settings.System.putString(
                context.getContentResolver(), setting, getTileStringFromList(newList));
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

