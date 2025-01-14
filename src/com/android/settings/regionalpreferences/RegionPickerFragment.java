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

package com.android.settings.regionalpreferences;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;

import com.android.internal.app.LocaleStore;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.flags.Flags;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RegionPickerFragment extends DashboardFragment{

    private static final String TAG = "RegionPickerFragment";
    private static final String KEY_PREFERENCE_SYSTEM_REGION_LIST = "system_region_list";
    private static final String KEY_PREFERENCE_SYSTEM_REGION_SUGGESTED_LIST =
            "system_region_suggested_list";

    @Override
    public void onCreate(@NonNull Bundle icicle) {
        super.onCreate(icicle);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        MetricsFeatureProvider metricsFeatureProvider =
                FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
        String action = getIntent() != null ? getIntent().getAction() : "";
        if (Settings.ACTION_REGION_SETTINGS.equals(action)) {
            metricsFeatureProvider.action(
                    context, SettingsEnums.ACTION_OPEN_REGION_OUTSIDE_SETTINGS);
        }
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.system_region_picker;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.REGION_SETTINGS;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context);
    }

    private List<AbstractPreferenceController> buildPreferenceControllers(
            @NonNull Context context) {
        Locale parentLocale = LocaleStore.getLocaleInfo(Locale.getDefault()).getParent();
        LocaleStore.LocaleInfo parentLocaleInfo = LocaleStore.getLocaleInfo(parentLocale);
        SystemRegionSuggestedListPreferenceController mSuggestedListPreferenceController =
                new SystemRegionSuggestedListPreferenceController(
                    context, KEY_PREFERENCE_SYSTEM_REGION_SUGGESTED_LIST, parentLocaleInfo);
        SystemRegionAllListPreferenceController mSystemRegionAllListPreferenceController =
                new SystemRegionAllListPreferenceController(
                    context, KEY_PREFERENCE_SYSTEM_REGION_LIST, parentLocaleInfo);
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        mSuggestedListPreferenceController.setFragment(this);
        mSystemRegionAllListPreferenceController.setFragment(this);
        controllers.add(mSuggestedListPreferenceController);
        controllers.add(mSystemRegionAllListPreferenceController);
        return controllers;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.system_region_picker) {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    if (!Flags.regionalPreferencesApiEnabled()) {
                        return false;
                    }
                    return true;
                }
            };
}
