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
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;

import androidx.annotation.CallSuper;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerListHelper;
import com.android.settings.core.SettingsBaseActivity;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.Indexable;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.Tile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Base fragment for dashboard style UI containing a list of static and dynamic setting items.
 */
public abstract class DashboardFragment extends SettingsPreferenceFragment
        implements SettingsBaseActivity.CategoryListener, Indexable,
        SummaryLoader.SummaryConsumer, PreferenceGroup.OnExpandButtonClickListener,
        BasePreferenceController.UiBlockListener {
    private static final String TAG = "DashboardFragment";

    private final Map<Class, List<AbstractPreferenceController>> mPreferenceControllers =
            new ArrayMap<>();
    private final Set<String> mDashboardTilePrefKeys = new ArraySet<>();

    private DashboardFeatureProvider mDashboardFeatureProvider;
    private DashboardTilePlaceholderPreferenceController mPlaceholderPreferenceController;
    private boolean mListeningToCategoryChange;
    private SummaryLoader mSummaryLoader;
    private List<String> mSuppressInjectedTileKeys;
    @VisibleForTesting
    UiBlockerController mBlockerController;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mSuppressInjectedTileKeys = Arrays.asList(context.getResources().getStringArray(
                R.array.config_suppress_injected_tile_keys));
        mDashboardFeatureProvider = FeatureFactory.getFactory(context).
                getDashboardFeatureProvider(context);
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        // Load preference controllers from code
        final List<AbstractPreferenceController> controllersFromCode =
                createPreferenceControllers(context);
        // Load preference controllers from xml definition
        final List<BasePreferenceController> controllersFromXml = PreferenceControllerListHelper
                .getPreferenceControllersFromXml(context, getPreferenceScreenResId());
        // Filter xml-based controllers in case a similar controller is created from code already.
        final List<BasePreferenceController> uniqueControllerFromXml =
                PreferenceControllerListHelper.filterControllers(
                        controllersFromXml, controllersFromCode);

        // Add unique controllers to list.
        if (controllersFromCode != null) {
            controllers.addAll(controllersFromCode);
        }
        controllers.addAll(uniqueControllerFromXml);

        // And wire up with lifecycle.
        final Lifecycle lifecycle = getSettingsLifecycle();
        uniqueControllerFromXml
                .stream()
                .filter(controller -> controller instanceof LifecycleObserver)
                .forEach(
                        controller -> lifecycle.addObserver((LifecycleObserver) controller));

        mPlaceholderPreferenceController =
                new DashboardTilePlaceholderPreferenceController(context);
        controllers.add(mPlaceholderPreferenceController);
        for (AbstractPreferenceController controller : controllers) {
            addPreferenceController(controller);
        }

        checkUiBlocker(controllers);
    }

    @VisibleForTesting
    void checkUiBlocker(List<AbstractPreferenceController> controllers) {
        final List<String> keys = new ArrayList<>();
        controllers
                .stream()
                .filter(controller -> controller instanceof BasePreferenceController.UiBlocker)
                .forEach(controller -> {
                    ((BasePreferenceController) controller).setUiBlockListener(this);
                    keys.add(controller.getPreferenceKey());
                });

        if (!keys.isEmpty()) {
            mBlockerController = new UiBlockerController(keys);
            mBlockerController.start(()->updatePreferenceVisibility(mPreferenceControllers));
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        // Set ComparisonCallback so we get better animation when list changes.
        getPreferenceManager().setPreferenceComparisonCallback(
                new PreferenceManager.SimplePreferenceComparisonCallback());
        if (icicle != null) {
            // Upon rotation configuration change we need to update preference states before any
            // editing dialog is recreated (that would happen before onResume is called).
            updatePreferenceStates();
        }
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
        if (mSummaryLoader != null) {
            // SummaryLoader can be null when there is no dynamic tiles.
            mSummaryLoader.setListening(true);
        }
        final Activity activity = getActivity();
        if (activity instanceof SettingsBaseActivity) {
            mListeningToCategoryChange = true;
            ((SettingsBaseActivity) activity).addCategoryListener(this);
        }
    }

    @Override
    public void notifySummaryChanged(Tile tile) {
        final String key = mDashboardFeatureProvider.getDashboardKeyForTile(tile);
        final Preference pref = getPreferenceScreen().findPreference(key);
        if (pref == null) {
            Log.d(getLogTag(), String.format(
                    "Can't find pref by key %s, skipping update summary %s",
                    key, tile.getDescription()));
            return;
        }
        pref.setSummary(tile.getSummary(pref.getContext()));
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePreferenceStates();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        Collection<List<AbstractPreferenceController>> controllers =
                mPreferenceControllers.values();
        // If preference contains intent, log it before handling.
        mMetricsFeatureProvider.logDashboardStartIntent(
                getContext(), preference.getIntent(), getMetricsCategory());
        // Give all controllers a chance to handle click.
        for (List<AbstractPreferenceController> controllerList : controllers) {
            for (AbstractPreferenceController controller : controllerList) {
                if (controller.handlePreferenceTreeClick(preference)) {
                    return true;
                }
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
            if (activity instanceof SettingsBaseActivity) {
                ((SettingsBaseActivity) activity).remCategoryListener(this);
            }
            mListeningToCategoryChange = false;
        }
    }

    @Override
    protected abstract int getPreferenceScreenResId();

    @Override
    public void onExpandButtonClick() {
        mMetricsFeatureProvider.action(SettingsEnums.PAGE_UNKNOWN,
                SettingsEnums.ACTION_SETTINGS_ADVANCED_BUTTON_EXPAND,
                getMetricsCategory(), null, 0);
    }

    protected boolean shouldForceRoundedIcon() {
        return false;
    }

    protected <T extends AbstractPreferenceController> T use(Class<T> clazz) {
        List<AbstractPreferenceController> controllerList = mPreferenceControllers.get(clazz);
        if (controllerList != null) {
            if (controllerList.size() > 1) {
                Log.w(TAG, "Multiple controllers of Class " + clazz.getSimpleName()
                        + " found, returning first one.");
            }
            return (T) controllerList.get(0);
        }

        return null;
    }

    protected void addPreferenceController(AbstractPreferenceController controller) {
        if (mPreferenceControllers.get(controller.getClass()) == null) {
            mPreferenceControllers.put(controller.getClass(), new ArrayList<>());
        }
        mPreferenceControllers.get(controller.getClass()).add(controller);
    }

    /**
     * Returns the CategoryKey for loading {@link DashboardCategory} for this fragment.
     */
    @VisibleForTesting
    public String getCategoryKey() {
        return DashboardFragmentRegistry.PARENT_TO_CATEGORY_KEY_MAP.get(getClass().getName());
    }

    /**
     * Get the tag string for logging.
     */
    protected abstract String getLogTag();

    /**
     * Get a list of {@link AbstractPreferenceController} for this fragment.
     */
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return null;
    }

    /**
     * Returns true if this tile should be displayed
     */
    @CallSuper
    protected boolean displayTile(Tile tile) {
        if (mSuppressInjectedTileKeys != null && tile.hasKey()) {
            // For suppressing injected tiles for OEMs.
            return !mSuppressInjectedTileKeys.contains(tile.getKey(getContext()));
        }
        return true;
    }

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
        screen.setOnExpandButtonClickListener(this);
        mPreferenceControllers.values().stream().flatMap(Collection::stream).forEach(
                controller -> controller.displayPreference(screen));
    }

    /**
     * Get current PreferenceController(s)
     */
    protected Collection<List<AbstractPreferenceController>> getPreferenceControllers() {
        return mPreferenceControllers.values();
    }

    /**
     * Update state of each preference managed by PreferenceController.
     */
    protected void updatePreferenceStates() {
        final PreferenceScreen screen = getPreferenceScreen();
        Collection<List<AbstractPreferenceController>> controllerLists =
                mPreferenceControllers.values();
        for (List<AbstractPreferenceController> controllerList : controllerLists) {
            for (AbstractPreferenceController controller : controllerList) {
                if (!controller.isAvailable()) {
                    continue;
                }

                final String key = controller.getPreferenceKey();
                if (TextUtils.isEmpty(key)) {
                    Log.d(TAG, String.format("Preference key is %s in Controller %s",
                            key, controller.getClass().getSimpleName()));
                    continue;
                }

                final Preference preference = screen.findPreference(key);
                if (preference == null) {
                    Log.d(TAG, String.format("Cannot find preference with key %s in Controller %s",
                            key, controller.getClass().getSimpleName()));
                    continue;
                }
                controller.updateState(preference);
            }
        }
    }

    /**
     * Refresh all preference items, including both static prefs from xml, and dynamic items from
     * DashboardCategory.
     */
    private void refreshAllPreferences(final String TAG) {
        final PreferenceScreen screen = getPreferenceScreen();
        // First remove old preferences.
        if (screen != null) {
            // Intentionally do not cache PreferenceScreen because it will be recreated later.
            screen.removeAll();
        }

        // Add resource based tiles.
        displayResourceTiles();

        refreshDashboardTiles(TAG);

        final Activity activity = getActivity();
        if (activity != null) {
            Log.d(TAG, "All preferences added, reporting fully drawn");
            activity.reportFullyDrawn();
        }

        updatePreferenceVisibility(mPreferenceControllers);
    }

    @VisibleForTesting
    void updatePreferenceVisibility(
            Map<Class, List<AbstractPreferenceController>> preferenceControllers) {
        final PreferenceScreen screen = getPreferenceScreen();
        if (screen == null || preferenceControllers == null || mBlockerController == null) {
            return;
        }

        final boolean visible = mBlockerController.isBlockerFinished();
        for (List<AbstractPreferenceController> controllerList :
                preferenceControllers.values()) {
            for (AbstractPreferenceController controller : controllerList) {
                final String key = controller.getPreferenceKey();
                final Preference preference = findPreference(key);
                if (preference != null) {
                    preference.setVisible(visible && controller.isAvailable());
                }
            }
        }
    }

    /**
     * Refresh preference items backed by DashboardCategory.
     */
    @VisibleForTesting
    void refreshDashboardTiles(final String TAG) {
        final PreferenceScreen screen = getPreferenceScreen();

        final DashboardCategory category =
                mDashboardFeatureProvider.getTilesForCategory(getCategoryKey());
        if (category == null) {
            Log.d(TAG, "NO dashboard tiles for " + TAG);
            return;
        }
        final List<Tile> tiles = category.getTiles();
        if (tiles == null) {
            Log.d(TAG, "tile list is empty, skipping category " + category.key);
            return;
        }
        // Create a list to track which tiles are to be removed.
        final List<String> remove = new ArrayList<>(mDashboardTilePrefKeys);

        // There are dashboard tiles, so we need to install SummaryLoader.
        if (mSummaryLoader != null) {
            mSummaryLoader.release();
        }
        final Context context = getContext();
        mSummaryLoader = new SummaryLoader(getActivity(), getCategoryKey());
        mSummaryLoader.setSummaryConsumer(this);
        // Install dashboard tiles.
        final boolean forceRoundedIcons = shouldForceRoundedIcon();
        for (Tile tile : tiles) {
            final String key = mDashboardFeatureProvider.getDashboardKeyForTile(tile);
            if (TextUtils.isEmpty(key)) {
                Log.d(TAG, "tile does not contain a key, skipping " + tile);
                continue;
            }
            if (!displayTile(tile)) {
                continue;
            }
            if (mDashboardTilePrefKeys.contains(key)) {
                // Have the key already, will rebind.
                final Preference preference = screen.findPreference(key);
                mDashboardFeatureProvider.bindPreferenceToTile(getActivity(), forceRoundedIcons,
                        getMetricsCategory(), preference, tile, key,
                        mPlaceholderPreferenceController.getOrder());
            } else {
                // Don't have this key, add it.
                final Preference pref = new Preference(getPrefContext());
                mDashboardFeatureProvider.bindPreferenceToTile(getActivity(), forceRoundedIcons,
                        getMetricsCategory(), pref, tile, key,
                        mPlaceholderPreferenceController.getOrder());
                screen.addPreference(pref);
                mDashboardTilePrefKeys.add(key);
            }
            remove.remove(key);
        }
        // Finally remove tiles that are gone.
        for (String key : remove) {
            mDashboardTilePrefKeys.remove(key);
            final Preference preference = screen.findPreference(key);
            if (preference != null) {
                screen.removePreference(preference);
            }
        }
        mSummaryLoader.setListening(true);
    }

    @Override
    public void onBlockerWorkFinished(BasePreferenceController controller) {
        mBlockerController.countDown(controller.getPreferenceKey());
    }
}
