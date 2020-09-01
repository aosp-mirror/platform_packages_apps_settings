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

import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.provider.Settings;
import android.service.notification.ZenModeConfig;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class ZenModeBehaviorFooterPreferenceController extends AbstractZenModePreferenceController {

    protected static final String KEY = "footer_preference";
    private final int mTitleRes;

    public ZenModeBehaviorFooterPreferenceController(Context context, Lifecycle lifecycle,
            int titleRes) {
        super(context, KEY, lifecycle);
        mTitleRes = titleRes;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setTitle(getFooterText());
    }

    protected String getFooterText() {
        if (isDeprecatedZenMode(getZenMode())) {
            ZenModeConfig config = getZenModeConfig();

            // DND turned on by manual rule with deprecated zen mode
            if (config.manualRule != null &&
                    isDeprecatedZenMode(config.manualRule.zenMode)) {
                final Uri id = config.manualRule.conditionId;
                if (config.manualRule.enabler != null) {
                    // app triggered manual rule
                    String appOwner = mZenModeConfigWrapper.getOwnerCaption(
                            config.manualRule.enabler);
                    if (!appOwner.isEmpty()) {
                        return mContext.getString(R.string.zen_mode_app_set_behavior, appOwner);
                    }
                } else {
                    return mContext.getString(R.string.zen_mode_qs_set_behavior);
                }
            }

            // DND turned on by an automatic rule with deprecated zen mode
            for (ZenModeConfig.ZenRule automaticRule : config.automaticRules.values()) {
                if (automaticRule.isAutomaticActive() && isDeprecatedZenMode(
                        automaticRule.zenMode)) {
                    ComponentName component = automaticRule.component;
                    if (component != null) {
                        return mContext.getString(R.string.zen_mode_app_set_behavior,
                                component.getPackageName());
                    }
                }
            }

            return mContext.getString(R.string.zen_mode_unknown_app_set_behavior);
        } else {
            return mContext.getString(mTitleRes);
        }
    }

    private boolean isDeprecatedZenMode(int zenMode) {
        switch (zenMode) {
            case Settings.Global.ZEN_MODE_NO_INTERRUPTIONS:
            case Settings.Global.ZEN_MODE_ALARMS:
                return true;
            default:
                return false;
        }
    }
}