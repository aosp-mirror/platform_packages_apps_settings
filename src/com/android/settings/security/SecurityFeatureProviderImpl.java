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

package com.android.settings.security;

import android.content.Context;
import android.content.IContentProvider;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Pair;

import com.android.settings.R;
import com.android.settings.trustagent.TrustAgentManager;
import com.android.settings.trustagent.TrustAgentManagerImpl;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.Tile;
import com.android.settingslib.drawer.TileUtils;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;

/** Implementation for {@code SecurityFeatureProvider}. */
public class SecurityFeatureProviderImpl implements SecurityFeatureProvider {

    private TrustAgentManager mTrustAgentManager;

    @VisibleForTesting
    static final Drawable DEFAULT_ICON = null;

    @VisibleForTesting
    static Map<String, Pair<String, Integer>> sIconCache = new TreeMap<>();

    @VisibleForTesting
    static Map<String, String> sSummaryCache = new TreeMap<>();

    /** Update preferences with data from associated tiles. */
    public void updatePreferences(final Context context, final PreferenceScreen preferenceScreen,
            final DashboardCategory dashboardCategory) {
        if (preferenceScreen == null) {
            return;
        }
        int tilesCount = (dashboardCategory != null) ? dashboardCategory.getTilesCount() : 0;
        if (tilesCount == 0) {
            return;
        }

        initPreferences(context, preferenceScreen, dashboardCategory);

        // Fetching the summary and icon from the provider introduces latency, so do this on a
        // separate thread.
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                updatePreferencesToRunOnWorkerThread(context, preferenceScreen, dashboardCategory);
            }
        });
    }

    @VisibleForTesting
    static void initPreferences(Context context, PreferenceScreen preferenceScreen,
            DashboardCategory dashboardCategory) {
        int tilesCount = (dashboardCategory != null) ? dashboardCategory.getTilesCount() : 0;
        for (int i = 0; i < tilesCount; i++) {
            Tile tile = dashboardCategory.getTile(i);
            // If the tile does not have a key or appropriate meta data, skip it.
            if (TextUtils.isEmpty(tile.key) || (tile.metaData == null)) {
                continue;
            }
            Preference matchingPref = preferenceScreen.findPreference(tile.key);
            // If the tile does not have a matching preference, skip it.
            if (matchingPref == null) {
                continue;
            }
            // Either remove an icon by replacing them with nothing, or use the cached one since
            // there is a delay in fetching the injected icon, and we don't want an inappropriate
            // icon to be displayed while waiting for the injected icon.
            final String iconUri =
                    tile.metaData.getString(TileUtils.META_DATA_PREFERENCE_ICON_URI, null);
            Drawable drawable = DEFAULT_ICON;
            if ((iconUri != null) && sIconCache.containsKey(iconUri)) {
                Pair<String, Integer> icon = sIconCache.get(iconUri);
                try {
                    drawable = context.getPackageManager()
                            .getResourcesForApplication(icon.first /* package name */)
                                    .getDrawable(icon.second /* res id */,
                                            context.getTheme());
                } catch (PackageManager.NameNotFoundException e) {
                    // Ignore and just load the default icon.
                }
            }
            matchingPref.setIcon(drawable);
            // Either reserve room for the summary or load the cached one. This prevents the title
            // from shifting when the final summary is injected.
            final String summaryUri =
                    tile.metaData.getString(TileUtils.META_DATA_PREFERENCE_SUMMARY_URI, null);
            String summary = context.getString(R.string.summary_placeholder);
            if ((summaryUri != null) && sSummaryCache.containsKey(summaryUri)) {
                summary = sSummaryCache.get(summaryUri);
            }
            matchingPref.setSummary(summary);
        }
    }

    @VisibleForTesting
    void updatePreferencesToRunOnWorkerThread(Context context, PreferenceScreen preferenceScreen,
            DashboardCategory dashboardCategory) {

        int tilesCount = (dashboardCategory != null) ? dashboardCategory.getTilesCount() : 0;
        Map<String, IContentProvider> providerMap = new ArrayMap<>();
        for (int i = 0; i < tilesCount; i++) {
            Tile tile = dashboardCategory.getTile(i);
            // If the tile does not have a key or appropriate meta data, skip it.
            if (TextUtils.isEmpty(tile.key) || (tile.metaData == null)) {
                continue;
            }
            Preference matchingPref = preferenceScreen.findPreference(tile.key);
            // If the tile does not have a matching preference, skip it.
            if (matchingPref == null) {
                continue;
            }
            // Check if the tile has content providers for dynamically updatable content.
            final String iconUri =
                    tile.metaData.getString(TileUtils.META_DATA_PREFERENCE_ICON_URI, null);
            final String summaryUri =
                    tile.metaData.getString(TileUtils.META_DATA_PREFERENCE_SUMMARY_URI, null);
            if (!TextUtils.isEmpty(iconUri)) {
                String packageName = null;
                if (tile.intent != null) {
                    Intent intent = tile.intent;
                    if (!TextUtils.isEmpty(intent.getPackage())) {
                        packageName = intent.getPackage();
                    } else if (intent.getComponent() != null) {
                        packageName = intent.getComponent().getPackageName();
                    }
                }
                Pair<String, Integer> icon =
                        TileUtils.getIconFromUri(context, packageName, iconUri, providerMap);
                if (icon != null) {
                    sIconCache.put(iconUri, icon);
                    // Icon is only returned if the icon belongs to Settings or the target app.
                    // setIcon must be called on the UI thread.
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                matchingPref.setIcon(context.getPackageManager()
                                        .getResourcesForApplication(icon.first /* package name */)
                                                .getDrawable(icon.second /* res id */,
                                                        context.getTheme()));
                            } catch (PackageManager.NameNotFoundException
                                    | Resources.NotFoundException e) {
                                // Intentionally ignored. If icon resources cannot be found, do not
                                // update.
                            }
                        }
                    });
                }
            }
            if (!TextUtils.isEmpty(summaryUri)) {
                String summary = TileUtils.getTextFromUri(context, summaryUri, providerMap,
                        TileUtils.META_DATA_PREFERENCE_SUMMARY);
                sSummaryCache.put(summaryUri, summary);
                // setSummary must be called on UI thread.
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        // Only update the summary if it has actually changed.
                        if (summary == null) {
                            if (matchingPref.getSummary() != null) {
                                matchingPref.setSummary(summary);
                            }
                        } else if (!summary.equals(matchingPref.getSummary())) {
                            matchingPref.setSummary(summary);
                        }
                    }
                });
            }
        }
    }

    @Override
    public TrustAgentManager getTrustAgentManager() {
        if (mTrustAgentManager == null) {
            mTrustAgentManager = new TrustAgentManagerImpl();
        }
        return mTrustAgentManager;
    }
}
