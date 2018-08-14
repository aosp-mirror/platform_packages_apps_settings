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

package com.android.settings.notification;

import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_AMBIENT;
import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_BADGE;
import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_FULL_SCREEN_INTENT;
import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_LIGHTS;
import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_NOTIFICATION_LIST;
import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_PEEK;
import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_STATUS_BAR;

import android.content.Context;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.support.v7.preference.CheckBoxPreference;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.List;

public class ZenModeBlockedEffectsSettings extends ZenModeSettingsBase implements Indexable {

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mFooterPreferenceMixin.createFooterPreference().setTitle(
                R.string.zen_mode_blocked_effects_footer);
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getLifecycle());
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Lifecycle lifecycle) {
        List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new ZenModeVisEffectPreferenceController(context, lifecycle,
                "zen_effect_intent", SUPPRESSED_EFFECT_FULL_SCREEN_INTENT,
                MetricsEvent.ACTION_ZEN_BLOCK_FULL_SCREEN_INTENTS, null));
        controllers.add(new ZenModeVisEffectPreferenceController(context, lifecycle,
                "zen_effect_light", SUPPRESSED_EFFECT_LIGHTS,
                MetricsEvent.ACTION_ZEN_BLOCK_LIGHT, null));
        controllers.add(new ZenModeVisEffectPreferenceController(context, lifecycle,
                "zen_effect_peek", SUPPRESSED_EFFECT_PEEK,
                MetricsEvent.ACTION_ZEN_BLOCK_PEEK, null));
        controllers.add(new ZenModeVisEffectPreferenceController(context, lifecycle,
                "zen_effect_status", SUPPRESSED_EFFECT_STATUS_BAR,
                MetricsEvent.ACTION_ZEN_BLOCK_STATUS,
                new int[] {SUPPRESSED_EFFECT_NOTIFICATION_LIST}));
        controllers.add(new ZenModeVisEffectPreferenceController(context, lifecycle,
                "zen_effect_badge", SUPPRESSED_EFFECT_BADGE,
                MetricsEvent.ACTION_ZEN_BLOCK_BADGE, null));
        controllers.add(new ZenModeVisEffectPreferenceController(context, lifecycle,
                "zen_effect_ambient", SUPPRESSED_EFFECT_AMBIENT,
                MetricsEvent.ACTION_ZEN_BLOCK_AMBIENT, null));
        controllers.add(new ZenModeVisEffectPreferenceController(context, lifecycle,
                "zen_effect_list", SUPPRESSED_EFFECT_NOTIFICATION_LIST,
                MetricsEvent.ACTION_ZEN_BLOCK_NOTIFICATION_LIST, null));
        return controllers;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.zen_mode_block_settings;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.ZEN_WHAT_TO_BLOCK;
    }

    /**
     * For Search.
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    final ArrayList<SearchIndexableResource> result = new ArrayList<>();

                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.zen_mode_block_settings;
                    result.add(sir);
                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    final List<String> keys = super.getNonIndexableKeys(context);
                    return keys;
                }

            @Override
            public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
                return buildPreferenceControllers(context, null);
            }
        };
}
