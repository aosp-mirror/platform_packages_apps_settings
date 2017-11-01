/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.VisibleForTesting;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.SettingsActivity;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.IndexDatabaseHelper;
import com.android.settings.search.IndexDatabaseHelper.IndexColumns;
import com.android.settings.search.IndexDatabaseHelper.SiteMapColumns;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.Tile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.android.settings.dashboard.DashboardFragmentRegistry.CATEGORY_KEY_TO_PARENT_MAP;

/**
 * A manager class that maintains a "site map" and look up breadcrumb for a certain page on demand.
 * <p/>
 * The methods on this class can only be called on a background thread.
 */
public class SiteMapManager {

    private static final String TAG = "SiteMapManager";
    private static final boolean DEBUG_TIMING = false;

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    public static final String[] SITE_MAP_COLUMNS = {
            SiteMapColumns.PARENT_CLASS,
            SiteMapColumns.PARENT_TITLE,
            SiteMapColumns.CHILD_CLASS,
            SiteMapColumns.CHILD_TITLE
    };

    private static final String[] CLASS_TO_SCREEN_TITLE_COLUMNS = {
            IndexColumns.CLASS_NAME,
            IndexColumns.SCREEN_TITLE,
    };

    private final List<SiteMapPair> mPairs = new ArrayList<>();

    private boolean mInitialized;

    /**
     * Given a fragment class name and its screen title, build a breadcrumb from Settings root to
     * this screen.
     * <p/>
     * Not all screens have a full breadcrumb path leading up to root, it's because either some
     * page in the breadcrumb path is not indexed, or it's only reachable via search.
     */
    @WorkerThread
    public synchronized List<String> buildBreadCrumb(Context context, String clazz,
            String screenTitle) {
        init(context);
        final long startTime = System.currentTimeMillis();
        final List<String> breadcrumbs = new ArrayList<>();
        if (!mInitialized) {
            Log.w(TAG, "SiteMap is not initialized yet, skipping");
            return breadcrumbs;
        }
        breadcrumbs.add(screenTitle);
        String currentClass = clazz;
        String currentTitle = screenTitle;
        // Look up current page's parent, if found add it to breadcrumb string list, and repeat.
        while (true) {
            final SiteMapPair pair = lookUpParent(currentClass, currentTitle);
            if (pair == null) {
                if (DEBUG_TIMING) {
                    Log.d(TAG, "BreadCrumb timing: " + (System.currentTimeMillis() - startTime));
                }
                return breadcrumbs;
            }
            breadcrumbs.add(0, pair.parentTitle);
            currentClass = pair.parentClass;
            currentTitle = pair.parentTitle;
        }
    }

    /**
     * Initialize a list of {@link SiteMapPair}s. Each pair knows about a single parent-child
     * page relationship.
     *
     * We get the knowledge of such mPairs from 2 sources:
     * 1. Static indexing time: we know which page(s) a parent can open by parsing its pref xml.
     * 2. IA: We know from {@link DashboardFeatureProvider} which page can be dynamically
     * injected to where.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    @WorkerThread
    synchronized void init(Context context) {
        if (mInitialized) {
            // Make sure only init once.
            return;
        }
        final long startTime = System.currentTimeMillis();
        // First load site map from static index table.
        final Context appContext = context.getApplicationContext();
        final SQLiteDatabase db = IndexDatabaseHelper.getInstance(appContext).getReadableDatabase();
        Cursor sitemap = db.query(IndexDatabaseHelper.Tables.TABLE_SITE_MAP, SITE_MAP_COLUMNS, null,
                null, null, null, null);
        while (sitemap.moveToNext()) {
            final SiteMapPair pair = new SiteMapPair(
                    sitemap.getString(sitemap.getColumnIndex(SiteMapColumns.PARENT_CLASS)),
                    sitemap.getString(sitemap.getColumnIndex(SiteMapColumns.PARENT_TITLE)),
                    sitemap.getString(sitemap.getColumnIndex(SiteMapColumns.CHILD_CLASS)),
                    sitemap.getString(sitemap.getColumnIndex(SiteMapColumns.CHILD_TITLE)));
            mPairs.add(pair);
        }
        sitemap.close();

        // Then prepare a local map that contains class name -> screen title mapping. This is needed
        // to figure out the display name for any fragment if it's injected dynamically through IA.
        final Map<String, String> classToTitleMap = new HashMap<>();
        final Cursor titleQuery = db.query(IndexDatabaseHelper.Tables.TABLE_PREFS_INDEX,
                CLASS_TO_SCREEN_TITLE_COLUMNS, null, null, null, null, null);
        while (titleQuery.moveToNext()) {
            classToTitleMap.put(
                    titleQuery.getString(titleQuery.getColumnIndex(IndexColumns.CLASS_NAME)),
                    titleQuery.getString(titleQuery.getColumnIndex(IndexColumns.SCREEN_TITLE)));
        }
        titleQuery.close();

        // Loop through all IA categories and pages and build additional SiteMapPairs
        List<DashboardCategory> categories = FeatureFactory.getFactory(context)
                .getDashboardFeatureProvider(context).getAllCategories();

        for (DashboardCategory category : categories) {
            // Find the category key first.
            final String parentClass = CATEGORY_KEY_TO_PARENT_MAP.get(category.key);
            if (parentClass == null) {
                continue;
            }
            // Use the key to look up parent (which page hosts this key)
            final String parentName = classToTitleMap.get(parentClass);
            if (parentName == null) {
                continue;
            }
            // Build parent-child mPairs for all children listed under this key.
            for (Tile tile : category.tiles) {
                final String childTitle = tile.title.toString();
                String childClass = null;
                if (tile.metaData != null) {
                    childClass = tile.metaData.getString(
                            SettingsActivity.META_DATA_KEY_FRAGMENT_CLASS);
                }
                if (childClass == null) {
                    continue;
                }
                mPairs.add(new SiteMapPair(parentClass, parentName, childClass, childTitle));
            }
        }
        // Done.
        mInitialized = true;
        if (DEBUG_TIMING) {
            Log.d(TAG, "Init timing: " + (System.currentTimeMillis() - startTime));
        }
    }

    @WorkerThread
    private SiteMapPair lookUpParent(String clazz, String title) {
        for (SiteMapPair pair : mPairs) {
            if (TextUtils.equals(pair.childClass, clazz)
                    && TextUtils.equals(title, pair.childTitle)) {
                return pair;
            }
        }
        return null;
    }

    /**
     * Data model for a parent-child page pair.
     */
    private static class SiteMapPair {
        public final String parentClass;
        public final String parentTitle;
        public final String childClass;
        public final String childTitle;

        public SiteMapPair(String parentClass, String parentTitle, String childClass,
                String childTitle) {
            this.parentClass = parentClass;
            this.parentTitle = parentTitle;
            this.childClass = childClass;
            this.childTitle = childTitle;
        }
    }
}
