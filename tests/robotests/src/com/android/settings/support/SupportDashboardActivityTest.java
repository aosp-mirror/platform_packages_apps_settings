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

package com.android.settings.support;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import com.android.settings.R;
import com.android.settingslib.search.SearchIndexableRaw;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class SupportDashboardActivityTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
    }

    @Test
    public void shouldIndexSearchActivityForSearch() {
        final List<SearchIndexableRaw> indexables =
                SupportDashboardActivity.SEARCH_INDEX_DATA_PROVIDER
                        .getRawDataToIndex(mContext, true /* enabled */);

        assertThat(indexables).hasSize(1);

        final SearchIndexableRaw value = indexables.get(0);

        assertThat(value.title).isEqualTo(mContext.getString(R.string.page_tab_title_support));
        assertThat(value.screenTitle).isEqualTo(
                mContext.getString(R.string.page_tab_title_support));
        assertThat(value.intentTargetPackage).isEqualTo(mContext.getPackageName());
        assertThat(value.intentTargetClass).isEqualTo(SupportDashboardActivity.class.getName());
        assertThat(value.intentAction).isEqualTo(Intent.ACTION_MAIN);
    }

    @Test
    public void shouldHandleIntentAction() {
        PackageManager packageManager = mContext.getPackageManager();
        // Intent action used by setup wizard to start support settings
        Intent intent = new Intent("com.android.settings.action.SUPPORT_SETTINGS");
        ResolveInfo resolveInfo =
                packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        assertThat(resolveInfo).isNotNull();
        assertThat(resolveInfo.activityInfo.name)
                .isEqualTo(SupportDashboardActivity.class.getName());
    }
}
