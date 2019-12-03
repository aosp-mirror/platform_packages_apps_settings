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

import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_AMBIENT;
import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_BADGE;
import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_FULL_SCREEN_INTENT;
import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_LIGHTS;
import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_NOTIFICATION_LIST;
import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_PEEK;
import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_STATUS_BAR;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

@SearchIndexable
public class ZenModeBlockedEffectsSettings extends ZenModeSettingsBase implements Indexable {

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getSettingsLifecycle());
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Lifecycle lifecycle) {
        List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new ZenModeVisEffectPreferenceController(context, lifecycle,
                "zen_effect_intent", SUPPRESSED_EFFECT_FULL_SCREEN_INTENT,
                SettingsEnums.ACTION_ZEN_BLOCK_FULL_SCREEN_INTENTS, null));
        controllers.add(new ZenModeVisEffectPreferenceController(context, lifecycle,
                "zen_effect_light", SUPPRESSED_EFFECT_LIGHTS,
                SettingsEnums.ACTION_ZEN_BLOCK_LIGHT, null));
        controllers.add(new ZenModeVisEffectPreferenceController(context, lifecycle,
                "zen_effect_peek", SUPPRESSED_EFFECT_PEEK,
                SettingsEnums.ACTION_ZEN_BLOCK_PEEK, null));
        controllers.add(new ZenModeVisEffectPreferenceController(context, lifecycle,
                "zen_effect_status", SUPPRESSED_EFFECT_STATUS_BAR,
                SettingsEnums.ACTION_ZEN_BLOCK_STATUS,
                new int[] {SUPPRESSED_EFFECT_NOTIFICATION_LIST}));
        controllers.add(new ZenModeVisEffectPreferenceController(context, lifecycle,
                "zen_effect_badge", SUPPRESSED_EFFECT_BADGE,
                SettingsEnums.ACTION_ZEN_BLOCK_BADGE, null));
        controllers.add(new ZenModeVisEffectPreferenceController(context, lifecycle,
                "zen_effect_ambient", SUPPRESSED_EFFECT_AMBIENT,
                SettingsEnums.ACTION_ZEN_BLOCK_AMBIENT, null));
        controllers.add(new ZenModeVisEffectPreferenceController(context, lifecycle,
                "zen_effect_list", SUPPRESSED_EFFECT_NOTIFICATION_LIST,
                SettingsEnums.ACTION_ZEN_BLOCK_NOTIFICATION_LIST, null));
        return controllers;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.zen_mode_block_settings;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ZEN_WHAT_TO_BLOCK;
    }

    /**
     * For Search.
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.zen_mode_block_settings) {

            @Override
            public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
                return buildPreferenceControllers(context, null);
            }
        };
}
