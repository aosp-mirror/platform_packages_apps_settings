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

import android.content.Context;
import android.net.Uri;
import android.provider.Settings;
import android.service.notification.ZenModeConfig;
import android.support.v7.preference.Preference;

import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class ZenModeSettingsFooterPreferenceController extends AbstractZenModePreferenceController {

    protected static final String KEY = "footer_preference";

    public ZenModeSettingsFooterPreferenceController(Context context, Lifecycle lifecycle) {
        super(context, KEY, lifecycle);
    }

    @Override
    public boolean isAvailable() {
        switch(getZenMode()) {
            case Settings.Global.ZEN_MODE_ALARMS:
            case Settings.Global.ZEN_MODE_NO_INTERRUPTIONS:
            case Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS:
                return true;
            case Settings.Global.ZEN_MODE_OFF:
            default:
                return false;
        }
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        boolean isAvailable = isAvailable();
        preference.setVisible(isAvailable);
        if (isAvailable) {
            preference.setTitle(getFooterText());
        }
    }

    protected String getFooterText() {
        ZenModeConfig config = getZenModeConfig();
        String footerText = "";
        long latestEndTime = -1;

        // DND turned on by manual rule
        if (config.manualRule != null) {
            final Uri id = config.manualRule.conditionId;
            if (config.manualRule.enabler != null) {
                // app triggered manual rule
                String appOwner = mZenModeConfigWrapper.getOwnerCaption(config.manualRule.enabler);
                if (!appOwner.isEmpty()) {
                    footerText = mContext.getString(
                            R.string.zen_mode_settings_dnd_automatic_rule_app, appOwner);
                }
            } else {
                if (id == null) {
                    return mContext.getString(
                            R.string.zen_mode_settings_dnd_manual_indefinite);
                } else {
                    latestEndTime = mZenModeConfigWrapper.parseManualRuleTime(id);
                    if (latestEndTime > 0) {
                        final CharSequence formattedTime = mZenModeConfigWrapper.getFormattedTime(
                                latestEndTime, mContext.getUserId());
                        footerText = mContext.getString(
                                R.string.zen_mode_settings_dnd_manual_end_time,
                                formattedTime);
                    }
                }
            }
        }

        // DND turned on by an automatic rule
        for (ZenModeConfig.ZenRule automaticRule : config.automaticRules.values()) {
            if (automaticRule.isAutomaticActive()) {
                // set footer if 3rd party rule
                if (!mZenModeConfigWrapper.isTimeRule(automaticRule.conditionId)) {
                    return mContext.getString(R.string.zen_mode_settings_dnd_automatic_rule,
                            automaticRule.name);
                } else {
                    // set footer if automatic rule end time is the latest active rule end time
                    long endTime = mZenModeConfigWrapper.parseAutomaticRuleEndTime(
                            automaticRule.conditionId);
                    if (endTime > latestEndTime) {
                        latestEndTime = endTime;
                        footerText = mContext.getString(
                                R.string.zen_mode_settings_dnd_automatic_rule, automaticRule.name);
                    }
                }
            }
        }
        return footerText;
    }
}
