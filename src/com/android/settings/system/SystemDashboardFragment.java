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
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.dashboard.DashboardFeatureProvider;
import com.android.settings.dashboard.DashboardTilePreference;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.Tile;

import java.util.List;

public class SystemDashboardFragment extends SettingsPreferenceFragment implements
        Preference.OnPreferenceClickListener {

    private DashboardFeatureProvider mDashboardFeatureProvider;

    @Override
    public int getMetricsCategory() {
        return SYSTEM_CATEGORY_FRAGMENT;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mDashboardFeatureProvider =
                FeatureFactory.getFactory(context).getDashboardFeatureProvider(context);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        addPreferencesFromResource(R.xml.system_dashboard_fragment);
        addDashboardCategoryAsPreference();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        // Needed to enable preference click ripple
        return false;
    }

    /**
     * Adds dynamic tiles for system category onto PreferenceScreen.
     */
    private void addDashboardCategoryAsPreference() {
        final Context context = getContext();
        final PreferenceScreen screen = getPreferenceScreen();
        final DashboardCategory category = mDashboardFeatureProvider.getTilesForSystemCategory();
        final List<Tile> tiles = category.tiles;
        for (Tile tile : tiles) {
            final DashboardTilePreference pref = new DashboardTilePreference(context);
            pref.setTitle(tile.title);
            pref.setSummary(tile.summary);
            if (tile.icon != null) {
                pref.setIcon(tile.icon.loadDrawable(context));
            }
            if (tile.intent != null) {
                pref.setIntent(tile.intent);
            }
            // Use negated priority for order, because tile priority is based on intent-filter
            // (larger value has higher priority). However pref order defines smaller value has
            // higher priority.
            pref.setOrder(-tile.priority);
            screen.addPreference(pref);
        }
    }

}