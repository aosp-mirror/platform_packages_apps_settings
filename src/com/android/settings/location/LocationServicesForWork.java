/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.location;

import android.app.settings.SettingsEnums;
import android.content.Context;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;

/**
 * A page that configures the Location Services settings for work profile.
 *
 * Note that this page should not provide indexes because it is only used in the two tabs
 * {@link ProfileSelectFragment} scenario and should not be launched independently.
 */
public class LocationServicesForWork extends DashboardFragment {
    private static final String TAG = "LocationServicesForWork";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.LOCATION_SERVICES_FOR_WORK;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.location_services_workprofile;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        use(LocationInjectedServicesForWorkPreferenceController.class).init(this);
    }
}
