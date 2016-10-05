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

package com.android.settings.deviceinfo;

import android.content.Context;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.SearchIndexableResource;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StorageDashboardFragment extends DashboardFragment {

    private static final String TAG = "StorageDashboardFrag";

    @Override
    public int getMetricsCategory() {
        return STORAGE_CATEGORY_FRAGMENT;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        addPreferenceController(new ManageStoragePreferenceController(context));
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        refreshAllPreferences();
    }

    @Override
    public void onCategoriesChanged() {
        refreshAllPreferences();
    }

    private void refreshAllPreferences() {
        PreferenceScreen screen = getPreferenceScreen();
        if (screen != null) {
            screen.removeAll();
        }
        addPreferencesFromResource(R.xml.storage_dashboard_fragment);

        getPreferenceController(ManageStoragePreferenceController.class)
                .displayPreference(getPreferenceScreen());

        displayTilesAsPreference(TAG, getPreferenceScreen(),
                mDashboardFeatureProvider.getTilesForStorageCategory());
    }

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
                    sir.xmlResId = R.xml.storage_dashboard_fragment;
                    return Arrays.asList(sir);
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    if (!FeatureFactory.getFactory(context).getDashboardFeatureProvider(context)
                            .isEnabled()) {
                        return null;
                    }
                    final ManageStoragePreferenceController controller =
                            new ManageStoragePreferenceController(context);
                    final List<String> keys = new ArrayList<>();
                    controller.updateNonIndexableKeys(keys);
                    return keys;
                }
            };
}
