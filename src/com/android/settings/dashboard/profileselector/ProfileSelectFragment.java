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

import android.annotation.IntDef;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.android.settings.R;
import com.android.settings.core.InstrumentedFragment;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Base fragment class for per profile settings.
 */
public abstract class ProfileSelectFragment extends InstrumentedFragment {

    /**
     * Denotes the profile type.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PERSONAL, WORK, ALL})
    public @interface ProfileType {
    }

    /**
     * It is personal work profile.
     */
    public static final int PERSONAL = 1;

    /**
     * It is work profile
     */
    public static final int WORK = 1 << 1;

    /**
     * It is personal and work profile
     */
    public static final int ALL = PERSONAL | WORK;

    private View mContentView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.profile_select_tablayout, null /* root */);
        final ViewPager viewPager = mContentView.findViewById(R.id.view_pager);
        viewPager.setAdapter(new ViewPagerAdapter(this));
        return mContentView;
    }

    @Override
    public int getMetricsCategory() {
        return METRICS_CATEGORY_UNKNOWN;
    }

    /**
     * Returns an array of {@link Fragment} to display in the
     * {@link com.google.android.material.tabs.TabLayout}
     */
    public abstract Fragment[] getFragments();

    static class ViewPagerAdapter extends FragmentStatePagerAdapter {

        private final Fragment[] mChildFragments;
        private final Context mContext;

        ViewPagerAdapter(ProfileSelectFragment fragment) {
            super(fragment.getActivity().getSupportFragmentManager());
            mContext = fragment.getContext();
            mChildFragments = fragment.getFragments();
        }

        @Override
        public Fragment getItem(int position) {
            return mChildFragments[position];
        }

        @Override
        public int getCount() {
            return mChildFragments.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (position == 0) {
                return mContext.getString(R.string.category_personal);
            } else {
                return mContext.getString(R.string.category_work);
            }
        }
    }
}
