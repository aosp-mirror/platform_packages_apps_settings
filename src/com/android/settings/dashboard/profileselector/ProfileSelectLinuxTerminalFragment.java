/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.android.settings.development.DeveloperOptionAwareMixin;
import com.android.settings.development.linuxterminal.LinuxTerminalDashboardFragment;

/** Linux terminal preferences at developers option for personal/managed profile. */
public class ProfileSelectLinuxTerminalFragment extends ProfileSelectFragment
        implements DeveloperOptionAwareMixin {

    private static final String TAG = "ProfileSelLinuxTerminalFrag";

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    @NonNull
    public Fragment[] getFragments() {
        return getFragments(
                getContext(),
                getArguments(),
                LinuxTerminalDashboardFragment::new,
                LinuxTerminalDashboardFragment::new,
                LinuxTerminalDashboardFragment::new);
    }
}
