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

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.android.settings.location.RecentLocationRequestSeeAllFragment;

/**
 * Recent location request page for personal/managed profile.
 */
public class ProfileSelectRecentLocationRequestFragment extends ProfileSelectFragment {

    @Override
    public Fragment[] getFragments() {
        final Bundle workOnly = new Bundle();
        workOnly.putInt(EXTRA_PROFILE, ProfileSelectFragment.ProfileType.WORK);
        final Fragment workFragment = new RecentLocationRequestSeeAllFragment();
        workFragment.setArguments(workOnly);

        final Bundle personalOnly = new Bundle();
        personalOnly.putInt(EXTRA_PROFILE, ProfileSelectFragment.ProfileType.PERSONAL);
        final Fragment personalFragment = new RecentLocationRequestSeeAllFragment();
        personalFragment.setArguments(personalOnly);
        return new Fragment[]{
                personalFragment, //0
                workFragment
        };
    }
}
