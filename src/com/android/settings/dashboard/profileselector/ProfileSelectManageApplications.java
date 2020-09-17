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

import com.android.settings.applications.manageapplications.ManageApplications;

/**
 * Application Setting page for personal/managed profile.
 */
public class ProfileSelectManageApplications extends ProfileSelectFragment {

    @Override
    public Fragment[] getFragments() {
        final Bundle workOnly = getArguments() != null ? getArguments().deepCopy() : new Bundle();
        workOnly.putInt(EXTRA_PROFILE, ProfileSelectFragment.ProfileType.WORK);
        final Fragment workFragment = new ManageApplications();
        workFragment.setArguments(workOnly);

        final Bundle personalOnly = getArguments() != null ? getArguments() : new Bundle();
        personalOnly.putInt(EXTRA_PROFILE, ProfileSelectFragment.ProfileType.PERSONAL);
        final Fragment personalFragment = new ManageApplications();
        personalFragment.setArguments(personalOnly);
        return new Fragment[]{
                personalFragment, //0
                workFragment
        };
    }
}
