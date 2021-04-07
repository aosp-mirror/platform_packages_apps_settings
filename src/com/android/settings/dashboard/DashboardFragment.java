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
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.FeatureFlagUtils;
import android.util.Log;

import androidx.annotation.CallSuper;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.FeatureFlags;
import com.android.settings.core.PreferenceControllerListHelper;
import com.android.settings.core.SettingsBaseActivity;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.MasterSwitchPreference;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.ProviderTile;
import com.android.settingslib.drawer.Tile;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.utils.ThreadUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Base fragment for dashboard style UI containing a list of static and dynamic setting items.
 */
public abstract class DashboardFragment extends SettingsPreferenceFragment
        implements SettingsBaseActivity.CategoryListener, Indexable,
        PreferenceGroup.OnExpandButtonClickListener,
        BasePreferenceController.UiBlockListener {
    public static final String CATEGORY = "category";
    private static final String TAG = "DashboardFragment";

    @VisibleForTesting
    final ArrayMap<String, List<DynamicDataObserver>> mDashboardTilePrefKeys = new ArrayMap<>();
    private final Map<Class, List<AbstractPreferenceController>> mPreferenceControllers =
            new ArrayMap<>();
    private final List<DynamicDataObserver> mRegisteredObservers = new ArrayList<>();
    private final List<AbstractPreferenceController> mControllers = new ArrayList<>();
    @VisibleForTesting
    UiBlockerController mBlockerController;
    private DashboardFeatureProvider mDashboardFeatureProvider;
    private DashboardTilePlaceholderPreferenceController mPlaceholderPreferenceController;
    private boolean mListeningToCategoryChange;
    private List<String> mSuppressInjectedTileKeys;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mSuppressInjectedTileKeys = Arrays.asList(context.getResources().getStringArray(
                R.array.config_suppress_injected_tile_keys));
        mDashboardFeatureProvider = FeatureFactory.getFactory(context).
                getDashboardFeatureProvider(context);
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
            mControllers.addAll(controllersFromCode);
        }
        mControllers.addAll(uniqueControllerFromXml);

        // And wire up with lifecycle.
        final Lifecycle lifecycle = getSettingsLifecycle();
        uniqueControllerFromXml.forEach(controller -> {
            if (controller instanceof LifecycleObserver) {
                lifecycle.addObserver((LifecycleObserver) controller);
            }
        });

        // Set metrics category for BasePreferenceController.
        final int metricCategory = getMetricsCategory();
        mControllers.forEach(controller -> {
            if (controller instanceof BasePreferenceController) {
                ((BasePreferenceController) controller).setMetricsCategory(metricCategory);
            }
        });

        mPlaceholderPreferenceController =
                new DashboardTilePlaceholderPreferenceController(context);
        mControllers.add(mPlaceholderPreferenceController);
        for (AbstractPreferenceController controller : mControllers) {
            addPreferenceController(controller);
        }
    }

    @VisibleForTesting
    void checkUiBlocker(List<AbstractPreferenceController> controllers) {
        final List<String> keys = new ArrayList<>();
        controllers.forEach(controller -> {
            if (controller instanceof BasePreferenceController.UiBlocker
                    && controller.isAvailable()) {
                ((BasePreferenceController) controller).setUiBlockListener(this);
                keys.add(controller.getPreferenceKey());
            }
        });

        if (!keys.isEmpty()) {
            mBlockerController = new UiBlockerController(keys);
            mBlockerController.start(() -> updatePreferenceVisibility(mPreferenceControllers));
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
    public void onCategoriesChanged(Set<String> categories) {
        final String categoryKey = getCategoryKey();
        final DashboardCategory dashboardCategory =
                mDashboardFeatureProvider.getTilesForCategory(categoryKey);
        if (dashboardCategory == null) {
            return;
        }

        if (categories == null) {
            // force refreshing
            refreshDashboardTiles(getLogTag());
        } else if (categories.contains(categoryKey)) {
            Log.i(TAG, "refresh tiles for " + categoryKey);
            refreshDashboardTiles(getLogTag());
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        checkUiBlocker(mControllers);
        refreshAllPreferences(getLogTag());
        mControllers.stream()
                .map(controller -> (Preference) findPreference(controller.getPreferenceKey()))
                .filter(Objects::nonNull)
                .forEach(preference -> {
                    // Give all controllers a chance to handle click.
                    preference.getExtras().putInt(CATEGORY, getMetricsCategory());
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        final DashboardCategory category =
                mDashboardFeatureProvider.getTilesForCategory(getCategoryKey());
        if (category == null) {
            return;
        }
        final Activity activity = getActivity();
        if (activity instanceof SettingsBaseActivity) {
            mListeningToCategoryChange = true;
            ((SettingsBaseActivity) activity).addCategoryListener(this);
        }
        final ContentResolver resolver = getContentResolver();
        mDashboardTilePrefKeys.values().stream()
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .forEach(observer -> {
                    if (!mRegisteredObservers.contains(observer)) {
                        registerDynamicDataObserver(resolver, observer);
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePreferenceStates();
        writeElapsedTimeMetric(SettingsEnums.ACTION_DASHBOARD_VISIBLE_TIME,
                "isParalleledControllers:" + isParalleledControllers());
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        final Collection<List<AbstractPreferenceController>> controllers =
                mPreferenceControllers.values();
        for (List<AbstractPreferenceController> controllerList : controllers) {
            for (AbstractPreferenceController controller : controllerList) {
                if (controller.handlePreferenceTreeClick(preference)) {
                    // log here since calling super.onPreferenceTreeClick will be skipped
                    writePreferenceClickMetric(preference);
                    return true;
                }
            }
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onStop() {
        super.onStop();
        unregisterDynamicDataObservers(new ArrayList<>(mRegisteredObservers));
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
        displayResourceTilesToScreen(screen);
    }

    /**
     * Perform {@link AbstractPreferenceController#displayPreference(PreferenceScreen)}
     * on all {@link AbstractPreferenceController}s.
     */
    protected void displayResourceTilesToScreen(PreferenceScreen screen) {
        mPreferenceControllers.values().stream().flatMap(Collection::stream).forEach(
                controller -> controller.displayPreference(screen));
    }

    /**
     * @return {@code true} if the underlying controllers should be executed in parallel.
     * Override this function to enable/disable the behavior.
     */
    protected boolean isParalleledControllers() {
        return false;
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
        if (isParalleledControllers() && FeatureFlagUtils.isEnabled(getContext(),
                FeatureFlags.CONTROLLER_ENHANCEMENT)) {
            updatePreferenceStatesInParallel();
            return;
        }
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
     * Use parallel method to update state of each preference managed by PreferenceController.
     */
    @VisibleForTesting
    void updatePreferenceStatesInParallel() {
        final PreferenceScreen screen = getPreferenceScreen();
        final Collection<List<AbstractPreferenceController>> controllerLists =
                mPreferenceControllers.values();
        final List<ControllerFutureTask> taskList = new ArrayList<>();
        for (List<AbstractPreferenceController> controllerList : controllerLists) {
            for (AbstractPreferenceController controller : controllerList) {
                final ControllerFutureTask task = new ControllerFutureTask(
                        new ControllerTask(controller, screen, mMetricsFeatureProvider,
                                getMetricsCategory()), null /* result */);
                taskList.add(task);
                ThreadUtils.postOnBackgroundThread(task);
            }
        }

        for (ControllerFutureTask task : taskList) {
            try {
                task.get();
            } catch (InterruptedException | ExecutionException e) {
                Log.w(TAG, task.getController().getPreferenceKey() + " " + e.getMessage());
            }
        }
    }

    /**
     * Refresh all preference items, including both static prefs from xml, and dynamic items from
     * DashboardCategory.
     */
    private void refreshAllPreferences(final String tag) {
        final PreferenceScreen screen = getPreferenceScreen();
        // First remove old preferences.
        if (screen != null) {
            // Intentionally do not cache PreferenceScreen because it will be recreated later.
            screen.removeAll();
        }

        // Add resource based tiles.
        displayResourceTiles();

        refreshDashboardTiles(tag);

        final Activity activity = getActivity();
        if (activity != null) {
            Log.d(tag, "All preferences added, reporting fully drawn");
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
    private void refreshDashboardTiles(final String tag) {
        final PreferenceScreen screen = getPreferenceScreen();

        final DashboardCategory category =
                mDashboardFeatureProvider.getTilesForCategory(getCategoryKey());
        if (category == null) {
            Log.d(tag, "NO dashboard tiles for " + tag);
            return;
        }
        final List<Tile> tiles = category.getTiles();
        if (tiles == null) {
            Log.d(tag, "tile list is empty, skipping category " + category.key);
            return;
        }
        // Create a list to track which tiles are to be removed.
        final Map<String, List<DynamicDataObserver>> remove = new ArrayMap(mDashboardTilePrefKeys);

        // Install dashboard tiles.
        final boolean forceRoundedIcons = shouldForceRoundedIcon();
        for (Tile tile : tiles) {
            final String key = mDashboardFeatureProvider.getDashboardKeyForTile(tile);
            if (TextUtils.isEmpty(key)) {
                Log.d(tag, "tile does not contain a key, skipping " + tile);
                continue;
            }
            if (!displayTile(tile)) {
                continue;
            }
            if (mDashboardTilePrefKeys.containsKey(key)) {
                // Have the key already, will rebind.
                final Preference preference = screen.findPreference(key);
                mDashboardFeatureProvider.bindPreferenceToTileAndGetObservers(getActivity(),
                        forceRoundedIcons, getMetricsCategory(), preference, tile, key,
                        mPlaceholderPreferenceController.getOrder());
            } else {
                // Don't have this key, add it.
                final Preference pref = createPreference(tile);
                final List<DynamicDataObserver> observers =
                        mDashboardFeatureProvider.bindPreferenceToTileAndGetObservers(getActivity(),
                                forceRoundedIcons, getMetricsCategory(), pref, tile, key,
                                mPlaceholderPreferenceController.getOrder());
                screen.addPreference(pref);
                registerDynamicDataObservers(observers);
                mDashboardTilePrefKeys.put(key, observers);
            }
            remove.remove(key);
        }
        // Finally remove tiles that are gone.
        for (Map.Entry<String, List<DynamicDataObserver>> entry : remove.entrySet()) {
            final String key = entry.getKey();
            mDashboardTilePrefKeys.remove(key);
            final Preference preference = screen.findPreference(key);
            if (preference != null) {
                screen.removePreference(preference);
            }
            unregisterDynamicDataObservers(entry.getValue());
        }
    }

    @Override
    public void onBlockerWorkFinished(BasePreferenceController controller) {
        mBlockerController.countDown(controller.getPreferenceKey());
    }

    @VisibleForTesting
    Preference createPreference(Tile tile) {
        return tile instanceof ProviderTile
                ? new SwitchPreference(getPrefContext())
                : tile.hasSwitch()
                        ? new MasterSwitchPreference(getPrefContext())
                        : new Preference(getPrefContext());
    }

    @VisibleForTesting
    void registerDynamicDataObservers(List<DynamicDataObserver> observers) {
        if (observers == null || observers.isEmpty()) {
            return;
        }
        final ContentResolver resolver = getContentResolver();
        observers.forEach(observer -> registerDynamicDataObserver(resolver, observer));
    }

    private void registerDynamicDataObserver(ContentResolver resolver,
            DynamicDataObserver observer) {
        Log.d(TAG, "register observer: @" + Integer.toHexString(observer.hashCode())
                + ", uri: " + observer.getUri());
        resolver.registerContentObserver(observer.getUri(), false, observer);
        mRegisteredObservers.add(observer);
    }

    private void unregisterDynamicDataObservers(List<DynamicDataObserver> observers) {
        if (observers == null || observers.isEmpty()) {
            return;
        }
        final ContentResolver resolver = getContentResolver();
        observers.forEach(observer -> {
            Log.d(TAG, "unregister observer: @" + Integer.toHexString(observer.hashCode())
                    + ", uri: " + observer.getUri());
            mRegisteredObservers.remove(observer);
            resolver.unregisterContentObserver(observer);
        });
    }
}
