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
 *
 */

package com.android.settings.search;

import android.content.ComponentName;
import android.content.Context;
import android.text.TextUtils;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.indexing.IndexData;

import java.util.Locale;

/**
 * FeatureProvider for the refactored search code.
 */
public class SearchFeatureProviderImpl implements SearchFeatureProvider {

    private static final String TAG = "SearchFeatureProvider";

    private static final String METRICS_ACTION_SETTINGS_INDEX = "search_synchronous_indexing";
    private DatabaseIndexingManager mDatabaseIndexingManager;
    private SearchIndexableResources mSearchIndexableResources;

    @Override
    public void verifyLaunchSearchResultPageCaller(Context context, ComponentName caller) {
        if (caller == null) {
            throw new IllegalArgumentException("ExternalSettingsTrampoline intents "
                    + "must be called with startActivityForResult");
        }
        final String packageName = caller.getPackageName();
        final boolean isSettingsPackage = TextUtils.equals(packageName, context.getPackageName())
                || TextUtils.equals(getSettingsIntelligencePkgName(), packageName);
        final boolean isWhitelistedPackage =
                isSignatureWhitelisted(context, caller.getPackageName());
        if (isSettingsPackage || isWhitelistedPackage) {
            return;
        }
        throw new SecurityException("Search result intents must be called with from a "
                + "whitelisted package.");
    }

    @Override
    public DatabaseIndexingManager getIndexingManager(Context context) {
        if (mDatabaseIndexingManager == null) {
            mDatabaseIndexingManager = new DatabaseIndexingManager(context.getApplicationContext());
        }
        return mDatabaseIndexingManager;
    }

    @Override
    public void updateIndex(Context context) {
        long indexStartTime = System.currentTimeMillis();
        getIndexingManager(context).performIndexing();
        int indexingTime = (int) (System.currentTimeMillis() - indexStartTime);
        FeatureFactory.getFactory(context).getMetricsFeatureProvider()
                .histogram(context, METRICS_ACTION_SETTINGS_INDEX, indexingTime);
    }

    @Override
    public SearchIndexableResources getSearchIndexableResources() {
        if (mSearchIndexableResources == null) {
            mSearchIndexableResources = new SearchIndexableResourcesImpl();
        }
        return mSearchIndexableResources;
    }

    protected boolean isSignatureWhitelisted(Context context, String callerPackage) {
        return false;
    }

    /**
     * A generic method to make the query suitable for searching the database.
     *
     * @return the cleaned query string
     */
    @VisibleForTesting
    String cleanQuery(String query) {
        if (TextUtils.isEmpty(query)) {
            return null;
        }
        if (Locale.getDefault().equals(Locale.JAPAN)) {
            query = IndexData.normalizeJapaneseString(query);
        }
        return query.trim();
    }
}
