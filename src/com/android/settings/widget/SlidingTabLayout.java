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

package com.android.settings.widget;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.settings.R;

/**
 * To be used with ViewPager to provide a tab indicator component which give constant feedback as
 * to the user's scroll progress.
 */
public final class SlidingTabLayout extends FrameLayout implements View.OnClickListener {

    private final LinearLayout mTitleView;
    private final View mIndicatorView;
    private final LayoutInflater mLayoutInflater;

    private RtlCompatibleViewPager mViewPager;
    private int mSelectedPosition;
    private float mSelectionOffset;

    public SlidingTabLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mLayoutInflater = LayoutInflater.from(context);
        mTitleView = new LinearLayout(context);
        mTitleView.setGravity(Gravity.CENTER_HORIZONTAL);
        mIndicatorView = mLayoutInflater.inflate(R.layout.sliding_tab_indicator_view, this, false);

        addView(mTitleView, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        addView(mIndicatorView, mIndicatorView.getLayoutParams());
    }

    /**
     * Sets the associated view pager. Note that the assumption here is that the pager content
     * (number of tabs and tab titles) does not change after this call has been made.
     */
    public void setViewPager(RtlCompatibleViewPager viewPager) {
        mTitleView.removeAllViews();

        mViewPager = viewPager;
        if (viewPager != null) {
            viewPager.addOnPageChangeListener(new InternalViewPagerListener());
            populateTabStrip();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int titleCount = mTitleView.getChildCount();
        if (titleCount > 0) {
            final int width = MeasureSpec.makeMeasureSpec(
                    mTitleView.getMeasuredWidth() / titleCount, MeasureSpec.EXACTLY);
            final int height = MeasureSpec.makeMeasureSpec(
                    mIndicatorView.getMeasuredHeight(), MeasureSpec.EXACTLY);
            mIndicatorView.measure(width, height);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (mTitleView.getChildCount() > 0) {
            final int indicatorBottom = getMeasuredHeight();
            final int indicatorHeight = mIndicatorView.getMeasuredHeight();
            final int indicatorWidth = mIndicatorView.getMeasuredWidth();
            final int totalWidth = getMeasuredWidth();
            final int leftPadding = getPaddingLeft();
            final int rightPadding = getPaddingRight();

            mTitleView.layout(leftPadding, 0, mTitleView.getMeasuredWidth() + rightPadding,
                    mTitleView.getMeasuredHeight());
            // IndicatorView should start on the right when RTL mode is enabled
            if (isRtlMode()) {
                mIndicatorView.layout(totalWidth - indicatorWidth,
                        indicatorBottom - indicatorHeight, totalWidth,
                        indicatorBottom);
            } else {
                mIndicatorView.layout(0, indicatorBottom - indicatorHeight,
                        indicatorWidth, indicatorBottom);
            }
        }
    }

    @Override
    public void onClick(View v) {
        final int titleCount = mTitleView.getChildCount();
        for (int i = 0; i < titleCount; i++) {
            if (v == mTitleView.getChildAt(i)) {
                mViewPager.setCurrentItem(i);
                return;
            }
        }
    }

    private void onViewPagerPageChanged(int position, float positionOffset) {
        mSelectedPosition = position;
        mSelectionOffset = positionOffset;
        // Translation should be reversed in RTL mode
        final int leftIndicator = isRtlMode() ? -getIndicatorLeft() : getIndicatorLeft();
        mIndicatorView.setTranslationX(leftIndicator);
    }

    private void populateTabStrip() {
        final PagerAdapter adapter = mViewPager.getAdapter();

        for (int i = 0; i < adapter.getCount(); i++) {
            final TextView tabTitleView = (TextView) mLayoutInflater.inflate(
                    R.layout.sliding_tab_title_view, mTitleView, false);

            tabTitleView.setText(adapter.getPageTitle(i));
            tabTitleView.setOnClickListener(this);

            mTitleView.addView(tabTitleView);
            tabTitleView.setSelected(i == mViewPager.getCurrentItem());
        }
    }

    private int getIndicatorLeft() {
        View selectedTitle = mTitleView.getChildAt(mSelectedPosition);
        int left = selectedTitle.getLeft();
        if (mSelectionOffset > 0f && mSelectedPosition < (getChildCount() - 1)) {
            View nextTitle = mTitleView.getChildAt(mSelectedPosition + 1);
            left = (int) (mSelectionOffset * nextTitle.getLeft()
                    + (1.0f - mSelectionOffset) * left);
        }
        return left;
    }

    private boolean isRtlMode() {
        return getLayoutDirection() == LAYOUT_DIRECTION_RTL;
    }

    private final class InternalViewPagerListener implements
            RtlCompatibleViewPager.OnPageChangeListener {
        private int mScrollState;

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            final int titleCount = mTitleView.getChildCount();
            if ((titleCount == 0) || (position < 0) || (position >= titleCount)) {
                return;
            }
            onViewPagerPageChanged(position, positionOffset);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            mScrollState = state;
        }

        @Override
        public void onPageSelected(int position) {
            position = mViewPager.getRtlAwareIndex(position);
            if (mScrollState == RtlCompatibleViewPager.SCROLL_STATE_IDLE) {
                onViewPagerPageChanged(position, 0f);
            }
            final int titleCount = mTitleView.getChildCount();
            for (int i = 0; i < titleCount; i++) {
                mTitleView.getChildAt(i).setSelected(position == i);
            }
        }
    }
}
