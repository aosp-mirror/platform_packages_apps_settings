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

package com.android.settings.notification;

import android.app.AutomaticZenRule;
import android.app.Fragment;
import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import java.util.Map;

public class ZenModeAutomaticRulesPreferenceController extends
        AbstractZenModeAutomaticRulePreferenceController {

    private final String KEY_AUTOMATIC_RULES;
    private PreferenceCategory mPreferenceCategory;
    Map.Entry<String, AutomaticZenRule>[] mSortedRules;

    public ZenModeAutomaticRulesPreferenceController(Context context, String key,
            Fragment parent) {
        super(context, parent);
        KEY_AUTOMATIC_RULES = key;
        mSortedRules = sortedRules();
    }

    @Override
    public String getPreferenceKey() {
        return KEY_AUTOMATIC_RULES;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceCategory = (PreferenceCategory) screen.findPreference(getPreferenceKey());
        mPreferenceCategory.setPersistent(false);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        // no need to update AutomaticRule if a rule was deleted
        // (on rule deletion, the preference removes itself from its parent)
        int oldRuleLength = mSortedRules.length;
        mSortedRules = sortedRules();
        if  (!wasRuleDeleted(oldRuleLength)) {
            updateAutomaticRules();
        }
    }

    private boolean wasRuleDeleted(int oldRuleLength) {
        int newRuleLength = mSortedRules.length;
        int prefCount =  mPreferenceCategory.getPreferenceCount();

        return (prefCount == oldRuleLength -1) && (prefCount == newRuleLength);
    }

    private void updateAutomaticRules() {
        for (Map.Entry<String, AutomaticZenRule> sortedRule : mSortedRules) {
            ZenRulePreference currPref = (ZenRulePreference)
                    mPreferenceCategory.findPreference(sortedRule.getKey());
            if (currPref != null && currPref.appExists) {
                // rule already exists in preferences, update it
                currPref.setAttributes(sortedRule.getValue());
            } else {
                // rule doesn't exist in preferences, add it
                ZenRulePreference pref = new ZenRulePreference(mPreferenceCategory.getContext(),
                        sortedRule, mPreferenceCategory);
                if (pref.appExists) {
                    mPreferenceCategory.addPreference(pref);
                }
            }
        }

    }
}



