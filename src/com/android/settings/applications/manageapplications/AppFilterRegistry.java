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
 * limitations under the License.
 */

package com.android.settings.applications.manageapplications;

import android.support.annotation.IntDef;

import com.android.settings.R;
import com.android.settings.applications.AppStateInstallAppsBridge;
import com.android.settings.applications.AppStateNotificationBridge;
import com.android.settings.applications.AppStateOverlayBridge;
import com.android.settings.applications.AppStatePowerBridge;
import com.android.settings.applications.AppStateStorageAccessBridge;
import com.android.settings.applications.AppStateUsageBridge;
import com.android.settings.applications.AppStateWriteSettingsBridge;
import com.android.settingslib.applications.ApplicationsState;

/**
 * A registry and helper class that manages all {@link AppFilterItem}s for ManageApplications UI.
 */
public class AppFilterRegistry {

    @IntDef(value = {
            FILTER_APPS_POWER_WHITELIST,
            FILTER_APPS_POWER_WHITELIST_ALL,
            FILTER_APPS_ALL,
            FILTER_APPS_ENABLED,
            FILTER_APPS_INSTANT,
            FILTER_APPS_DISABLED,
            FILTER_APPS_BLOCKED,
            FILTER_APPS_PERSONAL,
            FILTER_APPS_WORK,
            FILTER_APPS_USAGE_ACCESS,
            FILTER_APPS_WITH_OVERLAY,
            FILTER_APPS_WRITE_SETTINGS,
            FILTER_APPS_INSTALL_SOURCES,
    })
    @interface FilterType {
    }

    // Filter options used for displayed list of applications
    // Filters will appear sorted based on their value defined here.
    public static final int FILTER_APPS_POWER_WHITELIST = 0;
    public static final int FILTER_APPS_POWER_WHITELIST_ALL = 1;
    public static final int FILTER_APPS_ALL = 2;
    public static final int FILTER_APPS_ENABLED = 3;
    public static final int FILTER_APPS_INSTANT = 4;
    public static final int FILTER_APPS_DISABLED = 5;
    public static final int FILTER_APPS_BLOCKED = 6;
    public static final int FILTER_APPS_PERSONAL = 7;
    public static final int FILTER_APPS_WORK = 8;
    public static final int FILTER_APPS_USAGE_ACCESS = 9;
    public static final int FILTER_APPS_WITH_OVERLAY = 10;
    public static final int FILTER_APPS_WRITE_SETTINGS = 11;
    public static final int FILTER_APPS_INSTALL_SOURCES = 12;
    public static final int FILTER_APP_HAS_STORAGE_ACCESS = 13;
    // Next id: 14

    private static AppFilterRegistry sRegistry;

    private final AppFilterItem[] mFilters;

    private AppFilterRegistry() {
        mFilters = new AppFilterItem[14];

        // High power whitelist, on
        mFilters[FILTER_APPS_POWER_WHITELIST] = new AppFilterItem(
                new ApplicationsState.CompoundFilter(
                        AppStatePowerBridge.FILTER_POWER_WHITELISTED,
                        ApplicationsState.FILTER_ALL_ENABLED),
                FILTER_APPS_POWER_WHITELIST,
                R.string.high_power_filter_on);

        // Without disabled until used
        mFilters[FILTER_APPS_POWER_WHITELIST_ALL] = new AppFilterItem(
                new ApplicationsState.CompoundFilter(
                        ApplicationsState.FILTER_WITHOUT_DISABLED_UNTIL_USED,
                        ApplicationsState.FILTER_ALL_ENABLED),
                FILTER_APPS_POWER_WHITELIST_ALL,
                R.string.filter_all_apps);

        // All apps
        mFilters[FILTER_APPS_ALL] = new AppFilterItem(
                ApplicationsState.FILTER_EVERYTHING,
                FILTER_APPS_ALL,
                R.string.filter_all_apps);

        // Enabled
        mFilters[FILTER_APPS_ENABLED] = new AppFilterItem(
                ApplicationsState.FILTER_ALL_ENABLED,
                FILTER_APPS_ENABLED,
                R.string.filter_enabled_apps);

        // Disabled
        mFilters[FILTER_APPS_DISABLED] = new AppFilterItem(
                ApplicationsState.FILTER_DISABLED,
                FILTER_APPS_DISABLED,
                R.string.filter_apps_disabled);

        // Instant
        mFilters[FILTER_APPS_INSTANT] = new AppFilterItem(
                ApplicationsState.FILTER_INSTANT,
                FILTER_APPS_INSTANT,
                R.string.filter_instant_apps);

        // Blocked Notifications
        mFilters[FILTER_APPS_BLOCKED] = new AppFilterItem(
                AppStateNotificationBridge.FILTER_APP_NOTIFICATION_BLOCKED,
                FILTER_APPS_BLOCKED,
                R.string.filter_notif_blocked_apps);

        // Personal
        mFilters[FILTER_APPS_PERSONAL] = new AppFilterItem(
                ApplicationsState.FILTER_PERSONAL,
                FILTER_APPS_PERSONAL,
                R.string.filter_personal_apps);

        // Work
        mFilters[FILTER_APPS_WORK] = new AppFilterItem(
                ApplicationsState.FILTER_WORK,
                FILTER_APPS_WORK,
                R.string.filter_work_apps);

        // Usage access screen, never displayed.
        mFilters[FILTER_APPS_USAGE_ACCESS] = new AppFilterItem(
                AppStateUsageBridge.FILTER_APP_USAGE,
                FILTER_APPS_USAGE_ACCESS,
                R.string.filter_all_apps);

        // Apps that can draw overlays
        mFilters[FILTER_APPS_WITH_OVERLAY] = new AppFilterItem(
                AppStateOverlayBridge.FILTER_SYSTEM_ALERT_WINDOW,
                FILTER_APPS_WITH_OVERLAY,
                R.string.filter_overlay_apps);

        // Apps that can write system settings
        mFilters[FILTER_APPS_WRITE_SETTINGS] = new AppFilterItem(
                AppStateWriteSettingsBridge.FILTER_WRITE_SETTINGS,
                FILTER_APPS_WRITE_SETTINGS,
                R.string.filter_write_settings_apps);

        // Apps that are trusted sources of apks
        mFilters[FILTER_APPS_INSTALL_SOURCES] = new AppFilterItem(
                AppStateInstallAppsBridge.FILTER_APP_SOURCES,
                FILTER_APPS_INSTALL_SOURCES,
                R.string.filter_install_sources_apps);

        // Apps that interacted with storage access permissions (A.K.A. Scoped Directory Access)
        mFilters[FILTER_APP_HAS_STORAGE_ACCESS] = new AppFilterItem(
                AppStateStorageAccessBridge.FILTER_APP_HAS_STORAGE_ACCESS,
                FILTER_APP_HAS_STORAGE_ACCESS,
                R.string.filter_install_sources_apps);
    }

    public static AppFilterRegistry getInstance() {
        if (sRegistry == null) {
            sRegistry = new AppFilterRegistry();
        }
        return sRegistry;
    }

    @FilterType
    public int getDefaultFilterType(int listType) {
        switch (listType) {
            case ManageApplications.LIST_TYPE_USAGE_ACCESS:
                return FILTER_APPS_USAGE_ACCESS;
            case ManageApplications.LIST_TYPE_HIGH_POWER:
                return FILTER_APPS_POWER_WHITELIST;
            case ManageApplications.LIST_TYPE_OVERLAY:
                return FILTER_APPS_WITH_OVERLAY;
            case ManageApplications.LIST_TYPE_WRITE_SETTINGS:
                return FILTER_APPS_WRITE_SETTINGS;
            case ManageApplications.LIST_TYPE_MANAGE_SOURCES:
                return FILTER_APPS_INSTALL_SOURCES;
            case ManageApplications.LIST_TYPE_STORAGE_ACCESS:
                return FILTER_APP_HAS_STORAGE_ACCESS;
            default:
                return FILTER_APPS_ALL;
        }
    }

    public AppFilterItem get(@FilterType int filterType) {
        return mFilters[filterType];
    }
}
