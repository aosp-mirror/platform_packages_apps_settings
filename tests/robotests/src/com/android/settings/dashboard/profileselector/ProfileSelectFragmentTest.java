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

import static android.content.Intent.EXTRA_USER_ID;

import static com.android.settings.dashboard.profileselector.ProfileSelectFragment.PERSONAL_TAB;
import static com.android.settings.dashboard.profileselector.ProfileSelectFragment.WORK_TAB;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragmentTest;
import com.android.settings.testutils.shadow.ShadowUserManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.HashSet;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUserManager.class})
public class ProfileSelectFragmentTest {

    private Context mContext;
    private TestProfileSelectFragment mFragment;
    private FragmentActivity mActivity;
    private ShadowUserManager mUserManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mActivity = spy(Robolectric.buildActivity(SettingsActivity.class).get());
        mFragment = spy(new TestProfileSelectFragment());
        when(mFragment.getContext()).thenReturn(mContext);
        when(mFragment.getActivity()).thenReturn(mActivity);
        mUserManager = ShadowUserManager.getShadow();
    }

    @Test
    public void getTabId_no_setCorrectTab() {
        assertThat(mFragment.getTabId(mActivity, null)).isEqualTo(PERSONAL_TAB);
    }

    @Test
    public void getTabId_setArgumentWork_setCorrectTab() {
        final Bundle bundle = new Bundle();
        bundle.putInt(SettingsActivity.EXTRA_SHOW_FRAGMENT_TAB, WORK_TAB);

        assertThat(mFragment.getTabId(mActivity, bundle)).isEqualTo(WORK_TAB);
    }

    @Test
    public void getTabId_setArgumentPersonal_setCorrectTab() {
        final Bundle bundle = new Bundle();
        bundle.putInt(SettingsActivity.EXTRA_SHOW_FRAGMENT_TAB, PERSONAL_TAB);

        assertThat(mFragment.getTabId(mActivity, bundle)).isEqualTo(PERSONAL_TAB);
    }

    @Test
    public void getTabId_setWorkId_getCorrectTab() {
        final Bundle bundle = new Bundle();
        bundle.putInt(EXTRA_USER_ID, 10);
        final Set<Integer> profileIds = new HashSet<>();
        profileIds.add(10);
        mUserManager.setManagedProfiles(profileIds);

        assertThat(mFragment.getTabId(mActivity, bundle)).isEqualTo(WORK_TAB);
    }

    @Test
    public void getTabId_setPersonalId_getCorrectTab() {
        final Bundle bundle = new Bundle();
        bundle.putInt(EXTRA_USER_ID, 0);

        assertThat(mFragment.getTabId(mActivity, bundle)).isEqualTo(PERSONAL_TAB);
    }

    @Test
    public void getTabId_setPersonalIdByIntent_getCorrectTab() {
        final Set<Integer> profileIds = new HashSet<>();
        profileIds.add(10);
        mUserManager.setManagedProfiles(profileIds);
        final Intent intent = spy(new Intent());
        when(intent.getContentUserHint()).thenReturn(10);
        when(mActivity.getIntent()).thenReturn(intent);

        assertThat(mFragment.getTabId(mActivity, null)).isEqualTo(WORK_TAB);
    }

    public static class TestProfileSelectFragment extends ProfileSelectFragment {

        @Override
        public Fragment[] getFragments() {
            return new Fragment[]{
                    new SettingsPreferenceFragmentTest.TestFragment(), //0
                    new SettingsPreferenceFragmentTest.TestFragment()
            };
        }
    }
}
