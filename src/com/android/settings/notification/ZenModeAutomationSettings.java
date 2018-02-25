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
import android.provider.SearchIndexableResource;
import android.service.notification.ConditionProviderService;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.utils.ManagedServiceSettings;
import com.android.settings.utils.ZenServiceListing;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.List;

public class ZenModeAutomationSettings extends ZenModeSettingsBase {
    protected final ManagedServiceSettings.Config CONFIG = getConditionProviderConfig();

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        ZenServiceListing serviceListing = new ZenServiceListing(getContext(), CONFIG);
        serviceListing.reloadApprovedServices();
        return buildPreferenceControllers(context, this, serviceListing, getLifecycle());
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Fragment parent, ZenServiceListing serviceListing, Lifecycle lifecycle) {
        List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new ZenModeAddAutomaticRulePreferenceController(context, parent,
                serviceListing, lifecycle));
        controllers.add(new ZenModeAutomaticRulesPreferenceController(context, parent, lifecycle));

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
        return new ManagedServiceSettings.Config.Builder()
                .setTag(TAG)
                .setIntentAction(ConditionProviderService.SERVICE_INTERFACE)
                .setPermission(android.Manifest.permission.BIND_CONDITION_PROVIDER_SERVICE)
                .setNoun("condition provider")
                .build();
    }

    /**
     * For Search.
     */
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {

                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    final ArrayList<SearchIndexableResource> result = new ArrayList<>();

                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.zen_mode_automation_settings;
                    result.add(sir);
                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    final List<String> keys = super.getNonIndexableKeys(context);
                    keys.add(ZenModeAddAutomaticRulePreferenceController.KEY);
                    keys.add(ZenModeAutomaticRulesPreferenceController.KEY);
                    return keys;
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return buildPreferenceControllers(context, null, null, null);
                }
            };
}
