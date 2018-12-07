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

import static com.android.settingslib.drawer.CategoryKey.CATEGORY_HOMEPAGE;
import static com.android.settingslib.drawer.TileUtils.META_DATA_KEY_ORDER;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.util.Pair;

import com.android.settingslib.drawer.CategoryKey;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.Tile;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.HashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class CategoryManagerTest {

    private ActivityInfo mActivityInfo;
    private Context mContext;
    private CategoryManager mCategoryManager;
    private Map<Pair<String, String>, Tile> mTileByComponentCache;
    private Map<String, DashboardCategory> mCategoryByKeyMap;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mActivityInfo = new ActivityInfo();
        mActivityInfo.packageName = "pkg";
        mActivityInfo.name = "class";
        mActivityInfo.applicationInfo = new ApplicationInfo();
        mTileByComponentCache = new HashMap<>();
        mCategoryByKeyMap = new HashMap<>();
        mCategoryManager = CategoryManager.get(mContext);
    }

    @Test
    public void getInstance_shouldBeSingleton() {
        assertThat(mCategoryManager).isSameAs(CategoryManager.get(mContext));
    }

    @Test
    public void backwardCompatCleanupForCategory_shouldNotChangeCategoryForNewKeys() {
        final Tile tile1 = new Tile(mActivityInfo, CategoryKey.CATEGORY_ACCOUNT);
        final Tile tile2 = new Tile(mActivityInfo, CategoryKey.CATEGORY_ACCOUNT);
        final DashboardCategory category = new DashboardCategory(CategoryKey.CATEGORY_ACCOUNT);
        category.addTile(tile1);
        category.addTile(tile2);
        mCategoryByKeyMap.put(CategoryKey.CATEGORY_ACCOUNT, category);
        mTileByComponentCache.put(new Pair<>("PACKAGE", "1"), tile1);
        mTileByComponentCache.put(new Pair<>("PACKAGE", "2"), tile2);

        mCategoryManager.backwardCompatCleanupForCategory(mTileByComponentCache, mCategoryByKeyMap);

        assertThat(mCategoryByKeyMap.size()).isEqualTo(1);
        assertThat(mCategoryByKeyMap.get(CategoryKey.CATEGORY_ACCOUNT)).isNotNull();
    }

    @Test
    public void backwardCompatCleanupForCategory_shouldNotChangeCategoryForMixedKeys() {
        final Tile tile1 = new Tile(mActivityInfo, CategoryKey.CATEGORY_ACCOUNT);
        final String oldCategory = "com.android.settings.category.wireless";
        final Tile tile2 = new Tile(mActivityInfo, oldCategory);
        final DashboardCategory category1 = new DashboardCategory(CategoryKey.CATEGORY_ACCOUNT);
        category1.addTile(tile1);
        final DashboardCategory category2 = new DashboardCategory(oldCategory);
        category2.addTile(tile2);
        mCategoryByKeyMap.put(CategoryKey.CATEGORY_ACCOUNT, category1);
        mCategoryByKeyMap.put(oldCategory, category2);
        mTileByComponentCache.put(new Pair<>("PACKAGE", "CLASS1"), tile1);
        mTileByComponentCache.put(new Pair<>("PACKAGE", "CLASS2"), tile2);

        mCategoryManager.backwardCompatCleanupForCategory(mTileByComponentCache, mCategoryByKeyMap);

        assertThat(mCategoryByKeyMap.size()).isEqualTo(2);
        assertThat(
                mCategoryByKeyMap.get(CategoryKey.CATEGORY_ACCOUNT).getTilesCount()).isEqualTo(1);
        assertThat(mCategoryByKeyMap.get(oldCategory).getTilesCount()).isEqualTo(1);
    }

    @Test
    public void backwardCompatCleanupForCategory_shouldChangeCategoryForOldKeys() {
        final String oldCategory = "com.android.settings.category.wireless";
        final Tile tile1 = new Tile(mActivityInfo, oldCategory);
        tile1.setCategory(oldCategory);
        final DashboardCategory category1 = new DashboardCategory(oldCategory);
        category1.addTile(tile1);
        mCategoryByKeyMap.put(oldCategory, category1);
        mTileByComponentCache.put(new Pair<>("PACKAGE", "CLASS1"), tile1);

        mCategoryManager.backwardCompatCleanupForCategory(mTileByComponentCache, mCategoryByKeyMap);

        // Added 1 more category to category map.
        assertThat(mCategoryByKeyMap.size()).isEqualTo(2);
        // The new category map has CATEGORY_NETWORK type now, which contains 1 tile.
        assertThat(
                mCategoryByKeyMap.get(CategoryKey.CATEGORY_NETWORK).getTilesCount()).isEqualTo(1);
        // Old category still exists.
        assertThat(mCategoryByKeyMap.get(oldCategory).getTilesCount()).isEqualTo(1);
    }

    @Test
    public void sortCategories_singlePackage_shouldReorderBasedOnPriority() {
        // Create some fake tiles that are not sorted.
        final String testPackage = "com.android.test";
        final DashboardCategory category = new DashboardCategory(CATEGORY_HOMEPAGE);
        final ActivityInfo activityInfo1 = new ActivityInfo();
        activityInfo1.metaData = new Bundle();
        activityInfo1.metaData.putInt(META_DATA_KEY_ORDER, 100);
        activityInfo1.packageName = testPackage;
        activityInfo1.name = "class1";
        final ActivityInfo activityInfo2 = new ActivityInfo();
        activityInfo2.metaData = new Bundle();
        activityInfo2.metaData.putInt(META_DATA_KEY_ORDER, 50);
        activityInfo2.packageName = testPackage;
        activityInfo2.name = "class2";
        final ActivityInfo activityInfo3 = new ActivityInfo();
        activityInfo3.metaData = new Bundle();
        activityInfo3.metaData.putInt(META_DATA_KEY_ORDER, 200);
        activityInfo3.packageName = testPackage;
        activityInfo3.name = "class3";
        final Tile tile1 = new Tile(activityInfo1, category.key);
        final Tile tile2 = new Tile(activityInfo2, category.key);
        final Tile tile3 = new Tile(activityInfo3, category.key);

        category.addTile(tile1);
        category.addTile(tile2);
        category.addTile(tile3);
        mCategoryByKeyMap.put(CATEGORY_HOMEPAGE, category);

        // Sort their priorities
        mCategoryManager.sortCategories(RuntimeEnvironment.application, mCategoryByKeyMap);

        // Verify they are now sorted.
        assertThat(category.getTile(0)).isSameAs(tile3);
        assertThat(category.getTile(1)).isSameAs(tile1);
        assertThat(category.getTile(2)).isSameAs(tile2);
    }

    @Test
    public void sortCategories_multiPackage_shouldReorderBasedOnPackageAndPriority() {
        // Create some fake tiles that are not sorted.
        final String testPackage1 = "com.android.test1";
        final String testPackage2 = "com.android.test2";
        final DashboardCategory category = new DashboardCategory(CATEGORY_HOMEPAGE);
        final ActivityInfo activityInfo1 = new ActivityInfo();
        activityInfo1.metaData = new Bundle();
        activityInfo1.metaData.putInt(META_DATA_KEY_ORDER, 100);
        activityInfo1.packageName = testPackage2;
        activityInfo1.name = "class1";
        final ActivityInfo activityInfo2 = new ActivityInfo();
        activityInfo2.metaData = new Bundle();
        activityInfo2.metaData.putInt(META_DATA_KEY_ORDER, 100);
        activityInfo2.packageName = testPackage1;
        activityInfo2.name = "class2";
        final ActivityInfo activityInfo3 = new ActivityInfo();
        activityInfo3.metaData = new Bundle();
        activityInfo3.metaData.putInt(META_DATA_KEY_ORDER, 50);
        activityInfo3.packageName = testPackage1;
        activityInfo3.name = "class3";

        final Tile tile1 = new Tile(activityInfo1, category.key);
        final Tile tile2 = new Tile(activityInfo2, category.key);
        final Tile tile3 = new Tile(activityInfo3, category.key);
        category.addTile(tile1);
        category.addTile(tile2);
        category.addTile(tile3);
        mCategoryByKeyMap.put(CATEGORY_HOMEPAGE, category);

        // Sort their priorities
        mCategoryManager.sortCategories(mContext, mCategoryByKeyMap);

        // Verify they are now sorted.
        assertThat(category.getTile(0)).isSameAs(tile2);
        assertThat(category.getTile(1)).isSameAs(tile1);
        assertThat(category.getTile(2)).isSameAs(tile3);
    }

    @Test
    public void sortCategories_internalPackageTiles_shouldSkipTileForInternalPackage() {
        // Create some fake tiles that are not sorted.
        final String testPackage = mContext.getPackageName();
        final DashboardCategory category = new DashboardCategory(CATEGORY_HOMEPAGE);
        final ActivityInfo activityInfo1 = new ActivityInfo();
        activityInfo1.packageName = testPackage;
        activityInfo1.name = "class1";
        activityInfo1.metaData = new Bundle();
        activityInfo1.metaData.putInt(META_DATA_KEY_ORDER, 100);
        final ActivityInfo activityInfo2 = new ActivityInfo();
        activityInfo2.packageName = testPackage;
        activityInfo2.name = "class2";
        activityInfo2.metaData = new Bundle();
        activityInfo2.metaData.putInt(META_DATA_KEY_ORDER, 100);
        final ActivityInfo activityInfo3 = new ActivityInfo();
        activityInfo3.packageName = testPackage;
        activityInfo3.name = "class3";
        activityInfo3.metaData = new Bundle();
        activityInfo3.metaData.putInt(META_DATA_KEY_ORDER, 50);
        final Tile tile1 = new Tile(activityInfo1, category.key);
        final Tile tile2 = new Tile(activityInfo2, category.key);
        final Tile tile3 = new Tile(activityInfo3, category.key);
        category.addTile(tile1);
        category.addTile(tile2);
        category.addTile(tile3);
        mCategoryByKeyMap.put(CATEGORY_HOMEPAGE, category);

        // Sort their priorities
        mCategoryManager.sortCategories(mContext, mCategoryByKeyMap);

        // Verify the sorting order is not changed
        assertThat(category.getTile(0)).isSameAs(tile1);
        assertThat(category.getTile(1)).isSameAs(tile2);
        assertThat(category.getTile(2)).isSameAs(tile3);
    }

    @Test
    public void sortCategories_internalAndExternalPackageTiles_shouldRetainPriorityOrdering() {
        // Inject one external tile among internal tiles.
        final String testPackage = mContext.getPackageName();
        final String testPackage2 = "com.google.test2";
        final DashboardCategory category = new DashboardCategory(CATEGORY_HOMEPAGE);

        final ActivityInfo activityInfo1 = new ActivityInfo();
        activityInfo1.packageName = testPackage;
        activityInfo1.name = "class1";
        activityInfo1.metaData = new Bundle();
        activityInfo1.metaData.putInt(META_DATA_KEY_ORDER, 2);
        final ActivityInfo activityInfo2 = new ActivityInfo();
        activityInfo2.packageName = testPackage;
        activityInfo2.name = "class2";
        activityInfo2.metaData = new Bundle();
        activityInfo2.metaData.putInt(META_DATA_KEY_ORDER, 1);
        final ActivityInfo activityInfo3 = new ActivityInfo();
        activityInfo3.packageName = testPackage2;
        activityInfo3.name = "class0";
        activityInfo3.metaData = new Bundle();
        activityInfo3.metaData.putInt(META_DATA_KEY_ORDER, 0);
        final ActivityInfo activityInfo4 = new ActivityInfo();
        activityInfo4.packageName = testPackage;
        activityInfo4.name = "class3";
        activityInfo4.metaData = new Bundle();
        activityInfo4.metaData.putInt(META_DATA_KEY_ORDER, -1);

        final Tile tile1 = new Tile(activityInfo1, category.key);
        final Tile tile2 = new Tile(activityInfo2, category.key);
        final Tile tile4 = new Tile(activityInfo4, category.key);
        final Tile tile3 = new Tile(activityInfo3, category.key);
        category.addTile(tile1);
        category.addTile(tile2);
        category.addTile(tile3);
        category.addTile(tile4);
        mCategoryByKeyMap.put(CATEGORY_HOMEPAGE, category);

        // Sort their priorities
        mCategoryManager.sortCategories(mContext, mCategoryByKeyMap);

        // Verify the sorting order is not changed
        assertThat(category.getTile(0)).isSameAs(tile1);
        assertThat(category.getTile(1)).isSameAs(tile2);
        assertThat(category.getTile(2)).isSameAs(tile3);
        assertThat(category.getTile(3)).isSameAs(tile4);
    }

    @Test
    public void sortCategories_samePriority_internalPackageTileShouldTakePrecedence() {
        // Inject one external tile among internal tiles with same priority.
        final String testPackage = mContext.getPackageName();
        final String testPackage2 = "com.google.test2";
        final String testPackage3 = "com.abcde.test3";
        final DashboardCategory category = new DashboardCategory(CATEGORY_HOMEPAGE);
        final ActivityInfo activityInfo1 = new ActivityInfo();
        activityInfo1.packageName = testPackage2;
        activityInfo1.name = "class1";
        activityInfo1.metaData = new Bundle();
        activityInfo1.metaData.putInt(META_DATA_KEY_ORDER, 1);
        final ActivityInfo activityInfo2 = new ActivityInfo();
        activityInfo2.packageName = testPackage;
        activityInfo2.name = "class2";
        activityInfo2.metaData = new Bundle();
        activityInfo2.metaData.putInt(META_DATA_KEY_ORDER, 1);
        final ActivityInfo activityInfo3 = new ActivityInfo();
        activityInfo3.packageName = testPackage3;
        activityInfo3.name = "class3";
        activityInfo3.metaData = new Bundle();
        activityInfo3.metaData.putInt(META_DATA_KEY_ORDER, 1);
        final Tile tile1 = new Tile(activityInfo1, category.key);
        final Tile tile2 = new Tile(activityInfo2, category.key);
        final Tile tile3 = new Tile(activityInfo3, category.key);
        category.addTile(tile1);
        category.addTile(tile2);
        category.addTile(tile3);
        mCategoryByKeyMap.put(CATEGORY_HOMEPAGE, category);

        // Sort their priorities
        mCategoryManager.sortCategories(mContext, mCategoryByKeyMap);

        // Verify the sorting order is internal first, follow by package name ordering
        assertThat(category.getTile(0)).isSameAs(tile2);
        assertThat(category.getTile(1)).isSameAs(tile3);
        assertThat(category.getTile(2)).isSameAs(tile1);
    }

    @Test
    public void filterTiles_noDuplicate_noChange() {
        // Create some unique tiles
        final String testPackage = mContext.getPackageName();
        final DashboardCategory category = new DashboardCategory(CATEGORY_HOMEPAGE);
        final ActivityInfo activityInfo1 = new ActivityInfo();
        activityInfo1.packageName = testPackage;
        activityInfo1.name = "class1";
        activityInfo1.metaData = new Bundle();
        activityInfo1.metaData.putInt(META_DATA_KEY_ORDER, 100);
        final ActivityInfo activityInfo2 = new ActivityInfo();
        activityInfo2.packageName = testPackage;
        activityInfo2.name = "class2";
        activityInfo2.metaData = new Bundle();
        activityInfo2.metaData.putInt(META_DATA_KEY_ORDER, 100);
        final ActivityInfo activityInfo3 = new ActivityInfo();
        activityInfo3.packageName = testPackage;
        activityInfo3.name = "class3";
        activityInfo3.metaData = new Bundle();
        activityInfo3.metaData.putInt(META_DATA_KEY_ORDER, 50);
        final Tile tile1 = new Tile(activityInfo1, category.key);
        final Tile tile2 = new Tile(activityInfo2, category.key);
        final Tile tile3 = new Tile(activityInfo3, category.key);
        category.addTile(tile1);
        category.addTile(tile2);
        category.addTile(tile3);
        mCategoryByKeyMap.put(CATEGORY_HOMEPAGE, category);

        mCategoryManager.filterDuplicateTiles(mCategoryByKeyMap);

        assertThat(category.getTilesCount()).isEqualTo(3);
    }

    @Test
    public void filterTiles_hasDuplicate_shouldOnlyKeepUniqueTiles() {
        // Create tiles pointing to same intent.
        final String testPackage = mContext.getPackageName();
        final DashboardCategory category = new DashboardCategory(CATEGORY_HOMEPAGE);
        final ActivityInfo activityInfo1 = new ActivityInfo();
        activityInfo1.packageName = testPackage;
        activityInfo1.name = "class1";
        activityInfo1.metaData = new Bundle();
        activityInfo1.metaData.putInt(META_DATA_KEY_ORDER, 100);
        final ActivityInfo activityInfo2 = new ActivityInfo();
        activityInfo2.packageName = testPackage;
        activityInfo2.name = "class1";
        activityInfo2.metaData = new Bundle();
        activityInfo2.metaData.putInt(META_DATA_KEY_ORDER, 100);
        final ActivityInfo activityInfo3 = new ActivityInfo();
        activityInfo3.packageName = testPackage;
        activityInfo3.name = "class1";
        activityInfo3.metaData = new Bundle();
        activityInfo3.metaData.putInt(META_DATA_KEY_ORDER, 50);

        final Tile tile1 = new Tile(activityInfo1, category.key);
        final Tile tile2 = new Tile(activityInfo2, category.key);
        final Tile tile3 = new Tile(activityInfo3, category.key);
        category.addTile(tile1);
        category.addTile(tile2);
        category.addTile(tile3);
        mCategoryByKeyMap.put(CATEGORY_HOMEPAGE, category);

        mCategoryManager.filterDuplicateTiles(mCategoryByKeyMap);

        assertThat(category.getTilesCount()).isEqualTo(1);
    }
}
