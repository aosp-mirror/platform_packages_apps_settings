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

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.dashboard.DashboardFeatureProvider;
import com.android.settings.dashboard.DashboardTilePreference;
import com.android.settings.deviceinfo.SystemUpdatePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.Indexable;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.SettingsDrawerActivity;
import com.android.settingslib.drawer.Tile;

import java.util.List;

public class SystemDashboardFragment extends SettingsPreferenceFragment
        implements SettingsDrawerActivity.CategoryListener, Indexable {

    private static final String TAG = "SystemDashboardFrag";

    private DashboardFeatureProvider mDashboardFeatureProvider;
    private SystemUpdatePreferenceController mSystemUpdatePreferenceController;

    @Override
    public int getMetricsCategory() {
        return SYSTEM_CATEGORY_FRAGMENT;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mDashboardFeatureProvider =
                FeatureFactory.getFactory(context).getDashboardFeatureProvider(context);
        mSystemUpdatePreferenceController =
                new SystemUpdatePreferenceController(context, UserManager.get(context));
    }

    @Override
    public void onStart() {
        super.onStart();
        final Activity activity = getActivity();
        if (activity instanceof SettingsDrawerActivity) {
            ((SettingsDrawerActivity) activity).addCategoryListener(this);
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        refreshAllPreferences();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        final boolean handled =
                mSystemUpdatePreferenceController.handlePreferenceTreeClick(preference);
        return handled || super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onStop() {
        super.onStop();
        final Activity activity = getActivity();
        if (activity instanceof SettingsDrawerActivity) {
            ((SettingsDrawerActivity) activity).remCategoryListener(this);
        }
    }

    @Override
    public void onCategoriesChanged() {
        refreshAllPreferences();
    }

    /**
     * Refresh preference items using system category dashboard items.
     */
    private void refreshAllPreferences() {
        PreferenceScreen screen = getPreferenceScreen();
        if (screen != null) {
            screen.removeAll();
        }

        final Context context = getContext();
        final DashboardCategory category = mDashboardFeatureProvider.getTilesForSystemCategory();
        final List<Tile> tiles = category.tiles;

        addPreferencesFromResource(R.xml.system_dashboard_fragment);
        screen = getPreferenceScreen();
        mSystemUpdatePreferenceController.displayPreference(getPreferenceScreen());

        for (Tile tile : tiles) {
            final String key = mDashboardFeatureProvider.getDashboardKeyForTile(tile);
            if (TextUtils.isEmpty(key)) {
                Log.d(TAG, "tile does not contain a key, skipping " + tile);
                continue;
            }
            final Preference pref = new DashboardTilePreference(context);
            pref.setTitle(tile.title);
            pref.setKey(key);
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
