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

import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_KEYHINT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.drawer.CategoryKey;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.Tile;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SummaryLoaderTest {

    private static final String SUMMARY_1 = "summary1";
    private static final String SUMMARY_2 = "summary2";

    private Context mContext;
    private SummaryLoader mSummaryLoader;
    private boolean mCallbackInvoked;
    private Tile mTile;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void SetUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        final ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.packageName = "pkg";
        activityInfo.name = "class";
        mTile = new Tile(activityInfo, CategoryKey.CATEGORY_HOMEPAGE);
        mTile.overrideSummary(SUMMARY_1);
        mCallbackInvoked = false;

        final Activity activity = Robolectric.buildActivity(Activity.class).get();

        mSummaryLoader = new SummaryLoader(activity, CategoryKey.CATEGORY_HOMEPAGE);
        mSummaryLoader.setSummaryConsumer(tile -> mCallbackInvoked = true);
    }

    @Test
    public void newInstance_shouldNotLoadCategory() {
        verifyZeroInteractions(mFeatureFactory.dashboardFeatureProvider);
    }

    @Test
    public void testUpdateSummaryIfNeeded_SummaryIdentical_NoCallback() {
        mSummaryLoader.updateSummaryIfNeeded(mContext, mTile, SUMMARY_1);

        assertThat(mCallbackInvoked).isFalse();
    }

    @Test
    public void testUpdateSummaryIfNeeded_SummaryChanged_HasCallback() {
        mSummaryLoader.updateSummaryIfNeeded(mContext, mTile, SUMMARY_2);

        assertThat(mCallbackInvoked).isTrue();
    }

    @Test
    public void testUpdateSummaryToCache_hasCache_shouldUpdate() {
        final String testSummary = "test_summary";
        final DashboardCategory category = new DashboardCategory(CategoryKey.CATEGORY_HOMEPAGE);
        final ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.packageName = "pkg";
        activityInfo.name = "cls";
        activityInfo.metaData = new Bundle();
        activityInfo.metaData.putString(META_DATA_PREFERENCE_KEYHINT, "123");
        final Tile tile = new Tile(activityInfo, category.key);

        category.addTile(tile);
        when(mFeatureFactory.dashboardFeatureProvider.getDashboardKeyForTile(tile))
                .thenReturn(tile.getKey(RuntimeEnvironment.application));

        mSummaryLoader.updateSummaryIfNeeded(mContext, tile, testSummary);
        tile.overrideSummary(null);
        mSummaryLoader.updateSummaryToCache(category);

        assertThat(tile.getSummary(mContext)).isEqualTo(testSummary);
    }
}
