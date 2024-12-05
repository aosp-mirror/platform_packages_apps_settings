/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.network.telephony;

import android.os.UserManager;

import com.android.settings.R;
import com.android.settings.dashboard.RestrictedDashboardFragment;

public class SatelliteSettingsSosFragment extends RestrictedDashboardFragment {


    public SatelliteSettingsSosFragment() {
        super(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS);
    }

    @Override
    public int getMetricsCategory() {
        return 0;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.satellite_setting_sos;
    }

    @Override
    protected String getLogTag() {
        return "";
    }
}
