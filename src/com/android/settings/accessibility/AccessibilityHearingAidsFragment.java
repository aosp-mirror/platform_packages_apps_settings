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

import static android.os.UserManager.DISALLOW_CONFIG_BLUETOOTH;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.settings.R;
import com.android.settings.dashboard.RestrictedDashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

/** Accessibility settings for hearing aids. */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class AccessibilityHearingAidsFragment extends RestrictedDashboardFragment {

    private static final String TAG = "AccessibilityHearingAidsFragment";

    public AccessibilityHearingAidsFragment() {
        super(DISALLOW_CONFIG_BLUETOOTH);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // TODO(b/237625815): Add shortcutPreference by extending
        //  AccessibilityShortcutPreferenceFragment

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public int getMetricsCategory() {
        // TODO(b/262839191): To be updated settings_enums.proto
        return 0;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_hearing_aids;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.accessibility_hearing_aids);
}
