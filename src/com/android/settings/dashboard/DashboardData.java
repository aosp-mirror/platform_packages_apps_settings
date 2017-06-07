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

import android.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.settings.dashboard.conditional.Condition;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.Tile;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Description about data list used in the DashboardAdapter. In the data list each item can be
 * Condition, suggestion or category tile.
 * <p>
 * ItemsData has inner class Item, which represents the Item in data list.
 */
public class DashboardData {
    public static final int SUGGESTION_MODE_DEFAULT = 0;
    public static final int SUGGESTION_MODE_COLLAPSED = 1;
    public static final int SUGGESTION_MODE_EXPANDED = 2;
    public static final int POSITION_NOT_FOUND = -1;
    public static final int DEFAULT_SUGGESTION_COUNT = 2;

    // id namespace for different type of items.
    private static final int NS_SPACER = 0;
    private static final int NS_ITEMS = 2000;
    private static final int NS_CONDITION = 3000;

    private final List<Item> mItems;
    private final List<DashboardCategory> mCategories;
    private final List<Condition> mConditions;
    private final List<Tile> mSuggestions;
    private final int mSuggestionMode;
    private final Condition mExpandedCondition;
    private int mId;

    private DashboardData(Builder builder) {
        mCategories = builder.mCategories;
        mConditions = builder.mConditions;
        mSuggestions = builder.mSuggestions;
        mSuggestionMode = builder.mSuggestionMode;
        mExpandedCondition = builder.mExpandedCondition;

        mItems = new ArrayList<>();
        mId = 0;

        buildItemsData();
    }

    public int getItemIdByPosition(int position) {
        return mItems.get(position).id;
    }

    public int getItemTypeByPosition(int position) {
        return mItems.get(position).type;
    }

    public Object getItemEntityByPosition(int position) {
        return mItems.get(position).entity;
    }

    public List<Item> getItemList() {
        return mItems;
    }

    public int size() {
        return mItems.size();
    }

    public Object getItemEntityById(long id) {
        for (final Item item : mItems) {
            if (item.id == id) {
                return item.entity;
            }
        }
        return null;
    }

    public List<DashboardCategory> getCategories() {
        return mCategories;
    }

    public List<Condition> getConditions() {
        return mConditions;
    }

    public List<Tile> getSuggestions() {
        return mSuggestions;
    }

    public int getSuggestionMode() {
        return mSuggestionMode;
    }

    public Condition getExpandedCondition() {
        return mExpandedCondition;
    }

    /**
     * Find the position of the object in mItems list, using the equals method to compare
     *
     * @param entity the object that need to be found in list
     * @return position of the object, return POSITION_NOT_FOUND if object isn't in the list
     */
    public int getPositionByEntity(Object entity) {
        if (entity == null) return POSITION_NOT_FOUND;

        final int size = mItems.size();
        for (int i = 0; i < size; i++) {
            final Object item = mItems.get(i).entity;
            if (entity.equals(item)) {
                return i;
            }
        }

        return POSITION_NOT_FOUND;
    }

    /**
     * Find the position of the Tile object.
     * <p>
     * First, try to find the exact identical instance of the tile object, if not found,
     * then try to find a tile has the same title.
     *
     * @param tile tile that need to be found
     * @return position of the object, return INDEX_NOT_FOUND if object isn't in the list
     */
    public int getPositionByTile(Tile tile) {
        final int size = mItems.size();
        for (int i = 0; i < size; i++) {
            final Object entity = mItems.get(i).entity;
            if (entity == tile) {
                return i;
            } else if (entity instanceof Tile && tile.title.equals(((Tile) entity).title)) {
                return i;
            }
        }

        return POSITION_NOT_FOUND;
    }

    /**
     * Get the count of suggestions to display
     *
     * The displayable count mainly depends on the {@link #mSuggestionMode}
     * and the size of suggestions list.
     *
     * When in default mode, displayable count couldn't larger than
     * {@link #DEFAULT_SUGGESTION_COUNT}.
     *
     * When in expanded mode, display all the suggestions.
     *
     * @return the count of suggestions to display
     */
    public int getDisplayableSuggestionCount() {
        final int suggestionSize = mSuggestions.size();
        return mSuggestionMode == SUGGESTION_MODE_DEFAULT
                ? Math.min(DEFAULT_SUGGESTION_COUNT, suggestionSize)
                : mSuggestionMode == SUGGESTION_MODE_EXPANDED
                        ? suggestionSize : 0;
    }

    public boolean hasMoreSuggestions() {
        return mSuggestionMode == SUGGESTION_MODE_COLLAPSED
                || (mSuggestionMode == SUGGESTION_MODE_DEFAULT
                && mSuggestions.size() > DEFAULT_SUGGESTION_COUNT);
    }

    private void resetCount() {
        mId = 0;
    }

    /**
     * Count the item and add it into list when {@paramref add} is true.
     *
     * Note that {@link #mId} will increment automatically and the real
     * id stored in {@link Item} is shifted by {@paramref nameSpace}. This is a
     * simple way to keep the id stable.
     *
     * @param object    maybe {@link Condition}, {@link Tile}, {@link DashboardCategory} or null
     * @param type      type of the item, and value is the layout id
     * @param add       flag about whether to add item into list
     * @param nameSpace namespace based on the type
     */
    private void countItem(Object object, int type, boolean add, int nameSpace) {
        if (add) {
            mItems.add(new Item(object, type, mId + nameSpace, object == mExpandedCondition));
        }
        mId++;
    }

    /**
     * A special count item method for just suggestions. Id is calculated using suggestion hash
     * instead of the position of suggestion in list. This is a more stable id than countItem.
     */
    private void countSuggestion(Tile tile, boolean add) {
        if (add) {
            mItems.add(new Item(tile, R.layout.suggestion_tile, Objects.hash(tile.title), false));
        }
        mId++;
    }

    /**
     * Build the mItems list using mConditions, mSuggestions, mCategories data
     * and mIsShowingAll, mSuggestionMode flag.
     */
    private void buildItemsData() {
        boolean hasConditions = false;
        for (int i = 0; mConditions != null && i < mConditions.size(); i++) {
            boolean shouldShow = mConditions.get(i).shouldShow();
            hasConditions |= shouldShow;
            countItem(mConditions.get(i), R.layout.condition_card, shouldShow, NS_CONDITION);
        }

        resetCount();
        final boolean hasSuggestions = mSuggestions != null && mSuggestions.size() != 0;
        countItem(null, R.layout.dashboard_spacer, hasConditions && hasSuggestions, NS_SPACER);
        countItem(buildSuggestionHeaderData(), R.layout.suggestion_header, hasSuggestions,
                NS_SPACER);

        resetCount();
        if (mSuggestions != null) {
            int maxSuggestions = getDisplayableSuggestionCount();
            for (int i = 0; i < mSuggestions.size(); i++) {
                countSuggestion(mSuggestions.get(i), i < maxSuggestions);
            }
        }
        resetCount();
        for (int i = 0; mCategories != null && i < mCategories.size(); i++) {
            DashboardCategory category = mCategories.get(i);
            countItem(category, R.layout.dashboard_category,
                    !TextUtils.isEmpty(category.title), NS_ITEMS);
            for (int j = 0; j < category.tiles.size(); j++) {
                Tile tile = category.tiles.get(j);
                countItem(tile, R.layout.dashboard_tile, true, NS_ITEMS);
            }
        }
    }

    private SuggestionHeaderData buildSuggestionHeaderData() {
        SuggestionHeaderData data;
        if (mSuggestions == null) {
            data = new SuggestionHeaderData();
        } else {
            final boolean hasMoreSuggestions = hasMoreSuggestions();
            final int suggestionSize = mSuggestions.size();
            final int undisplayedSuggestionCount = suggestionSize - getDisplayableSuggestionCount();
            data = new SuggestionHeaderData(hasMoreSuggestions, suggestionSize,
                    undisplayedSuggestionCount);
        }

        return data;
    }

    /**
     * Builder used to build the ItemsData
     * <p>
     * {@link #mExpandedCondition} and {@link #mSuggestionMode} have default value
     * while others are not.
     */
    public static class Builder {
        private int mSuggestionMode = SUGGESTION_MODE_DEFAULT;
        private Condition mExpandedCondition = null;

        private List<DashboardCategory> mCategories;
        private List<Condition> mConditions;
        private List<Tile> mSuggestions;

        public Builder() {
        }

        public Builder(DashboardData dashboardData) {
            mCategories = dashboardData.mCategories;
            mConditions = dashboardData.mConditions;
            mSuggestions = dashboardData.mSuggestions;
            mSuggestionMode = dashboardData.mSuggestionMode;
            mExpandedCondition = dashboardData.mExpandedCondition;
        }

        public Builder setCategories(List<DashboardCategory> categories) {
            this.mCategories = categories;
            return this;
        }

        public Builder setConditions(List<Condition> conditions) {
            this.mConditions = conditions;
            return this;
        }

        public Builder setSuggestions(List<Tile> suggestions) {
            this.mSuggestions = suggestions;
            return this;
        }

        public Builder setSuggestionMode(int suggestionMode) {
            this.mSuggestionMode = suggestionMode;
            return this;
        }

        public Builder setExpandedCondition(Condition expandedCondition) {
            this.mExpandedCondition = expandedCondition;
            return this;
        }

        public DashboardData build() {
            return new DashboardData(this);
        }
    }

    /**
     * A DiffCallback to calculate the difference between old and new Item
     * List in DashboardData
     */
    public static class ItemsDataDiffCallback extends DiffUtil.Callback {
        final private List<Item> mOldItems;
        final private List<Item> mNewItems;

        public ItemsDataDiffCallback(List<Item> oldItems, List<Item> newItems) {
            mOldItems = oldItems;
            mNewItems = newItems;
        }

        @Override
        public int getOldListSize() {
            return mOldItems.size();
        }

        @Override
        public int getNewListSize() {
            return mNewItems.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return mOldItems.get(oldItemPosition).id == mNewItems.get(newItemPosition).id;
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return mOldItems.get(oldItemPosition).equals(mNewItems.get(newItemPosition));
        }

        @Nullable
        @Override
        public Object getChangePayload(int oldItemPosition, int newItemPosition) {
            if (mOldItems.get(oldItemPosition).type == Item.TYPE_CONDITION_CARD) {
                return "condition"; // return anything but null to mark the payload
            }
            return null;
        }
    }

    /**
     * An item contains the data needed in the DashboardData.
     */
    private static class Item {
        // valid types in field type
        private static final int TYPE_DASHBOARD_CATEGORY = R.layout.dashboard_category;
        private static final int TYPE_DASHBOARD_TILE = R.layout.dashboard_tile;
        private static final int TYPE_SUGGESTION_HEADER = R.layout.suggestion_header;
        private static final int TYPE_SUGGESTION_TILE = R.layout.suggestion_tile;
        private static final int TYPE_CONDITION_CARD = R.layout.condition_card;
        private static final int TYPE_DASHBOARD_SPACER = R.layout.dashboard_spacer;

        @IntDef({TYPE_DASHBOARD_CATEGORY, TYPE_DASHBOARD_TILE, TYPE_SUGGESTION_HEADER,
                TYPE_SUGGESTION_TILE, TYPE_CONDITION_CARD, TYPE_DASHBOARD_SPACER})
        @Retention(RetentionPolicy.SOURCE)
        public @interface ItemTypes{}

        /**
         * The main data object in item, usually is a {@link Tile}, {@link Condition} or
         * {@link DashboardCategory} object. This object can also be null when the
         * item is an divider line. Please refer to {@link #buildItemsData()} for
         * detail usage of the Item.
         */
        public final Object entity;

        /**
         * The type of item, value inside is the layout id(e.g. R.layout.dashboard_tile)
         */
        public final @ItemTypes int type;

        /**
         * Id of this item, used in the {@link ItemsDataDiffCallback} to identify the same item.
         */
        public final int id;

        /**
         * To store whether the condition is expanded, useless when {@link #type} is not
         * {@link #TYPE_CONDITION_CARD}
         */
        public final boolean conditionExpanded;

        public Item(Object entity, @ItemTypes int type, int id, boolean conditionExpanded) {
            this.entity = entity;
            this.type = type;
            this.id = id;
            this.conditionExpanded = conditionExpanded;
        }

        /**
         * Override it to make comparision in the {@link ItemsDataDiffCallback}
         * @param obj object to compared with
         * @return true if the same object or has equal value.
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (!(obj instanceof Item)) {
                return false;
            }

            final Item targetItem = (Item) obj;
            if (type != targetItem.type || id != targetItem.id) {
                return false;
            }

            switch (type) {
                case TYPE_DASHBOARD_CATEGORY:
                    // Only check title for dashboard category
                    return TextUtils.equals(((DashboardCategory) entity).title,
                            ((DashboardCategory) targetItem.entity).title);
                case TYPE_DASHBOARD_TILE:
                    final Tile localTile = (Tile) entity;
                    final Tile targetTile = (Tile) targetItem.entity;

                    // Only check title and summary for dashboard tile
                    return TextUtils.equals(localTile.title, targetTile.title)
                            && TextUtils.equals(localTile.summary, targetTile.summary);
                case TYPE_CONDITION_CARD:
                    // First check conditionExpanded for quick return
                    if (conditionExpanded != targetItem.conditionExpanded) {
                        return false;
                    }
                    // After that, go to default to do final check
                default:
                    return entity == null ? targetItem.entity == null
                            : entity.equals(targetItem.entity);
            }
        }
    }

    /**
     * This class contains the data needed to build the header. The data can also be
     * used to check the diff in DiffUtil.Callback
     */
    public static class SuggestionHeaderData {
        public final boolean hasMoreSuggestions;
        public final int suggestionSize;
        public final int undisplayedSuggestionCount;

        public SuggestionHeaderData(boolean moreSuggestions, int suggestionSize, int
                undisplayedSuggestionCount) {
            this.hasMoreSuggestions = moreSuggestions;
            this.suggestionSize = suggestionSize;
            this.undisplayedSuggestionCount = undisplayedSuggestionCount;
        }

        public SuggestionHeaderData() {
            hasMoreSuggestions = false;
            suggestionSize = 0;
            undisplayedSuggestionCount = 0;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (!(obj instanceof SuggestionHeaderData)) {
                return false;
            }

            SuggestionHeaderData targetData = (SuggestionHeaderData) obj;

            return hasMoreSuggestions == targetData.hasMoreSuggestions
                    && suggestionSize == targetData.suggestionSize
                    && undisplayedSuggestionCount == targetData.undisplayedSuggestionCount;
        }
    }

}