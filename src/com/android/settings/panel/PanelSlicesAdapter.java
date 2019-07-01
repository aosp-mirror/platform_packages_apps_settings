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

import static com.android.settings.slices.CustomSliceRegistry.MEDIA_OUTPUT_INDICATOR_SLICE_URI;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

/**
 * RecyclerView adapter for Slices in Settings Panels.
 */
public class PanelSlicesAdapter
        extends RecyclerView.Adapter<PanelSlicesAdapter.SliceRowViewHolder> {

    /**
     * Maximum number of slices allowed on the panel view.
     */
    @VisibleForTesting
    static final int MAX_NUM_OF_SLICES = 5;

    private final List<LiveData<Slice>> mSliceLiveData;
    private final int mMetricsCategory;
    private final PanelFragment mPanelFragment;

    public PanelSlicesAdapter(
            PanelFragment fragment, List<LiveData<Slice>> sliceLiveData, int metricsCategory) {
        mPanelFragment = fragment;
        mSliceLiveData = new ArrayList<>(sliceLiveData);
        mMetricsCategory = metricsCategory;
    }

    @NonNull
    @Override
    public SliceRowViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        final Context context = viewGroup.getContext();
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View view = inflater.inflate(R.layout.panel_slice_row, viewGroup, false);

        return new SliceRowViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SliceRowViewHolder sliceRowViewHolder, int position) {
        sliceRowViewHolder.onBind(mSliceLiveData.get(position));
    }

    /**
     * Return the number of available items in the adapter with max number of slices enforced.
     */
    @Override
    public int getItemCount() {
        return Math.min(mSliceLiveData.size(), MAX_NUM_OF_SLICES);
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

        public SliceRowViewHolder(View view) {
            super(view);
            sliceView = view.findViewById(R.id.slice_view);
            sliceView.setMode(SliceView.MODE_LARGE);
            sliceView.showTitleItems(true);
        }

        public void onBind(LiveData<Slice> sliceLiveData) {
            sliceLiveData.observe(mPanelFragment.getViewLifecycleOwner(), sliceView);

            // Do not show the divider above media devices switcher slice per request
            final Slice slice = sliceLiveData.getValue();
            if (slice != null && slice.getUri().equals(MEDIA_OUTPUT_INDICATOR_SLICE_URI)) {
                mDividerAllowedAbove = false;
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
            return true;
        }
    }
}
