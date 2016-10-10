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
        implements SettingsDrawerActivity.CategoryListener, Indexable,
        SummaryLoader.SummaryConsumer {

    private final Map<Class, PreferenceController> mPreferenceControllers =
            new ArrayMap<>();

    protected DashboardFeatureProvider mDashboardFeatureProvider;
    private boolean mListeningToCategoryChange;
    private SummaryLoader mSummaryLoader;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mDashboardFeatureProvider =
                FeatureFactory.getFactory(context).getDashboardFeatureProvider(context);
    }

    @Override
    public void onCategoriesChanged() {
        final DashboardCategory category =
                mDashboardFeatureProvider.getTilesForCategory(getCategoryKey());
        if (category == null) {
            return;
        }
        refreshAllPreferences(getLogTag());
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        refreshAllPreferences(getLogTag());
    }

    @Override
    public void onStart() {
        super.onStart();
        final DashboardCategory category =
                mDashboardFeatureProvider.getTilesForCategory(getCategoryKey());
        if (category == null) {
            return;
        }
        mSummaryLoader.setListening(true);
        final Activity activity = getActivity();
        if (activity instanceof SettingsDrawerActivity) {
            mListeningToCategoryChange = true;
            ((SettingsDrawerActivity) activity).addCategoryListener(this);
        }
    }

    @Override
    public void notifySummaryChanged(Tile tile) {
        final String key = mDashboardFeatureProvider.getDashboardKeyForTile(tile);
        final Preference pref = findPreference(key);
        if (pref == null) {
            Log.d(getLogTag(),
                    String.format("Can't find pref by key %s, skipping update summary %s/%s",
                            key, tile.title, tile.summary));
            return;
        }
        pref.setSummary(tile.summary);
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
        mSummaryLoader.setListening(false);
        if (mListeningToCategoryChange) {
            final Activity activity = getActivity();
            if (activity instanceof SettingsDrawerActivity) {
                ((SettingsDrawerActivity) activity).remCategoryListener(this);
            }
            mListeningToCategoryChange = false;
        }
    }

    protected <T extends PreferenceController> T getPreferenceController(Class<T> clazz) {
        PreferenceController controller = mPreferenceControllers.get(clazz);
        return (T) controller;
    }

    protected void addPreferenceController(PreferenceController controller) {
        mPreferenceControllers.put(controller.getClass(), controller);
    }

    /**
     * Returns the CategoryKey for loading {@link DashboardCategory} for this fragment.
     */
    protected abstract String getCategoryKey();

    /**
     * Displays resource based tiles.
     */
    protected abstract void displayResourceTiles();

    protected abstract String getLogTag();

    /**
     * Displays dashboard tiles as preference.
     */
    private final void displayDashboardTiles(final String TAG, PreferenceScreen screen) {
        final Context context = getContext();
        final DashboardCategory category =
                mDashboardFeatureProvider.getTilesForCategory(getCategoryKey());
        if (category == null) {
            Log.d(TAG, "NO dynamic tiles for " + TAG);
            return;
        }
        List<Tile> tiles = category.tiles;
        if (tiles == null) {
            Log.d(TAG, "tile list is empty, skipping category " + category.title);
            return;
        }
        // There are dashboard tiles, so we need to install SummaryLoader.
        if (mSummaryLoader != null) {
            mSummaryLoader.release();
        }
        mSummaryLoader = new SummaryLoader(getActivity(), getCategoryKey());
        mSummaryLoader.setSummaryConsumer(this);
        // Install dashboard tiles.
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

    /**
     * Refresh preference items using system category dashboard items.
     */
    private void refreshAllPreferences(final String TAG) {
        // First remove old preferences.
        PreferenceScreen screen = getPreferenceScreen();
        if (screen != null) {
            screen.removeAll();
        }
        // Add resource based tiles.
        displayResourceTiles();
        // Add dashboard tiles.
        displayDashboardTiles(TAG, getPreferenceScreen());
    }
}
