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

package com.android.settings.applications.manageapplications;

import static com.android.settings.applications.manageapplications.AppFilterRegistry
        .FILTER_APPS_ENABLED;
import static com.android.settings.applications.manageapplications.AppFilterRegistry
        .FILTER_APPS_USAGE_ACCESS;

import static com.google.common.truth.Truth.assertThat;

import com.android.settings.R;
import com.android.settings.applications.AppStateUsageBridge;
import com.android.settingslib.applications.ApplicationsState;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AppFilterItemTest {

    @Test
    public void equals_sameContent_true() {
        final AppFilterItem item = AppFilterRegistry.getInstance().get(FILTER_APPS_USAGE_ACCESS);
        final AppFilterItem item2 = new AppFilterItem(
                AppStateUsageBridge.FILTER_APP_USAGE,
                FILTER_APPS_USAGE_ACCESS,
                R.string.filter_all_apps);

        // Same instance, should be same
        // (Use isTrue as isEqualsTo will prioritize reference equality!)
        assertThat(item.equals(item)).isTrue();

        // Same content, should be same
        assertThat(item).isEqualTo(item2);
    }

    @Test
    public void compare_sameContent_return0() {
        final AppFilterItem item = AppFilterRegistry.getInstance().get(FILTER_APPS_USAGE_ACCESS);
        final AppFilterItem item2 = new AppFilterItem(
                AppStateUsageBridge.FILTER_APP_USAGE,
                FILTER_APPS_USAGE_ACCESS,
                R.string.filter_all_apps);

        assertThat(item.compareTo(item)).isEqualTo(0);
        assertThat(item.compareTo(item2)).isEqualTo(0);
        assertThat(item2.compareTo(item)).isEqualTo(0);
    }

    @Test
    public void compare_toNull_return1() {
        final AppFilterItem item = AppFilterRegistry.getInstance().get(FILTER_APPS_USAGE_ACCESS);
        assertThat(item.compareTo(null)).isEqualTo(1);
    }

    @Test
    public void compare_differentFilter_returnFilterDiff() {
        final AppFilterItem item = AppFilterRegistry.getInstance().get(FILTER_APPS_USAGE_ACCESS);
        final AppFilterItem item2 = new AppFilterItem(
                ApplicationsState.FILTER_ALL_ENABLED,
                FILTER_APPS_ENABLED,
                R.string.filter_enabled_apps);
        assertThat(item.compareTo(item2)).isNotEqualTo(0);
    }

    @Test
    public void hash_differentItem_differentHash() {
        final AppFilterItem item = AppFilterRegistry.getInstance().get(FILTER_APPS_USAGE_ACCESS);
        final AppFilterItem item2 = AppFilterRegistry.getInstance().get(FILTER_APPS_ENABLED);

        assertThat(item.hashCode()).isNotEqualTo(item2.hashCode());
    }
}
