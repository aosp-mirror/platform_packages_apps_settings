/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.display;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.DotsPageIndicator;
import com.android.settings.widget.LabeledSeekBar;

/**
 * Preference fragment shows a preview and a seek bar to adjust a specific settings.
 */
public abstract class PreviewSeekBarPreferenceFragment extends SettingsPreferenceFragment {

    /** List of entries corresponding the settings being set. */
    protected String[] mEntries;

    /** Index of the entry corresponding to initial value of the settings. */
    protected int mInitialIndex;

    /** Index of the entry corresponding to current value of the settings. */
    protected int mCurrentIndex;

    private ViewPager mPreviewPager;
    private PreviewPagerAdapter mPreviewPagerAdapter;
    private DotsPageIndicator mPageIndicator;

    private TextView mLabel;
    private LabeledSeekBar mSeekBar;
    private View mLarger;
    private View mSmaller;

    private class onPreviewSeekBarChangeListener implements OnSeekBarChangeListener {
        private boolean mSeekByTouch;

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            setPreviewLayer(progress, false);
            if (!mSeekByTouch) {
                commit();
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            mSeekByTouch = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (mPreviewPagerAdapter.isAnimating()) {
                mPreviewPagerAdapter.setAnimationEndAction(() -> commit());
            } else {
                commit();
            }
            mSeekByTouch = false;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);
        final ViewGroup listContainer = root.findViewById(android.R.id.list_container);
        listContainer.removeAllViews();

        final View content = inflater.inflate(getActivityLayoutResId(), listContainer, false);
        listContainer.addView(content);

        mLabel = content.findViewById(R.id.current_label);

        // The maximum SeekBar value always needs to be non-zero. If there's
        // only one available value, we'll handle this by disabling the
        // seek bar.
        final int max = Math.max(1, mEntries.length - 1);

        mSeekBar = content.findViewById(R.id.seek_bar);
        mSeekBar.setLabels(mEntries);
        mSeekBar.setMax(max);

        mSmaller = content.findViewById(R.id.smaller);
        mSmaller.setOnClickListener(v -> {
            final int progress = mSeekBar.getProgress();
            if (progress > 0) {
                mSeekBar.setProgress(progress - 1, true);
            }
        });

        mLarger = content.findViewById(R.id.larger);
        mLarger.setOnClickListener(v -> {
            final int progress = mSeekBar.getProgress();
            if (progress < mSeekBar.getMax()) {
                mSeekBar.setProgress(progress + 1, true);
            }
        });

        if (mEntries.length == 1) {
            // The larger and smaller buttons will be disabled when we call
            // setPreviewLayer() later in this method.
            mSeekBar.setEnabled(false);
        }

        final Context context = getContext();
        final Configuration origConfig = context.getResources().getConfiguration();
        final boolean isLayoutRtl = origConfig.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        Configuration[] configurations = new Configuration[mEntries.length];
        for (int i = 0; i < mEntries.length; ++i) {
            configurations[i] = createConfig(origConfig, i);
        }

        final int[] previews = getPreviewSampleResIds();
        mPreviewPager = content.findViewById(R.id.preview_pager);
        mPreviewPagerAdapter = new PreviewPagerAdapter(context, isLayoutRtl,
                previews, configurations);
        mPreviewPager.setAdapter(mPreviewPagerAdapter);
        mPreviewPager.setCurrentItem(isLayoutRtl ? previews.length - 1 : 0);
        mPreviewPager.addOnPageChangeListener(mPreviewPageChangeListener);

        mPageIndicator = content.findViewById(R.id.page_indicator);
        if (previews.length > 1) {
            mPageIndicator.setViewPager(mPreviewPager);
            mPageIndicator.setVisibility(View.VISIBLE);
            mPageIndicator.setOnPageChangeListener(mPageIndicatorPageChangeListener);
        } else {
            mPageIndicator.setVisibility(View.GONE);
        }

        setPreviewLayer(mInitialIndex, false);
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Set SeekBar listener here to avoid onProgressChanged() is called
        // during onRestoreInstanceState().
        mSeekBar.setProgress(mCurrentIndex);
        mSeekBar.setOnSeekBarChangeListener(new onPreviewSeekBarChangeListener());
    }

    @Override
    public void onStop() {
        super.onStop();
        mSeekBar.setOnSeekBarChangeListener(null);
    }

    /** Resource id of the layout for this preference fragment. */
    protected abstract int getActivityLayoutResId();

    /** Resource id of the layout that defines the contents inside preview screen. */
    protected abstract int[] getPreviewSampleResIds();

    /**
     * Creates new configuration based on the current position of the SeekBar.
     */
    protected abstract Configuration createConfig(Configuration origConfig, int index);

    /**
     * Persists the selected value and sends a configuration change.
     */
    protected abstract void commit();

    private void setPreviewLayer(int index, boolean animate) {
        mLabel.setText(mEntries[index]);
        mSmaller.setEnabled(index > 0);
        mLarger.setEnabled(index < mEntries.length - 1);
        setPagerIndicatorContentDescription(mPreviewPager.getCurrentItem());
        mPreviewPagerAdapter.setPreviewLayer(index, mCurrentIndex,
                mPreviewPager.getCurrentItem(), animate);

        mCurrentIndex = index;
    }

    private void setPagerIndicatorContentDescription(int position) {
        mPageIndicator.setContentDescription(
                getString(R.string.preview_page_indicator_content_description,
                        position + 1, getPreviewSampleResIds().length));
    }

    private OnPageChangeListener mPreviewPageChangeListener = new OnPageChangeListener() {
        @Override
        public void onPageScrollStateChanged(int state) {
            // Do nothing.
        }

        @Override
        public void onPageScrolled(int position, float positionOffset,
                int positionOffsetPixels) {
            // Do nothing.
        }

        @Override
        public void onPageSelected(int position) {
            mPreviewPager.sendAccessibilityEvent(AccessibilityEvent.TYPE_ANNOUNCEMENT);
        }
    };

    private OnPageChangeListener mPageIndicatorPageChangeListener = new OnPageChangeListener() {
        @Override
        public void onPageScrollStateChanged(int state) {
            // Do nothing.
        }

        @Override
        public void onPageScrolled(int position, float positionOffset,
                int positionOffsetPixels) {
            // Do nothing.
        }

        @Override
        public void onPageSelected(int position) {
            setPagerIndicatorContentDescription(position);
        }
    };
}
