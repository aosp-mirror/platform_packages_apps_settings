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
import android.util.ArrayMap;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.Map;

public class ZenModeAutomaticRulesPreferenceController extends
        AbstractZenModeAutomaticRulePreferenceController {

    protected static final String KEY = "zen_mode_automatic_rules";

    @VisibleForTesting
    protected PreferenceCategory mPreferenceCategory;

    // Map of rule key -> preference so that we can update each preference as needed
    @VisibleForTesting
    protected Map<String, ZenRulePreference> mZenRulePreferences = new ArrayMap<>();

    public ZenModeAutomaticRulesPreferenceController(Context context, Fragment parent, Lifecycle
            lifecycle) {
        super(context, KEY, parent, lifecycle);
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
        mPreferenceCategory = screen.findPreference(getPreferenceKey());
        mPreferenceCategory.setPersistent(false);

        // if mPreferenceCategory was un-set, make sure to clear out mZenRulePreferences too, just
        // in case
        if (mPreferenceCategory.getPreferenceCount() == 0) {
            mZenRulePreferences.clear();
        }
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        Map.Entry<String, AutomaticZenRule>[] sortedRules = getRules();

        // refresh the whole preference category list if the total number of rules has changed, or
        // if any individual rules have changed, so we can rebuild the list & keep things in sync
        boolean refreshPrefs = false;
        if (mPreferenceCategory.getPreferenceCount() != sortedRules.length) {
            refreshPrefs = true;
        } else {
            // check whether any rules in sortedRules are not in mZenRulePreferences; that should
            // be enough to see whether something has changed
            for (int i = 0; i < sortedRules.length; i++) {
                if (!mZenRulePreferences.containsKey(sortedRules[i].getKey())) {
                    refreshPrefs = true;
                    break;
                }
            }
        }

        // if we need to refresh the whole list, clear the preference category and also start a
        // new map of preferences according to the preference category contents
        // we need to not update the existing one yet, as we'll need to know what preferences
        // previously existed in order to update and re-attach them to the preference category
        Map<String, ZenRulePreference> newPrefs = new ArrayMap<>();
        if (refreshPrefs) {
            mPreferenceCategory.removeAll();
        }

        // Loop through each rule, either updating the existing rule or creating the rule's
        // preference if needed (and, in the case where we need to rebuild the preference category
        // list, do so as well)
        for (int i = 0; i < sortedRules.length; i++) {
            String key = sortedRules[i].getKey();
            if (mZenRulePreferences.containsKey(key)) {
                // existing rule; update its info if it's changed since the last display
                AutomaticZenRule rule = sortedRules[i].getValue();
                ZenRulePreference pref = mZenRulePreferences.get(key);
                pref.updatePreference(rule);

                // only add to preference category if the overall set of rules has changed so this
                // needs to be rearranged
                if (refreshPrefs) {
                    mPreferenceCategory.addPreference(pref);
                    newPrefs.put(key, pref);
                }
            } else {
                // new rule; create a new ZenRulePreference & add it to the preference category
                // and the map so we'll know about it later
                ZenRulePreference pref = createZenRulePreference(sortedRules[i]);
                mPreferenceCategory.addPreference(pref);
                newPrefs.put(key, pref);
            }
        }

        // If anything was new, then make sure we overwrite mZenRulePreferences with our new data
        if (refreshPrefs) {
            mZenRulePreferences = newPrefs;
        }
    }

    @VisibleForTesting
    ZenRulePreference createZenRulePreference(Map.Entry<String, AutomaticZenRule> rule) {
        return new ZenRulePreference(mPreferenceCategory.getContext(),
                rule, mParent, mMetricsFeatureProvider);
    }
}
