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
import android.content.Intent;
import android.content.IContentProvider;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import com.android.settingslib.drawer.DashboardCategory;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.ArrayMap;

import com.android.settingslib.drawer.Tile;
import com.android.settingslib.drawer.TileUtils;

import java.util.Map;

/** Implementation for {@code SecurityFeatureProvider}. */
public class SecurityFeatureProviderImpl implements SecurityFeatureProvider {

    /** Update preferences with data from associated tiles. */
    public void updatePreferences(Context context, PreferenceScreen preferenceScreen,
            DashboardCategory dashboardCategory) {
        if (preferenceScreen == null) {
            return;
        }
        int tilesCount = (dashboardCategory != null) ? dashboardCategory.getTilesCount() : 0;
        if (tilesCount == 0) {
            return;
        }
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
            String iconUri = tile.metaData.getString(TileUtils.META_DATA_PREFERENCE_ICON_URI, null);
            String summaryUri =
                    tile.metaData.getString(TileUtils.META_DATA_PREFERENCE_SUMMARY_URI, null);
            if (!TextUtils.isEmpty(iconUri)) {
                int icon = TileUtils.getIconFromUri(context, iconUri, providerMap);
                boolean updateIcon = true;
                String packageName = null;
                // Dynamic icon has to come from the same package that the preference launches.
                if (tile.intent != null) {
                        Intent intent = tile.intent;
                        if (!TextUtils.isEmpty(intent.getPackage())) {
                            packageName = intent.getPackage();
                        } else if (intent.getComponent() != null) {
                            packageName = intent.getComponent().getPackageName();
                        }
                }
                if (TextUtils.isEmpty(packageName)) {
                    updateIcon = false;
                } else {
                    if (tile.icon == null) {
                        // If the tile does not have an icon already, only update if the suggested
                        // icon is non-zero.
                        updateIcon = (icon != 0);
                    } else {
                        // If the existing icon has the same resource package and resource id, the
                        // icon does not need to be updated.
                        updateIcon = !(packageName.equals(tile.icon.getResPackage())
                                && (icon == tile.icon.getResId()));
                    }
                }
                if (updateIcon) {
                    try {
                        matchingPref.setIcon(context.getPackageManager()
                                .getResourcesForApplication(packageName)
                                        .getDrawable(icon, context.getTheme()));
                    } catch (PackageManager.NameNotFoundException | Resources.NotFoundException e) {
                        // Intentionally ignored. If icon resources cannot be found, do not update.
                    }
                }
            }
            if (!TextUtils.isEmpty(summaryUri)) {
                String summary = TileUtils.getTextFromUri(context, summaryUri, providerMap,
                        TileUtils.META_DATA_PREFERENCE_SUMMARY);
                // Only update the summary if it has actually changed.
                if (summary == null) {
                    if (matchingPref.getSummary() != null) {
                        matchingPref.setSummary(summary);
                    }
                } else if (!summary.equals(matchingPref.getSummary())) {
                    matchingPref.setSummary(summary);
                }
            }
        }
    }
}
