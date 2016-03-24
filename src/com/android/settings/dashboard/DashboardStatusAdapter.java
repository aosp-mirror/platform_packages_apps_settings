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

package com.android.settings.dashboard;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.LayoutRes;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.dashboard.conditional.Condition;
import com.android.settings.dashboard.conditional.ConditionAdapterUtils;
import com.android.settings.dashboard.status.StatusCategory;

import java.util.ArrayList;
import java.util.List;

/**
 * Data adapter for dashboard status fragment.
 */
public final class DashboardStatusAdapter
        extends RecyclerView.Adapter<DashboardStatusAdapter.ViewHolder>
        implements View.OnClickListener {

    public static final int GRID_COLUMN_COUNT = 2;

    // Namespaces
    private static final int NS_SPACER = 0;
    private static final int NS_CONDITION = 1000;
    private static final int NS_STATUS = 2000;

    // Item types
    private static final int TYPE_SPACER = R.layout.dashboard_spacer;
    private static final int TYPE_CONDITION = R.layout.condition_card;
    private static final int TYPE_STATUS = R.layout.dashboard_status_card;

    // Multi namespace support.
    private final Context mContext;
    private final List<Object> mItems = new ArrayList<>();
    private final List<Integer> mTypes = new ArrayList<>();
    private final List<Integer> mIds = new ArrayList<>();
    private int mId;

    // Layout control
    private final SpanSizeLookup mSpanSizeLookup;
    private final ItemDecoration mItemDecoration;

    private List<Condition> mConditions;
    private Condition mExpandedCondition = null;
    private List<StatusCategory> mStatus;

    public DashboardStatusAdapter(Context context) {
        mContext = context;
        mSpanSizeLookup = new SpanSizeLookup();
        mItemDecoration = new ItemDecoration(context);
        setHasStableIds(true);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false));

    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case TYPE_CONDITION:
                ConditionAdapterUtils.bindViews((Condition) mItems.get(position), holder,
                        mItems.get(position) == mExpandedCondition, this,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                onExpandClick(v);
                            }
                        });
                break;
            case TYPE_STATUS:
                final StatusCategory status = (StatusCategory) mItems.get(position);
                status.bindToViewHolder(holder);
                break;
        }
    }

    @Override
    public long getItemId(int position) {
        return mIds.get(position);
    }

    @Override
    public int getItemViewType(int position) {
        return mTypes.get(position);
    }

    @Override
    public int getItemCount() {
        return mIds.size();
    }

    @Override
    public void onClick(View v) {
        if (v.getTag() == mExpandedCondition) {
            mExpandedCondition.onPrimaryClick();
        } else {
            mExpandedCondition = (Condition) v.getTag();
            notifyDataSetChanged();
        }
    }

    public SpanSizeLookup getSpanSizeLookup() {
        return mSpanSizeLookup;
    }

    public ItemDecoration getItemDecoration() {
        return mItemDecoration;
    }

    public void setConditions(List<Condition> conditions) {
        mConditions = conditions;
        recountItems();
    }

    public Object getItem(long itemId) {
        for (int i = 0; i < mIds.size(); i++) {
            if (mIds.get(i) == itemId) {
                return mItems.get(i);
            }
        }
        return null;
    }

    private void countItem(Object object, @LayoutRes int type, boolean add, int nameSpace) {
        if (add) {
            mItems.add(object);
            mTypes.add(type);
            // TODO: Counting namespaces for handling of suggestions/conds appearing/disappearing.
            mIds.add(mId + nameSpace);
        }
        mId++;
    }

    private void reset() {
        mItems.clear();
        mTypes.clear();
        mIds.clear();
        resetCount();
    }

    private void resetCount() {
        mId = 0;
    }

    private void recountItems() {
        reset();
        countItem(null, TYPE_SPACER, true /* add */, NS_SPACER);
        boolean hasCondition = false;
        for (int i = 0; mConditions != null && i < mConditions.size(); i++) {
            boolean shouldShow = mConditions.get(i).shouldShow();
            countItem(mConditions.get(i), TYPE_CONDITION, shouldShow, NS_CONDITION);
            hasCondition |= shouldShow;
        }
        countItem(null, TYPE_SPACER, hasCondition, NS_SPACER);
        for (int i = 0; mStatus != null && i < mStatus.size(); i++) {
            countItem(mStatus.get(i), TYPE_STATUS, true, NS_STATUS);
        }
        notifyDataSetChanged();
    }

    private void onExpandClick(View v) {
        if (v.getTag() == mExpandedCondition) {
            mExpandedCondition = null;
        } else {
            mExpandedCondition = (Condition) v.getTag();
        }
        notifyDataSetChanged();
    }

    /**
     * {@link GridLayoutManager.SpanSizeLookup} that assigns column span for different item types.
     */
    private final class SpanSizeLookup extends GridLayoutManager.SpanSizeLookup {
        @Override
        public int getSpanSize(int position) {
            final int viewType = getItemViewType(position);
            switch (viewType) {
                case TYPE_CONDITION:
                case TYPE_SPACER:
                    return 2;
                default:
                    return 1;
            }
        }
    }

    /**
     * {@link ItemDecoration} that adds padding around different types of views during layout.
     */
    private static final class ItemDecoration extends RecyclerView.ItemDecoration {

        private final int mItemSpacing;

        public ItemDecoration(Context context) {
            mItemSpacing = context.getResources()
                    .getDimensionPixelSize(R.dimen.dashboard_status_item_padding);
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                RecyclerView.State state) {
            GridLayoutManager.LayoutParams layoutParams
                    = (GridLayoutManager.LayoutParams) view.getLayoutParams();
            final int position = layoutParams.getViewLayoutPosition();
            if (position == RecyclerView.NO_POSITION) {
                super.getItemOffsets(outRect, view, parent, state);
                return;
            }
            final int viewType = parent.getChildViewHolder(view).getItemViewType();
            switch (viewType) {
                case TYPE_SPACER:
                    // No padding for spacer.
                    super.getItemOffsets(outRect, view, parent, state);
                    return;
                case TYPE_CONDITION:
                    // Adds padding horizontally.
                    outRect.left = mItemSpacing;
                    outRect.right = mItemSpacing;
                    return;
                default:
                    // Adds padding around status card.
                    final int spanIndex = layoutParams.getSpanIndex();
                    outRect.left = spanIndex == 0 ? mItemSpacing : mItemSpacing / 2;
                    outRect.right = spanIndex + layoutParams.getSpanSize() == GRID_COLUMN_COUNT
                            ? mItemSpacing : mItemSpacing / 2;
                    outRect.top = mItemSpacing;
                    outRect.bottom = 0;
                    break;
            }
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView title;
        public final ImageView icon;
        public final TextView summary;
        public final ImageView icon2;
        public final TextView summary2;

        public ViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(android.R.id.title);
            icon = (ImageView) itemView.findViewById(android.R.id.icon);
            summary = (TextView) itemView.findViewById(android.R.id.summary);
            icon2 = (ImageView) itemView.findViewById(android.R.id.icon2);
            summary2 = (TextView) itemView.findViewById(android.R.id.text2);
        }
    }
}