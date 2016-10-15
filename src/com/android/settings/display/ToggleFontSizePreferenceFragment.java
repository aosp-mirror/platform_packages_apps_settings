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

package com.android.settings.display;

import android.annotation.Nullable;
import android.app.settings.SettingsEnums;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

/**
 * Preference fragment used to control font size.
 */
@SearchIndexable
public class ToggleFontSizePreferenceFragment extends PreviewSeekBarPreferenceFragment {

    private float[] mValues;

    @Override
    protected int getActivityLayoutResId() {
        return R.layout.font_size_activity;
    }

    @Override
    protected int[] getPreviewSampleResIds() {
        return new int[]{R.layout.font_size_preview};
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Resources res = getContext().getResources();
        final ContentResolver resolver = getContext().getContentResolver();
        // Mark the appropriate item in the preferences list.
        mEntries = res.getStringArray(R.array.entries_font_size_percent);
        final String[] strEntryValues = res.getStringArray(R.array.entryvalues_font_size);
        final float currentScale =
                Settings.System.getFloat(resolver, Settings.System.FONT_SCALE, 1.0f);
        mInitialIndex = fontSizeValueToIndex(currentScale, strEntryValues);
        mValues = new float[strEntryValues.length];
        for (int i = 0; i < strEntryValues.length; ++i) {
            mValues[i] = Float.parseFloat(strEntryValues[i]);
        }
        getActivity().setTitle(R.string.title_font_size);
    }

    @Override
    protected Configuration createConfig(Configuration origConfig, int index) {
        // Populate the sample layouts.
        final Configuration config = new Configuration(origConfig);
        config.fontScale = mValues[index];
        return config;
    }

    /**
     * Persists the selected font size.
     */
    @Override
    protected void commit() {
        if (getContext() == null) return;
        final ContentResolver resolver = getContext().getContentResolver();
        Settings.System.putFloat(resolver, Settings.System.FONT_SCALE, mValues[mCurrentIndex]);
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_font_size;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_FONT_SIZE;
    }

    /**
     * Utility function that returns the index in a string array with which the represented value is
     * the closest to a given float value.
     */
    public static int fontSizeValueToIndex(float val, String[] indices) {
        float lastVal = Float.parseFloat(indices[0]);
        for (int i = 1; i < indices.length; i++) {
            float thisVal = Float.parseFloat(indices[i]);
            if (val < (lastVal + (thisVal - lastVal) * .5f)) {
                return i - 1;
            }
            lastVal = thisVal;
        }
        return indices.length - 1;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return false;
                }
            };

}
