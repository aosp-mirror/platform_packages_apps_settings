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

import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;

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
     * Get all tiles, grouped by category.
     */
    List<DashboardCategory> getAllCategories();

    /**
     * Returns an unique string key for the tile.
     */
    String getDashboardKeyForTile(Tile tile);

    /**
     * Binds preference to data provided by tile and gets dynamic data observers.
     *
     * @param activity If tile contains intent to launch, it will be launched from this activity
     * @param forceRoundedIcon Whether or not injected tiles from other packages should be forced to
     * rounded icon.
     * @param sourceMetricsCategory The context (source) from which an action is performed
     * @param pref The preference to bind data
     * @param tile The binding data
     * @param key They key for preference. If null, we will generate one from tile data
     * @param baseOrder The order offset value. When binding, pref's order is determined by
     * both this value and tile's own priority.
     * @return The list of dynamic data observers
     */
    List<DynamicDataObserver> bindPreferenceToTileAndGetObservers(FragmentActivity activity,
            boolean forceRoundedIcon, int sourceMetricsCategory, Preference pref, Tile tile,
            String key, int baseOrder);

    /**
     * Opens a tile to its destination intent.
     */
    void openTileIntent(FragmentActivity activity, Tile tile);

}
