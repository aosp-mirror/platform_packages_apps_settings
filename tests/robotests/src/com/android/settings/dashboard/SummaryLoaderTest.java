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
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.Tile;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SummaryLoaderTest {
    private static final String SUMMARY_1 = "summary1";
    private static final String SUMMARY_2 = "summary2";
    private SummaryLoader mSummaryLoader;
    private boolean mCallbackInvoked;
    private Tile mTile;

    @Before
    public void SetUp() {
        mTile = new Tile();
        mTile.summary = SUMMARY_1;
        mCallbackInvoked = false;

        final Activity activity = Robolectric.buildActivity(Activity.class).get();
        final List<DashboardCategory> categories = new ArrayList<>();
        mSummaryLoader = new SummaryLoader(activity, categories);
        mSummaryLoader.setSummaryConsumer(new SummaryLoader.SummaryConsumer() {
            @Override
            public void notifySummaryChanged(Tile tile) {
                mCallbackInvoked = true;
            }
        });
    }

    @Test
    public void testUpdateSummaryIfNeeded_SummaryIdentical_NoCallback() {
        mSummaryLoader.updateSummaryIfNeeded(mTile, SUMMARY_1);

        assertThat(mCallbackInvoked).isFalse();
    }

    @Test
    public void testUpdateSummaryIfNeeded_SummaryChanged_HasCallback() {
        mSummaryLoader.updateSummaryIfNeeded(mTile, SUMMARY_2);

        assertThat(mCallbackInvoked).isTrue();
    }
}
