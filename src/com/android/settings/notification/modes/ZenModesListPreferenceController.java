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
package com.android.settings.notification.modes;

import android.app.Flags;
import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.notification.modes.ZenIconLoader;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;
import com.android.settingslib.search.SearchIndexableRaw;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for the PreferenceCategory on the modes aggregator page ({@link ZenModesListFragment})
 * containing links to each individual mode. This is a central controller that populates and updates
 * all the preferences that then lead to a mode configuration page.
 */
class ZenModesListPreferenceController extends BasePreferenceController
        implements BasePreferenceController.UiBlocker {
    protected static final String KEY = "zen_modes_list";

    private final ZenModesBackend mBackend;
    private final ZenIconLoader mIconLoader;

    ZenModesListPreferenceController(Context context, @NonNull ZenModesBackend backend, @NonNull
            ZenIconLoader iconLoader) {
        super(context, KEY);
        mBackend = backend;
        mIconLoader = iconLoader;
    }

    @Override
    @AvailabilityStatus
    public int getAvailabilityStatus() {
        return Flags.modesUi() ? AVAILABLE_UNSEARCHABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void updateState(Preference preference) {
        if (mBackend == null) {
            return;
        }

        // The preference given us is a PreferenceCategory; create one preference inside the
        // category for each rule that exists.
        PreferenceCategory category = (PreferenceCategory) preference;

        Map<String, ZenModesListItemPreference> originalPreferences = new HashMap<>();
        for (int i = 0; i < category.getPreferenceCount(); i++) {
            ZenModesListItemPreference pref = (ZenModesListItemPreference) category.getPreference(
                    i);
            originalPreferences.put(pref.getKey(), pref);
        }

        // Loop through each rule, either updating the existing rule or creating the rule's
        // preference
        List<ZenMode> modes = mBackend.getModes();
        for (ZenMode mode : modes) {
            ZenModesListItemPreference modePreference = originalPreferences.get(mode.getId());
            if (modePreference != null) {
                // existing rule; update its info if it's changed since the last display
                modePreference.setZenMode(mode);
            } else {
                // new rule; create a new ZenRulePreference & add it to the preference category
                modePreference = new ZenModesListItemPreference(mContext, mIconLoader, mode);
                category.addPreference(modePreference);
            }
            modePreference.setOrder(modes.indexOf(mode));

            originalPreferences.remove(mode.getId());
        }
        // Remove preferences that no longer have a rule
        for (String key : originalPreferences.keySet()) {
            category.removePreferenceRecursively(key);
        }

        setUiBlockerFinished(true);
    }

    // Provide search data for the modes, which will allow users to reach the modes page if they
    // search for a mode name.
    @Override
    public void updateDynamicRawDataToIndex(List<SearchIndexableRaw> rawData) {
        // Don't add anything if flag is off. In theory this preference controller itself shouldn't
        // be available in that case, but we check anyway to be sure.
        if (!Flags.modesUi()) {
            return;
        }
        if (mBackend == null) {
            return;
        }

        final Resources res = mContext.getResources();
        for (ZenMode mode : mBackend.getModes()) {
            SearchIndexableRaw data = new SearchIndexableRaw(mContext);
            data.key = mode.getId();
            data.title = mode.getName();
            data.screenTitle = res.getString(R.string.zen_modes_list_title);
            rawData.add(data);
        }
    }
}
