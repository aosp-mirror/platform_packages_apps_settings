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
 *
 */

package com.android.settings.search;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import com.android.settings.SettingsActivity;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.dashboard.SiteMapManager;
import com.android.settings.search.IndexDatabaseHelper.SiteMapColumns;
import com.android.settings.system.SystemDashboardFragment;
import com.android.settings.testutils.DatabaseTestUtils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.drawer.CategoryKey;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.Tile;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SiteMapManagerTest {

    private static final int STATIC_DB_DEPTH = 4;
    private static final String CLASS_PREFIX = "class_";
    private static final String TITLE_PREFIX = "title_";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mMockContext;
    private Context mContext;
    private SQLiteDatabase mDb;
    private SiteMapManager mSiteMapManager;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mMockContext);
        mFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mMockContext);

        mContext = RuntimeEnvironment.application;
        mDb = IndexDatabaseHelper.getInstance(mContext).getWritableDatabase();
        buildDb();
        mSiteMapManager = new SiteMapManager();
    }

    @After
    public void cleanUp() {
        DatabaseTestUtils.clearDb(mContext);
    }

    @Test
    public void buildBreadCrumb_onlyFromSiteMapDb_breadcrumbShouldLinkUp() {
        List<String> breadcrumb = mSiteMapManager.buildBreadCrumb(mContext,
                CLASS_PREFIX + 0, TITLE_PREFIX + 0);
        assertThat(breadcrumb.size()).isEqualTo(STATIC_DB_DEPTH + 1);
        for (int i = 0; i < STATIC_DB_DEPTH; i++) {
            assertThat(breadcrumb.get(i)).isEqualTo(TITLE_PREFIX + (STATIC_DB_DEPTH - i));
        }
    }

    @Test
    public void buildBreadCrumb_fromSiteMapDbAndDashboardProvider_breadcrumbShouldLinkUp() {
        final String iaClass = SystemDashboardFragment.class.getName();
        final String iaTitle = "ia_title";

        ContentValues index = new ContentValues();
        index.put(IndexDatabaseHelper.IndexColumns.CLASS_NAME, iaClass);
        index.put(IndexDatabaseHelper.IndexColumns.SCREEN_TITLE, iaTitle);
        mDb.replaceOrThrow(IndexDatabaseHelper.Tables.TABLE_PREFS_INDEX, null, index);

        final DashboardCategory category = new DashboardCategory();
        category.key = CategoryKey.CATEGORY_SYSTEM;
        category.tiles.add(new Tile());
        category.tiles.get(0).title = TITLE_PREFIX + STATIC_DB_DEPTH;
        category.tiles.get(0).metaData = new Bundle();
        category.tiles.get(0).metaData.putString(SettingsActivity.META_DATA_KEY_FRAGMENT_CLASS,
                CLASS_PREFIX + STATIC_DB_DEPTH);
        when(mFeatureFactory.dashboardFeatureProvider.getAllCategories())
                .thenReturn(Arrays.asList(category));

        final List<String> breadcrumb = mSiteMapManager.buildBreadCrumb(mContext,
                CLASS_PREFIX + 0, TITLE_PREFIX + 0);

        assertThat(breadcrumb.size()).isEqualTo(STATIC_DB_DEPTH + 2);
        assertThat(breadcrumb.get(0))
                .isEqualTo(iaTitle);
    }

    @Test
    public void buildBreadCrumb_classNotIndexed_shouldNotHaveBreadCrumb() {
        final String title = "wrong_title";

        final List<String> breadcrumb = mSiteMapManager.buildBreadCrumb(mContext,
                "wrong_class", title);

        assertThat(breadcrumb.size()).isEqualTo(1);
        assertThat(breadcrumb.get(0)).isEqualTo(title);
    }

    private void buildDb() {
        for (int i = 0; i < STATIC_DB_DEPTH; i++) {
            final ContentValues siteMapPair = new ContentValues();
            siteMapPair.put(SiteMapColumns.DOCID, i);
            siteMapPair.put(SiteMapColumns.PARENT_CLASS, CLASS_PREFIX + (i + 1));
            siteMapPair.put(SiteMapColumns.PARENT_TITLE, TITLE_PREFIX + (i + 1));
            siteMapPair.put(SiteMapColumns.CHILD_CLASS, CLASS_PREFIX + i);
            siteMapPair.put(SiteMapColumns.CHILD_TITLE, TITLE_PREFIX + i);
            mDb.replaceOrThrow(IndexDatabaseHelper.Tables.TABLE_SITE_MAP, null, siteMapPair);
        }
    }
}
