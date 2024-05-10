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
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager.OnActivityResultListener;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.CategoryMixin.CategoryHandler;
import com.android.settings.core.CategoryMixin.CategoryListener;
import com.android.settings.core.PreferenceControllerListHelper;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.PrimarySwitchPreference;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.Tile;
import com.android.settingslib.search.Indexable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Base fragment for dashboard style UI containing a list of static and dynamic setting items.
 */
public abstract class DashboardFragment extends SettingsPreferenceFragment
        implements CategoryListener, Indexable, PreferenceGroup.OnExpandButtonClickListener,
        BasePreferenceController.UiBlockListener {
    public static final String CATEGORY = "category";
    private static final String TAG = "DashboardFragment";
    private static final long TIMEOUT_MILLIS = 50L;

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
        mDashboardFeatureProvider =
                FeatureFactory.getFeatureFactory().getDashboardFeatureProvider();
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
        final List<BasePreferenceController> baseControllers = new ArrayList<>();
        controllers.forEach(controller -> {
            if (controller instanceof BasePreferenceController.UiBlocker
                    && controller.isAvailable()) {
                ((BasePreferenceController) controller).setUiBlockListener(this);
                keys.add(controller.getPreferenceKey());
                baseControllers.add((BasePreferenceController) controller);
            }
        });

        if (!keys.isEmpty()) {
            mBlockerController = new UiBlockerController(keys);
            mBlockerController.start(() -> {
                updatePreferenceVisibility(mPreferenceControllers);
                baseControllers.forEach(controller -> controller.setUiBlockerFinished(true));
            });
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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        LifecycleOwner viewLifecycleOwner = getViewLifecycleOwner();
        for (AbstractPreferenceController controller : mControllers) {
            controller.onViewCreated(viewLifecycleOwner);
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
        if (activity instanceof CategoryHandler) {
            mListeningToCategoryChange = true;
            ((CategoryHandler) activity).getCategoryMixin().addCategoryListener(this);
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
            if (activity instanceof CategoryHandler) {
                ((CategoryHandler) activity).getCategoryMixin().removeCategoryListener(this);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        for (List<AbstractPreferenceController> controllerList : mPreferenceControllers.values()) {
            for (AbstractPreferenceController controller : controllerList) {
                if (controller instanceof OnActivityResultListener) {
                    ((OnActivityResultListener) controller).onActivityResult(
                            requestCode, resultCode, data);
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
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

    /** Returns all controllers of type T. */
    protected <T extends AbstractPreferenceController> List<T> useAll(Class<T> clazz) {
        return (List<T>) mPreferenceControllers.getOrDefault(clazz, Collections.emptyList());
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

    /**
     * Force update all the preferences in this fragment.
     */
    public void forceUpdatePreferences() {
        final PreferenceScreen screen = getPreferenceScreen();
        if (screen == null || mPreferenceControllers == null) {
            return;
        }
        for (List<AbstractPreferenceController> controllerList : mPreferenceControllers.values()) {
            for (AbstractPreferenceController controller : controllerList) {
                final String key = controller.getPreferenceKey();
                final Preference preference = findPreference(key);
                if (preference == null) {
                    continue;
                }
                final boolean available = controller.isAvailable();
                if (available) {
                    controller.updateState(preference);
                }
                preference.setVisible(available);
            }
        }
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
                if (preference == null) {
                    continue;
                }
                if (controller instanceof BasePreferenceController.UiBlocker) {
                    final boolean prefVisible =
                            ((BasePreferenceController) controller).getSavedPrefVisibility();
                    preference.setVisible(visible && controller.isAvailable() && prefVisible);
                } else {
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

        // Install dashboard tiles and collect pending observers.
        final boolean forceRoundedIcons = shouldForceRoundedIcon();
        final List<DynamicDataObserver> pendingObservers = new ArrayList<>();

        // Move group tiles to the beginning of the list to ensure they are created before the
        // other tiles.
        tiles.sort(Comparator.comparingInt(tile -> tile.getType() == Tile.Type.GROUP ? 0 : 1));
        for (Tile tile : tiles) {
            final String key = mDashboardFeatureProvider.getDashboardKeyForTile(tile);
            if (TextUtils.isEmpty(key)) {
                Log.d(tag, "tile does not contain a key, skipping " + tile);
                continue;
            }
            if (!displayTile(tile)) {
                continue;
            }
            final List<DynamicDataObserver> observers;
            if (mDashboardTilePrefKeys.containsKey(key)) {
                // Have the key already, will rebind.
                final Preference preference = screen.findPreference(key);
                observers = mDashboardFeatureProvider.bindPreferenceToTileAndGetObservers(
                        getActivity(), this, forceRoundedIcons, preference, tile, key,
                        mPlaceholderPreferenceController.getOrder());
            } else {
                // Don't have this key, add it.
                final Preference pref = createPreference(tile);
                observers = mDashboardFeatureProvider.bindPreferenceToTileAndGetObservers(
                        getActivity(), this, forceRoundedIcons, pref, tile, key,
                        mPlaceholderPreferenceController.getOrder());
                if (tile.hasGroupKey() && mDashboardTilePrefKeys.containsKey(tile.getGroupKey())) {
                    final Preference group = screen.findPreference(tile.getGroupKey());
                    if (group instanceof PreferenceCategory) {
                        ((PreferenceCategory) group).addPreference(pref);
                    }
                } else {
                    screen.addPreference(pref);
                }
                registerDynamicDataObservers(observers);
                mDashboardTilePrefKeys.put(key, observers);
            }
            if (observers != null) {
                pendingObservers.addAll(observers);
            }
            remove.remove(key);
        }

        // Remove tiles that are gone.
        for (Map.Entry<String, List<DynamicDataObserver>> entry : remove.entrySet()) {
            final String key = entry.getKey();
            mDashboardTilePrefKeys.remove(key);
            final Preference preference = screen.findPreference(key);
            if (preference != null) {
                screen.removePreference(preference);
            }
            unregisterDynamicDataObservers(entry.getValue());
        }

        // Wait for pending observers to update UI.
        if (!pendingObservers.isEmpty()) {
            final CountDownLatch mainLatch = new CountDownLatch(1);
            new Thread(() -> {
                pendingObservers.forEach(observer ->
                        awaitObserverLatch(observer.getCountDownLatch()));
                mainLatch.countDown();
            }).start();
            Log.d(tag, "Start waiting observers");
            awaitObserverLatch(mainLatch);
            Log.d(tag, "Stop waiting observers");
            pendingObservers.forEach(DynamicDataObserver::updateUi);
        }
    }

    @Override
    public void onBlockerWorkFinished(BasePreferenceController controller) {
        mBlockerController.countDown(controller.getPreferenceKey());
        controller.setUiBlockerFinished(mBlockerController.isBlockerFinished());
    }

    protected Preference createPreference(Tile tile) {
        switch (tile.getType()) {
            case EXTERNAL_ACTION:
                Preference externalActionPreference = new Preference(getPrefContext());
                externalActionPreference
                        .setWidgetLayoutResource(R.layout.preference_external_action_icon);
                return externalActionPreference;
            case SWITCH:
                return new SwitchPreferenceCompat(getPrefContext());
            case SWITCH_WITH_ACTION:
                return new PrimarySwitchPreference(getPrefContext());
            case GROUP:
                mMetricsFeatureProvider.action(
                        mMetricsFeatureProvider.getAttribution(getActivity()),
                        SettingsEnums.ACTION_SETTINGS_GROUP_TILE_ADDED_TO_SCREEN,
                        getMetricsCategory(),
                        tile.getKey(getContext()),
                        /* value= */ 0);
                return new PreferenceCategory((getPrefContext()));
            case ACTION:
            default:
                return new Preference(getPrefContext());
        }
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

    private void awaitObserverLatch(CountDownLatch latch) {
        try {
            latch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // Do nothing
        }
    }
}
