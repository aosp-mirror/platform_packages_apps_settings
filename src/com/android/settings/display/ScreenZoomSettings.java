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

package com.android.settings.display;

import com.android.settings.InstrumentedFragment;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;

import android.annotation.Nullable;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Display;
import android.view.LayoutInflater;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Preference fragment used to control screen zoom.
 */
public class ScreenZoomSettings extends SettingsPreferenceFragment implements Indexable {
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

    private String[] mEntries;
    private int[] mValues;
    private int mNormalDensity;
    private int mInitialIndex;

    private int mCurrentIndex;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final DisplayDensityUtils density = new DisplayDensityUtils(getContext());

        final int initialIndex = density.getCurrentIndex();
        if (initialIndex < 0) {
            // Failed to obtain normal density, which means we failed to
            // connect to the window manager service. Just use the current
            // density and don't let the user change anything.
            final int densityDpi = getResources().getDisplayMetrics().densityDpi;
            mValues = new int[] { densityDpi };
            mEntries = new String[] { getString(R.string.screen_zoom_summary_normal) };
            mInitialIndex = 0;
            mNormalDensity = densityDpi;
        } else {
            mValues = density.getValues();
            mEntries = density.getEntries();
            mInitialIndex = initialIndex;
            mNormalDensity = density.getNormalDensity();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);
        final ViewGroup list_container = (ViewGroup) root.findViewById(android.R.id.list_container);
        list_container.removeAllViews();

        final View content = inflater.inflate(R.layout.screen_zoom_activity, list_container, false);
        list_container.addView(content);

        mLabel = (TextView) content.findViewById(R.id.current_density);

        // The maximum SeekBar value always needs to be non-zero. If there's
        // only one available zoom level, we'll handle this by disabling the
        // seek bar.
        final int max = Math.max(1, mValues.length - 1);

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

        if (mValues.length == 1) {
            // The larger and smaller buttons will be disabled when we call
            // setPreviewLayer() later in this method.
            seekBar.setEnabled(false);
        }

        mPreviewFrame = (FrameLayout) content.findViewById(R.id.preview_frame);

        // Populate the sample layouts.
        final Context context = getContext();
        final Configuration origConfig = context.getResources().getConfiguration();
        for (int mValue : mValues) {
            final Configuration config = new Configuration(origConfig);
            config.densityDpi = mValue;

            // Create a new configuration for the specified density. It won't
            // have any theme set, so manually apply the current theme.
            final Context configContext = context.createConfigurationContext(config);
            configContext.setTheme(context.getThemeResId());

            final LayoutInflater configInflater = LayoutInflater.from(configContext);
            final View sampleView = configInflater.inflate(
                    R.layout.screen_zoom_preview, mPreviewFrame, false);
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

        // This will adjust the density SLIGHTLY after the activity has
        // finished, which could be considered a feature or a bug...
        commit();
    }

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
     * Persists the selected density and sends a configuration change.
     */
    private void commit() {
        final int densityDpi = mValues[mCurrentIndex];
        if (densityDpi == mNormalDensity) {
            DisplayDensityUtils.clearForcedDisplayDensity(Display.DEFAULT_DISPLAY);
        } else {
            DisplayDensityUtils.setForcedDisplayDensity(Display.DEFAULT_DISPLAY, densityDpi);
        }
    }

    @Override
    protected int getMetricsCategory() {
        return InstrumentedFragment.DISPLAY_SCREEN_ZOOM;
    }

    /** Index provider used to expose this fragment in search. */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
                    final Resources res = context.getResources();
                    final SearchIndexableRaw data = new SearchIndexableRaw(context);
                    data.title = res.getString(R.string.screen_zoom_title);
                    data.screenTitle = res.getString(R.string.screen_zoom_title);
                    data.keywords = res.getString(R.string.screen_zoom_keywords);

                    final List<SearchIndexableRaw> result = new ArrayList<>(1);
                    result.add(data);
                    return result;
                }
            };
}
