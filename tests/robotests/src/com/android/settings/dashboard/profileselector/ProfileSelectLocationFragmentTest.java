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

import static android.os.UserManager.USER_TYPE_FULL_SYSTEM;
import static android.os.UserManager.USER_TYPE_PROFILE_MANAGED;
import static android.os.UserManager.USER_TYPE_PROFILE_PRIVATE;

import static com.android.settings.dashboard.profileselector.ProfileSelectFragment.EXTRA_PROFILE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.pm.UserInfo;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.shadow.ShadowUserManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@Config(shadows = {
        ShadowUserManager.class,
})
@RunWith(RobolectricTestRunner.class)
public class ProfileSelectLocationFragmentTest {
    private static final String PERSONAL_PROFILE_NAME = "personal";
    private static final String WORK_PROFILE_NAME = "work";
    private static final String PRIVATE_PROFILE_NAME = "private";
    @Rule
    public final MockitoRule rule = MockitoJUnit.rule();

    private ShadowUserManager mUserManager;
    private ProfileSelectLocationFragment mProfileSelectLocationFragment;

    @Before
    public void setUp() {
        mUserManager = ShadowUserManager.getShadow();
        mUserManager.addProfile(
                new UserInfo(0, PERSONAL_PROFILE_NAME, null, 0, USER_TYPE_FULL_SYSTEM));
        mUserManager.addProfile(
                new UserInfo(1, WORK_PROFILE_NAME, null, 0, USER_TYPE_PROFILE_MANAGED));
        mUserManager.addProfile(
                new UserInfo(11, PRIVATE_PROFILE_NAME, null, 0, USER_TYPE_PROFILE_PRIVATE));
        mProfileSelectLocationFragment = spy(new ProfileSelectLocationFragment());
        when(mProfileSelectLocationFragment.getContext()).thenReturn(
                ApplicationProvider.getApplicationContext());
    }

    @Test
    public void getFragments_containsCorrectBundle() {
        assertThat(mProfileSelectLocationFragment.getFragments().length).isEqualTo(3);
        assertThat(mProfileSelectLocationFragment.getFragments()[0].getArguments().getInt(
                EXTRA_PROFILE, -1)).isEqualTo(ProfileSelectFragment.ProfileType.PERSONAL);
        assertThat(mProfileSelectLocationFragment.getFragments()[1].getArguments().getInt(
                EXTRA_PROFILE, -1)).isEqualTo(ProfileSelectFragment.ProfileType.WORK);
        assertThat(mProfileSelectLocationFragment.getFragments()[2].getArguments().getInt(
                EXTRA_PROFILE, -1)).isEqualTo(ProfileSelectFragment.ProfileType.PRIVATE);
    }
}
