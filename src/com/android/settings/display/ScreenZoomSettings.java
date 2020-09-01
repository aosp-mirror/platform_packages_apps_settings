/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.display;

import android.annotation.Nullable;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Display;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.display.DisplayDensityConfiguration;
import com.android.settingslib.display.DisplayDensityUtils;
import com.android.settingslib.search.SearchIndexable;

/**
 * Preference fragment used to control screen zoom.
 */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class ScreenZoomSettings extends PreviewSeekBarPreferenceFragment {

    private int mDefaultDensity;
    private int[] mValues;

    @Override
    protected int getActivityLayoutResId() {
        return R.layout.screen_zoom_activity;
    }

    @Override
    protected int[] getPreviewSampleResIds() {
        return getContext().getResources().getBoolean(
                R.bool.config_enable_extra_screen_zoom_preview)
                ? new int[]{
                        R.layout.screen_zoom_preview_1,
                        R.layout.screen_zoom_preview_2,
                        R.layout.screen_zoom_preview_settings}
                : new int[]{R.layout.screen_zoom_preview_1};
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final DisplayDensityUtils density = new DisplayDensityUtils(getContext());

        final int initialIndex = density.getCurrentIndex();
        if (initialIndex < 0) {
            // Failed to obtain default density, which means we failed to
            // connect to the window manager service. Just use the current
            // density and don't let the user change anything.
            final int densityDpi = getResources().getDisplayMetrics().densityDpi;
            mValues = new int[]{densityDpi};
            mEntries = new String[]{getString(DisplayDensityUtils.SUMMARY_DEFAULT)};
            mInitialIndex = 0;
            mDefaultDensity = densityDpi;
        } else {
            mValues = density.getValues();
            mEntries = density.getEntries();
            mInitialIndex = initialIndex;
            mDefaultDensity = density.getDefaultDensity();
        }

        getActivity().setTitle(R.string.screen_zoom_title);
    }

    @Override
    protected Configuration createConfig(Configuration origConfig, int index) {
        // Populate the sample layouts.
        final Configuration config = new Configuration(origConfig);
        config.densityDpi = mValues[index];
        return config;
    }

    /**
     * Persists the selected density and sends a configuration change.
     */
    @Override
    protected void commit() {
        final int densityDpi = mValues[mCurrentIndex];
        if (densityDpi == mDefaultDensity) {
            DisplayDensityConfiguration.clearForcedDisplayDensity(Display.DEFAULT_DISPLAY);
        } else {
            DisplayDensityConfiguration.setForcedDisplayDensity(Display.DEFAULT_DISPLAY, densityDpi);
        }
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_display_size;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DISPLAY_SCREEN_ZOOM;
    }

    /** Index provider used to expose this fragment in search. */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return false;
                }
            };
}
