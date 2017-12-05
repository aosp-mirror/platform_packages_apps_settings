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


import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import com.android.settings.TestConfig;
import com.android.settings.testutils.DatabaseTestUtils;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SavedQueryRecorderAndRemoverTest {

    private Context mContext;
    private SavedQueryRecorder mRecorder;
    private SavedQueryRemover mRemover;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
    }

    @After
    public void cleanUp() {
        DatabaseTestUtils.clearDb(mContext);
    }

    @Test
    public void canSaveAndRemoveQuery() {
        final String query = "test";
        mRecorder = new SavedQueryRecorder(mContext, query);
        mRemover = new SavedQueryRemover(mContext);

        // Record a new query and load all queries from DB
        mRecorder.loadInBackground();
        final SavedQueryLoader loader = new SavedQueryLoader(mContext);
        List<? extends SearchResult> results = loader.loadInBackground();

        // Should contain the newly recorded query
        assertThat(results.size()).isEqualTo(1);
        assertThat(results.get(0).title).isEqualTo(query);

        // Remove the query and load all queries from DB
        mRemover.loadInBackground();
        results = loader.loadInBackground();

        // Saved query list should be empty because it's removed.
        assertThat(results).isEmpty();
    }

    @Test
    public void canRemoveAllQueriesAtOnce() {
        mRemover = new SavedQueryRemover(mContext);;

        // Record a new query and load all queries from DB
        new SavedQueryRecorder(mContext, "Test1").loadInBackground();
        new SavedQueryRecorder(mContext, "Test2").loadInBackground();
        final SavedQueryLoader loader = new SavedQueryLoader(mContext);
        List<? extends SearchResult> results = loader.loadInBackground();
        assertThat(results.size()).isEqualTo(2);

        mRemover.loadInBackground();
        results = loader.loadInBackground();

        // Saved query list should be empty because it's removed.
        assertThat(results).isEmpty();
    }
}
