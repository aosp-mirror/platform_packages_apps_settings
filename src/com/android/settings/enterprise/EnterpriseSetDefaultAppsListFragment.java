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
 * limitations under the License
 */

package com.android.settings.enterprise;

import android.app.settings.SettingsEnums;
import android.content.Context;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for displaying a list of default applications set by profile or device admin.
 */
public class EnterpriseSetDefaultAppsListFragment extends DashboardFragment {
    static final String TAG = "EnterprisePrivacySettings";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ENTERPRISE_PRIVACY_DEFAULT_APPS;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.enterprise_set_default_apps_settings;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        final EnterpriseSetDefaultAppsListPreferenceController controller =
                new EnterpriseSetDefaultAppsListPreferenceController(
                        context, this, context.getPackageManager());
        controllers.add(controller);
        return controllers;
    }
}
