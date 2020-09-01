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
import android.os.Bundle;
import android.service.notification.ZenPolicy;

import com.android.settings.R;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

public class ZenCustomRuleBlockedEffectsSettings extends ZenCustomRuleSettingsBase {

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.zen_mode_block_settings;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        mControllers = new ArrayList<>();
        mControllers.add(new ZenRuleVisEffectPreferenceController(context, getSettingsLifecycle(),
                "zen_effect_intent", ZenPolicy.VISUAL_EFFECT_FULL_SCREEN_INTENT,
                SettingsEnums.ACTION_ZEN_BLOCK_FULL_SCREEN_INTENTS, null));
        mControllers.add(new ZenRuleVisEffectPreferenceController(context, getSettingsLifecycle(),
                "zen_effect_light", ZenPolicy.VISUAL_EFFECT_LIGHTS,
                SettingsEnums.ACTION_ZEN_BLOCK_LIGHT, null));
        mControllers.add(new ZenRuleVisEffectPreferenceController(context, getSettingsLifecycle(),
                "zen_effect_peek", ZenPolicy.VISUAL_EFFECT_PEEK,
                SettingsEnums.ACTION_ZEN_BLOCK_PEEK, null));
        mControllers.add(new ZenRuleVisEffectPreferenceController(context, getSettingsLifecycle(),
                "zen_effect_status", ZenPolicy.VISUAL_EFFECT_STATUS_BAR,
                SettingsEnums.ACTION_ZEN_BLOCK_STATUS,
                new int[] {ZenPolicy.VISUAL_EFFECT_NOTIFICATION_LIST}));
        mControllers.add(new ZenRuleVisEffectPreferenceController(context, getSettingsLifecycle(),
                "zen_effect_badge", ZenPolicy.VISUAL_EFFECT_BADGE,
                SettingsEnums.ACTION_ZEN_BLOCK_BADGE, null));
        mControllers.add(new ZenRuleVisEffectPreferenceController(context, getSettingsLifecycle(),
                "zen_effect_ambient", ZenPolicy.VISUAL_EFFECT_AMBIENT,
                SettingsEnums.ACTION_ZEN_BLOCK_AMBIENT, null));
        mControllers.add(new ZenRuleVisEffectPreferenceController(context, getSettingsLifecycle(),
                "zen_effect_list", ZenPolicy.VISUAL_EFFECT_NOTIFICATION_LIST,
                SettingsEnums.ACTION_ZEN_BLOCK_NOTIFICATION_LIST, null));
        return mControllers;
    }

    @Override
    String getPreferenceCategoryKey() {
        return null;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ZEN_CUSTOM_RULE_VIS_EFFECTS;
    }
}
