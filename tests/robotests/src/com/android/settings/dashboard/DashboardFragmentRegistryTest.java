/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import com.android.settings.accounts.AccountDashboardFragment;
import com.android.settingslib.drawer.CategoryKey;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class DashboardFragmentRegistryTest {
    @Test
    public void pageAndKeyShouldHave1to1Mapping() {
        assertThat(DashboardFragmentRegistry.CATEGORY_KEY_TO_PARENT_MAP.size())
                .isEqualTo(DashboardFragmentRegistry.PARENT_TO_CATEGORY_KEY_MAP.size());
    }

    @Test
    public void accountDetailCategoryShouldRedirectToAccountDashboardFragment() {
        final String fragment = DashboardFragmentRegistry.CATEGORY_KEY_TO_PARENT_MAP.get(
                CategoryKey.CATEGORY_ACCOUNT_DETAIL);

        assertThat(fragment).isEqualTo(AccountDashboardFragment.class.getName());
    }
}
