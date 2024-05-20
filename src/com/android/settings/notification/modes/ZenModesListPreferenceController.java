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

import android.app.AutomaticZenRule;
import android.app.Flags;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.android.settingslib.core.AbstractPreferenceController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for the PreferenceCategory on the modes aggregator page ({@link ZenModesListFragment})
 * containing links to each individual mode. This is a central controller that populates and updates
 * all the preferences that then lead to a mode configuration page.
 */
public class ZenModesListPreferenceController extends AbstractPreferenceController {
    protected static final String KEY = "zen_modes_list";

    @Nullable
    protected Fragment mParent;
    protected ZenModesBackend mBackend;

    public ZenModesListPreferenceController(Context context, @Nullable Fragment parent,
            @NonNull ZenModesBackend backend) {
        super(context);
        mParent = parent;
        mBackend = backend;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public boolean isAvailable() {
        return Flags.modesUi();
    }

    @Override
    public void updateState(Preference preference) {
        if (mBackend == null) {
            return;
        }

        // The preference given us is a PreferenceCategory; create one preference inside the
        // category for each rule that exists.
        PreferenceCategory category = (PreferenceCategory) preference;

        Map<String, ZenModeListPreference> originalPreferences = new HashMap<>();
        for (int i = 0; i < category.getPreferenceCount(); i++) {
            ZenModeListPreference pref = (ZenModeListPreference) category.getPreference(i);
            originalPreferences.put(pref.getKey(), pref);
        }

        // Loop through each rule, either updating the existing rule or creating the rule's
        // preference
        for (ZenMode mode : mBackend.getModes()) {
            if (originalPreferences.containsKey(mode.getId())) {
                // existing rule; update its info if it's changed since the last display
                AutomaticZenRule rule = mode.getRule();
                originalPreferences.get(mode.getId()).setZenMode(mode);
            } else {
                // new rule; create a new ZenRulePreference & add it to the preference category
                Preference pref = new ZenModeListPreference(mContext, mode);
                category.addPreference(pref);
            }

            originalPreferences.remove(mode.getId());
        }
        // Remove preferences that no longer have a rule
        for (String key : originalPreferences.keySet()) {
            category.removePreferenceRecursively(key);
        }
    }
}
