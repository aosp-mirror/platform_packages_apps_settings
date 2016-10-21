/*
 * Copyright (C) 2016 The Android Open Source Project
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
import android.os.Bundle;
import android.provider.SearchIndexableResource;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.drawer.CategoryKey;

import java.util.Arrays;
import java.util.List;

public class AdvancedAppSettings extends DashboardFragment {

    static final String TAG = "AdvancedAppSettings";

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
    }

    @Override
    protected String getCategoryKey() {
        return CategoryKey.CATEGORY_APPS_DEFAULT;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return mDashboardFeatureProvider.isEnabled()
                ? R.xml.app_default_settings
                : R.xml.advanced_apps;
    }

    @Override
    protected List<PreferenceController> getPreferenceControllers(Context context) {
        return null;
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
                    sir.xmlResId = FeatureFactory.getFactory(context)
                            .getDashboardFeatureProvider(context).isEnabled()
                            ? R.xml.app_default_settings
                            : R.xml.advanced_apps;
                    return Arrays.asList(sir);
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    return Utils.getNonIndexable(R.xml.advanced_apps, context);
                }
            };
}
