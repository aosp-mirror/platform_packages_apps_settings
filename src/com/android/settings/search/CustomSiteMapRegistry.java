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

package com.android.settings.search;

import android.util.ArrayMap;

import com.android.settings.DisplaySettings;
import com.android.settings.backup.UserBackupSettingsActivity;
import com.android.settings.connecteddevice.ConnectedDeviceDashboardFragment;
import com.android.settings.connecteddevice.usb.UsbDetailsFragment;
import com.android.settings.fuelgauge.PowerUsageAdvanced;
import com.android.settings.fuelgauge.PowerUsageSummary;
import com.android.settings.gestures.GestureNavigationSettingsFragment;
import com.android.settings.gestures.SystemNavigationGestureSettings;
import com.android.settings.location.LocationSettings;
import com.android.settings.location.RecentLocationRequestSeeAllFragment;
import com.android.settings.network.NetworkDashboardFragment;
import com.android.settings.notification.zen.ZenModeBlockedEffectsSettings;
import com.android.settings.notification.zen.ZenModeRestrictNotificationsSettings;
import com.android.settings.security.SecuritySettings;
import com.android.settings.security.screenlock.ScreenLockSettings;
import com.android.settings.system.SystemDashboardFragment;
import com.android.settings.wallpaper.WallpaperSuggestionActivity;
import com.android.settings.wifi.WifiSettings2;

import java.util.Map;

/**
 * A registry of custom site map.
 */
public class CustomSiteMapRegistry {

    /**
     * Map from child class to parent class.
     */
    public static final Map<String, String> CUSTOM_SITE_MAP;

    static {
        CUSTOM_SITE_MAP = new ArrayMap<>();
        CUSTOM_SITE_MAP.put(ScreenLockSettings.class.getName(), SecuritySettings.class.getName());
        CUSTOM_SITE_MAP.put(
                WallpaperSuggestionActivity.class.getName(), DisplaySettings.class.getName());
        CUSTOM_SITE_MAP.put(
                WifiSettings2.class.getName(), NetworkDashboardFragment.class.getName());
        CUSTOM_SITE_MAP.put(PowerUsageAdvanced.class.getName(), PowerUsageSummary.class.getName());
        CUSTOM_SITE_MAP.put(RecentLocationRequestSeeAllFragment.class.getName(),
                LocationSettings.class.getName());
        CUSTOM_SITE_MAP.put(UsbDetailsFragment.class.getName(),
                ConnectedDeviceDashboardFragment.class.getName());
        CUSTOM_SITE_MAP.put(UserBackupSettingsActivity.class.getName(),
                SystemDashboardFragment.class.getName());
        CUSTOM_SITE_MAP.put(ZenModeBlockedEffectsSettings.class.getName(),
                ZenModeRestrictNotificationsSettings.class.getName());
        CUSTOM_SITE_MAP.put(GestureNavigationSettingsFragment.class.getName(),
                SystemNavigationGestureSettings.class.getName());
    }
}
