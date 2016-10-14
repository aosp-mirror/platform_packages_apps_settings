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
package com.android.settings.system;

import android.content.Context;
import android.os.UserManager;
import android.provider.SearchIndexableResource;

import com.android.settings.R;
import com.android.settings.core.PreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.deviceinfo.AdditionalSystemUpdatePreferenceController;
import com.android.settings.deviceinfo.SystemUpdatePreferenceController;
import com.android.settings.localepicker.LocaleFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.drawer.CategoryKey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class SystemDashboardFragment extends DashboardFragment {

    private static final String TAG = "SystemDashboardFrag";

    @Override
    public int getMetricsCategory() {
        return SYSTEM_CATEGORY_FRAGMENT;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.system_dashboard_fragment;
    }

    @Override
    protected String getCategoryKey() {
        return CategoryKey.CATEGORY_SYSTEM;
    }

    @Override
    protected List<PreferenceController> getPreferenceControllers(Context context) {
        final List<PreferenceController> controllers = new ArrayList<>();
        controllers.add(new SystemUpdatePreferenceController(context, UserManager.get(context)));
        controllers.add(new AdditionalSystemUpdatePreferenceController(context));
        return controllers;
    }

    /**
     * For Summary
     */
    private static class SummaryProvider implements SummaryLoader.SummaryProvider {

        private final Context mContext;
        private final SummaryLoader mSummaryLoader;
        private final LocaleFeatureProvider mLocaleFeatureProvider;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            mContext = context;
            mSummaryLoader = summaryLoader;
            mLocaleFeatureProvider = FeatureFactory.getFactory(context).getLocaleFeatureProvider();
        }

        @Override
        public void setListening(boolean listening) {
            if (listening) {
                final String language = mContext.getString(
                        R.string.system_dashboard_summary, mLocaleFeatureProvider.getLocaleNames());
                mSummaryLoader.setSummary(this, language);
            }
        }
    }

    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY =
            (context, summaryLoader) -> new SummaryProvider(context, summaryLoader);

    /**
     * For Search.
     */
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    if (!FeatureFactory.getFactory(context).getDashboardFeatureProvider(context)
                            .isEnabled()) {
                        return null;
                    }
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.system_dashboard_fragment;
                    return Arrays.asList(sir);
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    if (!FeatureFactory.getFactory(context).getDashboardFeatureProvider(context)
                            .isEnabled()) {
                        return null;
                    }
                    final List<String> keys = new ArrayList<>();
                    new SystemUpdatePreferenceController(context, UserManager.get(context))
                            .updateNonIndexableKeys(keys);
                    new AdditionalSystemUpdatePreferenceController(context)
                            .updateNonIndexableKeys(keys);
                    return keys;
                }
            };
}
