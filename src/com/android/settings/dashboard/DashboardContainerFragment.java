/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.dashboard;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.settings.InstrumentedFragment;
import com.android.settings.R;

/**
 * Container for Dashboard fragments.
 */
public final class DashboardContainerFragment extends InstrumentedFragment {

    private ViewPager mViewPager;
    private DashboardViewPagerAdapter mPagerAdapter;

    @Override
    protected int getMetricsCategory() {
        return DASHBOARD_CONTAINER;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        final View content = inflater.inflate(R.layout.dashboard_container, parent, false);
        mViewPager = (ViewPager) content.findViewById(R.id.pager);
        mPagerAdapter = new DashboardViewPagerAdapter(getChildFragmentManager());
        mViewPager.setAdapter(mPagerAdapter);
        return content;
    }

    private static final class DashboardViewPagerAdapter extends FragmentPagerAdapter {


        public DashboardViewPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new DashboardSummary();
                default:
                    throw new IllegalArgumentException(
                            String.format(
                                    "Position %d does not map to a valid dashboard fragment",
                                    position));
            }
        }

        @Override
        public int getCount() {
            return 1;
        }
    }
}
