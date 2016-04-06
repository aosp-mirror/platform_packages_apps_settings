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
    private static final int NS_CONDITION = 0;

    // Item types
    private static final int TYPE_CONDITION = R.layout.condition_card;

    // Multi namespace support.
    private final Context mContext;
    private final List<Object> mItems = new ArrayList<>();
    private final List<Integer> mTypes = new ArrayList<>();
    private final List<Integer> mIds = new ArrayList<>();
    private int mId;

    // Layout control
    private final SpanSizeLookup mSpanSizeLookup;

    private List<Condition> mConditions;
    private Condition mExpandedCondition = null;

    public DashboardStatusAdapter(Context context) {
        mContext = context;
        mSpanSizeLookup = new SpanSizeLookup();
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

    private void countItem(Object object, int type, boolean add, int nameSpace) {
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
        for (int i = 0; mConditions != null && i < mConditions.size(); i++) {
            boolean shouldShow = mConditions.get(i).shouldShow();
            countItem(mConditions.get(i), TYPE_CONDITION, shouldShow, NS_CONDITION);
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

    private final class SpanSizeLookup extends GridLayoutManager.SpanSizeLookup {
        @Override
        public int getSpanSize(int position) {
            final int viewType = getItemViewType(position);
            switch (viewType) {
                case TYPE_CONDITION:
                    return 2;
                default:
                    return 1;
            }
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageView icon;
        public final TextView title;
        public final TextView summary;

        public ViewHolder(View itemView) {
            super(itemView);
            icon = (ImageView) itemView.findViewById(android.R.id.icon);
            title = (TextView) itemView.findViewById(android.R.id.title);
            summary = (TextView) itemView.findViewById(android.R.id.summary);
        }
    }
}