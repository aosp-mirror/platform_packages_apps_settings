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

import android.annotation.Nullable;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
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
    protected int mPreviewSampleResId;

    /** Duration to use when cross-fading between previews. */
    private static final long CROSS_FADE_DURATION_MS = 400;

    /** Interpolator to use when cross-fading between previews. */
    private static final Interpolator FADE_IN_INTERPOLATOR = new DecelerateInterpolator();

    /** Interpolator to use when cross-fading between previews. */
    private static final Interpolator FADE_OUT_INTERPOLATOR = new AccelerateInterpolator();

    private ViewGroup mPreviewFrame;
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
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
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

        mPreviewFrame = (FrameLayout) content.findViewById(R.id.preview_frame);

        // Populate the sample layouts.
        final Context context = getContext();
        final Configuration origConfig = context.getResources().getConfiguration();
        for (int i = 0; i < mEntries.length; ++i) {
            final Configuration config = createConfig(origConfig, i);

            // Create a new configuration for the specified value. It won't
            // have any theme set, so manually apply the current theme.
            final Context configContext = context.createConfigurationContext(config);
            configContext.setTheme(context.getThemeResId());

            final LayoutInflater configInflater = LayoutInflater.from(configContext);
            final View sampleView = configInflater.inflate(mPreviewSampleResId, mPreviewFrame, false);
            sampleView.setAlpha(0);
            sampleView.setVisibility(View.INVISIBLE);

            mPreviewFrame.addView(sampleView);
        }

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

        if (mCurrentIndex >= 0) {
            final View lastLayer = mPreviewFrame.getChildAt(mCurrentIndex);
            if (animate) {
                lastLayer.animate()
                        .alpha(0)
                        .setInterpolator(FADE_OUT_INTERPOLATOR)
                        .setDuration(CROSS_FADE_DURATION_MS)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                lastLayer.setVisibility(View.INVISIBLE);
                            }
                        });
            } else {
                lastLayer.setAlpha(0);
                lastLayer.setVisibility(View.INVISIBLE);
            }
        }

        final View nextLayer = mPreviewFrame.getChildAt(index);
        if (animate) {
            nextLayer.animate()
                    .alpha(1)
                    .setInterpolator(FADE_IN_INTERPOLATOR)
                    .setDuration(CROSS_FADE_DURATION_MS)
                    .withStartAction(new Runnable() {
                        @Override
                        public void run() {
                            nextLayer.setVisibility(View.VISIBLE);
                        }
                    });
        } else {
            nextLayer.setVisibility(View.VISIBLE);
            nextLayer.setAlpha(1);
        }

        mSmaller.setEnabled(index > 0);
        mLarger.setEnabled(index < mEntries.length - 1);

        mCurrentIndex = index;
    }

    /**
     * Persists the selected value and sends a configuration change.
     */
    protected abstract void commit();
}
