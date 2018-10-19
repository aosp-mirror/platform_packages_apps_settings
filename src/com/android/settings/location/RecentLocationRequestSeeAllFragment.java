/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.location;


import android.content.Context;
import android.provider.SearchIndexableResource;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Dashboard Fragment to display all recent location requests, sorted by recency. */
public class RecentLocationRequestSeeAllFragment extends DashboardFragment {

    private static final String TAG = "RecentLocationReqAll";

    public static final String PATH =
            "com.android.settings.location.RecentLocationRequestSeeAllFragment";

    @Override
    public int getMetricsCategory() {
      return MetricsEvent.RECENT_LOCATION_REQUESTS_ALL;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.location_recent_requests_see_all;
    }

    @Override
    protected String getLogTag() {
      return TAG;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getLifecycle(), this);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(
            Context context, Lifecycle lifecycle, RecentLocationRequestSeeAllFragment fragment) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(
                new RecentLocationRequestSeeAllPreferenceController(context, lifecycle, fragment));
        return controllers;
    }

    /**
     * For Search.
     */
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.location_recent_requests_see_all;
                    return Arrays.asList(sir);
                }

                @Override
                public List<AbstractPreferenceController> getPreferenceControllers(Context
                        context) {
                    return buildPreferenceControllers(
                            context, /* lifecycle = */ null, /* fragment = */ null);
                }
            };
}
