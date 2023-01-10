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

package com.android.settings.applications.manageapplications;

import static com.android.settings.applications.manageapplications.AppFilterRegistry.FILTER_ALARMS_AND_REMINDERS;
import static com.android.settings.applications.manageapplications.AppFilterRegistry.FILTER_APPS_ALL;
import static com.android.settings.applications.manageapplications.AppFilterRegistry.FILTER_APPS_BATTERY_OPTIMIZED;
import static com.android.settings.applications.manageapplications.AppFilterRegistry.FILTER_APPS_INSTALL_SOURCES;
import static com.android.settings.applications.manageapplications.AppFilterRegistry.FILTER_APPS_MEDIA_MANAGEMENT;
import static com.android.settings.applications.manageapplications.AppFilterRegistry.FILTER_APPS_POWER_ALLOWLIST;
import static com.android.settings.applications.manageapplications.AppFilterRegistry.FILTER_APPS_RECENT;
import static com.android.settings.applications.manageapplications.AppFilterRegistry.FILTER_APPS_USAGE_ACCESS;
import static com.android.settings.applications.manageapplications.AppFilterRegistry.FILTER_APPS_WITH_OVERLAY;
import static com.android.settings.applications.manageapplications.AppFilterRegistry.FILTER_APPS_WRITE_SETTINGS;
import static com.android.settings.applications.manageapplications.ManageApplications.LIST_TYPE_ALARMS_AND_REMINDERS;
import static com.android.settings.applications.manageapplications.ManageApplications.LIST_TYPE_BATTERY_OPTIMIZATION;
import static com.android.settings.applications.manageapplications.ManageApplications.LIST_TYPE_GAMES;
import static com.android.settings.applications.manageapplications.ManageApplications.LIST_TYPE_HIGH_POWER;
import static com.android.settings.applications.manageapplications.ManageApplications.LIST_TYPE_MAIN;
import static com.android.settings.applications.manageapplications.ManageApplications.LIST_TYPE_MANAGE_SOURCES;
import static com.android.settings.applications.manageapplications.ManageApplications.LIST_TYPE_MEDIA_MANAGEMENT_APPS;
import static com.android.settings.applications.manageapplications.ManageApplications.LIST_TYPE_NOTIFICATION;
import static com.android.settings.applications.manageapplications.ManageApplications.LIST_TYPE_OVERLAY;
import static com.android.settings.applications.manageapplications.ManageApplications.LIST_TYPE_STORAGE;
import static com.android.settings.applications.manageapplications.ManageApplications.LIST_TYPE_USAGE_ACCESS;
import static com.android.settings.applications.manageapplications.ManageApplications.LIST_TYPE_WRITE_SETTINGS;

import static com.google.common.truth.Truth.assertThat;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AppFilterRegistryTest {

    @Test
    public void getDefaultType_shouldMatchForAllListType() {
        final AppFilterRegistry registry = AppFilterRegistry.getInstance();
        assertThat(registry.getDefaultFilterType(LIST_TYPE_USAGE_ACCESS))
                .isEqualTo(FILTER_APPS_USAGE_ACCESS);
        assertThat(registry.getDefaultFilterType(LIST_TYPE_HIGH_POWER))
                .isEqualTo(FILTER_APPS_POWER_ALLOWLIST);
        assertThat(registry.getDefaultFilterType(LIST_TYPE_OVERLAY))
                .isEqualTo(FILTER_APPS_WITH_OVERLAY);
        assertThat(registry.getDefaultFilterType(LIST_TYPE_WRITE_SETTINGS))
                .isEqualTo(FILTER_APPS_WRITE_SETTINGS);
        assertThat(registry.getDefaultFilterType(LIST_TYPE_MANAGE_SOURCES))
                .isEqualTo(FILTER_APPS_INSTALL_SOURCES);

        assertThat(registry.getDefaultFilterType(LIST_TYPE_ALARMS_AND_REMINDERS))
                .isEqualTo(FILTER_ALARMS_AND_REMINDERS);

        assertThat(registry.getDefaultFilterType(LIST_TYPE_MEDIA_MANAGEMENT_APPS))
                .isEqualTo(FILTER_APPS_MEDIA_MANAGEMENT);
        assertThat(registry.getDefaultFilterType(LIST_TYPE_BATTERY_OPTIMIZATION))
                .isEqualTo(FILTER_APPS_BATTERY_OPTIMIZED);

        assertThat(registry.getDefaultFilterType(LIST_TYPE_MAIN)).isEqualTo(FILTER_APPS_ALL);
        assertThat(registry.getDefaultFilterType(LIST_TYPE_NOTIFICATION))
                .isEqualTo(FILTER_APPS_RECENT);
        assertThat(registry.getDefaultFilterType(LIST_TYPE_STORAGE)).isEqualTo(FILTER_APPS_ALL);

        assertThat(registry.getDefaultFilterType(LIST_TYPE_GAMES)).isEqualTo(FILTER_APPS_ALL);
    }
}
