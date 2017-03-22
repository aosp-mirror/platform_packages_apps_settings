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
package com.android.settings.applications;

import android.content.Context;
import android.provider.SearchIndexableResource;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.applications.defaultapps.DefaultBrowserPreferenceController;
import com.android.settings.applications.defaultapps.DefaultEmergencyPreferenceController;
import com.android.settings.applications.defaultapps.DefaultHomePreferenceController;
import com.android.settings.applications.defaultapps.DefaultPhonePreferenceController;
import com.android.settings.applications.defaultapps.DefaultSmsPreferenceController;
import com.android.settings.applications.defaultapps.DefaultWorkBrowserPreferenceController;
import com.android.settings.applications.defaultapps.DefaultWorkPhonePreferenceController;
import com.android.settings.core.PreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdvancedAppSettings extends DashboardFragment {

    static final String TAG = "AdvancedAppSettings";

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.app_default_settings;
    }

    @Override
    protected List<PreferenceController> getPreferenceControllers(Context context) {
        final List<PreferenceController> controllers = new ArrayList<>();
        controllers.add(new DefaultBrowserPreferenceController(context));
        controllers.add(new DefaultWorkBrowserPreferenceController(context));
        controllers.add(new DefaultPhonePreferenceController(context));
        controllers.add(new DefaultWorkPhonePreferenceController(context));
        controllers.add(new DefaultSmsPreferenceController(context));
        controllers.add(new DefaultEmergencyPreferenceController(context));
        controllers.add(new DefaultHomePreferenceController(context));
        return controllers;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.APPLICATIONS_ADVANCED;
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.app_default_settings;
                    return Arrays.asList(sir);
                }
            };
}
