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
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.core.PreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.Indexable;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.SettingsDrawerActivity;
import com.android.settingslib.drawer.Tile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Base fragment for dashboard style UI containing a list of static and dynamic setting items.
 */
public abstract class DashboardFragment extends SettingsPreferenceFragment
        implements SettingsDrawerActivity.CategoryListener, Indexable,
        SummaryLoader.SummaryConsumer {
    private static final String TAG = "DashboardFragment";

    private final Map<Class, PreferenceController> mPreferenceControllers =
            new ArrayMap<>();
    private final Set<String> mDashboardTilePrefKeys = new ArraySet<>();
    private DashboardDividerDecoration mDividerDecoration;

    protected ProgressiveDisclosureMixin mProgressiveDisclosureMixin;
    protected DashboardFeatureProvider mDashboardFeatureProvider;
    private boolean mListeningToCategoryChange;
    private SummaryLoader mSummaryLoader;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mDashboardFeatureProvider =
                FeatureFactory.getFactory(context).getDashboardFeatureProvider(context);
        mProgressiveDisclosureMixin = mDashboardFeatureProvider
                .getProgressiveDisclosureMixin(context, this);
        getLifecycle().addObserver(mProgressiveDisclosureMixin);

        final List<PreferenceController> controllers = getPreferenceControllers(context);
        if (controllers == null) {
            return;
        }
        for (PreferenceController controller : controllers) {
            addPreferenceController(controller);
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        // Set ComparisonCallback so we get better animation when list changes.
        getPreferenceManager().setPreferenceComparisonCallback(
                new PreferenceManager.SimplePreferenceComparisonCallback());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        if (mDashboardFeatureProvider.isEnabled()) {
            getListView().addItemDecoration(mDividerDecoration);
        }
        return view;
    }

    @Override
    public void onCategoriesChanged() {
        final DashboardCategory category =
                mDashboardFeatureProvider.getTilesForCategory(getCategoryKey());
        if (category == null) {
            return;
        }
        refreshDashboardTiles(getLogTag());
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        mDividerDecoration = new DashboardDividerDecoration(getContext());
        refreshAllPreferences(getLogTag());
    }

    @Override
    public void setDivider(Drawable divider) {
        if (mDashboardFeatureProvider.isEnabled()) {
            // Intercept divider and set it transparent so system divider decoration is disabled.
            // We will use our decoration to draw divider more intelligently.
            mDividerDecoration.setDivider(divider);
            super.setDivider(new ColorDrawable(Color.TRANSPARENT));
        } else {
            super.setDivider(divider);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        final DashboardCategory category =
                mDashboardFeatureProvider.getTilesForCategory(getCategoryKey());
        if (category == null) {
            return;
        }
        if (mSummaryLoader != null) {
            // SummaryLoader can be null when there is no dynamic tiles.
            mSummaryLoader.setListening(true);
        }
        final Activity activity = getActivity();
        if (activity instanceof SettingsDrawerActivity) {
            mListeningToCategoryChange = true;
            ((SettingsDrawerActivity) activity).addCategoryListener(this);
        }
    }

    @Override
    public void notifySummaryChanged(Tile tile) {
        final String key = mDashboardFeatureProvider.getDashboardKeyForTile(tile);
        final Preference pref = mProgressiveDisclosureMixin.findPreference(
                getPreferenceScreen(), key);
        if (pref == null) {
            Log.d(getLogTag(),
                    String.format("Can't find pref by key %s, skipping update summary %s/%s",
                            key, tile.title, tile.summary));
            return;
        }
        pref.setSummary(tile.summary);
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePreferenceStates();
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
        if (mSummaryLoader != null) {
            // SummaryLoader can be null when there is no dynamic tiles.
            mSummaryLoader.setListening(false);
        }
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
     * Get the tag string for logging.
     */
    protected abstract String getLogTag();

    /**
     * Get the res id for static preference xml for this fragment.
     */
    protected abstract int getPreferenceScreenResId();

    /**
     * Get a list of {@link PreferenceController} for this fragment.
     */
    protected abstract List<PreferenceController> getPreferenceControllers(Context context);

    /**
     * Displays resource based tiles.
     */
    private void displayResourceTiles() {
        final int resId = getPreferenceScreenResId();
        if (resId <= 0) {
            return;
        }
        addPreferencesFromResource(resId);
        final PreferenceScreen screen = getPreferenceScreen();
        Collection<PreferenceController> controllers = mPreferenceControllers.values();
        for (PreferenceController controller : controllers) {
            controller.displayPreference(screen);
        }
    }

    /**
     * Update state of each preference managed by PreferenceController.
     */
    private void updatePreferenceStates() {
        Collection<PreferenceController> controllers = mPreferenceControllers.values();
        final PreferenceScreen screen = getPreferenceScreen();
        for (PreferenceController controller : controllers) {
            if (!controller.isAvailable()) {
                continue;
            }
            final String key = controller.getPreferenceKey();

            final Preference preference = mProgressiveDisclosureMixin.findPreference(screen, key);
            if (preference == null) {
                Log.d(TAG, String.format("Cannot find preference with key %s in Controller %s",
                        key, controller.getClass().getSimpleName()));
                continue;
            }
            controller.updateState(preference);
        }
    }

    /**
     * Refresh all preference items, including both static prefs from xml, and dynamic items from
     * DashboardCategory.
     */
    private void refreshAllPreferences(final String TAG) {
        // First remove old preferences.
        if (getPreferenceScreen() != null) {
            // Intentionally do not cache PreferenceScreen because it will be recreated later.
            getPreferenceScreen().removeAll();
        }

        // Add resource based tiles.
        displayResourceTiles();

        refreshDashboardTiles(TAG);
    }

    /**
     * Refresh preference items backed by DashboardCategory.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    void refreshDashboardTiles(final String TAG) {
        final PreferenceScreen screen = getPreferenceScreen();

        final DashboardCategory category =
                mDashboardFeatureProvider.getTilesForCategory(getCategoryKey());
        if (category == null) {
            Log.d(TAG, "NO dashboard tiles for " + TAG);
            return;
        }
        List<Tile> tiles = category.tiles;
        if (tiles == null) {
            Log.d(TAG, "tile list is empty, skipping category " + category.title);
            return;
        }
        // Create a list to track which tiles are to be removed.
        final List<String> remove = new ArrayList<>(mDashboardTilePrefKeys);

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
            if (mDashboardTilePrefKeys.contains(key)) {
                // Have the key already, will rebind.
                final Preference preference = mProgressiveDisclosureMixin.findPreference(
                        screen, key);
                mDashboardFeatureProvider.bindPreferenceToTile(
                        getActivity(), preference, tile, key);
            } else {
                // Don't have this key, add it.
                final Preference pref = new Preference(getPrefContext());
                mDashboardFeatureProvider.bindPreferenceToTile(getActivity(), pref, tile, key);
                mProgressiveDisclosureMixin.addPreference(screen, pref);
                mDashboardTilePrefKeys.add(key);
            }
            remove.remove(key);
        }
        // Finally remove tiles that are gone.
        for (String key : remove) {
            mDashboardTilePrefKeys.remove(key);
            mProgressiveDisclosureMixin.removePreference(screen, key);
        }
    }
}
