/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.network;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.UserManager;
import android.provider.SearchIndexableResource;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class MobileNetworkListFragment extends DashboardFragment {
    private static final String LOG_TAG = "NetworkListFragment";

    static final String KEY_PREFERENCE_CATEGORY_SIM = "provider_model_sim_category";
    @VisibleForTesting
    static final String KEY_PREFERENCE_CATEGORY_DOWNLOADED_SIM =
            "provider_model_downloaded_sim_category";

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.network_provider_sims_list;
    }

    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.MOBILE_NETWORK_LIST;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        if (!SubscriptionUtil.isSimHardwareVisible(getContext())) {
            finish();
            return controllers;
        }

        NetworkProviderSimsCategoryController simCategoryPrefCtrl =
                new NetworkProviderSimsCategoryController(context, KEY_PREFERENCE_CATEGORY_SIM,
                        getSettingsLifecycle());
        controllers.add(simCategoryPrefCtrl);
        NetworkProviderDownloadedSimsCategoryController downloadedSimsCategoryCtrl =
                new NetworkProviderDownloadedSimsCategoryController(context,
                        KEY_PREFERENCE_CATEGORY_DOWNLOADED_SIM, getSettingsLifecycle());
        controllers.add(downloadedSimsCategoryCtrl);

        return controllers;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {

                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    final ArrayList<SearchIndexableResource> result = new ArrayList<>();
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.network_provider_sims_list;
                    result.add(sir);
                    return result;
                }

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return SubscriptionUtil.isSimHardwareVisible(context) &&
                            context.getSystemService(UserManager.class).isAdminUser();
                }
            };
}
