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
package com.android.settings.dashboard;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.util.ArrayUtils;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.dashboard.conditional.Condition;
import com.android.settings.dashboard.conditional.ConditionAdapterUtils;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.DashboardTile;

import java.util.ArrayList;
import java.util.List;

public class DashboardAdapter extends RecyclerView.Adapter<DashboardAdapter.DashboardItemHolder> implements View.OnClickListener {
    public static final String TAG = "DashboardAdapter";

    private final List<Object> mItems = new ArrayList<>();
    private final List<Integer> mTypes = new ArrayList<>();
    private final List<Integer> mIds = new ArrayList<>();

    private final Context mContext;

    private List<DashboardCategory> mCategories;
    private List<Condition> mConditions;

    private boolean mIsShowingAll;
    // Used for counting items;
    private int mId;

    private Condition mExpandedCondition = null;

    public DashboardAdapter(Context context) {
        mContext = context;

        setHasStableIds(true);
    }

    public void setCategories(List<DashboardCategory> categories) {
        mCategories = categories;

        // TODO: Better place for tinting?
        TypedValue tintColor = new TypedValue();
        mContext.getTheme().resolveAttribute(com.android.internal.R.attr.colorAccent,
                tintColor, true);
        for (int i = 0; i < categories.size(); i++) {
            for (int j = 0; j < categories.get(i).tiles.size(); j++) {
                DashboardTile tile = categories.get(i).tiles.get(j);

                if (!mContext.getPackageName().equals(
                        tile.intent.getComponent().getPackageName())) {
                    // If this drawable is coming from outside Settings, tint it to match the
                    // color.
                    tile.icon.setTint(tintColor.data);
                }
            }
        }
        setShowingAll(mIsShowingAll);
    }

    public void setConditions(List<Condition> conditions) {
        mConditions = conditions;
        setShowingAll(mIsShowingAll);
    }

    public boolean isShowingAll() {
        return mIsShowingAll;
    }

    public void notifyChanged(DashboardTile tile) {
        for (int i = 0; i < mItems.size(); i++) {
            if (mItems.get(i) == tile) {
                notifyItemChanged(i);
                return;
            }
        }
    }

    public void setShowingAll(boolean showingAll) {
        mIsShowingAll = showingAll;
        reset();
        for (int i = 0; mConditions != null && i < mConditions.size(); i++) {
            countItem(mConditions.get(i), R.layout.condition_card, mConditions.get(i).shouldShow());
        }
        countItem(null, R.layout.dashboard_spacer, true);
        for (int i = 0; mCategories != null && i < mCategories.size(); i++) {
            DashboardCategory category = mCategories.get(i);
            countItem(category, R.layout.dashboard_category, mIsShowingAll);
            for (int j = 0; j < category.tiles.size(); j++) {
                DashboardTile tile = category.tiles.get(j);
                countItem(tile, R.layout.dashboard_tile, mIsShowingAll
                        || ArrayUtils.contains(DashboardSummary.INITIAL_ITEMS,
                        tile.intent.getComponent().getClassName()));
            }
        }
        countItem(null, R.layout.see_all, true);
        notifyDataSetChanged();
    }

    private void reset() {
        mItems.clear();
        mTypes.clear();
        mIds.clear();
        mId = 0;
    }

    private void countItem(Object object, int type, boolean add) {
        if (add) {
            mItems.add(object);
            mTypes.add(type);
            mIds.add(mId);
        }
        mId++;
    }

    @Override
    public DashboardItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new DashboardItemHolder(LayoutInflater.from(parent.getContext()).inflate(
                viewType, parent, false));
    }

    @Override
    public void onBindViewHolder(DashboardItemHolder holder, int position) {
        switch (mTypes.get(position)) {
            case R.layout.dashboard_category:
                onBindCategory(holder, (DashboardCategory) mItems.get(position));
                break;
            case R.layout.dashboard_tile:
                final DashboardTile tile = (DashboardTile) mItems.get(position);
                onBindTile(holder, tile);
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((SettingsActivity) mContext).openTile(tile);
                    }
                });
                break;
            case R.layout.see_all:
                onBindSeeAll(holder);
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setShowingAll(!mIsShowingAll);
                    }
                });
                break;
            case R.layout.condition_card:
                ConditionAdapterUtils.bindViews((Condition) mItems.get(position), holder,
                        mItems.get(position) == mExpandedCondition, this,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                onExpandClick(v);
                            }
                        });
                break;
        }
    }

    private void onBindTile(DashboardItemHolder holder, DashboardTile dashboardTile) {
        holder.icon.setImageIcon(dashboardTile.icon);
        holder.title.setText(dashboardTile.title);
        if (!TextUtils.isEmpty(dashboardTile.summary)) {
            holder.summary.setText(dashboardTile.summary);
            holder.summary.setVisibility(View.VISIBLE);
        } else {
            holder.summary.setVisibility(View.GONE);
        }
    }

    private void onBindCategory(DashboardItemHolder holder, DashboardCategory category) {
        holder.title.setText(category.title);
    }

    private void onBindSeeAll(DashboardItemHolder holder) {
        holder.title.setText(mIsShowingAll ? R.string.see_less
                : R.string.see_all);
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

    public void onExpandClick(View v) {
        if (v.getTag() == mExpandedCondition) {
            mExpandedCondition = null;
        } else {
            mExpandedCondition = (Condition) v.getTag();
        }
        notifyDataSetChanged();
    }

    public Object getItem(long itemId) {
        for (int i = 0; i < mIds.size(); i++) {
            if (mIds.get(i) == itemId) {
                return mItems.get(i);
            }
        }
        return null;
    }

    public static class DashboardItemHolder extends RecyclerView.ViewHolder {
        public final ImageView icon;
        public final TextView title;
        public final TextView summary;

        public DashboardItemHolder(View itemView) {
            super(itemView);
            icon = (ImageView) itemView.findViewById(android.R.id.icon);
            title = (TextView) itemView.findViewById(android.R.id.title);
            summary = (TextView) itemView.findViewById(android.R.id.summary);
        }
    }
}
