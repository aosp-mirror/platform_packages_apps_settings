/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.search;

import com.android.settings.ChooseLockGeneric;
import com.android.settings.DateTimeSettings;
import com.android.settings.DevelopmentSettings;
import com.android.settings.DeviceInfoSettings;
import com.android.settings.DisplaySettings;
import com.android.settings.LegalSettings;
import com.android.settings.PrivacySettings;
import com.android.settings.ScreenPinningSettings;
import com.android.settings.SecuritySettings;
import com.android.settings.WallpaperTypeSettings;
import com.android.settings.WirelessSettings;
import com.android.settings.accessibility.AccessibilitySettings;
import com.android.settings.accounts.AccountSettings;
import com.android.settings.applications.AdvancedAppSettings;
import com.android.settings.applications.SpecialAccessSettings;
import com.android.settings.bluetooth.BluetoothSettings;
import com.android.settings.datausage.DataUsageMeteredSettings;
import com.android.settings.datausage.DataUsageSummary;
import com.android.settings.deviceinfo.StorageSettings;
import com.android.settings.display.ScreenZoomSettings;
import com.android.settings.fuelgauge.BatterySaverSettings;
import com.android.settings.fuelgauge.PowerUsageSummary;
import com.android.settings.gestures.GestureSettings;
import com.android.settings.inputmethod.InputMethodAndLanguageSettings;
import com.android.settings.location.LocationSettings;
import com.android.settings.location.ScanningSettings;
import com.android.settings.notification.ConfigureNotificationSettings;
import com.android.settings.notification.OtherSoundSettings;
import com.android.settings.notification.SoundSettings;
import com.android.settings.notification.ZenModeAutomationSettings;
import com.android.settings.notification.ZenModePrioritySettings;
import com.android.settings.notification.ZenModeSettings;
import com.android.settings.notification.ZenModeVisualInterruptionSettings;
import com.android.settings.print.PrintSettingsFragment;
import com.android.settings.sim.SimSettings;
import com.android.settings.users.UserSettings;
import com.android.settings.wifi.AdvancedWifiSettings;
import com.android.settings.wifi.SavedAccessPointsWifiSettings;
import com.android.settings.wifi.WifiSettings;

import java.util.HashMap;

/**
 * Utility class for dealing with Search Ranking.
 */
public final class Ranking {

    public static final int RANK_WIFI = 1;
    public static final int RANK_BT = 2;
    public static final int RANK_SIM = 3;
    public static final int RANK_DATA_USAGE = 4;
    public static final int RANK_WIRELESS = 5;
    public static final int RANK_DISPLAY = 6;
    public static final int RANK_WALLPAPER = 7;
    public static final int RANK_NOTIFICATIONS = 8;
    public static final int RANK_SOUND = 9;
    public static final int RANK_APPS = 10;
    public static final int RANK_STORAGE = 11;
    public static final int RANK_POWER_USAGE = 12;
    public static final int RANK_USERS = 13;
    public static final int RANK_LOCATION = 14;
    public static final int RANK_SECURITY = 15;
    public static final int RANK_ACCOUNT = 16;
    public static final int RANK_IME = 17;
    public static final int RANK_PRIVACY = 18;
    public static final int RANK_DATE_TIME = 19;
    public static final int RANK_ACCESSIBILITY = 20;
    public static final int RANK_PRINTING = 21;
    public static final int RANK_DEVELOPEMENT = 22;
    public static final int RANK_DEVICE_INFO = 23;
    public static final int RANK_GESTURE = 24;

    public static final int RANK_UNDEFINED = -1;
    public static final int RANK_OTHERS = 1024;
    public static final int BASE_RANK_DEFAULT = 2048;

    public static int sCurrentBaseRank = BASE_RANK_DEFAULT;

    private static HashMap<String, Integer> sRankMap = new HashMap<String, Integer>();
    private static HashMap<String, Integer> sBaseRankMap = new HashMap<String, Integer>();

    static {
        // Wi-Fi
        sRankMap.put(WifiSettings.class.getName(), RANK_WIFI);
        sRankMap.put(AdvancedWifiSettings.class.getName(), RANK_WIFI);
        sRankMap.put(SavedAccessPointsWifiSettings.class.getName(), RANK_WIFI);

        // BT
        sRankMap.put(BluetoothSettings.class.getName(), RANK_BT);

        // SIM Cards
        sRankMap.put(SimSettings.class.getName(), RANK_SIM);

        // DataUsage
        sRankMap.put(DataUsageSummary.class.getName(), RANK_DATA_USAGE);
        sRankMap.put(DataUsageMeteredSettings.class.getName(), RANK_DATA_USAGE);

        // Other wireless settinfs
        sRankMap.put(WirelessSettings.class.getName(), RANK_WIRELESS);

        // Display
        sRankMap.put(DisplaySettings.class.getName(), RANK_DISPLAY);
        sRankMap.put(ScreenZoomSettings.class.getName(), RANK_WIFI);

        // Wallpapers
        sRankMap.put(WallpaperTypeSettings.class.getName(), RANK_WALLPAPER);

        // Sound
        sRankMap.put(SoundSettings.class.getName(), RANK_SOUND);

        // Notifications
        sRankMap.put(ConfigureNotificationSettings.class.getName(), RANK_NOTIFICATIONS);
        sRankMap.put(OtherSoundSettings.class.getName(), RANK_NOTIFICATIONS);
        sRankMap.put(ZenModeSettings.class.getName(), RANK_NOTIFICATIONS);
        sRankMap.put(ZenModePrioritySettings.class.getName(), RANK_NOTIFICATIONS);
        sRankMap.put(ZenModeAutomationSettings.class.getName(), RANK_NOTIFICATIONS);
        sRankMap.put(ZenModeVisualInterruptionSettings.class.getName(), RANK_NOTIFICATIONS);

        // Storage
        sRankMap.put(StorageSettings.class.getName(), RANK_STORAGE);

        // Battery
        sRankMap.put(PowerUsageSummary.class.getName(), RANK_POWER_USAGE);
        sRankMap.put(BatterySaverSettings.class.getName(), RANK_POWER_USAGE);

        // Advanced app settings
        sRankMap.put(AdvancedAppSettings.class.getName(), RANK_APPS);
        sRankMap.put(SpecialAccessSettings.class.getName(), RANK_APPS);

        // Users
        sRankMap.put(UserSettings.class.getName(), RANK_USERS);

        // Gestures
        sRankMap.put(GestureSettings.class.getName(), RANK_GESTURE);

        // Location
        sRankMap.put(LocationSettings.class.getName(), RANK_LOCATION);
        sRankMap.put(ScanningSettings.class.getName(), RANK_LOCATION);

        // Security
        sRankMap.put(SecuritySettings.class.getName(), RANK_SECURITY);
        sRankMap.put(ChooseLockGeneric.ChooseLockGenericFragment.class.getName(), RANK_SECURITY);
        sRankMap.put(ScreenPinningSettings.class.getName(), RANK_SECURITY);

        // Accounts
        sRankMap.put(AccountSettings.class.getName(), RANK_ACCOUNT);

        // IMEs
        sRankMap.put(InputMethodAndLanguageSettings.class.getName(), RANK_IME);

        // Privacy
        sRankMap.put(PrivacySettings.class.getName(), RANK_PRIVACY);

        // Date / Time
        sRankMap.put(DateTimeSettings.class.getName(), RANK_DATE_TIME);

        // Accessibility
        sRankMap.put(AccessibilitySettings.class.getName(), RANK_ACCESSIBILITY);

        // Print
        sRankMap.put(PrintSettingsFragment.class.getName(), RANK_PRINTING);

        // Development
        sRankMap.put(DevelopmentSettings.class.getName(), RANK_DEVELOPEMENT);

        // Device infos
        sRankMap.put(DeviceInfoSettings.class.getName(), RANK_DEVICE_INFO);
        sRankMap.put(LegalSettings.class.getName(), RANK_DEVICE_INFO);

        sBaseRankMap.put("com.android.settings", 0);
    }

    public static int getRankForClassName(String className) {
        Integer rank = sRankMap.get(className);
        return (rank != null) ? (int) rank: RANK_OTHERS;
    }

    public static int getBaseRankForAuthority(String authority) {
        synchronized (sBaseRankMap) {
            Integer base = sBaseRankMap.get(authority);
            if (base != null) {
                return base;
            }
            sCurrentBaseRank++;
            sBaseRankMap.put(authority, sCurrentBaseRank);
            return sCurrentBaseRank;
        }
    }
}
