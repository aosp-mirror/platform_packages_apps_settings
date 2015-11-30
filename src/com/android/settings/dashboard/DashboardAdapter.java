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
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.util.ArrayUtils;
import com.android.settings.SettingsActivity;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.DashboardTile;

import java.util.ArrayList;
import java.util.List;

public class DashboardAdapter extends RecyclerView.Adapter<DashboardAdapter.DashboardItemHolder> {
    public static final String TAG = "DashboardAdapter";

    private final List<Object> mItems = new ArrayList<>();
    private final List<Integer> mTypes = new ArrayList<>();
    private final List<Integer> mIds = new ArrayList<>();

    private final List<DashboardCategory> mCategories;
    private final Context mContext;

    private boolean mIsShowingAll;
    // Used for counting items;
    private int mId;

    public DashboardAdapter(Context context, List<DashboardCategory> categories) {
        mContext = context;
        mCategories = categories;

        // TODO: Better place for tinting?
        TypedValue tintColor = new TypedValue();
        context.getTheme().resolveAttribute(com.android.internal.R.attr.colorAccent,
                tintColor, true);
        for (int i = 0; i < categories.size(); i++) {
            for (int j = 0; j < categories.get(i).tiles.size(); j++) {
                DashboardTile tile = categories.get(i).tiles.get(j);

                if (!context.getPackageName().equals(
                        tile.intent.getComponent().getPackageName())) {
                    // If this drawable is coming from outside Settings, tint it to match the
                    // color.
                    tile.icon.setTint(tintColor.data);
                }
            }
        }

        setShowingAll(false);
        setHasStableIds(true);
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
        countItem(null, com.android.settings.R.layout.dashboard_spacer, true);
        for (int i = 0; i < mCategories.size(); i++) {
            DashboardCategory category = mCategories.get(i);
            countItem(category, com.android.settings.R.layout.dashboard_category, mIsShowingAll);
            for (int j = 0; j < category.tiles.size(); j++) {
                DashboardTile tile = category.tiles.get(j);
                Log.d(TAG, "Maybe adding " + tile.intent.getComponent().getClassName());
                countItem(tile, com.android.settings.R.layout.dashboard_tile, mIsShowingAll
                        || ArrayUtils.contains(DashboardSummary.INITIAL_ITEMS,
                        tile.intent.getComponent().getClassName()));
            }
        }
        countItem(null, com.android.settings.R.layout.see_all, true);
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
            case com.android.settings.R.layout.dashboard_category:
                onBindCategory(holder, (DashboardCategory) mItems.get(position));
                break;
            case com.android.settings.R.layout.dashboard_tile:
                final DashboardTile tile = (DashboardTile) mItems.get(position);
                onBindTile(holder, tile);
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((SettingsActivity) mContext).openTile(tile);
                    }
                });
                break;
            case com.android.settings.R.layout.see_all:
                onBindSeeAll(holder);
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setShowingAll(!mIsShowingAll);
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
        holder.title.setText(mIsShowingAll ? com.android.settings.R.string.see_less
                : com.android.settings.R.string.see_all);
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

    public static class DashboardItemHolder extends RecyclerView.ViewHolder {
        private final ImageView icon;
        private final TextView title;
        private final TextView summary;

        public DashboardItemHolder(View itemView) {
            super(itemView);
            icon = (ImageView) itemView.findViewById(android.R.id.icon);
            title = (TextView) itemView.findViewById(android.R.id.title);
            summary = (TextView) itemView.findViewById(android.R.id.summary);
        }
    }
}
