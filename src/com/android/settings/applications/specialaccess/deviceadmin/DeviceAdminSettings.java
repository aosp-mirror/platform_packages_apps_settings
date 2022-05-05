/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings.applications.specialaccess.deviceadmin;

import static android.app.admin.DevicePolicyResources.Strings.Settings.MANAGE_DEVICE_ADMIN_APPS;
import static android.app.admin.DevicePolicyResources.Strings.Settings.NO_DEVICE_ADMINS;

import android.app.settings.SettingsEnums;
import android.os.Bundle;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

@SearchIndexable
public class DeviceAdminSettings extends DashboardFragment {
    static final String TAG = "DeviceAdminSettings";

    public int getMetricsCategory() {
        return SettingsEnums.DEVICE_ADMIN_SETTINGS;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        replaceEnterprisePreferenceScreenTitle(
                MANAGE_DEVICE_ADMIN_APPS, R.string.manage_device_admin);
        replaceEnterpriseStringTitle("device_admin_footer",
                NO_DEVICE_ADMINS, R.string.no_device_admins);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.device_admin_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.device_admin_settings);
}
