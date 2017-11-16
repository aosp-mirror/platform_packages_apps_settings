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

import android.app.Fragment;
import android.content.Context;
import android.service.notification.ConditionProviderService;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.utils.ManagedServiceSettings;
import com.android.settings.utils.ZenServiceListing;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

public class ZenModeAutomationSettings extends ZenModeSettingsBase {
    private static final String KEY_ADD_RULE = "zen_mode_add_automatic_rule";
    private static final String KEY_AUTOMATIC_RULES = "zen_mode_automatic_rules";
    protected final ManagedServiceSettings.Config CONFIG = getConditionProviderConfig();

    @Override
    protected List<AbstractPreferenceController> getPreferenceControllers(Context context) {
        ZenServiceListing serviceListing = new ZenServiceListing(getContext(), CONFIG);
        serviceListing.reloadApprovedServices();
        return buildPreferenceControllers(context, this, serviceListing);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Fragment parent, ZenServiceListing serviceListing) {
        List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new ZenModeAddAutomaticRulePreferenceController(context, KEY_ADD_RULE,
                parent, serviceListing));
        controllers.add(new ZenModeAutomaticRulesPreferenceController(context,
                KEY_AUTOMATIC_RULES, parent));

        return controllers;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.zen_mode_automation_settings;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.NOTIFICATION_ZEN_MODE_AUTOMATION;
    }

    protected static ManagedServiceSettings.Config getConditionProviderConfig() {
        final ManagedServiceSettings.Config c = new ManagedServiceSettings.Config();
        c.tag = TAG;
        c.intentAction = ConditionProviderService.SERVICE_INTERFACE;
        c.permission = android.Manifest.permission.BIND_CONDITION_PROVIDER_SERVICE;
        c.noun = "condition provider";
        return c;
    }

    /**
     * For Search.
     */
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
        @Override
        public List<String> getNonIndexableKeys(Context context) {
            final List<String> keys = super.getNonIndexableKeys(context);
            keys.add(KEY_ADD_RULE);
            keys.add(KEY_AUTOMATIC_RULES);
            return keys;
        }

        @Override
        public List<AbstractPreferenceController> getPreferenceControllers(Context context) {
            return buildPreferenceControllers(context, null, null);
        }
    };
}
