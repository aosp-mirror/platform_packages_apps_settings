/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.communal;

import android.app.settings.SettingsEnums;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;

/**
 * Dashboard fragment for the top-level Communal settings.
 */
public class CommunalDashboardFragment extends DashboardFragment {
    private static final String TAG = "CommunalFragment";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.COMMUNAL_MODE_SETTINGS;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.communal_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }
}
