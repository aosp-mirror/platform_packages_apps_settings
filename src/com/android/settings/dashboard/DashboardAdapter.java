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

import android.annotation.AttrRes;
import android.annotation.ColorInt;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.R.id;
import com.android.settings.SettingsActivity;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.dashboard.DashboardData.SuggestionConditionHeaderData;
import com.android.settings.dashboard.conditional.Condition;
import com.android.settings.dashboard.conditional.ConditionAdapter;
import com.android.settings.dashboard.conditional.ConditionAdapterUtils;
import com.android.settings.dashboard.conditional.FocusRecyclerView;
import com.android.settings.dashboard.suggestions.SuggestionAdapter;
import com.android.settings.dashboard.suggestions.SuggestionDismissController;
import com.android.settings.dashboard.suggestions.SuggestionFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.Utils;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.Tile;

import com.android.settingslib.suggestions.SuggestionParser;
import java.util.ArrayList;
import java.util.List;

public class DashboardAdapter extends RecyclerView.Adapter<DashboardAdapter.DashboardItemHolder>
        implements SummaryLoader.SummaryConsumer {
    public static final String TAG = "DashboardAdapter";
    private static final String STATE_SUGGESTION_LIST = "suggestion_list";
    private static final String STATE_CATEGORY_LIST = "category_list";
    private static final String STATE_SUGGESTION_MODE = "suggestion_mode";
    private static final String STATE_SUGGESTIONS_SHOWN_LOGGED = "suggestions_shown_logged";
    private static final int DONT_SET_BACKGROUND_ATTR = -1;
    private static final String STATE_SUGGESTION_CONDITION_MODE = "suggestion_condition_mode";

    private final IconCache mCache;
    private final Context mContext;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final DashboardFeatureProvider mDashboardFeatureProvider;
    private final SuggestionFeatureProvider mSuggestionFeatureProvider;
    private final ArrayList<String> mSuggestionsShownLogged;
    private boolean mFirstFrameDrawn;
    private boolean mCombineSuggestionAndCondition;
    private RecyclerView mRecyclerView;
    private SuggestionParser mSuggestionParser;
    private SuggestionAdapter mSuggestionAdapter;
    private SuggestionDismissController mSuggestionDismissHandler;
    private SuggestionDismissController.Callback mCallback;

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
            if (mCombineSuggestionAndCondition) {
                Condition condition = (Condition) v.getTag();
                //TODO: get rid of setTag/getTag
                mMetricsFeatureProvider.action(mContext,
                    MetricsEvent.ACTION_SETTINGS_CONDITION_CLICK,
                    condition.getMetricsConstant());
                condition.onPrimaryClick();
            } else {
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
        }
    };

    @Deprecated
    public DashboardAdapter(Context context, Bundle savedInstanceState,
        List<Condition> conditions) {
        this(context, savedInstanceState, conditions, null, null);
    }

    public DashboardAdapter(Context context, Bundle savedInstanceState,
            List<Condition> conditions, SuggestionParser suggestionParser,
            SuggestionDismissController.Callback callback) {
        List<Tile> suggestions = null;
        List<DashboardCategory> categories = null;
        int suggestionMode = DashboardData.SUGGESTION_MODE_DEFAULT;
        int suggestionConditionMode = DashboardData.HEADER_MODE_DEFAULT;

        mContext = context;
        final FeatureFactory factory = FeatureFactory.getFactory(context);
        mMetricsFeatureProvider = factory.getMetricsFeatureProvider();
        mDashboardFeatureProvider = factory.getDashboardFeatureProvider(context);
        mSuggestionFeatureProvider = factory.getSuggestionFeatureProvider(context);
        mCombineSuggestionAndCondition = mDashboardFeatureProvider.combineSuggestionAndCondition();
        mCache = new IconCache(context);
        mSuggestionParser = suggestionParser;
        mCallback = callback;

        setHasStableIds(true);

        if (savedInstanceState != null) {
            suggestions = savedInstanceState.getParcelableArrayList(STATE_SUGGESTION_LIST);
            categories = savedInstanceState.getParcelableArrayList(STATE_CATEGORY_LIST);
            suggestionConditionMode = savedInstanceState.getInt(
                STATE_SUGGESTION_CONDITION_MODE, suggestionConditionMode);
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
                .setCombineSuggestionAndCondition(mCombineSuggestionAndCondition)
                .setSuggestionConditionMode(suggestionConditionMode)
                .build();
    }

    public List<Tile> getSuggestions() {
        return mDashboardData.getSuggestions();
    }

    public void setCategoriesAndSuggestions(List<DashboardCategory> categories,
            List<Tile> suggestions) {
        if (mDashboardFeatureProvider.shouldTintIcon()) {
            // TODO: Better place for tinting?
            final TypedArray a = mContext.obtainStyledAttributes(new int[]{
                    android.R.attr.colorControlNormal});
            final int tintColor = a.getColor(0, mContext.getColor(R.color.fallback_tintColor));
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
        }

        final DashboardData prevData = mDashboardData;
        mDashboardData = new DashboardData.Builder(prevData)
                .setSuggestions(suggestions)
                .setCategories(categories)
                .build();
        notifyDashboardDataChanged(prevData);
        List<Tile> shownSuggestions = null;
        if (mCombineSuggestionAndCondition) {
            final int mode = mDashboardData.getSuggestionConditionMode();
            if (mode == DashboardData.HEADER_MODE_DEFAULT) {
                shownSuggestions = suggestions.subList(0,
                    Math.min(suggestions.size(), DashboardData.DEFAULT_SUGGESTION_COUNT));
            } else if (mode != DashboardData.HEADER_MODE_COLLAPSED) {
                shownSuggestions = suggestions;
            }
        } else {
            switch (mDashboardData.getSuggestionMode()) {
                case DashboardData.SUGGESTION_MODE_DEFAULT:
                    shownSuggestions = suggestions.subList(0,
                        Math.min(suggestions.size(), DashboardData.DEFAULT_SUGGESTION_COUNT));
                    break;
                case DashboardData.SUGGESTION_MODE_EXPANDED:
                    shownSuggestions = suggestions;
                    break;
            }
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
        if (mCombineSuggestionAndCondition) {
            mDashboardData = new DashboardData.Builder(prevData)
                .setConditions(conditions)
                .build();
        } else {
            mDashboardData = new DashboardData.Builder(prevData)
                .setConditions(conditions)
                .setExpandedCondition(null)
                .build();
        }
        notifyDashboardDataChanged(prevData);
    }

    public void onSuggestionDismissed() {
        final List<Tile> suggestions = mDashboardData.getSuggestions();
        if (suggestions != null && suggestions.size() == 1) {
            // The only suggestion is dismissed, and the the empty suggestion container will
            // remain as the dashboard item. Need to refresh the dashboard list.
            final DashboardData prevData = mDashboardData;
            mDashboardData = new DashboardData.Builder(prevData)
                    .setSuggestions(null)
                    .build();
            notifyDashboardDataChanged(prevData);
        }
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
        final View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        if (viewType == R.layout.suggestion_condition_header) {
            return new SuggestionAndConditionHeaderHolder(view);
        }
        if (viewType == R.layout.suggestion_condition_container) {
            return new SuggestionAndConditionContainerHolder(view);
        }
        return new DashboardItemHolder(view);
    }

    @Override
    public void onBindViewHolder(DashboardItemHolder holder, int position) {
        final int type = mDashboardData.getItemTypeByPosition(position);
        switch (type) {
            case R.layout.dashboard_header_spacer:
                onBindHeaderSpacer(holder, position);
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
            case R.layout.suggestion_tile_card:
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
                View clickHandler = holder.itemView;
                // If a view with @android:id/primary is defined, use that as the click handler
                // instead.
                final View primaryAction = holder.itemView.findViewById(android.R.id.primary);
                if (primaryAction != null) {
                    clickHandler = primaryAction;
                    // set the item view to disabled to remove any touch effects
                    holder.itemView.setEnabled(false);
                }
                clickHandler.setOnClickListener(v -> {
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
            case R.layout.suggestion_condition_container:
                onBindConditionAndSuggestion(
                    (SuggestionAndConditionContainerHolder) holder, position);
                break;
            case R.layout.suggestion_condition_header:
                /* There are 2 different headers for the suggestions/conditions section. To minimize
                   visual animation when expanding and collapsing the suggestions/conditions, we are
                   using the same layout to represent the 2 headers:
                   1. Suggestion header - when there is any suggestion shown, the suggestion header
                      will be the first item on the section. It only has the text "Suggestion", and
                      do nothing when clicked. This header will not be shown when the section is
                      collapsed, in which case, the SuggestionCondition header will be
                      shown instead to show the summary info.
                   2. SuggestionCondition header - the header that shows the summary info for the
                      suggestion/condition that is currently hidden. It has the expand button to
                      expand the section. */
                if (mDashboardData.getDisplayableSuggestionCount() > 0 && position == 1
                        && mDashboardData.getSuggestionConditionMode()
                            != DashboardData.HEADER_MODE_COLLAPSED) {
                    onBindSuggestionHeader((SuggestionAndConditionHeaderHolder) holder);
                } else {
                    onBindSuggestionConditionHeader((SuggestionAndConditionHeaderHolder) holder,
                        (SuggestionConditionHeaderData)
                            mDashboardData.getItemEntityByPosition(position));
                }
                break;
            case R.layout.suggestion_condition_footer:
                holder.itemView.setOnClickListener(v -> {
                    mMetricsFeatureProvider.action(mContext,
                            MetricsEvent.ACTION_SETTINGS_CONDITION_EXPAND, false);
                    DashboardData prevData = mDashboardData;
                    mDashboardData = new DashboardData.Builder(prevData).setSuggestionConditionMode(
                        DashboardData.HEADER_MODE_COLLAPSED).build();
                    notifyDashboardDataChanged(prevData);
                    mRecyclerView.scrollToPosition(1);
                });
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

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        // save the view so that we can scroll it when expanding/collapsing the suggestion and
        // conditions.
        mRecyclerView = recyclerView;
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

    // condition card is always expanded in new suggestion/condition UI.
    // TODO: Remove when completely move to new suggestion/condition UI
    @Deprecated
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

    public Tile getSuggestion(int position) {
        if (mCombineSuggestionAndCondition) {
            return mSuggestionAdapter.getSuggestion(position);
        }
        return (Tile) getItem(getItemId(position));
    }

    @VisibleForTesting
    void notifyDashboardDataChanged(DashboardData prevData) {
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

    private void onBindHeaderSpacer(DashboardItemHolder holder, int position) {
        if (mDashboardData.size() > (position + 1)) {
            // The spacer that goes underneath the search bar needs to match the
            // background of the first real view. That view is either a condition,
            // a suggestion, or the dashboard item.
            //
            // If it's a dashboard item, set null background so it uses the parent's
            // background like the other views. Otherwise, match the colors.
            int nextType = mDashboardData.getItemTypeByPosition(position + 1);
            int colorAttr = nextType == R.layout.suggestion_header
                    ? android.R.attr.colorSecondary
                    : nextType == R.layout.condition_card
                            ? android.R.attr.colorAccent
                            : DONT_SET_BACKGROUND_ATTR;

            if (colorAttr != DONT_SET_BACKGROUND_ATTR) {
                TypedArray array = holder.itemView.getContext()
                        .obtainStyledAttributes(new int[]{colorAttr});
                @ColorInt int color = array.getColor(0, 0);
                array.recycle();
                holder.itemView.setBackgroundColor(color);
            } else {
                holder.itemView.setBackground(null);
            }
        }
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
                logSuggestions();
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

    private void logSuggestions() {
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
    }

    private void onBindSuggestionHeader(final SuggestionAndConditionHeaderHolder holder) {
        holder.title.setText(R.string.suggestions_title);
        holder.title.setTextColor(Color.BLACK);
        holder.icon.setVisibility(View.INVISIBLE);
        holder.icons.removeAllViews();
        holder.icons.setVisibility(View.INVISIBLE);
        holder.summary.setVisibility(View.INVISIBLE);
        holder.expandIndicator.setVisibility(View.INVISIBLE);
        holder.itemView.setOnClickListener(null);
    }

    private void onBindSuggestionConditionHeader(final SuggestionAndConditionHeaderHolder holder,
            SuggestionConditionHeaderData data) {
        final int curMode = mDashboardData.getSuggestionConditionMode();
        final int nextMode = data.hiddenSuggestionCount > 0 && data.conditionCount > 0
            && curMode != DashboardData.HEADER_MODE_SUGGESTION_EXPANDED
            ? DashboardData.HEADER_MODE_SUGGESTION_EXPANDED
            : DashboardData.HEADER_MODE_FULLY_EXPANDED;
        final boolean moreSuggestions = data.hiddenSuggestionCount > 0;
        final boolean hasConditions = data.conditionCount > 0;
        if (data.conditionCount > 0) {
            holder.icon.setImageIcon(data.conditionIcons.get(0));
            holder.icon.setVisibility(View.VISIBLE);
            if (data.conditionCount == 1) {
                holder.title.setText(data.title);
                holder.title.setTextColor(Utils.getColorAccent(mContext));
                holder.icons.setVisibility(View.INVISIBLE);
            } else {
                holder.title.setText(null);
                updateConditionIcons(data.conditionIcons, holder.icons);
                holder.icons.setVisibility(View.VISIBLE);
            }
        } else {
            holder.icon.setVisibility(View.INVISIBLE);
            holder.icons.setVisibility(View.INVISIBLE);
        }

        if (data.hiddenSuggestionCount > 0) {
            holder.summary.setTextColor(Color.BLACK);
            if (curMode == DashboardData.HEADER_MODE_COLLAPSED) {
                if (data.conditionCount > 0) {
                    holder.summary.setText(mContext.getResources().getQuantityString(
                        R.plurals.suggestions_collapsed_summary,
                        data.hiddenSuggestionCount, data.hiddenSuggestionCount));
                } else {
                    holder.title.setText(mContext.getResources().getQuantityString(
                        R.plurals.suggestions_collapsed_title,
                        data.hiddenSuggestionCount, data.hiddenSuggestionCount));
                    holder.title.setTextColor(Color.BLACK);
                    holder.summary.setText(null);
                }
            } else if (curMode == DashboardData.HEADER_MODE_DEFAULT) {
                if (data.conditionCount > 0) {
                    holder.summary.setText(mContext.getString(
                        R.string.suggestions_summary, data.hiddenSuggestionCount));
                } else {
                    holder.title.setText(mContext.getString(
                        R.string.suggestions_more_title, data.hiddenSuggestionCount));
                    holder.title.setTextColor(Color.BLACK);
                    holder.summary.setText(null);
                }
            }
        } else if (data.conditionCount > 1) {
            holder.summary.setTextColor(Utils.getColorAccent(mContext));
            holder.summary.setText(
                mContext.getString(R.string.condition_summary, data.conditionCount));
        } else {
            holder.summary.setText(null);
        }
        holder.summary.setVisibility(View.VISIBLE);
        holder.expandIndicator.setVisibility(View.VISIBLE);

        holder.itemView.setOnClickListener(v -> {
            if (moreSuggestions ) {
                logSuggestions();
            } else if (hasConditions) {
                mMetricsFeatureProvider.action(mContext,
                    MetricsEvent.ACTION_SETTINGS_CONDITION_EXPAND, true);
            }
            DashboardData prevData = mDashboardData;
            final boolean wasCollapsed = curMode == DashboardData.HEADER_MODE_COLLAPSED;
            mDashboardData = new DashboardData.Builder(prevData)
                .setSuggestionConditionMode(nextMode).build();
            notifyDashboardDataChanged(prevData);
            if (wasCollapsed) {
                mRecyclerView.scrollToPosition(1);
            }
        });
    }

    private void onBindConditionAndSuggestion(final SuggestionAndConditionContainerHolder holder,
            int position) {
        RecyclerView.Adapter<DashboardItemHolder> adapter;
        // If there is suggestions to show, it will be at position 2 (position 0 = header spacer,
        // position 1 is suggestion header.
        if (position == 2 && mDashboardData.getSuggestions() != null) {
            mSuggestionAdapter = new SuggestionAdapter(mContext, (List<Tile>)
                mDashboardData.getItemEntityByPosition(position), mSuggestionsShownLogged);
            adapter = mSuggestionAdapter;
            mSuggestionDismissHandler = new SuggestionDismissController(mContext,
                holder.data, mSuggestionParser, mCallback);
        } else {
            ConditionAdapterUtils.addDismiss(holder.data);
            adapter = new ConditionAdapter(mContext,
                (List<Condition>) mDashboardData.getItemEntityByPosition(position),
                    mDashboardData.getSuggestionConditionMode());
        }
        holder.data.setLayoutManager(new LinearLayoutManager(mContext));
        holder.data.setAdapter(adapter);
    }

    private void onBindTile(DashboardItemHolder holder, Tile tile) {
        if (tile.remoteViews != null) {
            final ViewGroup itemView = (ViewGroup) holder.itemView;
            itemView.removeAllViews();
            itemView.addView(tile.remoteViews.apply(itemView.getContext(), itemView));
        } else {
            holder.icon.setImageDrawable(mCache.getIcon(tile.icon));
            holder.title.setText(tile.title);
            if (!TextUtils.isEmpty(tile.summary)) {
                holder.summary.setText(tile.summary);
                holder.summary.setVisibility(View.VISIBLE);
            } else {
                holder.summary.setVisibility(View.GONE);
            }
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
        outState.putInt(STATE_SUGGESTION_CONDITION_MODE,
            mDashboardData.getSuggestionConditionMode());
    }

    private void updateConditionIcons(List<Icon> icons, ViewGroup parent) {
        if (icons == null || icons.size() < 2) {
            parent.setVisibility(View.INVISIBLE);
            return;
        }
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        parent.removeAllViews();
        for (int i = 1, size = icons.size(); i < size; i++) {
            ImageView icon = (ImageView) inflater.inflate(
                R.layout.condition_header_icon, parent, false);
            icon.setImageIcon(icons.get(i));
            parent.addView(icon);
        }
        parent.setVisibility(View.VISIBLE);
    }

    public static class IconCache {
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

    public static class SuggestionAndConditionHeaderHolder extends DashboardItemHolder {
        public final LinearLayout icons;
        public final ImageView expandIndicator;

        public SuggestionAndConditionHeaderHolder(View itemView) {
            super(itemView);
            icons = itemView.findViewById(id.additional_icons);
            expandIndicator = itemView.findViewById(id.expand_indicator);
        }
    }

    public static class SuggestionAndConditionContainerHolder extends DashboardItemHolder {
        public final RecyclerView data;

        public SuggestionAndConditionContainerHolder(View itemView) {
            super(itemView);
            data = itemView.findViewById(id.data);
        }
    }

}
