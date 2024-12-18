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

import static android.app.slice.Slice.HINT_ERROR;
import static android.app.slice.SliceItem.FORMAT_SLICE;
import static android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.RecyclerView;
import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.widget.SliceView;

import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;

import com.google.android.setupdesign.DividerItemDecoration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RecyclerView adapter for Slices in Settings Panels.
 *
 * @deprecated this is no longer used after V and will be removed.
 */
@Deprecated(forRemoval = true)
public class PanelSlicesAdapter
        extends RecyclerView.Adapter<PanelSlicesAdapter.SliceRowViewHolder> {

    /**
     * Maximum number of slices allowed on the panel view.
     */
    @VisibleForTesting
    static final int MAX_NUM_OF_SLICES = 9;

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
        final View view;
        if (viewType == PanelContent.VIEW_TYPE_SLIDER) {
            view = inflater.inflate(R.layout.panel_slice_slider_row, viewGroup, false);
        } else {
            view = inflater.inflate(R.layout.panel_slice_row, viewGroup, false);
        }
        return new SliceRowViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SliceRowViewHolder sliceRowViewHolder, int position) {
        sliceRowViewHolder.onBind(mSliceLiveData.get(position).getValue());
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
     *
     * @deprecated this is no longer used after V and will be removed.
     */
    @Deprecated(forRemoval = true)
    public class SliceRowViewHolder extends RecyclerView.ViewHolder
            implements DividerItemDecoration.DividedViewHolder {

        private static final int ROW_VIEW_ID = androidx.slice.view.R.id.row_view;
        private static final int ROW_VIEW_TAG = R.id.tag_row_view;

        @VisibleForTesting
        final SliceView sliceView;
        @VisibleForTesting
        final LinearLayout mSliceSliderLayout;

        public SliceRowViewHolder(View view) {
            super(view);
            sliceView = view.findViewById(R.id.slice_view);
            sliceView.setMode(SliceView.MODE_LARGE);
            sliceView.setShowTitleItems(true);
            sliceView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            mSliceSliderLayout = view.findViewById(R.id.slice_slider_layout);
        }

        /**
         * Called when the view is displayed.
         */
        public void onBind(Slice slice) {
            // Hides slice which reports with error hint or not contain any slice sub-item.
            if (slice == null || !isValidSlice(slice)) {
                updateActionLabel();
                sliceView.setVisibility(View.GONE);
                return;
            } else {
                sliceView.setSlice(slice);
                sliceView.setVisibility(View.VISIBLE);
            }

            // Add divider for the end icon
            sliceView.setShowActionDividers(true);

            // Log Panel interaction
            sliceView.setOnSliceActionListener(
                    ((eventInfo, sliceItem) -> {
                        FeatureFactory.getFeatureFactory()
                                .getMetricsFeatureProvider()
                                .action(0 /* attribution */,
                                        SettingsEnums.ACTION_PANEL_INTERACTION,
                                        mMetricsCategory,
                                        slice.getUri().getLastPathSegment()
                                        /* log key */,
                                        eventInfo.actionType /* value */);
                    })
            );
            updateActionLabel();
        }

        /**
         * Either set the action label if the row view is inflated into Slice, or set a listener to
         * do so later when the row is available.
         */
        @VisibleForTesting void updateActionLabel() {
            if (sliceView == null) {
                return;
            }

            final LinearLayout llRow = sliceView.findViewById(ROW_VIEW_ID);
            if (llRow != null) {
                // Just set the label for the row. if is already laid out, there is no need for
                // listening to future changes.
                setActionLabel(llRow);
            } else { // set the accessibility delegate when row_view is laid out
                Object alreadyAddedListener = sliceView.getTag(ROW_VIEW_TAG);
                if (alreadyAddedListener != null) {
                    return;
                }
                sliceView.setTag(ROW_VIEW_TAG, new Object());
                sliceView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                    @Override
                    public void onLayoutChange(View v, int left, int top, int right, int bottom,
                            int oldLeft, int oldTop, int oldRight, int oldBottom) {
                        LinearLayout row = sliceView.findViewById(ROW_VIEW_ID);
                        if (row != null) {
                            setActionLabel(row);
                            sliceView.removeOnLayoutChangeListener(this);
                        }
                    }
                });
            }
        }

        /**
         * Update the action label for TalkBack to be more specific
         * @param view the RowView within the Slice
         */
        @VisibleForTesting void setActionLabel(View view) {
            view.setAccessibilityDelegate(new View.AccessibilityDelegate() {
                @Override
                public void onInitializeAccessibilityNodeInfo(View host,
                        AccessibilityNodeInfo info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);

                    AccessibilityNodeInfo.AccessibilityAction customClick =
                            new AccessibilityNodeInfo.AccessibilityAction(ACTION_CLICK, host
                                    .getResources()
                                    .getString(R.string.accessibility_action_label_panel_slice));
                    info.addAction(customClick);
                }
            });
        }

        private boolean isValidSlice(Slice slice) {
            if (slice.getHints().contains(HINT_ERROR)) {
                return false;
            }
            for (SliceItem item : slice.getItems()) {
                if (item.getFormat().equals(FORMAT_SLICE)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean isDividerAllowedAbove() {
            return false;
        }

        @Override
        public boolean isDividerAllowedBelow() {
            return false;
        }
    }
}
