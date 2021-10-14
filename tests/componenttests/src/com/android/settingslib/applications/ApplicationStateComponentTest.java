/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settingslib.applications;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.android.settings.Settings;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ApplicationStateComponentTest {
    private static final String TAG =
            ApplicationStateComponentTest.class.getSimpleName();
    private Context mRuntimeApplication;
    private ApplicationsState mApplicationsState;

    @Rule
    public ActivityScenarioRule<Settings.ManageApplicationsActivity> rule =
            new ActivityScenarioRule<>(
                    new Intent(
                            android.provider.Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));

    private ApplicationsState.AppEntry createAppEntry(String label, String packageName, int id) {
        ApplicationInfo appInfo = createApplicationInfo(packageName, id);
        ApplicationsState.AppEntry appEntry = new ApplicationsState.AppEntry(mRuntimeApplication,
                appInfo, id);
        appEntry.label = label;
        appEntry.mounted = true;
        return appEntry;
    }

    private ApplicationInfo createApplicationInfo(String packageName, int uid) {
        ApplicationInfo appInfo = new ApplicationInfo();
        appInfo.sourceDir = "foo";
        appInfo.flags |= ApplicationInfo.FLAG_INSTALLED;
        appInfo.storageUuid = UUID.randomUUID();
        appInfo.packageName = packageName;
        appInfo.uid = uid;
        return appInfo;
    }

    @Test
    public void test_all_apps_sorting_alpha() {
        // TODO: Potential unit test candidate.
        // To test all app list has sorted alphabetical, only need to verify sort function.
        // This case focus on logic in sort function, and ignore origin locale sorting rule by Java.

        ActivityScenario scenario = rule.getScenario();

        scenario.onActivity(activity -> {
            mRuntimeApplication = activity.getApplication();
            mApplicationsState = ApplicationsState.getInstance(activity.getApplication());

            ApplicationsState.AppEntry entry1 = createAppEntry("Info01", "Package1", 0);
            ApplicationsState.AppEntry entry2 = createAppEntry("Info02", "Package1", 0);
            ApplicationsState.AppEntry entry3 = createAppEntry("Info01", "Package2", 0);
            ApplicationsState.AppEntry entry4 = createAppEntry("Info02", "Package2", 0);
            ApplicationsState.AppEntry entry5 = createAppEntry("Info02", "Package2", 1);
            assertThat(ApplicationsState.ALPHA_COMPARATOR.compare(entry1, entry2)).isEqualTo(-1);
            assertThat(ApplicationsState.ALPHA_COMPARATOR.compare(entry2, entry3)).isEqualTo(1);
            assertThat(ApplicationsState.ALPHA_COMPARATOR.compare(entry3, entry2)).isEqualTo(-1);
            assertThat(ApplicationsState.ALPHA_COMPARATOR.compare(entry3, entry3)).isEqualTo(0);
            assertThat(ApplicationsState.ALPHA_COMPARATOR.compare(entry1, entry3)).isEqualTo(-1);
            assertThat(ApplicationsState.ALPHA_COMPARATOR.compare(entry4, entry5)).isEqualTo(-1);
            assertThat(ApplicationsState.ALPHA_COMPARATOR.compare(entry5, entry3)).isEqualTo(1);

        });

    }
}
