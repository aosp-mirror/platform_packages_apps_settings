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

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.dashboard.conditional.Condition;
import com.android.settings.dashboard.conditional.ConditionAdapterUtils;
import com.android.settings.dashboard.suggestions.SuggestionDismissController;
import com.android.settings.dashboard.suggestions.SuggestionFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.Tile;

import java.util.ArrayList;
import java.util.List;

public class DashboardAdapter extends RecyclerView.Adapter<DashboardAdapter.DashboardItemHolder>
        implements SummaryLoader.SummaryConsumer, SuggestionDismissController.Callback {
    public static final String TAG = "DashboardAdapter";
    private static final String STATE_SUGGESTION_LIST = "suggestion_list";
    private static final String STATE_CATEGORY_LIST = "category_list";
    private static final String STATE_SUGGESTION_MODE = "suggestion_mode";
    private static final String STATE_SUGGESTIONS_SHOWN_LOGGED = "suggestions_shown_logged";

    private final IconCache mCache;
    private final Context mContext;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final DashboardFeatureProvider mDashboardFeatureProvider;
    private final SuggestionFeatureProvider mSuggestionFeatureProvider;
    private final ArrayList<String> mSuggestionsShownLogged;
    private boolean mFirstFrameDrawn;

    @VisibleForTesting
    DashboardData mDashboardData;

    private View.OnClickListener mTileClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //TODO: get rid of setTag/getTag
            mDashboardFeatureProvider.openTileIntent((Activity) mContext, (Tile) v.getTag());
        }
    };

    private View.OnClickListener mConditionClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            Condition expandedCondition = mDashboardData.getExpandedCondition();

            //TODO: get rid of setTag/getTag
            if (v.getTag() == expandedCondition) {
                mMetricsFeatureProvider.action(mContext,
                        MetricsEvent.ACTION_SETTINGS_CONDITION_CLICK,
                        expandedCondition.getMetricsConstant());
                expandedCondition.onPrimaryClick();
            } else {
                expandedCondition = (Condition) v.getTag();
                mMetricsFeatureProvider.action(mContext,
                        MetricsEvent.ACTION_SETTINGS_CONDITION_EXPAND,
                        expandedCondition.getMetricsConstant());

                updateExpandedCondition(expandedCondition);
            }
        }
    };

    public DashboardAdapter(Context context, Bundle savedInstanceState,
            List<Condition> conditions) {
        List<Tile> suggestions = null;
        List<DashboardCategory> categories = null;
        int suggestionMode = DashboardData.SUGGESTION_MODE_DEFAULT;

        mContext = context;
        final FeatureFactory factory = FeatureFactory.getFactory(context);
        mMetricsFeatureProvider = factory.getMetricsFeatureProvider();
        mDashboardFeatureProvider = factory.getDashboardFeatureProvider(context);
        mSuggestionFeatureProvider = factory.getSuggestionFeatureProvider(context);
        mCache = new IconCache(context);

        setHasStableIds(true);

        if (savedInstanceState != null) {
            suggestions = savedInstanceState.getParcelableArrayList(STATE_SUGGESTION_LIST);
            categories = savedInstanceState.getParcelableArrayList(STATE_CATEGORY_LIST);
            suggestionMode = savedInstanceState.getInt(
                    STATE_SUGGESTION_MODE, DashboardData.SUGGESTION_MODE_DEFAULT);
            mSuggestionsShownLogged = savedInstanceState.getStringArrayList(
                    STATE_SUGGESTIONS_SHOWN_LOGGED);
        } else {
            mSuggestionsShownLogged = new ArrayList<>();
        }

        mDashboardData = new DashboardData.Builder()
                .setConditions(conditions)
                .setSuggestions(suggestions)
                .setCategories(categories)
                .setSuggestionMode(suggestionMode)
                .build();
    }

    public List<Tile> getSuggestions() {
        return mDashboardData.getSuggestions();
    }

    public void setCategoriesAndSuggestions(List<DashboardCategory> categories,
            List<Tile> suggestions) {
        // TODO: Better place for tinting?
        final TypedArray a = mContext.obtainStyledAttributes(new int[]{
                android.R.attr.colorControlNormal});
        int tintColor = a.getColor(0, mContext.getColor(android.R.color.white));
        a.recycle();
        for (int i = 0; i < categories.size(); i++) {
            for (int j = 0; j < categories.get(i).tiles.size(); j++) {
                final Tile tile = categories.get(i).tiles.get(j);

                if (!mContext.getPackageName().equals(
                        tile.intent.getComponent().getPackageName())) {
                    // If this drawable is coming from outside Settings, tint it to match the
                    // color.
                    tile.icon.setTint(tintColor);
                }
            }
        }

        final DashboardData prevData = mDashboardData;
        mDashboardData = new DashboardData.Builder(prevData)
                .setSuggestions(suggestions)
                .setCategories(categories)
                .build();
        notifyDashboardDataChanged(prevData);
        List<Tile> shownSuggestions = null;
        switch (mDashboardData.getSuggestionMode()) {
            case DashboardData.SUGGESTION_MODE_DEFAULT:
                shownSuggestions = suggestions.subList(0,
                        Math.min(suggestions.size(), DashboardData.DEFAULT_SUGGESTION_COUNT));
                break;
            case DashboardData.SUGGESTION_MODE_EXPANDED:
                shownSuggestions = suggestions;
                break;
        }
        if (shownSuggestions != null) {
            for (Tile suggestion : shownSuggestions) {
                final String identifier = mSuggestionFeatureProvider.getSuggestionIdentifier(
                        mContext, suggestion);
                mMetricsFeatureProvider.action(
                        mContext, MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION, identifier);
                mSuggestionsShownLogged.add(identifier);
            }
        }
    }

    public void setCategory(List<DashboardCategory> category) {
        final DashboardData prevData = mDashboardData;
        Log.d(TAG, "adapter setCategory called");
        mDashboardData = new DashboardData.Builder(prevData)
                .setCategories(category)
                .build();
        notifyDashboardDataChanged(prevData);
    }

    public void setConditions(List<Condition> conditions) {
        final DashboardData prevData = mDashboardData;
        Log.d(TAG, "adapter setConditions called");
        mDashboardData = new DashboardData.Builder(prevData)
                .setConditions(conditions)
                .setExpandedCondition(null)
                .build();
        notifyDashboardDataChanged(prevData);
    }

    @Override
    public void notifySummaryChanged(Tile tile) {
        final int position = mDashboardData.getPositionByTile(tile);
        if (position != DashboardData.POSITION_NOT_FOUND) {
            // Since usually tile in parameter and tile in mCategories are same instance,
            // which is hard to be detected by DiffUtil, so we notifyItemChanged directly.
            notifyItemChanged(position, mDashboardData.getItemTypeByPosition(position));
        }
    }

    @Override
    public DashboardItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new DashboardItemHolder(LayoutInflater.from(parent.getContext()).inflate(
                viewType, parent, false));
    }

    @Override
    public void onBindViewHolder(DashboardItemHolder holder, int position) {
        final int type = mDashboardData.getItemTypeByPosition(position);
        switch (type) {
            case R.layout.dashboard_category:
                onBindCategory(holder,
                        (DashboardCategory) mDashboardData.getItemEntityByPosition(position));
                break;
            case R.layout.dashboard_tile:
                final Tile tile = (Tile) mDashboardData.getItemEntityByPosition(position);
                onBindTile(holder, tile);
                holder.itemView.setTag(tile);
                holder.itemView.setOnClickListener(mTileClickListener);
                break;
            case R.layout.suggestion_header:
                onBindSuggestionHeader(holder, (DashboardData.SuggestionHeaderData)
                        mDashboardData.getItemEntityByPosition(position));
                break;
            case R.layout.suggestion_tile:
                final Tile suggestion = (Tile) mDashboardData.getItemEntityByPosition(position);
                final String suggestionId = mSuggestionFeatureProvider.getSuggestionIdentifier(
                        mContext, suggestion);
                // This is for cases when a suggestion is dismissed and the next one comes to view
                if (!mSuggestionsShownLogged.contains(suggestionId)) {
                    mMetricsFeatureProvider.action(
                            mContext, MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION, suggestionId);
                    mSuggestionsShownLogged.add(suggestionId);
                }
                onBindTile(holder, suggestion);
                holder.itemView.setOnClickListener(v -> {
                    mMetricsFeatureProvider.action(mContext,
                            MetricsEvent.ACTION_SETTINGS_SUGGESTION, suggestionId);
                    ((SettingsActivity) mContext).startSuggestion(suggestion.intent);
                });
                break;
            case R.layout.condition_card:
                final boolean isExpanded = mDashboardData.getItemEntityByPosition(position)
                        == mDashboardData.getExpandedCondition();
                ConditionAdapterUtils.bindViews(
                        (Condition) mDashboardData.getItemEntityByPosition(position),
                        holder, isExpanded, mConditionClickListener, v -> onExpandClick(v));
                break;
        }
    }

    @Override
    public long getItemId(int position) {
        return mDashboardData.getItemIdByPosition(position);
    }

    @Override
    public int getItemViewType(int position) {
        return mDashboardData.getItemTypeByPosition(position);
    }

    @Override
    public int getItemCount() {
        return mDashboardData.size();
    }

    public void onPause() {
        if (mDashboardData.getSuggestions() == null) {
            return;
        }
        for (Tile suggestion : mDashboardData.getSuggestions()) {
            String suggestionId = mSuggestionFeatureProvider.getSuggestionIdentifier(
                    mContext, suggestion);
            if (mSuggestionsShownLogged.contains(suggestionId)) {
                mMetricsFeatureProvider.action(
                        mContext, MetricsEvent.ACTION_HIDE_SETTINGS_SUGGESTION, suggestionId);
            }
        }
        mSuggestionsShownLogged.clear();
    }

    public void onExpandClick(View v) {
        Condition expandedCondition = mDashboardData.getExpandedCondition();
        if (v.getTag() == expandedCondition) {
            mMetricsFeatureProvider.action(mContext,
                    MetricsEvent.ACTION_SETTINGS_CONDITION_COLLAPSE,
                    expandedCondition.getMetricsConstant());
            expandedCondition = null;
        } else {
            expandedCondition = (Condition) v.getTag();
            mMetricsFeatureProvider.action(mContext, MetricsEvent.ACTION_SETTINGS_CONDITION_EXPAND,
                    expandedCondition.getMetricsConstant());
        }

        updateExpandedCondition(expandedCondition);
    }

    public Object getItem(long itemId) {
        return mDashboardData.getItemEntityById(itemId);
    }

    private void notifyDashboardDataChanged(DashboardData prevData) {
        if (mFirstFrameDrawn && prevData != null) {
            final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DashboardData
                    .ItemsDataDiffCallback(prevData.getItemList(), mDashboardData.getItemList()));
            diffResult.dispatchUpdatesTo(this);
        } else {
            mFirstFrameDrawn = true;
            notifyDataSetChanged();
        }
    }

    private void updateExpandedCondition(Condition condition) {
        final DashboardData prevData = mDashboardData;
        mDashboardData = new DashboardData.Builder(prevData)
                .setExpandedCondition(condition)
                .build();
        notifyDashboardDataChanged(prevData);
    }

    @Override
    public Tile getSuggestionForPosition(int position) {
        return (Tile) mDashboardData.getItemEntityByPosition(position);
    }

    @Override
    public void onSuggestionDismissed(Tile suggestion) {
        final List<Tile> suggestions = mDashboardData.getSuggestions();
        if (suggestions == null) {
            return;
        }
        suggestions.remove(suggestion);

        final DashboardData prevData = mDashboardData;
        mDashboardData = new DashboardData.Builder(prevData)
                .setSuggestions(suggestions)
                .build();
        notifyDashboardDataChanged(prevData);
    }

    @VisibleForTesting
    void onBindSuggestionHeader(final DashboardItemHolder holder, DashboardData
            .SuggestionHeaderData data) {
        final boolean moreSuggestions = data.hasMoreSuggestions;
        final int undisplayedSuggestionCount = data.undisplayedSuggestionCount;

        holder.icon.setImageResource(moreSuggestions ? R.drawable.ic_expand_more
                : R.drawable.ic_expand_less);
        holder.title.setText(mContext.getString(R.string.suggestions_title, data.suggestionSize));
        String summaryContentDescription;
        if (moreSuggestions) {
            summaryContentDescription = mContext.getResources().getQuantityString(
                    R.plurals.settings_suggestion_header_summary_hidden_items,
                    undisplayedSuggestionCount, undisplayedSuggestionCount);
        } else {
            summaryContentDescription = mContext.getString(R.string.condition_expand_hide);
        }
        holder.summary.setContentDescription(summaryContentDescription);

        if (undisplayedSuggestionCount == 0) {
            holder.summary.setText(null);
        } else {
            holder.summary.setText(
                    mContext.getString(R.string.suggestions_summary, undisplayedSuggestionCount));
        }
        holder.itemView.setOnClickListener(v -> {
            final int suggestionMode;
            if (moreSuggestions) {
                suggestionMode = DashboardData.SUGGESTION_MODE_EXPANDED;

                for (Tile suggestion : mDashboardData.getSuggestions()) {
                    final String suggestionId = mSuggestionFeatureProvider.getSuggestionIdentifier(
                            mContext, suggestion);
                    if (!mSuggestionsShownLogged.contains(suggestionId)) {
                        mMetricsFeatureProvider.action(
                                mContext, MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                                suggestionId);
                        mSuggestionsShownLogged.add(suggestionId);
                    }
                }
            } else {
                suggestionMode = DashboardData.SUGGESTION_MODE_COLLAPSED;
            }

            DashboardData prevData = mDashboardData;
            mDashboardData = new DashboardData.Builder(prevData)
                    .setSuggestionMode(suggestionMode)
                    .build();
            notifyDashboardDataChanged(prevData);
        });
    }

    private void onBindTile(DashboardItemHolder holder, Tile tile) {
        holder.icon.setImageDrawable(mCache.getIcon(tile.icon));
        holder.title.setText(tile.title);
        if (!TextUtils.isEmpty(tile.summary)) {
            holder.summary.setText(tile.summary);
            holder.summary.setVisibility(View.VISIBLE);
        } else {
            holder.summary.setVisibility(View.GONE);
        }
    }

    private void onBindCategory(DashboardItemHolder holder, DashboardCategory category) {
        holder.title.setText(category.title);
    }

    void onSaveInstanceState(Bundle outState) {
        final List<Tile> suggestions = mDashboardData.getSuggestions();
        final List<DashboardCategory> categories = mDashboardData.getCategories();
        if (suggestions != null) {
            outState.putParcelableArrayList(STATE_SUGGESTION_LIST, new ArrayList<>(suggestions));
        }
        if (categories != null) {
            outState.putParcelableArrayList(STATE_CATEGORY_LIST, new ArrayList<>(categories));
        }
        outState.putInt(STATE_SUGGESTION_MODE, mDashboardData.getSuggestionMode());
        outState.putStringArrayList(STATE_SUGGESTIONS_SHOWN_LOGGED, mSuggestionsShownLogged);
    }

    private static class IconCache {
        private final Context mContext;
        private final ArrayMap<Icon, Drawable> mMap = new ArrayMap<>();

        public IconCache(Context context) {
            mContext = context;
        }

        public Drawable getIcon(Icon icon) {
            Drawable drawable = mMap.get(icon);
            if (drawable == null) {
                drawable = icon.loadDrawable(mContext);
                mMap.put(icon, drawable);
            }
            return drawable;
        }
    }

    public static class DashboardItemHolder extends RecyclerView.ViewHolder {
        public final ImageView icon;
        public final TextView title;
        public final TextView summary;

        public DashboardItemHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(android.R.id.icon);
            title = itemView.findViewById(android.R.id.title);
            summary = itemView.findViewById(android.R.id.summary);
        }
    }
}
