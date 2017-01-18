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

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto;
import com.android.settings.InstrumentedFragment;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.overlay.SupportFeatureProvider;
import com.android.settings.widget.RtlCompatibleViewPager;
import com.android.settings.widget.SlidingTabLayout;
import com.android.settingslib.drawer.SettingsDrawerActivity;

/**
 * Container for Dashboard fragments.
 */
public final class DashboardContainerFragment extends InstrumentedFragment {

    public static final String EXTRA_SELECT_SETTINGS_TAB = ":settings:select_settings_tab";

    private static final String ARG_SUPPORT_TAB = "SUPPORT";
    private static final String ARG_SUMMARY_TAB = "SUMMARY";
    private static final int INDEX_SUMMARY_FRAGMENT = 0;
    private static final int INDEX_SUPPORT_FRAGMENT = 1;

    private RtlCompatibleViewPager mViewPager;
    private View mHeaderView;
    private DashboardViewPagerAdapter mPagerAdapter;

    @Override
    protected int getMetricsCategory() {
        return MetricsProto.MetricsEvent.DASHBOARD_CONTAINER;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        final View content = inflater.inflate(R.layout.dashboard_container, parent, false);
        mViewPager = (RtlCompatibleViewPager) content.findViewById(R.id.pager);
        mPagerAdapter = new DashboardViewPagerAdapter(getContext(),
                getChildFragmentManager(), mViewPager);
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.addOnPageChangeListener(
                new TabChangeListener((SettingsActivity) getActivity()));

        // check if support tab needs to be selected
        final String selectedTab = getArguments().
            getString(EXTRA_SELECT_SETTINGS_TAB, ARG_SUMMARY_TAB);
        if (TextUtils.equals(selectedTab, ARG_SUPPORT_TAB)) {
            mViewPager.setCurrentItem(INDEX_SUPPORT_FRAGMENT);
        } else {
            mViewPager.setCurrentItem(INDEX_SUMMARY_FRAGMENT);
        }

        mHeaderView = inflater.inflate(R.layout.dashboard_container_header, parent, false);
        ((SlidingTabLayout) mHeaderView).setViewPager(mViewPager);
        return content;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mPagerAdapter.getCount() > 1) {
            final Activity activity = getActivity();
            if (activity instanceof SettingsDrawerActivity) {
                ((SettingsDrawerActivity) getActivity()).setContentHeaderView(mHeaderView);
            }
        }
    }

    private static final class DashboardViewPagerAdapter extends FragmentPagerAdapter {

        private final Context mContext;
        private final SupportFeatureProvider mSupportFeatureProvider;
        private final RtlCompatibleViewPager mViewPager;

        public DashboardViewPagerAdapter(Context context, FragmentManager fragmentManager,
                RtlCompatibleViewPager viewPager) {
            super(fragmentManager);
            mContext = context;
            mSupportFeatureProvider =
                    FeatureFactory.getFactory(context).getSupportFeatureProvider(context);
            mViewPager = viewPager;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case INDEX_SUMMARY_FRAGMENT:
                    return mContext.getString(R.string.page_tab_title_summary);
                case INDEX_SUPPORT_FRAGMENT:
                    return mContext.getString(R.string.page_tab_title_support);
            }
            return super.getPageTitle(position);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case INDEX_SUMMARY_FRAGMENT:
                    return new DashboardSummary();
                case INDEX_SUPPORT_FRAGMENT:
                    return new SupportFragment();
                default:
                    throw new IllegalArgumentException(
                            String.format(
                                    "Position %d does not map to a valid dashboard fragment",
                                    position));
            }
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            return super.instantiateItem(container,
                    mViewPager.getRtlAwareIndex(position));
        }

        @Override
        public int getCount() {
            return mSupportFeatureProvider == null ? 1 : 2;
        }
    }

    private static final class TabChangeListener
            implements RtlCompatibleViewPager.OnPageChangeListener {

        private final SettingsActivity mActivity;

        public TabChangeListener(SettingsActivity activity) {
            mActivity = activity;
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            // Do nothing.
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            // Do nothing
        }

        @Override
        public void onPageSelected(int position) {
            switch (position) {
                case INDEX_SUMMARY_FRAGMENT:
                    MetricsLogger.action(
                            mActivity, MetricsProto.MetricsEvent.ACTION_SELECT_SUMMARY);
                    mActivity.setDisplaySearchMenu(true);
                    break;
                case INDEX_SUPPORT_FRAGMENT:
                    MetricsLogger.action(
                            mActivity, MetricsProto.MetricsEvent.ACTION_SELECT_SUPPORT_FRAGMENT);
                    mActivity.setDisplaySearchMenu(false);
                    break;
            }
        }
    }
}
