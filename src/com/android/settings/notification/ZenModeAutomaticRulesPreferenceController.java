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
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;

import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.Map;

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
        mPreferenceCategory = (PreferenceCategory) screen.findPreference(getPreferenceKey());
        mPreferenceCategory.setPersistent(false);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        mPreferenceCategory.removeAll();
        Map.Entry<String, AutomaticZenRule>[] sortedRules = sortedRules();
        for (Map.Entry<String, AutomaticZenRule> sortedRule : sortedRules) {
            ZenRulePreference pref = new ZenRulePreference(mPreferenceCategory.getContext(),
                    sortedRule, mParent, mMetricsFeatureProvider);
            mPreferenceCategory.addPreference(pref);
        }
    }
}



