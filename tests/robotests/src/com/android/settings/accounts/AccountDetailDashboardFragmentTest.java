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
 * limitations under the License
 */
package com.android.settings.accounts;

import android.os.Bundle;

import com.android.settings.TestConfig;
import com.android.settingslib.drawer.CategoryKey;
import com.android.settingslib.drawer.Tile;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class AccountDetailDashboardFragmentTest {

    private static final String METADATA_CATEGORY = "com.android.settings.category";
    private static final String METADATA_ACCOUNT_TYPE = "com.android.settings.ia.account";

    private AccountDetailDashboardFragment mFragment;

    @Before
    public void setUp() {
        mFragment = new AccountDetailDashboardFragment();
        final Bundle args = new Bundle();
        args.putString(METADATA_ACCOUNT_TYPE, "com.abc");
        mFragment.mAccountType = "com.abc";
    }

    @Test
    public void testCategory_isAccount() {
        assertThat(mFragment.getCategoryKey()).isEqualTo(CategoryKey.CATEGORY_ACCOUNT);
    }

    @Test
    public void refreshDashboardTiles_HasAccountType_shouldDisplay() {
        final Tile tile = new Tile();
        final Bundle metaData = new Bundle();
        metaData.putString(METADATA_CATEGORY, CategoryKey.CATEGORY_ACCOUNT);
        metaData.putString(METADATA_ACCOUNT_TYPE, "com.abc");
        tile.metaData = metaData;

        assertThat(mFragment.displayTile(tile)).isTrue();
    }

    @Test
    public void refreshDashboardTiles_NoAccountType_shouldNotDisplay() {
        final Tile tile = new Tile();
        final Bundle metaData = new Bundle();
        metaData.putString(METADATA_CATEGORY, CategoryKey.CATEGORY_ACCOUNT);
        tile.metaData = metaData;

        assertThat(mFragment.displayTile(tile)).isFalse();
    }

    @Test
    public void refreshDashboardTiles_OtherAccountType_shouldNotDisplay() {
        final Tile tile = new Tile();
        final Bundle metaData = new Bundle();
        metaData.putString(METADATA_CATEGORY, CategoryKey.CATEGORY_ACCOUNT);
        metaData.putString(METADATA_ACCOUNT_TYPE, "com.other");
        tile.metaData = metaData;

        assertThat(mFragment.displayTile(tile)).isFalse();
    }

}
