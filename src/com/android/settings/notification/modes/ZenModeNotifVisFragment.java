/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.notification.modes;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.service.notification.ZenPolicy;

import com.android.settings.R;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings page that shows what notification visuals will change when this mode is on.
 */
public class ZenModeNotifVisFragment extends ZenModeFragmentBase {
    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        List<AbstractPreferenceController> prefControllers = new ArrayList<>();
        prefControllers.add(new ZenModeNotifVisPreferenceController(context,
                "zen_effect_intent", ZenPolicy.VISUAL_EFFECT_FULL_SCREEN_INTENT, null, mBackend));
        prefControllers.add(new ZenModeNotifVisPreferenceController(context,
                "zen_effect_light", ZenPolicy.VISUAL_EFFECT_LIGHTS, null, mBackend));
        prefControllers.add(new ZenModeNotifVisPreferenceController(context,
                "zen_effect_peek", ZenPolicy.VISUAL_EFFECT_PEEK, null, mBackend));
        prefControllers.add(new ZenModeNotifVisPreferenceController(context,
                "zen_effect_status", ZenPolicy.VISUAL_EFFECT_STATUS_BAR,
                new int[] {ZenPolicy.VISUAL_EFFECT_NOTIFICATION_LIST}, mBackend));
        prefControllers.add(new ZenModeNotifVisPreferenceController(context,
                "zen_effect_badge", ZenPolicy.VISUAL_EFFECT_BADGE, null, mBackend));
        prefControllers.add(new ZenModeNotifVisPreferenceController(context,
                "zen_effect_ambient", ZenPolicy.VISUAL_EFFECT_AMBIENT, null, mBackend));
        prefControllers.add(new ZenModeNotifVisPreferenceController(context,
                "zen_effect_list", ZenPolicy.VISUAL_EFFECT_NOTIFICATION_LIST, null, mBackend));
        return prefControllers;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.modes_notif_vis_settings;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ZEN_CUSTOM_RULE_VIS_EFFECTS;
    }
}
