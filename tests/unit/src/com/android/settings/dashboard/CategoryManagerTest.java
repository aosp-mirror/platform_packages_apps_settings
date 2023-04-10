/*
 * Copyright (C) 2020 The Android Open Source Project
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
import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_KEYHINT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ProviderInfo;
import android.os.Bundle;
import android.util.Pair;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.safetycenter.SafetyCenterManagerWrapper;
import com.android.settingslib.drawer.ActivityTile;
import com.android.settingslib.drawer.CategoryKey;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.ProviderTile;
import com.android.settingslib.drawer.Tile;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class CategoryManagerTest {

    private ActivityInfo mActivityInfo;
    private Context mContext;
    private CategoryManager mCategoryManager;
    private Map<Pair<String, String>, Tile> mTileByComponentCache;
    private Map<String, DashboardCategory> mCategoryByKeyMap;

    @Mock private SafetyCenterManagerWrapper mSafetyCenterManagerWrapper;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();
        mActivityInfo = new ActivityInfo();
        mActivityInfo.packageName = "pkg";
        mActivityInfo.name = "class";
        mActivityInfo.applicationInfo = new ApplicationInfo();
        mTileByComponentCache = new HashMap<>();
        mCategoryByKeyMap = new HashMap<>();
        mCategoryManager = CategoryManager.get(mContext);
        SafetyCenterManagerWrapper.sInstance = mSafetyCenterManagerWrapper;
    }

    @Test
    public void getInstance_shouldBeSingleton() {
        assertThat(mCategoryManager).isSameInstanceAs(CategoryManager.get(mContext));
    }

    @Test
    public void backwardCompatCleanupForCategory_shouldNotChangeCategoryForNewKeys() {
        final Tile tile1 = new ActivityTile(mActivityInfo, CategoryKey.CATEGORY_ACCOUNT);
        final Tile tile2 = new ActivityTile(mActivityInfo, CategoryKey.CATEGORY_ACCOUNT);
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
        final Tile tile1 = new ActivityTile(mActivityInfo, CategoryKey.CATEGORY_ACCOUNT);
        final String oldCategory = "com.android.settings.category.wireless";
        final Tile tile2 = new ActivityTile(mActivityInfo, oldCategory);
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
        final Tile tile1 = new ActivityTile(mActivityInfo, oldCategory);
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
    public void mergeSecurityPrivacyKeys_safetyCenterEnabled_shouldNotChangeOtherKeys() {
        when(mSafetyCenterManagerWrapper.isEnabled(mContext)).thenReturn(true);

        final Tile tile1 = new ActivityTile(mActivityInfo, CategoryKey.CATEGORY_ACCOUNT);
        final String oldCategory = "com.android.settings.category.wireless";
        final Tile tile2 = new ActivityTile(mActivityInfo, oldCategory);
        final DashboardCategory category1 = new DashboardCategory(CategoryKey.CATEGORY_ACCOUNT);
        category1.addTile(tile1);
        final DashboardCategory category2 = new DashboardCategory(oldCategory);
        category2.addTile(tile2);
        mCategoryByKeyMap.put(CategoryKey.CATEGORY_ACCOUNT, category1);
        mCategoryByKeyMap.put(oldCategory, category2);
        mTileByComponentCache.put(new Pair<>("PACKAGE", "CLASS1"), tile1);
        mTileByComponentCache.put(new Pair<>("PACKAGE", "CLASS2"), tile2);

        mCategoryManager.mergeSecurityPrivacyKeys(
                mContext, mTileByComponentCache, mCategoryByKeyMap);

        assertThat(mCategoryByKeyMap.size()).isEqualTo(2);
        assertThat(mCategoryByKeyMap.get(CategoryKey.CATEGORY_ACCOUNT).getTilesCount())
                .isEqualTo(1);
        assertThat(mCategoryByKeyMap.get(oldCategory).getTilesCount()).isEqualTo(1);
        assertThat(mCategoryByKeyMap.get(CategoryKey.CATEGORY_MORE_SECURITY_PRIVACY_SETTINGS))
                .isNull();
    }

    @Test
    public void mergeSecurityPrivacyKeys_safetyCenterEnabled_shouldChangeSecurityPrivacyKeys() {
        when(mSafetyCenterManagerWrapper.isEnabled(mContext)).thenReturn(true);

        final Tile tileWithSecurityCategory =
                new ActivityTile(mActivityInfo, CategoryKey.CATEGORY_SECURITY_ADVANCED_SETTINGS);
        final Tile tileWithPrivacyCategory =
                new ActivityTile(mActivityInfo, CategoryKey.CATEGORY_PRIVACY);
        final DashboardCategory categoryAdvancedSecurity =
                new DashboardCategory(CategoryKey.CATEGORY_SECURITY_ADVANCED_SETTINGS);
        categoryAdvancedSecurity.addTile(tileWithSecurityCategory);
        final DashboardCategory categoryPrivacy =
                new DashboardCategory(CategoryKey.CATEGORY_PRIVACY);
        categoryPrivacy.addTile(tileWithPrivacyCategory);
        mCategoryByKeyMap.put(
                CategoryKey.CATEGORY_SECURITY_ADVANCED_SETTINGS, categoryAdvancedSecurity);
        mCategoryByKeyMap.put(CategoryKey.CATEGORY_PRIVACY, categoryPrivacy);
        mTileByComponentCache.put(new Pair<>("PACKAGE", "CLASS1"), tileWithSecurityCategory);
        mTileByComponentCache.put(new Pair<>("PACKAGE", "CLASS2"), tileWithPrivacyCategory);

        mCategoryManager.mergeSecurityPrivacyKeys(
                mContext, mTileByComponentCache, mCategoryByKeyMap);

        assertThat(
                        mCategoryByKeyMap
                                .get(CategoryKey.CATEGORY_MORE_SECURITY_PRIVACY_SETTINGS)
                                .getTilesCount())
                .isEqualTo(2);
    }

    @Test
    public void mergeSecurityPrivacyKeys_safetyCenterDisabled_shouldNotChangeSecurityPrivacyKeys() {
        when(mSafetyCenterManagerWrapper.isEnabled(mContext)).thenReturn(false);

        final Tile tileWithSecurityCategory =
                new ActivityTile(mActivityInfo, CategoryKey.CATEGORY_SECURITY_ADVANCED_SETTINGS);
        final Tile tileWithPrivacyCategory =
                new ActivityTile(mActivityInfo, CategoryKey.CATEGORY_PRIVACY);
        final DashboardCategory categoryAdvancedSecurity =
                new DashboardCategory(CategoryKey.CATEGORY_SECURITY_ADVANCED_SETTINGS);
        categoryAdvancedSecurity.addTile(tileWithSecurityCategory);
        final DashboardCategory categoryPrivacy =
                new DashboardCategory(CategoryKey.CATEGORY_PRIVACY);
        categoryPrivacy.addTile(tileWithPrivacyCategory);
        mCategoryByKeyMap.put(
                CategoryKey.CATEGORY_SECURITY_ADVANCED_SETTINGS, categoryAdvancedSecurity);
        mCategoryByKeyMap.put(CategoryKey.CATEGORY_PRIVACY, categoryPrivacy);
        mTileByComponentCache.put(new Pair<>("PACKAGE", "CLASS1"), tileWithSecurityCategory);
        mTileByComponentCache.put(new Pair<>("PACKAGE", "CLASS2"), tileWithPrivacyCategory);

        mCategoryManager.mergeSecurityPrivacyKeys(
                mContext, mTileByComponentCache, mCategoryByKeyMap);

        assertThat(mCategoryByKeyMap.get(CategoryKey.CATEGORY_MORE_SECURITY_PRIVACY_SETTINGS))
                .isNull();
    }

    @Test
    public void sortCategories_singlePackage_shouldReorderBasedOnPriority() {
        // Create some fake tiles that are not sorted.
        final String testPackage = "com.android.test";
        final DashboardCategory category = new DashboardCategory(CATEGORY_HOMEPAGE);
        final Tile tile1 = createActivityTile(category.key, testPackage, "class1", 100);
        final Tile tile2 = createActivityTile(category.key, testPackage, "class2", 50);
        final Tile tile3 = createActivityTile(category.key, testPackage, "class3", 200);
        category.addTile(tile1);
        category.addTile(tile2);
        category.addTile(tile3);
        mCategoryByKeyMap.put(CATEGORY_HOMEPAGE, category);

        // Sort their priorities
        mCategoryManager.sortCategories(ApplicationProvider.getApplicationContext(),
                mCategoryByKeyMap);

        // Verify they are now sorted.
        assertThat(category.getTile(0)).isSameInstanceAs(tile3);
        assertThat(category.getTile(1)).isSameInstanceAs(tile1);
        assertThat(category.getTile(2)).isSameInstanceAs(tile2);
    }

    @Test
    public void sortCategories_multiPackage_shouldReorderBasedOnPackageAndPriority() {
        // Create some fake tiles that are not sorted.
        final String testPackage1 = "com.android.test1";
        final String testPackage2 = "com.android.test2";
        final DashboardCategory category = new DashboardCategory(CATEGORY_HOMEPAGE);
        final Tile tile1 = createActivityTile(category.key, testPackage2, "class1", 100);
        final Tile tile2 = createActivityTile(category.key, testPackage1, "class2", 100);
        final Tile tile3 = createActivityTile(category.key, testPackage1, "class3", 50);
        category.addTile(tile1);
        category.addTile(tile2);
        category.addTile(tile3);
        mCategoryByKeyMap.put(CATEGORY_HOMEPAGE, category);

        // Sort their priorities
        mCategoryManager.sortCategories(mContext, mCategoryByKeyMap);

        // Verify they are now sorted.
        assertThat(category.getTile(0)).isSameInstanceAs(tile2);
        assertThat(category.getTile(1)).isSameInstanceAs(tile1);
        assertThat(category.getTile(2)).isSameInstanceAs(tile3);
    }

    @Test
    public void sortCategories_internalPackageTiles_shouldSkipTileForInternalPackage() {
        // Create some fake tiles that are not sorted.
        final String testPackage = mContext.getPackageName();
        final DashboardCategory category = new DashboardCategory(CATEGORY_HOMEPAGE);
        final Tile tile1 = createActivityTile(category.key, testPackage, "class1", 100);
        final Tile tile2 = createActivityTile(category.key, testPackage, "class2", 100);
        final Tile tile3 = createActivityTile(category.key, testPackage, "class3", 50);
        category.addTile(tile1);
        category.addTile(tile2);
        category.addTile(tile3);
        mCategoryByKeyMap.put(CATEGORY_HOMEPAGE, category);

        // Sort their priorities
        mCategoryManager.sortCategories(mContext, mCategoryByKeyMap);

        // Verify the sorting order is not changed
        assertThat(category.getTile(0)).isSameInstanceAs(tile1);
        assertThat(category.getTile(1)).isSameInstanceAs(tile2);
        assertThat(category.getTile(2)).isSameInstanceAs(tile3);
    }

    @Test
    public void sortCategories_internalAndExternalPackageTiles_shouldRetainPriorityOrdering() {
        // Inject one external tile among internal tiles.
        final String testPackage = mContext.getPackageName();
        final String testPackage2 = "com.google.test2";
        final DashboardCategory category = new DashboardCategory(CATEGORY_HOMEPAGE);
        final Tile tile1 = createActivityTile(category.key, testPackage, "class1", 2);
        final Tile tile2 = createActivityTile(category.key, testPackage, "class2", 1);
        final Tile tile3 = createActivityTile(category.key, testPackage2, "class0", 0);
        final Tile tile4 = createActivityTile(category.key, testPackage, "class3", -1);
        category.addTile(tile1);
        category.addTile(tile2);
        category.addTile(tile3);
        category.addTile(tile4);
        mCategoryByKeyMap.put(CATEGORY_HOMEPAGE, category);

        // Sort their priorities
        mCategoryManager.sortCategories(mContext, mCategoryByKeyMap);

        // Verify the sorting order is not changed
        assertThat(category.getTile(0)).isSameInstanceAs(tile1);
        assertThat(category.getTile(1)).isSameInstanceAs(tile2);
        assertThat(category.getTile(2)).isSameInstanceAs(tile3);
        assertThat(category.getTile(3)).isSameInstanceAs(tile4);
    }

    @Test
    public void sortCategories_samePriority_internalPackageTileShouldTakePrecedence() {
        // Inject one external tile among internal tiles with same priority.
        final String testPackage = mContext.getPackageName();
        final String testPackage2 = "com.google.test2";
        final String testPackage3 = "com.abcde.test3";
        final DashboardCategory category = new DashboardCategory(CATEGORY_HOMEPAGE);
        final Tile tile1 = createActivityTile(category.key, testPackage2, "class1", 1);
        final Tile tile2 = createActivityTile(category.key, testPackage, "class2", 1);
        final Tile tile3 = createActivityTile(category.key, testPackage3, "class3", 1);
        category.addTile(tile1);
        category.addTile(tile2);
        category.addTile(tile3);
        mCategoryByKeyMap.put(CATEGORY_HOMEPAGE, category);

        // Sort their priorities
        mCategoryManager.sortCategories(mContext, mCategoryByKeyMap);

        // Verify the sorting order is internal first, follow by package name ordering
        assertThat(category.getTile(0)).isSameInstanceAs(tile2);
        assertThat(category.getTile(1)).isSameInstanceAs(tile3);
        assertThat(category.getTile(2)).isSameInstanceAs(tile1);
    }

    @Test
    public void filterTiles_noDuplicate_noChange() {
        // Create some unique tiles
        final String testPackage = mContext.getPackageName();
        final DashboardCategory category = new DashboardCategory(CATEGORY_HOMEPAGE);
        final Tile tile1 = createActivityTile(category.key, testPackage, "class1", 100);
        final Tile tile2 = createActivityTile(category.key, testPackage, "class2", 100);
        final Tile tile3 = createActivityTile(category.key, testPackage, "class3", 50);
        final Tile tile4 = createProviderTile(category.key, testPackage, "class1", "authority1",
                "key1", 100);
        final Tile tile5 = createProviderTile(category.key, testPackage, "class1", "authority2",
                "key2", 100);
        final Tile tile6 = createProviderTile(category.key, testPackage, "class1", "authority1",
                "key2", 50);
        category.addTile(tile1);
        category.addTile(tile2);
        category.addTile(tile3);
        category.addTile(tile4);
        category.addTile(tile5);
        category.addTile(tile6);
        mCategoryByKeyMap.put(CATEGORY_HOMEPAGE, category);

        mCategoryManager.filterDuplicateTiles(mCategoryByKeyMap);

        assertThat(category.getTilesCount()).isEqualTo(6);
    }

    @Test
    public void filterTiles_hasDuplicateActivityTiles_shouldOnlyKeepUniqueTiles() {
        // Create tiles pointing to same intent.
        final String testPackage = mContext.getPackageName();
        final DashboardCategory category = new DashboardCategory(CATEGORY_HOMEPAGE);
        final Tile tile1 = createActivityTile(category.key, testPackage, "class1", 100);
        final Tile tile2 = createActivityTile(category.key, testPackage, "class1", 100);
        final Tile tile3 = createActivityTile(category.key, testPackage, "class1", 50);
        category.addTile(tile1);
        category.addTile(tile2);
        category.addTile(tile3);
        mCategoryByKeyMap.put(CATEGORY_HOMEPAGE, category);

        mCategoryManager.filterDuplicateTiles(mCategoryByKeyMap);

        assertThat(category.getTilesCount()).isEqualTo(1);
    }

    @Test
    public void filterTiles_hasDuplicateProviderTiles_shouldOnlyKeepUniqueTiles() {
        // Create tiles pointing to same authority and key.
        final String testPackage = mContext.getPackageName();
        final DashboardCategory category = new DashboardCategory(CATEGORY_HOMEPAGE);
        final Tile tile1 = createProviderTile(category.key, testPackage, "class1", "authority1",
                "key1", 100);
        final Tile tile2 = createProviderTile(category.key, testPackage, "class2", "authority1",
                "key1", 100);
        final Tile tile3 = createProviderTile(category.key, testPackage, "class3", "authority1",
                "key1", 50);
        category.addTile(tile1);
        category.addTile(tile2);
        category.addTile(tile3);
        mCategoryByKeyMap.put(CATEGORY_HOMEPAGE, category);

        mCategoryManager.filterDuplicateTiles(mCategoryByKeyMap);

        assertThat(category.getTilesCount()).isEqualTo(1);
    }

    private Tile createActivityTile(String categoryKey, String packageName, String className,
            int order) {
        final ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.packageName = packageName;
        activityInfo.name = className;
        activityInfo.metaData = new Bundle();
        activityInfo.metaData.putInt(META_DATA_KEY_ORDER, order);
        return new ActivityTile(activityInfo, categoryKey);
    }

    private Tile createProviderTile(String categoryKey, String packageName, String className,
            String authority, String key, int order) {
        final ProviderInfo providerInfo = new ProviderInfo();
        final Bundle metaData = new Bundle();
        providerInfo.packageName = packageName;
        providerInfo.name = className;
        providerInfo.authority = authority;
        metaData.putString(META_DATA_PREFERENCE_KEYHINT, key);
        metaData.putInt(META_DATA_KEY_ORDER, order);
        return new ProviderTile(providerInfo, categoryKey, metaData);
    }
}
