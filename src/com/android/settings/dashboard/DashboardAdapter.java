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
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.util.ArrayUtils;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.dashboard.conditional.Condition;
import com.android.settings.dashboard.conditional.ConditionAdapterUtils;
import com.android.settingslib.SuggestionParser;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.Tile;

import java.util.ArrayList;
import java.util.List;

public class DashboardAdapter extends RecyclerView.Adapter<DashboardAdapter.DashboardItemHolder>
        implements View.OnClickListener {
    public static final String TAG = "DashboardAdapter";
    private static final String STATE_SUGGESTION_LIST = "suggestion_list";
    private static final String STATE_CATEGORY_LIST = "category_list";
    private static final String STATE_IS_SHOWING_ALL = "is_showing_all";
    private static final String STATE_SUGGESTION_MODE = "suggestion_mode";
    private static final int NS_SPACER = 0;
    private static final int NS_SUGGESTION = 1000;
    private static final int NS_ITEMS = 2000;
    private static final int NS_CONDITION = 3000;

    private static int SUGGESTION_MODE_DEFAULT = 0;
    private static int SUGGESTION_MODE_COLLAPSED = 1;
    private static int SUGGESTION_MODE_EXPANDED = 2;

    private static final int DEFAULT_SUGGESTION_COUNT = 2;

    private final List<Object> mItems = new ArrayList<>();
    private final List<Integer> mTypes = new ArrayList<>();
    private final List<Integer> mIds = new ArrayList<>();
    private final IconCache mCache;

    private final Context mContext;

    private List<DashboardCategory> mCategories;
    private List<Condition> mConditions;
    private List<Tile> mSuggestions;

    private boolean mIsShowingAll;
    // Used for counting items;
    private int mId;

    private int mSuggestionMode = SUGGESTION_MODE_DEFAULT;

    private Condition mExpandedCondition = null;
    private SuggestionParser mSuggestionParser;

    public DashboardAdapter(Context context, SuggestionParser parser, Bundle savedInstanceState,
                List<Condition> conditions) {
        mContext = context;
        mCache = new IconCache(context);
        mSuggestionParser = parser;
        mConditions = conditions;

        setHasStableIds(true);

        boolean showAll = true;
        if (savedInstanceState != null) {
            mSuggestions = savedInstanceState.getParcelableArrayList(STATE_SUGGESTION_LIST);
            mCategories = savedInstanceState.getParcelableArrayList(STATE_CATEGORY_LIST);
            showAll = savedInstanceState.getBoolean(STATE_IS_SHOWING_ALL, true);
            mSuggestionMode = savedInstanceState.getInt(
                    STATE_SUGGESTION_MODE, SUGGESTION_MODE_DEFAULT);
        }
        setShowingAll(showAll);
    }

    public List<Tile> getSuggestions() {
        return mSuggestions;
    }

    public void setCategoriesAndSuggestions(List<DashboardCategory> categories,
            List<Tile> suggestions) {
        mSuggestions = suggestions;
        mCategories = categories;

        // TODO: Better place for tinting?
        TypedValue tintColor = new TypedValue();
        mContext.getTheme().resolveAttribute(com.android.internal.R.attr.colorAccent,
                tintColor, true);
        for (int i = 0; i < categories.size(); i++) {
            for (int j = 0; j < categories.get(i).tiles.size(); j++) {
                Tile tile = categories.get(i).tiles.get(j);

                if (!mContext.getPackageName().equals(
                        tile.intent.getComponent().getPackageName())) {
                    // If this drawable is coming from outside Settings, tint it to match the
                    // color.
                    tile.icon.setTint(tintColor.data);
                }
            }
        }
        recountItems();
    }

    public void setConditions(List<Condition> conditions) {
        mConditions = conditions;
        recountItems();
    }

    public boolean isShowingAll() {
        return mIsShowingAll;
    }

    public void notifyChanged(Tile tile) {
        notifyDataSetChanged();
    }

    public void setShowingAll(boolean showingAll) {
        mIsShowingAll = showingAll;
        recountItems();
    }

    private void recountItems() {
        reset();
        boolean hasConditions = false;
        for (int i = 0; mConditions != null && i < mConditions.size(); i++) {
            boolean shouldShow = mConditions.get(i).shouldShow();
            hasConditions |= shouldShow;
            countItem(mConditions.get(i), R.layout.condition_card, shouldShow, NS_CONDITION);
        }
        boolean hasSuggestions = mSuggestions != null && mSuggestions.size() != 0;
        countItem(null, R.layout.dashboard_spacer, hasConditions && hasSuggestions, NS_SPACER);
        countItem(null, R.layout.suggestion_header, hasSuggestions, NS_SPACER);
        resetCount();
        if (mSuggestions != null) {
            int maxSuggestions = getDisplayableSuggestionCount();
            for (int i = 0; i < mSuggestions.size(); i++) {
                countItem(mSuggestions.get(i), R.layout.suggestion_tile, i < maxSuggestions,
                        NS_SUGGESTION);
            }
        }
        resetCount();
        for (int i = 0; mCategories != null && i < mCategories.size(); i++) {
            DashboardCategory category = mCategories.get(i);
            countItem(category, R.layout.dashboard_category, mIsShowingAll, NS_ITEMS);
            for (int j = 0; j < category.tiles.size(); j++) {
                Tile tile = category.tiles.get(j);
                countItem(tile, R.layout.dashboard_tile, mIsShowingAll
                        || ArrayUtils.contains(DashboardSummary.INITIAL_ITEMS,
                        tile.intent.getComponent().getClassName()), NS_ITEMS);
            }
        }
        notifyDataSetChanged();
    }

    private void resetCount() {
        mId = 0;
    }

    private void reset() {
        mItems.clear();
        mTypes.clear();
        mIds.clear();
        mId = 0;
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

    private int getDisplayableSuggestionCount() {
        final int suggestionSize = mSuggestions.size();
        return mSuggestionMode == SUGGESTION_MODE_DEFAULT
                ? Math.min(DEFAULT_SUGGESTION_COUNT, suggestionSize)
                : mSuggestionMode == SUGGESTION_MODE_EXPANDED
                        ? suggestionSize : 0;
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
                final Tile tile = (Tile) mItems.get(position);
                onBindTile(holder, tile);
                holder.itemView.setTag(tile);
                holder.itemView.setOnClickListener(this);
                break;
            case R.layout.suggestion_header:
                onBindSuggestionHeader(holder);
                break;
            case R.layout.suggestion_tile:
                final Tile suggestion = (Tile) mItems.get(position);
                onBindTile(holder, suggestion);
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        MetricsLogger.action(mContext, MetricsEvent.ACTION_SETTINGS_SUGGESTION,
                                DashboardAdapter.getSuggestionIdentifier(mContext, suggestion));
                        ((SettingsActivity) mContext).startSuggestion(suggestion.intent);
                    }
                });
                holder.itemView.findViewById(R.id.overflow).setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                showRemoveOption(v, suggestion);
                            }
                        });
                break;
            case R.layout.see_all:
                onBindSeeAll(holder);
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

    private void showRemoveOption(View v, final Tile suggestion) {
        PopupMenu popup = new PopupMenu(
                new ContextThemeWrapper(mContext, R.style.Theme_AppCompat_DayNight), v);
        popup.getMenu().add(R.string.suggestion_remove).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                MetricsLogger.action(mContext, MetricsEvent.ACTION_SETTINGS_DISMISS_SUGGESTION,
                        DashboardAdapter.getSuggestionIdentifier(mContext, suggestion));
                disableSuggestion(suggestion);
                mSuggestions.remove(suggestion);
                recountItems();
                return true;
            }
        });
        popup.show();
    }

    public void disableSuggestion(Tile suggestion) {
        if (mSuggestionParser == null) {
            return;
        }
        if (mSuggestionParser.dismissSuggestion(suggestion)) {
            mContext.getPackageManager().setComponentEnabledSetting(
                    suggestion.intent.getComponent(),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
            mSuggestionParser.markCategoryDone(suggestion.category);
        }
    }

    private void onBindSuggestionHeader(final DashboardItemHolder holder) {
        holder.icon.setImageResource(hasMoreSuggestions() ? R.drawable.ic_expand_more
                : R.drawable.ic_expand_less);
        holder.title.setText(mContext.getString(R.string.suggestions_title, mSuggestions.size()));
        final int undisplayedSuggestionCount =
                mSuggestions.size() - getDisplayableSuggestionCount();
        if (undisplayedSuggestionCount == 0) {
            holder.summary.setText(null);
        } else {
            holder.summary.setText(
                    mContext.getString(R.string.suggestions_summary, undisplayedSuggestionCount));
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hasMoreSuggestions()) {
                    mSuggestionMode = SUGGESTION_MODE_EXPANDED;
                } else {
                    mSuggestionMode = SUGGESTION_MODE_COLLAPSED;
                }
                recountItems();
            }
        });
    }

    private boolean hasMoreSuggestions() {
        return mSuggestionMode == SUGGESTION_MODE_COLLAPSED
                || (mSuggestionMode == SUGGESTION_MODE_DEFAULT
                && mSuggestions.size() > DEFAULT_SUGGESTION_COUNT);
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

    private void onBindSeeAll(DashboardItemHolder holder) {
        holder.title.setText(mIsShowingAll ? R.string.see_less
                : R.string.see_all);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setShowingAll(!mIsShowingAll);
            }
        });
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
        if (v.getId() == R.id.dashboard_tile) {
            ((SettingsActivity) mContext).openTile((Tile) v.getTag());
            return;
        }
        if (v.getTag() == mExpandedCondition) {
            MetricsLogger.action(mContext, MetricsEvent.ACTION_SETTINGS_CONDITION_CLICK,
                    mExpandedCondition.getMetricsConstant());
            mExpandedCondition.onPrimaryClick();
        } else {
            mExpandedCondition = (Condition) v.getTag();
            MetricsLogger.action(mContext, MetricsEvent.ACTION_SETTINGS_CONDITION_EXPAND,
                    mExpandedCondition.getMetricsConstant());
            notifyDataSetChanged();
        }
    }

    public void onExpandClick(View v) {
        if (v.getTag() == mExpandedCondition) {
            MetricsLogger.action(mContext, MetricsEvent.ACTION_SETTINGS_CONDITION_COLLAPSE,
                    mExpandedCondition.getMetricsConstant());
            mExpandedCondition = null;
        } else {
            mExpandedCondition = (Condition) v.getTag();
            MetricsLogger.action(mContext, MetricsEvent.ACTION_SETTINGS_CONDITION_EXPAND,
                    mExpandedCondition.getMetricsConstant());
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

    public static String getSuggestionIdentifier(Context context, Tile suggestion) {
        String packageName = suggestion.intent.getComponent().getPackageName();
        if (packageName.equals(context.getPackageName())) {
            // Since Settings provides several suggestions, fill in the class instead of the
            // package for these.
            packageName = suggestion.intent.getComponent().getClassName();
        }
        return packageName;
    }

    void onSaveInstanceState(Bundle outState) {
        if (mSuggestions != null) {
            outState.putParcelableArrayList(STATE_SUGGESTION_LIST,
                    new ArrayList<Tile>(mSuggestions));
        }
        if (mCategories != null) {
            outState.putParcelableArrayList(STATE_CATEGORY_LIST,
                    new ArrayList<DashboardCategory>(mCategories));
        }
        outState.putBoolean(STATE_IS_SHOWING_ALL, mIsShowingAll);
        outState.putInt(STATE_SUGGESTION_MODE, mSuggestionMode);
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
            icon = (ImageView) itemView.findViewById(android.R.id.icon);
            title = (TextView) itemView.findViewById(android.R.id.title);
            summary = (TextView) itemView.findViewById(android.R.id.summary);
        }
    }
}
