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

package com.android.settings.accessibility;

import android.app.settings.SettingsEnums;
import android.content.Context;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

/**
 * Accessibility settings for adjusting the system features which are related to the reading. For
 * example, bold text, high contrast text, display size, font size and so on.
 */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class TextReadingPreferenceFragment extends DashboardFragment {
    private static final String TAG = "TextReadingPreferenceFragment";
    private static final String FONT_SIZE_KEY = "font_size";
    private static final String DISPLAY_SIZE_KEY = "display_size";
    private static final String PREVIEW_KEY = "preview";

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_text_reading_options;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_TEXT_READING_OPTIONS;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        final FontSizeData fontSizeData = new FontSizeData(context);
        final DisplaySizeData displaySizeData = new DisplaySizeData(context);

        final TextReadingPreviewController previewController = new TextReadingPreviewController(
                context, PREVIEW_KEY, fontSizeData, displaySizeData);
        controllers.add(previewController);

        final PreviewSizeSeekBarController fontSizeController = new PreviewSizeSeekBarController(
                context, FONT_SIZE_KEY, fontSizeData);
        fontSizeController.setInteractionListener(previewController);
        controllers.add(fontSizeController);

        final PreviewSizeSeekBarController displaySizeController = new PreviewSizeSeekBarController(
                context, DISPLAY_SIZE_KEY, displaySizeData);
        displaySizeController.setInteractionListener(previewController);
        controllers.add(displaySizeController);

        return controllers;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.accessibility_text_reading_options);
}
