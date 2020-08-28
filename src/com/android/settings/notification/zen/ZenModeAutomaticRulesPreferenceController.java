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

import java.util.Map;
import java.util.Objects;

public class ZenModeAutomaticRulesPreferenceController extends
        AbstractZenModeAutomaticRulePreferenceController {

    protected static final String KEY = "zen_mode_automatic_rules";

    @VisibleForTesting
    protected PreferenceCategory mPreferenceCategory;

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
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        Map.Entry<String, AutomaticZenRule>[] sortedRules = getRules();
        final int currNumPreferences = mPreferenceCategory.getPreferenceCount();
        if (currNumPreferences == sortedRules.length) {
            for (int i = 0; i < sortedRules.length; i++) {
                ZenRulePreference pref = (ZenRulePreference) mPreferenceCategory.getPreference(i);
                // we are either:
                // 1. updating everything about the rule
                // 2. rule was added or deleted, so reload the entire list
                if (Objects.equals(pref.mId, sortedRules[i].getKey())) {
                    AutomaticZenRule rule = sortedRules[i].getValue();
                    pref.updatePreference(rule);
                } else {
                    reloadAllRules(sortedRules);
                    break;
                }
            }
        } else {
            reloadAllRules(sortedRules);
        }
    }

    @VisibleForTesting
    void reloadAllRules(Map.Entry<String, AutomaticZenRule>[] rules) {
        mPreferenceCategory.removeAll();
        for (Map.Entry<String, AutomaticZenRule> rule : rules) {
            ZenRulePreference pref = createZenRulePreference(rule);
            mPreferenceCategory.addPreference(pref);
        }
    }

    @VisibleForTesting
    ZenRulePreference createZenRulePreference(Map.Entry<String, AutomaticZenRule> rule) {
        return new ZenRulePreference(mPreferenceCategory.getContext(),
                rule, mParent, mMetricsFeatureProvider);
    }
}
