/*
 * Copyright (C) 2020 The Android Open Source Project
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
import android.provider.SearchIndexableResource;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

/**
 * DND Calls Settings page to determine which priority senders can bypass DND.
 */
@SearchIndexable
public class ZenModeCallsSettings extends ZenModeSettingsBase {

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getSettingsLifecycle());
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Lifecycle lifecycle) {
        List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new ZenModePrioritySendersPreferenceController(context,
                "zen_mode_settings_category_calls", lifecycle, false));
        controllers.add(new ZenModeSendersImagePreferenceController(context,
                "zen_mode_calls_image", lifecycle, false));
        controllers.add(new ZenModeRepeatCallersPreferenceController(context, lifecycle,
                context.getResources().getInteger(com.android.internal.R.integer
                        .config_zen_repeat_callers_threshold)));
        controllers.add(new ZenModeBehaviorFooterPreferenceController(
                context, lifecycle, R.string.zen_mode_calls_footer));
        return controllers;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.zen_mode_calls_settings;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DND_CALLS;
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
            sir.xmlResId = R.xml.zen_mode_calls_settings;
            result.add(sir);
            return result;
        }

        @Override
        public List<AbstractPreferenceController> createPreferenceControllers(
                Context context) {
            return buildPreferenceControllers(context, null);
        }
    };
}
