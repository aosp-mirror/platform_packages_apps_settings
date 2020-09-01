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

package com.android.settings.panel;

import static com.android.settings.slices.CustomSliceRegistry.MEDIA_OUTPUT_GROUP_SLICE_URI;
import static com.android.settings.slices.CustomSliceRegistry.MEDIA_OUTPUT_INDICATOR_SLICE_URI;
import static com.android.settings.slices.CustomSliceRegistry.MEDIA_OUTPUT_SLICE_URI;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.RecyclerView;
import androidx.slice.Slice;
import androidx.slice.widget.SliceView;

import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;

import com.google.android.setupdesign.DividerItemDecoration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RecyclerView adapter for Slices in Settings Panels.
 */
public class PanelSlicesAdapter
        extends RecyclerView.Adapter<PanelSlicesAdapter.SliceRowViewHolder> {

    /**
     * Maximum number of slices allowed on the panel view.
     */
    @VisibleForTesting
    static final int MAX_NUM_OF_SLICES = 6;

    private final List<LiveData<Slice>> mSliceLiveData;
    private final int mMetricsCategory;
    private final PanelFragment mPanelFragment;

    public PanelSlicesAdapter(
            PanelFragment fragment, Map<Uri, LiveData<Slice>> sliceLiveData, int metricsCategory) {
        mPanelFragment = fragment;
        mSliceLiveData = new ArrayList<>(sliceLiveData.values());
        mMetricsCategory = metricsCategory;
    }

    @NonNull
    @Override
    public SliceRowViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        final Context context = viewGroup.getContext();
        final LayoutInflater inflater = LayoutInflater.from(context);
        View view;
        if (viewType == PanelContent.VIEW_TYPE_SLIDER) {
            view = inflater.inflate(R.layout.panel_slice_slider_row, viewGroup, false);
        } else if (viewType == PanelContent.VIEW_TYPE_SLIDER_LARGE_ICON) {
            view = inflater.inflate(R.layout.panel_slice_slider_row_large_icon, viewGroup, false);
        } else {
            view = inflater.inflate(R.layout.panel_slice_row, viewGroup, false);
        }

        return new SliceRowViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SliceRowViewHolder sliceRowViewHolder, int position) {
        sliceRowViewHolder.onBind(mSliceLiveData.get(position), position);
    }

    /**
     * Return the number of available items in the adapter with max number of slices enforced.
     */
    @Override
    public int getItemCount() {
        return Math.min(mSliceLiveData.size(), MAX_NUM_OF_SLICES);
    }

    @Override
    public int getItemViewType(int position) {
        return mPanelFragment.getPanelViewType();
    }

    /**
     * Return the available data from the adapter. If the number of Slices over the max number
     * allowed, the list will only have the first MAX_NUM_OF_SLICES of slices.
     */
    @VisibleForTesting
    List<LiveData<Slice>> getData() {
        return mSliceLiveData.subList(0, getItemCount());
    }

    /**
     * ViewHolder for binding Slices to SliceViews.
     */
    public class SliceRowViewHolder extends RecyclerView.ViewHolder
            implements DividerItemDecoration.DividedViewHolder {

        private boolean mDividerAllowedAbove = true;

        @VisibleForTesting
        final SliceView sliceView;
        @VisibleForTesting
        final LinearLayout mSliceSliderLayout;

        public SliceRowViewHolder(View view) {
            super(view);
            sliceView = view.findViewById(R.id.slice_view);
            sliceView.setMode(SliceView.MODE_LARGE);
            sliceView.setShowTitleItems(true);
            mSliceSliderLayout = view.findViewById(R.id.slice_slider_layout);
        }

        /**
         * Called when the view is displayed.
         */
        public void onBind(LiveData<Slice> sliceLiveData, int position) {
            sliceLiveData.observe(mPanelFragment.getViewLifecycleOwner(), sliceView);

            // Do not show the divider above media devices switcher slice per request
            final Slice slice = sliceLiveData.getValue();
            if (slice == null || slice.getUri().equals(MEDIA_OUTPUT_INDICATOR_SLICE_URI)) {
                mDividerAllowedAbove = false;
            } else if (position == 0 && (slice.getUri().equals(MEDIA_OUTPUT_SLICE_URI)
                    || slice.getUri().equals(MEDIA_OUTPUT_GROUP_SLICE_URI))) {
                sliceView.setClickable(false);
                // Customize output switcher slice padding
                final int padding = mPanelFragment.getResources().getDimensionPixelSize(
                        R.dimen.output_switcher_slice_padding_top);
                mSliceSliderLayout.setPadding(mSliceSliderLayout.getPaddingLeft(), padding,
                        mSliceSliderLayout.getPaddingRight(),
                        padding);
            }

            // Log Panel interaction
            sliceView.setOnSliceActionListener(
                    ((eventInfo, sliceItem) -> {
                        FeatureFactory.getFactory(sliceView.getContext())
                                .getMetricsFeatureProvider()
                                .action(0 /* attribution */,
                                        SettingsEnums.ACTION_PANEL_INTERACTION,
                                        mMetricsCategory,
                                        sliceLiveData.getValue().getUri().getLastPathSegment()
                                        /* log key */,
                                        eventInfo.actionType /* value */);
                    })
            );
        }

        @Override
        public boolean isDividerAllowedAbove() {
            return mDividerAllowedAbove;
        }

        @Override
        public boolean isDividerAllowedBelow() {
            return mPanelFragment.getPanelViewType() != PanelContent.VIEW_TYPE_SLIDER_LARGE_ICON;
        }
    }
}
