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

import android.provider.SearchIndexableResource;
import com.android.settings.DataUsageSummary;
import com.android.settings.DateTimeSettings;
import com.android.settings.DevelopmentSettings;
import com.android.settings.DeviceInfoSettings;
import com.android.settings.DisplaySettings;
import com.android.settings.HomeSettings;
import com.android.settings.NotificationSettings;
import com.android.settings.PrivacySettings;
import com.android.settings.R;
import com.android.settings.SecuritySettings;
import com.android.settings.SoundSettings;
import com.android.settings.WallpaperTypeSettings;
import com.android.settings.WirelessSettings;
import com.android.settings.ZenModeSettings;
import com.android.settings.accessibility.AccessibilitySettings;
import com.android.settings.bluetooth.BluetoothSettings;
import com.android.settings.deviceinfo.Memory;
import com.android.settings.fuelgauge.PowerUsageSummary;
import com.android.settings.inputmethod.InputMethodAndLanguageSettings;
import com.android.settings.location.LocationSettings;
import com.android.settings.net.DataUsageMeteredSettings;
import com.android.settings.print.PrintSettingsFragment;
import com.android.settings.users.UserSettings;
import com.android.settings.wifi.WifiSettings;

import java.util.Collection;
import java.util.HashMap;

public final class SearchIndexableResources {

    public static int NO_DATA_RES_ID = 0;

    private static final int RANK_WIFI = 1;
    private static final int RANK_BT = 2;
    private static final int RANK_DATA_USAGE = 3;
    private static final int RANK_WIRELESS = 4;
    private static final int RANK_HOME = 5;
    private static final int RANK_SOUND = 6;
    private static final int RANK_DISPLAY = 7;
    private static final int RANK_WALLPAPER = 7;
    private static final int RANK_NOTIFICATIONS = 8;
    private static final int RANK_MEMORY = 9;
    private static final int RANK_POWER_USAGE = 10;
    private static final int RANK_USERS = 11;
    private static final int RANK_LOCATION = 12;
    private static final int RANK_SECURITY = 13;
    private static final int RANK_IME = 14;
    private static final int RANK_PRIVACY = 15;
    private static final int RANK_DATE_TIME = 16;
    private static final int RANK_ACCESSIBILITY = 17;
    private static final int RANK_PRINTING = 18;
    private static final int RANK_DEVELOPEMENT = 19;
    private static final int RANK_DEVICE_INFO = 20;

    private static HashMap<String, SearchIndexableResource> sResMap =
            new HashMap<String, SearchIndexableResource>();


    static {
        sResMap.put(WifiSettings.class.getName(),
                new SearchIndexableResource(RANK_WIFI,
                        NO_DATA_RES_ID,
                        WifiSettings.class.getName(),
                        R.drawable.ic_settings_wireless));

        sResMap.put(BluetoothSettings.class.getName(),
                new SearchIndexableResource(RANK_BT,
                        NO_DATA_RES_ID,
                        BluetoothSettings.class.getName(),
                        R.drawable.ic_settings_bluetooth2));

        sResMap.put(DataUsageSummary.class.getName(),
                new SearchIndexableResource(RANK_DATA_USAGE,
                        NO_DATA_RES_ID,
                        DataUsageSummary.class.getName(),
                        R.drawable.ic_settings_data_usage));

        sResMap.put(WirelessSettings.class.getName(),
                new SearchIndexableResource(RANK_WIRELESS,
                        R.xml.wireless_settings,
                        WirelessSettings.class.getName(),
                        R.drawable.empty_icon));

        sResMap.put(HomeSettings.class.getName(),
                new SearchIndexableResource(RANK_HOME,
                        R.xml.home_selection,
                        HomeSettings.class.getName(),
                        R.drawable.ic_settings_home));

        sResMap.put(SoundSettings.class.getName(),
                new SearchIndexableResource(RANK_SOUND,
                        R.xml.sound_settings,
                        SoundSettings.class.getName(),
                        R.drawable.ic_settings_sound));

        sResMap.put(DisplaySettings.class.getName(),
                new SearchIndexableResource(RANK_DISPLAY,
                        R.xml.display_settings,
                        DisplaySettings.class.getName(),
                        R.drawable.ic_settings_display));

        sResMap.put(WallpaperTypeSettings.class.getName(),
                new SearchIndexableResource(RANK_WALLPAPER,
                        NO_DATA_RES_ID,
                        WallpaperTypeSettings.class.getName(),
                        R.drawable.ic_settings_display));

        sResMap.put(NotificationSettings.class.getName(),
                new SearchIndexableResource(RANK_NOTIFICATIONS,
                        R.xml.notification_settings,
                        NotificationSettings.class.getName(),
                        R.drawable.ic_settings_notifications));

        sResMap.put(ZenModeSettings.class.getName(),
                new SearchIndexableResource(RANK_NOTIFICATIONS,
                        NO_DATA_RES_ID,
                        ZenModeSettings.class.getName(),
                        R.drawable.ic_settings_notifications));

        sResMap.put(Memory.class.getName(),
                new SearchIndexableResource(RANK_MEMORY,
                        NO_DATA_RES_ID,
                        Memory.class.getName(),
                        R.drawable.ic_settings_storage));

        sResMap.put(PowerUsageSummary.class.getName(),
                new SearchIndexableResource(RANK_POWER_USAGE,
                        R.xml.power_usage_summary,
                        PowerUsageSummary.class.getName(),
                        R.drawable.ic_settings_battery));

        sResMap.put(UserSettings.class.getName(),
                new SearchIndexableResource(RANK_USERS,
                        R.xml.user_settings,
                        UserSettings.class.getName(),
                        R.drawable.ic_settings_multiuser));

        sResMap.put(LocationSettings.class.getName(),
                new SearchIndexableResource(RANK_LOCATION,
                        R.xml.location_settings,
                        LocationSettings.class.getName(),
                        R.drawable.ic_settings_location));

        sResMap.put(SecuritySettings.class.getName(),
                new SearchIndexableResource(RANK_SECURITY,
                        R.xml.security_settings,
                        SecuritySettings.class.getName(),
                        R.drawable.ic_settings_security));

        sResMap.put(InputMethodAndLanguageSettings.class.getName(),
                new SearchIndexableResource(RANK_IME,
                        R.xml.language_settings,
                        InputMethodAndLanguageSettings.class.getName(),
                        R.drawable.ic_settings_language));

        sResMap.put(PrivacySettings.class.getName(),
                new SearchIndexableResource(RANK_PRIVACY,
                        R.xml.privacy_settings,
                        PrivacySettings.class.getName(),
                        R.drawable.ic_settings_backup));

        sResMap.put(DateTimeSettings.class.getName(),
                new SearchIndexableResource(RANK_DATE_TIME,
                        R.xml.date_time_prefs,
                        DateTimeSettings.class.getName(),
                        R.drawable.ic_settings_date_time));

        sResMap.put(AccessibilitySettings.class.getName(),
                new SearchIndexableResource(RANK_ACCESSIBILITY,
                        NO_DATA_RES_ID,
                        AccessibilitySettings.class.getName(),
                        R.drawable.ic_settings_accessibility));

        sResMap.put(PrintSettingsFragment.class.getName(),
                new SearchIndexableResource(RANK_PRINTING,
                        NO_DATA_RES_ID,
                        PrintSettingsFragment.class.getName(),
                        com.android.internal.R.drawable.ic_print));

        sResMap.put(DevelopmentSettings.class.getName(),
                new SearchIndexableResource(RANK_DEVELOPEMENT,
                        R.xml.development_prefs,
                        DevelopmentSettings.class.getName(),
                        R.drawable.ic_settings_development));

        sResMap.put(DeviceInfoSettings.class.getName(),
                new SearchIndexableResource(RANK_DEVICE_INFO,
                        R.xml.device_info_settings,
                        DeviceInfoSettings.class.getName(),
                        R.drawable.ic_settings_about));
    }

    private SearchIndexableResources() {
    }

    public static int size() {
        return sResMap.size();
    }

    public static SearchIndexableResource getResourceByName(String className) {
        return sResMap.get(className);
    }

    public static Collection<SearchIndexableResource> values() {
        return sResMap.values();
    }
}
