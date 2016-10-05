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
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.core.PreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.Indexable;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.SettingsDrawerActivity;
import com.android.settingslib.drawer.Tile;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Base fragment for dashboard style UI containing a list of static and dynamic setting items.
 */
public abstract class DashboardFragment extends SettingsPreferenceFragment
        implements SettingsDrawerActivity.CategoryListener, Indexable {

    private final Map<Class, PreferenceController> mPreferenceControllers =
            new ArrayMap<>();

    protected DashboardFeatureProvider mDashboardFeatureProvider;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mDashboardFeatureProvider =
                FeatureFactory.getFactory(context).getDashboardFeatureProvider(context);
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
    public boolean onPreferenceTreeClick(Preference preference) {
        Collection<PreferenceController> controllers = mPreferenceControllers.values();
        // Give all controllers a chance to handle click.
        for (PreferenceController controller : controllers) {
            if (controller.handlePreferenceTreeClick(preference)) {
                return true;
            }
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onStop() {
        super.onStop();
        final Activity activity = getActivity();
        if (activity instanceof SettingsDrawerActivity) {
            ((SettingsDrawerActivity) activity).remCategoryListener(this);
        }
    }

    protected <T extends PreferenceController> T getPreferenceController(Class<T> clazz) {
        PreferenceController controller = mPreferenceControllers.get(clazz);
        return (T) controller;
    }

    protected void addPreferenceController(PreferenceController controller) {
        mPreferenceControllers.put(controller.getClass(), controller);
    }

    protected final void displayTilesAsPreference(String TAG, PreferenceScreen screen,
            DashboardCategory category) {
        final Context context = getContext();
        List<Tile> tiles = category.tiles;
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
