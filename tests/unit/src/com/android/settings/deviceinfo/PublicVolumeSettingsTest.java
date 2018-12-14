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

package com.android.settings.deviceinfo;

import android.content.Intent;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import com.android.settings.Settings;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class PublicVolumeSettingsTest {
    @Rule
    public ActivityTestRule<Settings.PublicVolumeSettingsActivity> mActivityRule =
            new ActivityTestRule<>(Settings.PublicVolumeSettingsActivity.class, true, true);

    @Test
    public void test_invalidSetupDoesNotCrashSettings() {
        Intent intent = new Intent();
        intent.setAction(android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS);
        intent.setClassName(InstrumentationRegistry.getTargetContext().getPackageName(),
                Settings.PublicVolumeSettingsActivity.class.getName());
        mActivityRule.launchActivity(intent);

        // Should exit gracefully without crashing.
    }
}
