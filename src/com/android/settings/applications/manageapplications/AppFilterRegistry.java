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

import androidx.annotation.IntDef;

import com.android.settings.R;
import com.android.settings.applications.AppStateAlarmsAndRemindersBridge;
import com.android.settings.applications.AppStateAppBatteryUsageBridge;
import com.android.settings.applications.AppStateClonedAppsBridge;
import com.android.settings.applications.AppStateInstallAppsBridge;
import com.android.settings.applications.AppStateLocaleBridge;
import com.android.settings.applications.AppStateLongBackgroundTasksBridge;
import com.android.settings.applications.AppStateManageExternalStorageBridge;
import com.android.settings.applications.AppStateMediaManagementAppsBridge;
import com.android.settings.applications.AppStateNotificationBridge;
import com.android.settings.applications.AppStateOverlayBridge;
import com.android.settings.applications.AppStatePowerBridge;
import com.android.settings.applications.AppStateTurnScreenOnBridge;
import com.android.settings.applications.AppStateUsageBridge;
import com.android.settings.applications.AppStateWriteSettingsBridge;
import com.android.settings.nfc.AppStateNfcTagAppsBridge;
import com.android.settings.wifi.AppStateChangeWifiStateBridge;
import com.android.settingslib.applications.ApplicationsState;

/**
 * A registry and helper class that manages all {@link AppFilterItem}s for ManageApplications UI.
 */
public class AppFilterRegistry {

    @IntDef(
            value = {
                FILTER_APPS_POWER_ALLOWLIST,
                FILTER_APPS_POWER_ALLOWLIST_ALL,
                FILTER_APPS_ALL,
                FILTER_APPS_ENABLED,
                FILTER_APPS_INSTANT,
                FILTER_APPS_DISABLED,
                FILTER_APPS_RECENT,
                FILTER_APPS_FREQUENT,
                FILTER_APPS_PERSONAL,
                FILTER_APPS_WORK,
                FILTER_APPS_USAGE_ACCESS,
                FILTER_APPS_WITH_OVERLAY,
                FILTER_APPS_WRITE_SETTINGS,
                FILTER_APPS_INSTALL_SOURCES,
                FILTER_APPS_BLOCKED,
                FILTER_ALARMS_AND_REMINDERS,
                FILTER_APPS_MEDIA_MANAGEMENT,
                FILTER_APPS_LOCALE,
                FILTER_APPS_BATTERY_UNRESTRICTED,
                FILTER_APPS_BATTERY_OPTIMIZED,
                FILTER_APPS_BATTERY_RESTRICTED,
                FILTER_LONG_BACKGROUND_TASKS,
                FILTER_APPS_CLONE,
                FILTER_APPS_NFC_TAG,
                FILTER_APPS_TURN_SCREEN_ON,
            })
    @interface FilterType {}

    // Filter options used for displayed list of applications
    // Filters will appear sorted based on their value defined here.
    public static final int FILTER_APPS_POWER_ALLOWLIST = 0;
    public static final int FILTER_APPS_POWER_ALLOWLIST_ALL = 1;
    public static final int FILTER_APPS_RECENT = 2;
    public static final int FILTER_APPS_FREQUENT = 3;
    public static final int FILTER_APPS_ALL = 4;
    public static final int FILTER_APPS_ENABLED = 5;
    public static final int FILTER_APPS_INSTANT = 6;
    public static final int FILTER_APPS_DISABLED = 7;
    public static final int FILTER_APPS_PERSONAL = 8;
    public static final int FILTER_APPS_WORK = 9;
    public static final int FILTER_APPS_USAGE_ACCESS = 10;
    public static final int FILTER_APPS_WITH_OVERLAY = 11;
    public static final int FILTER_APPS_WRITE_SETTINGS = 12;
    public static final int FILTER_APPS_INSTALL_SOURCES = 13;
    public static final int FILTER_APP_CAN_CHANGE_WIFI_STATE = 15;
    public static final int FILTER_APPS_BLOCKED = 16;
    public static final int FILTER_MANAGE_EXTERNAL_STORAGE = 17;
    public static final int FILTER_ALARMS_AND_REMINDERS = 18;
    public static final int FILTER_APPS_MEDIA_MANAGEMENT = 19;
    public static final int FILTER_APPS_LOCALE = 20;
    public static final int FILTER_APPS_BATTERY_UNRESTRICTED = 21;
    public static final int FILTER_APPS_BATTERY_OPTIMIZED = 22;
    public static final int FILTER_APPS_BATTERY_RESTRICTED = 23;
    public static final int FILTER_LONG_BACKGROUND_TASKS = 24;
    public static final int FILTER_APPS_CLONE = 25;
    public static final int FILTER_APPS_NFC_TAG = 26;
    public static final int FILTER_APPS_TURN_SCREEN_ON = 27;
    private static final int NUM_FILTER_ENTRIES = 28;
    // Next id: 28. If you add an entry here, please change NUM_FILTER_ENTRIES.

    private static AppFilterRegistry sRegistry;

    private final AppFilterItem[] mFilters;

    private AppFilterRegistry() {
        mFilters = new AppFilterItem[NUM_FILTER_ENTRIES];

        // High power allowlist, on
        mFilters[FILTER_APPS_POWER_ALLOWLIST] = new AppFilterItem(
                new ApplicationsState.CompoundFilter(
                        AppStatePowerBridge.FILTER_POWER_ALLOWLISTED,
                        ApplicationsState.FILTER_ALL_ENABLED),
                FILTER_APPS_POWER_ALLOWLIST,
                R.string.high_power_filter_on);

        // Without disabled until used
        mFilters[FILTER_APPS_POWER_ALLOWLIST_ALL] = new AppFilterItem(
                new ApplicationsState.CompoundFilter(
                        ApplicationsState.FILTER_WITHOUT_DISABLED_UNTIL_USED,
                        ApplicationsState.FILTER_ALL_ENABLED),
                FILTER_APPS_POWER_ALLOWLIST_ALL,
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

        // Recent Notifications
        mFilters[FILTER_APPS_RECENT] = new AppFilterItem(
                AppStateNotificationBridge.FILTER_APP_NOTIFICATION_RECENCY,
                FILTER_APPS_RECENT,
                R.string.sort_order_recent_notification);

        // Frequent Notifications
        mFilters[FILTER_APPS_FREQUENT] = new AppFilterItem(
                AppStateNotificationBridge.FILTER_APP_NOTIFICATION_FREQUENCY,
                FILTER_APPS_FREQUENT,
                R.string.sort_order_frequent_notification);

        // Personal
        mFilters[FILTER_APPS_PERSONAL] = new AppFilterItem(
                ApplicationsState.FILTER_PERSONAL,
                FILTER_APPS_PERSONAL,
                com.android.settingslib.R.string.category_personal);

        // Work
        mFilters[FILTER_APPS_WORK] = new AppFilterItem(
                ApplicationsState.FILTER_WORK,
                FILTER_APPS_WORK,
                com.android.settingslib.R.string.category_work);

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

        mFilters[FILTER_APP_CAN_CHANGE_WIFI_STATE] = new AppFilterItem(
                AppStateChangeWifiStateBridge.FILTER_CHANGE_WIFI_STATE,
                FILTER_APP_CAN_CHANGE_WIFI_STATE,
                R.string.filter_write_settings_apps);

        // Blocked Notifications
        mFilters[FILTER_APPS_BLOCKED] = new AppFilterItem(
                AppStateNotificationBridge.FILTER_APP_NOTIFICATION_BLOCKED,
                FILTER_APPS_BLOCKED,
                R.string.filter_notif_blocked_apps);

        mFilters[FILTER_MANAGE_EXTERNAL_STORAGE] = new AppFilterItem(
                AppStateManageExternalStorageBridge.FILTER_MANAGE_EXTERNAL_STORAGE,
                FILTER_MANAGE_EXTERNAL_STORAGE,
                R.string.filter_manage_external_storage);

        // Apps that can schedule alarms and reminders
        mFilters[FILTER_ALARMS_AND_REMINDERS] = new AppFilterItem(
                AppStateAlarmsAndRemindersBridge.FILTER_CLOCK_APPS,
                FILTER_ALARMS_AND_REMINDERS,
                com.android.settingslib.R.string.alarms_and_reminders_title);

        // Apps that can manage media files
        mFilters[FILTER_APPS_MEDIA_MANAGEMENT] = new AppFilterItem(
                AppStateMediaManagementAppsBridge.FILTER_MEDIA_MANAGEMENT_APPS,
                FILTER_APPS_MEDIA_MANAGEMENT,
                R.string.media_management_apps_title);

        // Apps that can configurate appication's locale.
        mFilters[FILTER_APPS_LOCALE] = new AppFilterItem(
                AppStateLocaleBridge.FILTER_APPS_LOCALE,
                FILTER_APPS_LOCALE,
                R.string.app_locale_picker_title);

        // Battery optimization app states:
        // Unrestricted
        mFilters[FILTER_APPS_BATTERY_UNRESTRICTED] =
                new AppFilterItem(
                        AppStateAppBatteryUsageBridge.FILTER_BATTERY_UNRESTRICTED_APPS,
                        FILTER_APPS_BATTERY_UNRESTRICTED,
                        R.string.filter_battery_unrestricted_title);

        // Optimized
        mFilters[FILTER_APPS_BATTERY_OPTIMIZED] =
                new AppFilterItem(
                        AppStateAppBatteryUsageBridge.FILTER_BATTERY_OPTIMIZED_APPS,
                        FILTER_APPS_BATTERY_OPTIMIZED,
                        R.string.filter_battery_optimized_title);

        // Unrestricted
        mFilters[FILTER_APPS_BATTERY_RESTRICTED] =
                new AppFilterItem(
                        AppStateAppBatteryUsageBridge.FILTER_BATTERY_RESTRICTED_APPS,
                        FILTER_APPS_BATTERY_RESTRICTED,
                        R.string.filter_battery_restricted_title);

        // Apps that can run long background tasks
        mFilters[FILTER_LONG_BACKGROUND_TASKS] = new AppFilterItem(
                AppStateLongBackgroundTasksBridge.FILTER_LONG_JOBS_APPS,
                FILTER_LONG_BACKGROUND_TASKS,
                R.string.long_background_tasks_title);

        // Apps that are cloneable or cloned.
        mFilters[FILTER_APPS_CLONE] =
                new AppFilterItem(
                        AppStateClonedAppsBridge.FILTER_APPS_CLONE,
                        FILTER_APPS_CLONE,
                        R.string.cloned_apps_dashboard_title);

        // Apps that are nfc tag allowlisted.
        mFilters[FILTER_APPS_NFC_TAG] =
                new AppFilterItem(
                        AppStateNfcTagAppsBridge.FILTER_APPS_NFC_TAG,
                        FILTER_APPS_NFC_TAG,
                        R.string.change_nfc_tag_apps_title);

        // Apps that are allowed to turn the screen on.
        mFilters[FILTER_APPS_TURN_SCREEN_ON] = new AppFilterItem(
                AppStateTurnScreenOnBridge.FILTER_TURN_SCREEN_ON_APPS,
                FILTER_APPS_TURN_SCREEN_ON,
                com.android.settingslib.R.string.turn_screen_on_title);
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
                return FILTER_APPS_POWER_ALLOWLIST;
            case ManageApplications.LIST_TYPE_OVERLAY:
                return FILTER_APPS_WITH_OVERLAY;
            case ManageApplications.LIST_TYPE_WRITE_SETTINGS:
                return FILTER_APPS_WRITE_SETTINGS;
            case ManageApplications.LIST_TYPE_MANAGE_SOURCES:
                return FILTER_APPS_INSTALL_SOURCES;
            case ManageApplications.LIST_TYPE_WIFI_ACCESS:
                return FILTER_APP_CAN_CHANGE_WIFI_STATE;
            case ManageApplications.LIST_TYPE_NOTIFICATION:
                return FILTER_APPS_RECENT;
            case ManageApplications.LIST_MANAGE_EXTERNAL_STORAGE:
                return FILTER_MANAGE_EXTERNAL_STORAGE;
            case ManageApplications.LIST_TYPE_ALARMS_AND_REMINDERS:
                return FILTER_ALARMS_AND_REMINDERS;
            case ManageApplications.LIST_TYPE_MEDIA_MANAGEMENT_APPS:
                return FILTER_APPS_MEDIA_MANAGEMENT;
            case ManageApplications.LIST_TYPE_APPS_LOCALE:
                return FILTER_APPS_LOCALE;
            case ManageApplications.LIST_TYPE_BATTERY_OPTIMIZATION:
                return FILTER_APPS_BATTERY_OPTIMIZED;
            case ManageApplications.LIST_TYPE_LONG_BACKGROUND_TASKS:
                return FILTER_LONG_BACKGROUND_TASKS;
            case ManageApplications.LIST_TYPE_CLONED_APPS:
                return FILTER_APPS_CLONE;
            case ManageApplications.LIST_TYPE_NFC_TAG_APPS:
                return FILTER_APPS_NFC_TAG;
            case ManageApplications.LIST_TYPE_TURN_SCREEN_ON:
                return FILTER_APPS_TURN_SCREEN_ON;
            default:
                return FILTER_APPS_ALL;
        }
    }

    public AppFilterItem get(@FilterType int filterType) {
        return mFilters[filterType];
    }
}
