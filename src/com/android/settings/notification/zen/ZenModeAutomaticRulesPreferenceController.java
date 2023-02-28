/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.notification.zen;

import android.app.AutomaticZenRule;
import android.content.Context;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ZenModeAutomaticRulesPreferenceController extends
        AbstractZenModeAutomaticRulePreferenceController {

    protected static final String KEY = "zen_mode_automatic_rules";

    Map.Entry<String, AutomaticZenRule>[] mSortedRules;

    public ZenModeAutomaticRulesPreferenceController(Context context, Fragment parent, Lifecycle
            lifecycle, ZenModeBackend backend) {
        super(context, KEY, parent, lifecycle);
        mBackend = backend;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        PreferenceCategory preferenceCategory = screen.findPreference(getPreferenceKey());
        preferenceCategory.setPersistent(false);
        mSortedRules = getRules();
        updateRules(preferenceCategory);
    }

    @Override
    public void updateState(Preference preference) {
        Map.Entry<String, AutomaticZenRule>[] sortedRules = getRules();
        boolean rulesChanged = false;
        if (sortedRules.length != mSortedRules.length) {
            rulesChanged = true;
        } else {
            for (int i = 0; i < mSortedRules.length; i++) {
                if (!Objects.equals(mSortedRules[i].getKey(), sortedRules[i].getKey())
                || !Objects.equals(mSortedRules[i].getValue(), sortedRules[i].getValue())) {
                    rulesChanged = true;
                    break;
                }
            }
        }

        if (rulesChanged) {
            mSortedRules = sortedRules;
            updateRules((PreferenceCategory) preference);
        }
    }

    private void updateRules(PreferenceCategory preferenceCategory) {
        Map<String, ZenRulePreference> originalPreferences = new HashMap<>();
        for (int i = 0; i < preferenceCategory.getPreferenceCount(); i++) {
            ZenRulePreference pref = (ZenRulePreference) preferenceCategory.getPreference(i);
            originalPreferences.put(pref.getKey(), pref);
        }

        // Loop through each rule, either updating the existing rule or creating the rule's
        // preference
        for (int i = 0; i < mSortedRules.length; i++) {
            String key = mSortedRules[i].getKey();

            if (originalPreferences.containsKey(key)) {
                // existing rule; update its info if it's changed since the last display
                AutomaticZenRule rule = mSortedRules[i].getValue();
                originalPreferences.get(key).updatePreference(rule);
            } else {
                // new rule; create a new ZenRulePreference & add it to the preference category
                ZenRulePreference pref = createZenRulePreference(
                        mSortedRules[i], preferenceCategory);
                preferenceCategory.addPreference(pref);
            }

            originalPreferences.remove(key);
        }
        // Remove preferences that no longer have a rule
        for (String key : originalPreferences.keySet()) {
            preferenceCategory.removePreferenceRecursively(key);
        }
    }

    @VisibleForTesting
    ZenRulePreference createZenRulePreference(Map.Entry<String, AutomaticZenRule> rule,
            PreferenceCategory preferenceCategory) {
        return new ZenRulePreference(preferenceCategory.getContext(),
                rule, mParent, mMetricsFeatureProvider, mBackend);
    }
}
