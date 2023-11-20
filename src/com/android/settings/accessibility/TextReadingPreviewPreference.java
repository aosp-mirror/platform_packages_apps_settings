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

package com.android.settings.accessibility;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import androidx.viewpager.widget.ViewPager;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import com.android.settings.R;
import com.android.settings.display.PreviewPagerAdapter;
import com.android.settings.widget.DotsPageIndicator;

/**
 * A {@link Preference} that could show the preview related to the text and reading options.
 */
public class TextReadingPreviewPreference extends Preference {
    private int mCurrentItem;
    private int mLastLayerIndex;
    private PreviewPagerAdapter mPreviewAdapter;

    private int mLayoutMinHorizontalPadding = 0;
    private int mBackgroundMinHorizontalPadding = 0;

    private final ViewPager.OnPageChangeListener mPageChangeListener =
            new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int i, float v, int i1) {
            // Do nothing
        }

        @Override
        public void onPageSelected(int i) {
            mCurrentItem = i;
        }

        @Override
        public void onPageScrollStateChanged(int i) {
            // Do nothing
        }
    };

    TextReadingPreviewPreference(Context context) {
        super(context);
        init();
    }

    public TextReadingPreviewPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    TextReadingPreviewPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    TextReadingPreviewPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        FrameLayout previewLayout = (FrameLayout) holder.itemView;
        LinearLayout backgroundView = previewLayout.findViewById(R.id.preview_background);
        adjustPaddings(previewLayout, backgroundView);

        final ViewPager viewPager = (ViewPager) holder.findViewById(R.id.preview_pager);
        viewPager.addOnPageChangeListener(mPageChangeListener);
        final DotsPageIndicator pageIndicator =
                (DotsPageIndicator) holder.findViewById(R.id.page_indicator);
        updateAdapterIfNeeded(viewPager, pageIndicator, mPreviewAdapter);
        updatePagerAndIndicator(viewPager, pageIndicator);
    }

    /**
     * Set the minimum preview layout horizontal inner padding.
     */
    void setLayoutMinHorizontalPadding(int layoutMinHorizontalPadding) {
        mLayoutMinHorizontalPadding = layoutMinHorizontalPadding;
    }

    /**
     * Set the minimum preview background view horizontal inner padding.
     */
    void setBackgroundMinHorizontalPadding(int backgroundMinHorizontalPadding) {
        mBackgroundMinHorizontalPadding = backgroundMinHorizontalPadding;
    }

    @VisibleForTesting
    void adjustPaddings(FrameLayout previewLayout, LinearLayout backgroundView) {
        previewLayout.setPadding(
                Math.max(previewLayout.getPaddingStart(), mLayoutMinHorizontalPadding),
                previewLayout.getPaddingTop(),
                Math.max(previewLayout.getPaddingEnd(), mLayoutMinHorizontalPadding),
                previewLayout.getPaddingBottom()
        );
        backgroundView.setPadding(
                Math.max(backgroundView.getPaddingStart(), mBackgroundMinHorizontalPadding),
                backgroundView.getPaddingTop(),
                Math.max(backgroundView.getPaddingEnd(), mBackgroundMinHorizontalPadding),
                backgroundView.getPaddingBottom()
        );
    }

    void setPreviewAdapter(PreviewPagerAdapter previewAdapter) {
        if (previewAdapter != mPreviewAdapter) {
            mPreviewAdapter = previewAdapter;
            notifyChanged();
        }
    }

    void setCurrentItem(int currentItem) {
        Preconditions.checkNotNull(mPreviewAdapter,
                "Preview adapter is null, you should init the preview adapter first");

        if (currentItem != mCurrentItem) {
            mCurrentItem = currentItem;
            notifyChanged();
        }
    }

    void setLastLayerIndex(int lastLayerIndex) {
        mLastLayerIndex = lastLayerIndex;
    }

    int getCurrentItem() {
        return mCurrentItem;
    }

    private void updateAdapterIfNeeded(ViewPager viewPager, DotsPageIndicator pageIndicator,
            PreviewPagerAdapter previewAdapter) {
        if (viewPager.getAdapter() == previewAdapter) {
            return;
        }

        viewPager.setAdapter(previewAdapter);

        if (previewAdapter != null) {
            pageIndicator.setViewPager(viewPager);
        } else {
            mCurrentItem = 0;
        }
    }

    private void updatePagerAndIndicator(ViewPager viewPager, DotsPageIndicator pageIndicator) {
        if (viewPager.getAdapter() == null) {
            return;
        }

        if (viewPager.getCurrentItem() != mCurrentItem) {
            viewPager.setCurrentItem(mCurrentItem);
        }

        pageIndicator.setVisibility(
                viewPager.getAdapter().getCount() > 1 ? View.VISIBLE : View.GONE);
    }

    private void init() {
        // set up the minimum horizontal paddings
        setLayoutMinHorizontalPadding(getContext().getResources().getDimensionPixelSize(
                R.dimen.text_reading_preview_layout_padding_horizontal_min));
        setBackgroundMinHorizontalPadding(getContext().getResources().getDimensionPixelSize(
                R.dimen.text_reading_preview_background_padding_horizontal_min));

        setLayoutResource(R.layout.accessibility_text_reading_preview);
    }

    void notifyPreviewPagerChanged(int pagerIndex) {
        Preconditions.checkNotNull(mPreviewAdapter,
                "Preview adapter is null, you should init the preview adapter first");

        if (pagerIndex != mLastLayerIndex) {
            mPreviewAdapter.setPreviewLayer(pagerIndex, mLastLayerIndex, getCurrentItem(),
                    /* animate= */ false);
        }

        mLastLayerIndex = pagerIndex;
    }
}
