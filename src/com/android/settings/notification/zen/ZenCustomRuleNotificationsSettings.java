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
import com.android.settingslib.widget.FooterPreference;

import java.util.ArrayList;
import java.util.List;

public class ZenCustomRuleNotificationsSettings extends ZenCustomRuleSettingsBase {
    private static final String VIS_EFFECTS_ALL_KEY = "zen_mute_notifications";
    private static final String VIS_EFFECTS_NONE_KEY = "zen_hide_notifications";
    private static final String VIS_EFFECTS_CUSTOM_KEY = "zen_custom";
    private static final String PREFERENCE_CATEGORY_KEY = "restrict_category";

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.zen_mode_restrict_notifications_settings;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        mControllers = new ArrayList<>();
        mControllers.add(new ZenRuleVisEffectsAllPreferenceController(
                context, getSettingsLifecycle(), VIS_EFFECTS_ALL_KEY));
        mControllers.add(new ZenRuleVisEffectsNonePreferenceController(
                context, getSettingsLifecycle(), VIS_EFFECTS_NONE_KEY));
        mControllers.add(new ZenRuleVisEffectsCustomPreferenceController(
                context, getSettingsLifecycle(), VIS_EFFECTS_CUSTOM_KEY));
        mControllers.add(new ZenRuleNotifFooterPreferenceController(context, getSettingsLifecycle(),
                FooterPreference.KEY_FOOTER));
        return mControllers;
    }

    @Override
    String getPreferenceCategoryKey() {
        return PREFERENCE_CATEGORY_KEY;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ZEN_CUSTOM_RULE_NOTIFICATION_RESTRICTIONS;
    }
}
