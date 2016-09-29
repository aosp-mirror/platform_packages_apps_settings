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

import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.Tile;

import java.util.List;

/**
 * FeatureProvider for dashboard (aka settings homepage).
 */
public interface DashboardFeatureProvider {

    /**
     * Whether or not this feature is enabled.
     */
    boolean isEnabled();

    /**
     * Get tiles (wrapped in {@link DashboardCategory}) for homepage.
     */
    DashboardCategory getTilesForHomepage();

    /**
     * Get tiles (wrapped in {@link DashboardCategory}) for system category.
     */
    DashboardCategory getTilesForSystemCategory();

    /**
     * Get all tiles, grouped by category.
     */
    List<DashboardCategory> getAllCategories();

    /**
     * Returns a priority group for tile. priority level is grouped into hundreds. tiles with
     * priority 100 - 199 belongs to priority level 100, tiles with priority 200 - 299 is in
     * group 200, and so on.
     */
    int getPriorityGroup(Tile tile);
}
