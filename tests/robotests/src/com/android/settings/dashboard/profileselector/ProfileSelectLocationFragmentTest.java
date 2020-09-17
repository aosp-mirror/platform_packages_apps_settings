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

import static com.android.settings.dashboard.profileselector.ProfileSelectFragment.EXTRA_PROFILE;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ProfileSelectLocationFragmentTest {

    private ProfileSelectLocationFragment mProfileSelectLocationFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mProfileSelectLocationFragment = new ProfileSelectLocationFragment();
    }

    @Test
    public void getFragments_containsCorrectBundle() {
        assertThat(mProfileSelectLocationFragment.getFragments().length).isEqualTo(2);
        assertThat(mProfileSelectLocationFragment.getFragments()[0].getArguments().getInt(
                EXTRA_PROFILE, -1)).isEqualTo(ProfileSelectFragment.ProfileType.PERSONAL);
        assertThat(mProfileSelectLocationFragment.getFragments()[1].getArguments().getInt(
                EXTRA_PROFILE, -1)).isEqualTo(ProfileSelectFragment.ProfileType.WORK);
    }
}
