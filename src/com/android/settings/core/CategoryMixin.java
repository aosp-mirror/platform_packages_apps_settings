/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.core;

import static androidx.lifecycle.Lifecycle.Event.ON_PAUSE;
import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.android.settings.dashboard.CategoryManager;
import com.android.settingslib.drawer.Tile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A mixin that handles live categories for Injection
 */
public class CategoryMixin implements LifecycleObserver {

    private static final String TAG = "CategoryMixin";
    private static final String DATA_SCHEME_PKG = "package";

    // Serves as a temporary list of tiles to ignore until we heard back from the PM that they
    // are disabled.
    private static final ArraySet<ComponentName> sTileDenylist = new ArraySet<>();

    private final Context mContext;
    private final PackageReceiver mPackageReceiver = new PackageReceiver();
    private final List<CategoryListener> mCategoryListeners = new ArrayList<>();
    private int mCategoriesUpdateTaskCount;
    private boolean mFirstOnResume = true;

    public CategoryMixin(Context context) {
        mContext = context;
    }

    /**
     * Resume Lifecycle event
     */
    @OnLifecycleEvent(ON_RESUME)
    public void onResume() {
        final IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme(DATA_SCHEME_PKG);
        mContext.registerReceiver(mPackageReceiver, filter);

        if (mFirstOnResume) {
            // Skip since all tiles have been refreshed in DashboardFragment.onCreatePreferences().
            Log.d(TAG, "Skip categories update");
            mFirstOnResume = false;
            return;
        }
        updateCategories();
    }

    /**
     * Pause Lifecycle event
     */
    @OnLifecycleEvent(ON_PAUSE)
    public void onPause() {
        mContext.unregisterReceiver(mPackageReceiver);
    }

    /**
     * Add a category listener
     */
    public void addCategoryListener(CategoryListener listener) {
        mCategoryListeners.add(listener);
    }

    /**
     * Remove a category listener
     */
    public void removeCategoryListener(CategoryListener listener) {
        mCategoryListeners.remove(listener);
    }

    /**
     * Updates dashboard categories.
     */
    public void updateCategories() {
        updateCategories(false /* fromBroadcast */);
    }

    void addToDenylist(ComponentName component) {
        sTileDenylist.add(component);
    }

    void removeFromDenylist(ComponentName component) {
        sTileDenylist.remove(component);
    }

    @VisibleForTesting
    void onCategoriesChanged(Set<String> categories) {
        mCategoryListeners.forEach(listener -> listener.onCategoriesChanged(categories));
    }

    private void updateCategories(boolean fromBroadcast) {
        // Only allow at most 2 tasks existing at the same time since when the first one is
        // executing, there may be new data from the second update request.
        // Ignore the third update request because the second task is still waiting for the first
        // task to complete in a serial thread, which will get the latest data.
        if (mCategoriesUpdateTaskCount < 2) {
            new CategoriesUpdateTask().execute(fromBroadcast);
        }
    }

    /**
     * A handler implementing a {@link CategoryMixin}
     */
    public interface CategoryHandler {
        /** returns a {@link CategoryMixin} */
        CategoryMixin getCategoryMixin();
    }

    /**
     *  A listener receiving category change events.
     */
    public interface CategoryListener {
        /**
         * @param categories the changed categories that have to be refreshed, or null to force
         *                   refreshing all.
         */
        void onCategoriesChanged(@Nullable Set<String> categories);
    }

    private class CategoriesUpdateTask extends AsyncTask<Boolean, Void, Set<String>> {

        private final CategoryManager mCategoryManager;
        private Map<ComponentName, Tile> mPreviousTileMap;

        CategoriesUpdateTask() {
            mCategoriesUpdateTaskCount++;
            mCategoryManager = CategoryManager.get(mContext);
        }

        @Override
        protected Set<String> doInBackground(Boolean... params) {
            mPreviousTileMap = mCategoryManager.getTileByComponentMap();
            mCategoryManager.reloadAllCategories(mContext);
            mCategoryManager.updateCategoryFromDenylist(sTileDenylist);
            return getChangedCategories(params[0]);
        }

        @Override
        protected void onPostExecute(Set<String> categories) {
            if (categories == null || !categories.isEmpty()) {
                onCategoriesChanged(categories);
            }
            mCategoriesUpdateTaskCount--;
        }

        // Return the changed categories that have to be refreshed, or null to force refreshing all.
        private Set<String> getChangedCategories(boolean fromBroadcast) {
            if (!fromBroadcast) {
                // Always refresh for non-broadcast case.
                return null;
            }

            final Set<String> changedCategories = new ArraySet<>();
            final Map<ComponentName, Tile> currentTileMap =
                    mCategoryManager.getTileByComponentMap();
            currentTileMap.forEach((component, currentTile) -> {
                final Tile previousTile = mPreviousTileMap.get(component);
                // Check if the tile is newly added.
                if (previousTile == null) {
                    Log.i(TAG, "Tile added: " + component.flattenToShortString());
                    changedCategories.add(currentTile.getCategory());
                    return;
                }

                // Check if the title or summary has changed.
                if (!TextUtils.equals(currentTile.getTitle(mContext),
                        previousTile.getTitle(mContext))
                        || !TextUtils.equals(currentTile.getSummary(mContext),
                        previousTile.getSummary(mContext))) {
                    Log.i(TAG, "Tile changed: " + component.flattenToShortString());
                    changedCategories.add(currentTile.getCategory());
                }
            });

            // Check if any previous tile is removed.
            final Set<ComponentName> removal = new ArraySet(mPreviousTileMap.keySet());
            removal.removeAll(currentTileMap.keySet());
            removal.forEach(component -> {
                Log.i(TAG, "Tile removed: " + component.flattenToShortString());
                changedCategories.add(mPreviousTileMap.get(component).getCategory());
            });

            return changedCategories;
        }
    }

    private class PackageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateCategories(true /* fromBroadcast */);
        }
    }
}
