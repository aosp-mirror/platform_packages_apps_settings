/*
 * Copyright (C) 2018 The Android Open Source Project
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
import android.os.Bundle;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

abstract class ZenCustomRuleSettingsBase extends ZenModeSettingsBase {
    static final String TAG = "ZenCustomRuleSettings";
    static final String RULE_ID = "RULE_ID";

    String mId;
    AutomaticZenRule mRule;
    List<AbstractPreferenceController> mControllers = new ArrayList<>();
    private boolean mIsFirstLaunch = true;

    /**
     * @return null if no preference category exists
     */
    abstract String getPreferenceCategoryKey();

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Bundle bundle = getArguments();
        if (bundle != null && bundle.containsKey(RULE_ID)) {
            mId = bundle.getString(RULE_ID);
            updateRule();
        } else {
            Log.d(TAG, "Rule id required to set custom dnd rule config settings");
            this.finish();
        }
    }

    @Override
    public void onResume() {
        if (!mIsFirstLaunch) {
            // Rule will be used in updatePreferenceStates() in super.onResume().
            updateRule();
        }
        super.onResume();
        updatePreferences();
    }

    @Override
    public void onZenModeConfigChanged() {
        super.onZenModeConfigChanged();
        updateRule();
        updatePreferences();
        updatePreferenceStates();
    }

    private void updateRule() {
        mRule = mBackend.getAutomaticZenRule(mId);
        for (AbstractPreferenceController controller : mControllers) {
            AbstractZenCustomRulePreferenceController zenRuleController =
                    (AbstractZenCustomRulePreferenceController) controller;
            zenRuleController.setIdAndRule(mId, mRule);
        }
    }

    public void updatePreferences() {
        final PreferenceScreen screen = getPreferenceScreen();
        String categoryKey = getPreferenceCategoryKey();
        if (categoryKey != null) {
            Preference prefCategory = screen.findPreference(categoryKey);
            if (prefCategory != null) {
                prefCategory.setTitle(mContext.getResources().getString(
                        com.android.settings.R.string.zen_mode_custom_behavior_category_title,
                        mRule.getName()));
            }
        }

        for (AbstractPreferenceController controller : mControllers) {
            AbstractZenCustomRulePreferenceController zenRuleController =
                    (AbstractZenCustomRulePreferenceController) controller;
            zenRuleController.onResume();
            if (!mIsFirstLaunch) {
                // In first launch, displayPreference() is already called in DashboardFragment's
                // onCreate().
                zenRuleController.displayPreference(screen);
            }
        }

        mIsFirstLaunch = false;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_uri_interruptions;
    }

    Bundle createZenRuleBundle() {
        Bundle bundle = new Bundle();
        bundle.putString(RULE_ID, mId);
        return bundle;
    }
}
