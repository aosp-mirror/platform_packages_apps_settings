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

import android.content.Context;
import android.content.res.Resources;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.search.SearchIndexableRaw;

import java.util.List;

/**
 * Preference controller for accessibility button preference.
 */
public class AccessibilityButtonPreferenceController extends BasePreferenceController {

    public AccessibilityButtonPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final Preference preference = screen.findPreference(getPreferenceKey());
        preference.setTitle(getPreferenceTitleResource());

    }

    @Override
    public void updateDynamicRawDataToIndex(List<SearchIndexableRaw> rawData) {
        SearchIndexableRaw data = new SearchIndexableRaw(mContext);
        data.key = getPreferenceKey();
        final Resources res = mContext.getResources();
        data.title = res.getString(getPreferenceTitleResource());
        data.screenTitle = res.getString(R.string.accessibility_shortcuts_settings_title);
        rawData.add(data);
    }

    private int getPreferenceTitleResource() {
        return AccessibilityUtil.isGestureNavigateEnabled(mContext)
                ? R.string.accessibility_button_gesture_title : R.string.accessibility_button_title;
    }
}
