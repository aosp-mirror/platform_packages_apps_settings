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

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.RecyclerView;
import androidx.slice.Slice;
import androidx.slice.widget.SliceLiveData;
import androidx.slice.widget.SliceView;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for Slices in Settings Panels.
 */
public class PanelSlicesAdapter
        extends RecyclerView.Adapter<PanelSlicesAdapter.SliceRowViewHolder> {

    private final List<Uri> mSliceUris;
    private final PanelFragment mPanelFragment;

    public PanelSlicesAdapter(PanelFragment fragment, List<Uri> sliceUris) {
        mPanelFragment = fragment;
        mSliceUris = new ArrayList<>(sliceUris);
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
        sliceRowViewHolder.onBind(mPanelFragment, mSliceUris.get(position));
    }

    @Override
    public int getItemCount() {
        return mSliceUris.size();
    }

    @VisibleForTesting
    List<Uri> getData() {
        return mSliceUris;
    }

    /**
     * ViewHolder for binding Slices to SliceViews.
     */
    public static class SliceRowViewHolder extends RecyclerView.ViewHolder {

        @VisibleForTesting
        LiveData<Slice> sliceLiveData;

        @VisibleForTesting
        final SliceView sliceView;

        public SliceRowViewHolder(View view) {
            super(view);
            sliceView = view.findViewById(R.id.slice_view);
            sliceView.setMode(SliceView.MODE_LARGE);
        }

        public void onBind(PanelFragment fragment, Uri sliceUri) {
            final Context context = sliceView.getContext();
            sliceLiveData = SliceLiveData.fromUri(context, sliceUri);
            sliceLiveData.observe(fragment.getViewLifecycleOwner(), sliceView);
        }
    }
}
