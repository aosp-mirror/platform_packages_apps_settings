/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.dashboard.profileselector;
import androidx.fragment.app.Fragment;

import com.android.settings.accounts.AccountPersonalDashboardFragment;
import com.android.settings.accounts.AccountWorkProfileDashboardFragment;

/**
 * Account Setting page for personal/managed profile.
 */
public class ProfileSelectAccountFragment extends ProfileSelectFragment {

    @Override
    public Fragment[] getFragments() {
        return new Fragment[] {
                new AccountPersonalDashboardFragment(),
                new AccountWorkProfileDashboardFragment()
        };
    }
}
