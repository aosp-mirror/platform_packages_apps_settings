/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings;

import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.security.IKeyChainService;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.android.settings.dashboard.DashboardFragment;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Main fragment to display trusted credentials settings.
 */
public class TrustedCredentialsSettings extends DashboardFragment {

    private static final String TAG = "TrustedCredentialsSettings";

    public static final String ARG_SHOW_NEW_FOR_USER = "ARG_SHOW_NEW_FOR_USER";

    static final ImmutableList<Tab> TABS = ImmutableList.of(Tab.SYSTEM, Tab.USER);

    private static final String USER_ACTION = "com.android.settings.TRUSTED_CREDENTIALS_USER";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.TRUSTED_CREDENTIALS;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setTitle(R.string.trusted_credentials);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.placeholder_preference_screen;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        View tabContainer = view.findViewById(R.id.tab_container);
        tabContainer.setVisibility(View.VISIBLE);

        ViewPager2 viewPager = tabContainer.findViewById(R.id.view_pager);
        viewPager.setAdapter(new FragmentAdapter(this));
        viewPager.setUserInputEnabled(false);

        Intent intent = getActivity().getIntent();
        if (intent != null && USER_ACTION.equals(intent.getAction())) {
            viewPager.setCurrentItem(TABS.indexOf(Tab.USER), false);
        }

        TabLayout tabLayout = tabContainer.findViewById(R.id.tabs);
        new TabLayoutMediator(tabLayout, viewPager, false, false,
                (tab, position) -> tab.setText(TABS.get(position).mLabel)).attach();
    }

    private static class FragmentAdapter extends FragmentStateAdapter {
        FragmentAdapter(Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            TrustedCredentialsFragment fragment = new TrustedCredentialsFragment();
            Bundle args = new Bundle();
            args.putInt(TrustedCredentialsFragment.ARG_POSITION, position);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getItemCount() {
            return TrustedCredentialsSettings.TABS.size();
        }
    }

    enum Tab {
        SYSTEM(R.string.trusted_credentials_system_tab, true),
        USER(R.string.trusted_credentials_user_tab, false);

        private final int mLabel;
        final boolean mSwitch;

        Tab(int label, boolean withSwitch) {
            mLabel = label;
            mSwitch = withSwitch;
        }

        List<String> getAliases(IKeyChainService service) throws RemoteException {
            switch (this) {
                case SYSTEM: {
                    return service.getSystemCaAliases().getList();
                }
                case USER:
                    return service.getUserCaAliases().getList();
            }
            throw new AssertionError();
        }

        boolean deleted(IKeyChainService service, String alias) throws RemoteException {
            switch (this) {
                case SYSTEM:
                    return !service.containsCaAlias(alias);
                case USER:
                    return false;
            }
            throw new AssertionError();
        }
    }
}
