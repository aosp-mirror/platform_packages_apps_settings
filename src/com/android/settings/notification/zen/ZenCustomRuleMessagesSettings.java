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
import android.service.notification.ZenPolicy;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.widget.FooterPreference;

import java.util.ArrayList;
import java.util.List;

public class ZenCustomRuleMessagesSettings extends ZenCustomRuleSettingsBase {
    private static final String MESSAGES_KEY = "zen_mode_messages";
    private static final String STARRED_CONTACTS_KEY = "zen_mode_starred_contacts_messages";
    private static final String PREFERENCE_CATEGORY_KEY = "zen_mode_settings_category_messages";

    @Override
    protected int getPreferenceScreenResId() {
        return com.android.settings.R.xml.zen_mode_custom_rule_messages_settings;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ZEN_CUSTOM_RULE_MESSAGES;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        mControllers = new ArrayList<>();
        mControllers.add(new ZenRuleMessagesPreferenceController(context, MESSAGES_KEY,
                getSettingsLifecycle()));
        mControllers.add(new ZenRuleStarredContactsPreferenceController(context,
                getSettingsLifecycle(), ZenPolicy.PRIORITY_CATEGORY_MESSAGES,
                STARRED_CONTACTS_KEY));
        return mControllers;
    }

    @Override
    String getPreferenceCategoryKey() {
        return PREFERENCE_CATEGORY_KEY;
    }

    @Override
    public void updatePreferences() {
        super.updatePreferences();
        PreferenceScreen screen = getPreferenceScreen();
        Preference footerPreference = screen.findPreference(FooterPreference.KEY_FOOTER);
        footerPreference.setTitle(mContext.getResources().getString(
                R.string.zen_mode_custom_messages_footer, mRule.getName()));
    }
}
