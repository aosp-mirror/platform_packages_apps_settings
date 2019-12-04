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

import static com.android.settings.dashboard.profileselector.ProfileSelectFragment.PERSONAL_TAB;
import static com.android.settings.dashboard.profileselector.ProfileSelectFragment.WORK_TAB;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragmentTest;

import com.google.android.material.tabs.TabLayout;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.androidx.fragment.FragmentController;

@RunWith(RobolectricTestRunner.class)
public class ProfileSelectFragmentTest {

    private Context mContext;
    private TestProfileSelectFragment mFragment;
    private FragmentActivity mActivity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mActivity = spy(Robolectric.buildActivity(SettingsActivity.class).get());
        mFragment = spy(new TestProfileSelectFragment());
        when(mFragment.getContext()).thenReturn(mContext);
        when(mFragment.getActivity()).thenReturn(mActivity);
    }

    @Test
    @Config(shadows = ShadowPreferenceFragmentCompat.class)
    public void onCreateView_no_setCorrectTab() {
        FragmentController.of(mFragment, new Intent()).create(0 /* containerViewId*/,
                null /* bundle */).start().resume().visible().get();
        final TabLayout tabs = mFragment.getView().findViewById(R.id.tabs);

        assertThat(tabs.getSelectedTabPosition()).isEqualTo(PERSONAL_TAB);
    }

    @Test
    @Config(shadows = ShadowPreferenceFragmentCompat.class)
    public void onCreateView_setArgument_setCorrectTab() {
        final Bundle bundle = new Bundle();
        bundle.putInt(SettingsActivity.EXTRA_SHOW_FRAGMENT_TAB, WORK_TAB);
        mFragment.setArguments(bundle);

        FragmentController.of(mFragment, new Intent()).create(0 /* containerViewId*/,
                bundle).start().resume().visible().get();
        final TabLayout tabs = mFragment.getView().findViewById(R.id.tabs);

        assertThat(tabs.getSelectedTabPosition()).isEqualTo(WORK_TAB);
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

    @Implements(PreferenceFragmentCompat.class)
    public static class ShadowPreferenceFragmentCompat {

        private Context mContext;

        @Implementation
        protected View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                @Nullable Bundle savedInstanceState) {
            mContext = inflater.getContext();
            return inflater.inflate(R.layout.preference_list_fragment, container);
        }

        @Implementation
        protected RecyclerView getListView() {
            final RecyclerView recyclerView = new RecyclerView(mContext);
            recyclerView.setAdapter(new RecyclerView.Adapter() {
                @NonNull
                @Override
                public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup,
                        int i) {
                    return null;
                }

                @Override
                public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {

                }

                @Override
                public int getItemCount() {
                    return 0;
                }
            });
            return recyclerView;
        }
    }
}
