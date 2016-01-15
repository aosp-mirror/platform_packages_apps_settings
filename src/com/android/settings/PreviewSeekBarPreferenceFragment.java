/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;


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

    /** Resource id of the layout for this preference fragment. */
    protected int mActivityLayoutResId;

    /** Resource id of the layout that defines the contents instide preview screen. */
    protected int[] mPreviewSampleResIds;

    private ViewPager mPreviewPager;
    private PreviewPagerAdapter mPreviewPagerAdapter;

    private TextView mLabel;
    private View mLarger;
    private View mSmaller;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);
        final ViewGroup listContainer = (ViewGroup) root.findViewById(android.R.id.list_container);
        listContainer.removeAllViews();

        final View content = inflater.inflate(mActivityLayoutResId, listContainer, false);
        listContainer.addView(content);

        mLabel = (TextView) content.findViewById(R.id.current_label);

        // The maximum SeekBar value always needs to be non-zero. If there's
        // only one available value, we'll handle this by disabling the
        // seek bar.
        final int max = Math.max(1, mEntries.length - 1);

        final SeekBar seekBar = (SeekBar) content.findViewById(R.id.seek_bar);
        seekBar.setMax(max);
        seekBar.setProgress(mInitialIndex);
        seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setPreviewLayer(progress, true);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        mSmaller = content.findViewById(R.id.smaller);
        mSmaller.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final int progress = seekBar.getProgress();
                if (progress > 0) {
                    seekBar.setProgress(progress - 1, true);
                }
            }
        });

        mLarger = content.findViewById(R.id.larger);
        mLarger.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final int progress = seekBar.getProgress();
                if (progress < seekBar.getMax()) {
                    seekBar.setProgress(progress + 1, true);
                }
            }
        });

        if (mEntries.length == 1) {
            // The larger and smaller buttons will be disabled when we call
            // setPreviewLayer() later in this method.
            seekBar.setEnabled(false);
        }

        final Context context = getContext();
        final Configuration origConfig = context.getResources().getConfiguration();
        Configuration[] configurations = new Configuration[mEntries.length];
        for (int i = 0; i < mEntries.length; ++i) {
            configurations[i] = createConfig(origConfig, i);
        }

        mPreviewPagerAdapter = new PreviewPagerAdapter(context, mPreviewSampleResIds,
                configurations);
        mPreviewPager = (ViewPager) content.findViewById(R.id.preview_pager);
        mPreviewPager.setAdapter(mPreviewPagerAdapter);

        setPreviewLayer(mInitialIndex, false);
        return root;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // This will commit the change SLIGHTLY after the activity has
        // finished, which could be considered a feature or a bug...
        commit();
    }

    /**
     * Creates new configuration based on the current position of the SeekBar.
     */
    protected abstract Configuration createConfig(Configuration origConfig, int index);

    private void setPreviewLayer(int index, boolean animate) {
        mLabel.setText(mEntries[index]);
        mSmaller.setEnabled(index > 0);
        mLarger.setEnabled(index < mEntries.length - 1);

        mPreviewPagerAdapter.setPreviewLayer(index, mCurrentIndex, mPreviewPager.getCurrentItem(),
                animate);
        mCurrentIndex = index;
    }

    /**
     * Persists the selected value and sends a configuration change.
     */
    protected abstract void commit();
}
