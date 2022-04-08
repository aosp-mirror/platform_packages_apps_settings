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

import android.util.ArrayMap;

import com.android.settings.accounts.AccountDashboardFragment;
import com.android.settings.applications.manageapplications.ManageApplications;
import com.android.settings.location.LocationSettings;
import com.android.settings.location.RecentLocationRequestSeeAllFragment;

import java.util.Map;

/**
 * A registry to keep track of which page and its own profile selection page.
 */
public class ProfileFragmentBridge {

    /**
     * Map from parent fragment to category key. The parent fragment hosts child with
     * category_key.
     */
    public static final Map<String, String> FRAGMENT_MAP;

    static {
        FRAGMENT_MAP = new ArrayMap<>();
        FRAGMENT_MAP.put(AccountDashboardFragment.class.getName(),
                ProfileSelectAccountFragment.class.getName());
        FRAGMENT_MAP.put(ManageApplications.class.getName(),
                ProfileSelectManageApplications.class.getName());
        FRAGMENT_MAP.put(LocationSettings.class.getName(),
                ProfileSelectLocationFragment.class.getName());
        FRAGMENT_MAP.put(RecentLocationRequestSeeAllFragment.class.getName(),
                ProfileSelectRecentLocationRequestFragment.class.getName());
    }
}
