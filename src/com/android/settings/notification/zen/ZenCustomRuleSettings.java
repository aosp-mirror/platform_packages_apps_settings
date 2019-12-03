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

import android.app.settings.SettingsEnums;
import android.content.Context;

import com.android.settings.R;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

public class ZenCustomRuleSettings extends ZenCustomRuleSettingsBase {
    private static final String RULE_DEFAULT_POLICY_KEY = "zen_custom_rule_setting_default";
    private static final String CUSTOM_RULE_POLICY_KEY = "zen_custom_rule_setting";
    private static final String PREFERENCE_CATEGORY_KEY = "zen_custom_rule_category";

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.zen_mode_custom_rule_settings;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ZEN_CUSTOM_RULE_SETTINGS;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        mControllers = new ArrayList<>();
        mControllers.add(new ZenRuleDefaultPolicyPreferenceController(
                context, getSettingsLifecycle(), RULE_DEFAULT_POLICY_KEY));
        mControllers.add(new ZenRuleCustomPolicyPreferenceController(
                context, getSettingsLifecycle(), CUSTOM_RULE_POLICY_KEY));
        return mControllers;
    }

    @Override
    String getPreferenceCategoryKey() {
        return PREFERENCE_CATEGORY_KEY;
    }
}
