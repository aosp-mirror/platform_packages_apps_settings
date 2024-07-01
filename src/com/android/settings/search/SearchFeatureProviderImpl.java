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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.android.settingslib.search.SearchIndexableResources;
import com.android.settingslib.search.SearchIndexableResourcesMobile;

/**
 * FeatureProvider for the refactored search code.
 */
public class SearchFeatureProviderImpl implements SearchFeatureProvider {

    private SearchIndexableResources mSearchIndexableResources;

    @Override
    public void verifyLaunchSearchResultPageCaller(@NonNull Context context,
            @NonNull String callerPackage) {
        if (TextUtils.isEmpty(callerPackage)) {
            throw new IllegalArgumentException("ExternalSettingsTrampoline intents "
                    + "must be called with startActivityForResult");
        }
        final boolean isSettingsPackage = TextUtils.equals(callerPackage, context.getPackageName())
                || TextUtils.equals(getSettingsIntelligencePkgName(context), callerPackage);
        final boolean isAllowlistedPackage = isSignatureAllowlisted(context, callerPackage);
        if (isSettingsPackage || isAllowlistedPackage) {
            return;
        }
        throw new SecurityException("Search result intents must be called with from an "
                + "allowlisted package.");
    }

    @Override
    public SearchIndexableResources getSearchIndexableResources() {
        if (mSearchIndexableResources == null) {
            mSearchIndexableResources = new SearchIndexableResourcesMobile();
        }
        return mSearchIndexableResources;
    }

    @Override
    public Intent buildSearchIntent(Context context, int pageId) {
        return new Intent(Settings.ACTION_APP_SEARCH_SETTINGS)
                .setPackage(getSettingsIntelligencePkgName(context))
                .putExtra(Intent.EXTRA_REFERRER, buildReferrer(context, pageId));
    }

    protected boolean isSignatureAllowlisted(Context context, String callerPackage) {
        return false;
    }

    private static Uri buildReferrer(Context context, int pageId) {
        return new Uri.Builder()
                .scheme("android-app")
                .authority(context.getPackageName())
                .path(String.valueOf(pageId))
                .build();
    }
}
