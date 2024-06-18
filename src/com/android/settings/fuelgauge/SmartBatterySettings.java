/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.fuelgauge;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.provider.SearchIndexableResource;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Fragment to show smart battery and restricted app controls */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class SmartBatterySettings extends DashboardFragment {
    public static final String TAG = "SmartBatterySettings";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.OPEN_BATTERY_ADAPTIVE_PREFERENCES;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.smart_battery_detail;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_uri_smart_battery_settings;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, (SettingsActivity) getActivity(), this);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(
            Context context,
            SettingsActivity settingsActivity,
            InstrumentedPreferenceFragment fragment) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        if (settingsActivity != null && fragment != null) {
            controllers.add(new RestrictAppPreferenceController(fragment));
        } else {
            controllers.add(new RestrictAppPreferenceController(context));
        }

        return controllers;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.smart_battery_detail;
                    return Arrays.asList(sir);
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return buildPreferenceControllers(context, null, null);
                }

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return FeatureFactory.getFeatureFactory()
                            .getPowerUsageFeatureProvider()
                            .isSmartBatterySupported();
                }
            };
}
