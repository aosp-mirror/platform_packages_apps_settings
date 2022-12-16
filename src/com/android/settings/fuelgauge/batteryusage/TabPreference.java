/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.fuelgauge.batteryusage;

import android.content.Context;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/** A preference which contains a tab selection. */
public class TabPreference extends Preference {
    private static final String TAG = "TabPreference";

    private Fragment mRootFragment;
    private ViewPager2 mViewPager;
    private ViewPager2.OnPageChangeCallback mOnPageChangeCallback;

    @VisibleForTesting
    String[] mTabTitles;
    @VisibleForTesting
    int mSavedTabPosition;
    @VisibleForTesting
    TabLayout mTabLayout;

    public TabPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_tab);
    }

    void initializeTabs(Fragment rootFragment, String[] tabTitles) {
        mRootFragment = rootFragment;
        mTabTitles = tabTitles;
    }

    void setOnPageChangeCallback(ViewPager2.OnPageChangeCallback callback) {
        mOnPageChangeCallback = callback;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        if (mViewPager != null && mTabLayout != null) {
            return;
        }

        mViewPager = (ViewPager2) view.findViewById(R.id.view_pager);
        mViewPager.setAdapter(new FragmentAdapter(mRootFragment, mTabTitles.length));
        mViewPager.setUserInputEnabled(false);
        if (mOnPageChangeCallback != null) {
            mViewPager.registerOnPageChangeCallback(mOnPageChangeCallback);
        }

        mTabLayout = (TabLayout) view.findViewById(R.id.tabs);
        new TabLayoutMediator(
                mTabLayout, mViewPager, /* autoRefresh= */ true, /* smoothScroll= */ false,
                (tab, position) -> tab.setText(mTabTitles[position])).attach();
        mTabLayout.getTabAt(mSavedTabPosition).select();
    }

    @Override
    public void onDetached() {
        super.onDetached();
        if (mViewPager != null && mOnPageChangeCallback != null) {
            mViewPager.unregisterOnPageChangeCallback(mOnPageChangeCallback);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Log.d(TAG, "onSaveInstanceState() tabPosition=" + mTabLayout.getSelectedTabPosition());
        return new SavedState(super.onSaveInstanceState(), mTabLayout.getSelectedTabPosition());
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        mSavedTabPosition = savedState.getTabPosition();
        Log.d(TAG, "onRestoreInstanceState() tabPosition=" + savedState.getTabPosition());
    }

    @VisibleForTesting
    static class SavedState extends BaseSavedState {
        private int mTabPosition;

        SavedState(Parcelable superState, int tabPosition) {
            super(superState);
            mTabPosition = tabPosition;
        }

        int getTabPosition() {
            return mTabPosition;
        }
    }

    private static class FragmentAdapter extends FragmentStateAdapter {
        private final int mItemCount;
        private final Fragment[] mItemFragments;

        FragmentAdapter(@NonNull Fragment rootFragment, int itemCount) {
            super(rootFragment);
            mItemCount = itemCount;
            mItemFragments = new Fragment[mItemCount];
            for (int i = 0; i < mItemCount; i++) {
                // Empty tab pages.
                mItemFragments[i] = new Fragment();
            }
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return mItemFragments[position];
        }

        @Override
        public int getItemCount() {
            return mItemCount;
        }
    }
}
