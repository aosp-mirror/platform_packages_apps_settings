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

package com.android.settings;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SettingsActivityTest {

    private SettingsActivity mActivity;

    @Test
    public void testQueryTextChange_shouldUpdate() {
        final String testQuery = "abc";
        mActivity = new SettingsActivity();

        assertThat(mActivity.mSearchQuery).isNull();
        try {
            mActivity.onQueryTextChange(testQuery);
        } catch (NullPointerException e) {
            // Expected, because searchFeatureProvider is not wired up.
        }

        assertThat(mActivity.mSearchQuery).isEqualTo(testQuery);
    }
}
