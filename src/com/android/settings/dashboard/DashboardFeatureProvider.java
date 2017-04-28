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
package com.android.settings.dashboard;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.preference.Preference;

import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.Tile;

import java.util.List;

/**
 * FeatureProvider for dashboard (aka settings homepage).
 */
public interface DashboardFeatureProvider {

    /**
     * Get tiles (wrapped in {@link DashboardCategory}) for key defined in CategoryKey.
     */
    DashboardCategory getTilesForCategory(String key);

    /**
     * Get tiles (wrapped as a list of Preference) for key defined in CategoryKey.
     *
     * @param activity Activity hosting the preference
     * @param context UI context to inflate preference
     * @param sourceMetricsCategory The context (source) from which an action is performed
     * @param key Value from CategoryKey
     * @deprecated Pages implementing {@code DashboardFragment} should use
     * {@link #getTilesForCategory(String)} instead. Using this method will not get the benefit
     * of auto-ordering, progressive disclosure, auto-refreshing summary text etc.
     */
    @Deprecated
    List<Preference> getPreferencesForCategory(Activity activity, Context context,
            int sourceMetricsCategory, String key);

    /**
     * Get all tiles, grouped by category.
     */
    List<DashboardCategory> getAllCategories();

    /**
     * Whether or not we should tint icons in setting pages.
     */
    boolean shouldTintIcon();

    /**
     * Returns an unique string key for the tile.
     */
    String getDashboardKeyForTile(Tile tile);

    /**
     * Binds preference to data provided by tile.
     *
     * @param activity If tile contains intent to launch, it will be launched from this activity
     * @param sourceMetricsCategory The context (source) from which an action is performed
     * @param pref The preference to bind data
     * @param tile The binding data
     * @param key They key for preference. If null, we will generate one from tile data
     * @param baseOrder The order offset value. When binding, pref's order is determined by
     * both this value and tile's own priority.
     */
    void bindPreferenceToTile(Activity activity, int sourceMetricsCategory, Preference pref,
            Tile tile, String key, int baseOrder);

    /**
     * Returns a {@link ProgressiveDisclosureMixin} for specified fragment.
     */
    ProgressiveDisclosureMixin getProgressiveDisclosureMixin(Context context,
            DashboardFragment fragment, Bundle args);

    /**
     * Returns additional intent filter action for dashboard tiles
     */
    String getExtraIntentAction();

    /**
     * Opens a tile to its destination intent.
     */
    void openTileIntent(Activity activity, Tile tile);

}
