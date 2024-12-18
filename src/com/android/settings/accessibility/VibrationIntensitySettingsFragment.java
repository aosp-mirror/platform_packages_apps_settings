/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.VisibleForTesting;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

/** Accessibility settings for the vibration. */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class VibrationIntensitySettingsFragment extends DashboardFragment {

    private static final String TAG = "VibrationIntensitySettings";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_VIBRATION;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_uri_accessibility_vibration;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_vibration_intensity_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @VisibleForTesting
    static boolean isPageSearchEnabled(Context context) {
        final int supportedIntensityLevels = context.getResources().getInteger(
                R.integer.config_vibration_supported_intensity_levels);
        final boolean hasVibrator = context.getSystemService(Vibrator.class).hasVibrator();
        return hasVibrator && supportedIntensityLevels > 1;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        final Resources res = view.getResources();
        final RecyclerView rv = getListView();
        if (rv != null) {
            final int bottom_padding = res.getDimensionPixelSize(
                    com.android.settingslib.widget.theme.R.dimen
                            .settingslib_listPreferredItemPaddingEnd);
            rv.setPaddingRelative(rv.getPaddingStart(), rv.getPaddingTop(), rv.getPaddingEnd(),
                    rv.getPaddingBottom() + bottom_padding);
        }
        return view;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.accessibility_vibration_intensity_settings) {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return VibrationIntensitySettingsFragment.isPageSearchEnabled(context);
                }
            };
}
