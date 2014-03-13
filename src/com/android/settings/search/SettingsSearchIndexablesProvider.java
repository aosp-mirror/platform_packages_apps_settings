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

import android.database.Cursor;
import android.database.MatrixCursor;
import android.provider.SearchIndexableResource;
import android.provider.SearchIndexablesProvider;
import com.android.settings.DateTimeSettings;
import com.android.settings.DevelopmentSettings;
import com.android.settings.DeviceInfoSettings;
import com.android.settings.DisplaySettings;
import com.android.settings.HomeSettings;
import com.android.settings.PrivacySettings;
import com.android.settings.R;
import com.android.settings.SecuritySettings;
import com.android.settings.SoundSettings;
import com.android.settings.WallpaperTypeSettings;
import com.android.settings.WirelessSettings;
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

import static android.provider.SearchIndexablesContract.INDEXABLES_XML_RES_COLUMNS;
import static android.provider.SearchIndexablesContract.INDEXABLES_RAW_COLUMNS;

public class SettingsSearchIndexablesProvider extends SearchIndexablesProvider {
    private static final String TAG = "SettingsSearchIndexablesProvider";

    private static int NO_DATA_RES_ID = 0;

    private static SearchIndexableResource[] INDEXABLE_REFS = new SearchIndexableResource[] {
            new SearchIndexableResource(1, NO_DATA_RES_ID,
                    WifiSettings.class.getName(),
                    R.drawable.ic_settings_wireless),
            new SearchIndexableResource(2, R.xml.bluetooth_settings,
                    BluetoothSettings.class.getName(),
                    R.drawable.ic_settings_bluetooth2),
            new SearchIndexableResource(3, R.xml.data_usage_metered_prefs,
                    DataUsageMeteredSettings.class.getName(),
                    R.drawable.ic_settings_data_usage),
            new SearchIndexableResource(4, R.xml.wireless_settings,
                    WirelessSettings.class.getName(),
                    R.drawable.empty_icon),
            new SearchIndexableResource(5, R.xml.home_selection,
                    HomeSettings.class.getName(),
                    R.drawable.ic_settings_home),
            new SearchIndexableResource(6, R.xml.sound_settings,
                    SoundSettings.class.getName(),
                    R.drawable.ic_settings_sound),
            new SearchIndexableResource(7, R.xml.display_settings,
                    DisplaySettings.class.getName(),
                    R.drawable.ic_settings_display),
            new SearchIndexableResource(7, NO_DATA_RES_ID,
                    WallpaperTypeSettings.class.getName(),
                    R.drawable.ic_settings_display),
            new SearchIndexableResource(8, R.xml.device_info_memory,
                    Memory.class.getName(),
                    R.drawable.ic_settings_storage),
            new SearchIndexableResource(9, R.xml.power_usage_summary,
                    PowerUsageSummary.class.getName(),
                    R.drawable.ic_settings_battery),
            new SearchIndexableResource(10, R.xml.user_settings,
                    UserSettings.class.getName(),
                    R.drawable.ic_settings_multiuser),
            new SearchIndexableResource(11, R.xml.location_settings,
                    LocationSettings.class.getName(),
                    R.drawable.ic_settings_location),
            new SearchIndexableResource(12, R.xml.security_settings,
                    SecuritySettings.class.getName(),
                    R.drawable.ic_settings_security),
            new SearchIndexableResource(13, R.xml.language_settings,
                    InputMethodAndLanguageSettings.class.getName(),
                    R.drawable.ic_settings_language),
            new SearchIndexableResource(14, R.xml.privacy_settings,
                    PrivacySettings.class.getName(),
                    R.drawable.ic_settings_backup),
            new SearchIndexableResource(15, R.xml.date_time_prefs,
                    DateTimeSettings.class.getName(),
                    R.drawable.ic_settings_date_time),
            new SearchIndexableResource(16, R.xml.accessibility_settings,
                    AccessibilitySettings.class.getName(),
                    R.drawable.ic_settings_accessibility),
            new SearchIndexableResource(17, R.xml.print_settings,
                    PrintSettingsFragment.class.getName(),
                    com.android.internal.R.drawable.ic_print),
            new SearchIndexableResource(18, R.xml.development_prefs,
                    DevelopmentSettings.class.getName(),
                    R.drawable.ic_settings_development),
            new SearchIndexableResource(19, R.xml.device_info_settings,
                    DeviceInfoSettings.class.getName(),
                    R.drawable.ic_settings_about),
    };

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor queryXmlResources(String[] projection) {
        MatrixCursor cursor = new MatrixCursor(INDEXABLES_XML_RES_COLUMNS);
        final int count = INDEXABLE_REFS.length;
        for (int n = 0; n < count; n++) {
            Object[] ref = new Object[7];
            ref[0] = INDEXABLE_REFS[n].rank;
            ref[1] = INDEXABLE_REFS[n].xmlResId;
            ref[2] = INDEXABLE_REFS[n].className;
            ref[3] = INDEXABLE_REFS[n].iconResId;
            ref[4] = null; // intent action
            ref[5] = null; // intent target package
            ref[6] = null; // intent target class
            cursor.addRow(ref);
        }
        return cursor;
    }

    @Override
    public Cursor queryRawData(String[] projection) {
        Cursor result = new MatrixCursor(INDEXABLES_RAW_COLUMNS);
        return result;
    }
}
