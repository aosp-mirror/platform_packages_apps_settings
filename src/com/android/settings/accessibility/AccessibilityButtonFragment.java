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

package com.android.settings.accessibility;

import android.app.settings.SettingsEnums;
import android.os.Bundle;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

/** Settings fragment containing accessibility button properties. */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class AccessibilityButtonFragment extends DashboardFragment {

    private static final String TAG = "AccessibilityButtonFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final int titleResource;
        if (android.provider.Flags.a11yStandaloneGestureEnabled()) {
            titleResource = R.string.accessibility_button_title;
        } else {
            titleResource = AccessibilityUtil.isGestureNavigateEnabled(getPrefContext())
                    ? R.string.accessibility_button_gesture_title
                    : R.string.accessibility_button_title;
        }
        getActivity().setTitle(titleResource);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_button_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_BUTTON_SETTINGS;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.accessibility_button_settings);
}
