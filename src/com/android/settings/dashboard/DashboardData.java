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
import android.graphics.drawable.Icon;
import android.support.annotation.VisibleForTesting;
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
    public static final int HEADER_MODE_DEFAULT = 0;
    public static final int HEADER_MODE_SUGGESTION_EXPANDED = 1;
    public static final int HEADER_MODE_FULLY_EXPANDED = 2;
    public static final int HEADER_MODE_COLLAPSED = 3;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({HEADER_MODE_DEFAULT, HEADER_MODE_SUGGESTION_EXPANDED, HEADER_MODE_FULLY_EXPANDED,
            HEADER_MODE_COLLAPSED})
    public @interface HeaderMode {
    }

    public static final int POSITION_NOT_FOUND = -1;
    public static final int DEFAULT_SUGGESTION_COUNT = 2;

    // stable id for different type of items.
    @VisibleForTesting
    static final int STABLE_ID_SUGGESTION_CONDITION_TOP_HEADER = 0;
    @VisibleForTesting
    static final int STABLE_ID_SUGGESTION_CONDITION_MIDDLE_HEADER = 1;
    @VisibleForTesting
    static final int STABLE_ID_SUGGESTION_CONDITION_FOOTER = 2;
    @VisibleForTesting
    static final int STABLE_ID_SUGGESTION_CONTAINER = 3;
    @VisibleForTesting
    static final int STABLE_ID_CONDITION_CONTAINER = 4;

    private final List<Item> mItems;
    private final DashboardCategory mCategory;
    private final List<Condition> mConditions;
    private final List<Tile> mSuggestions;
    @HeaderMode
    private final int mSuggestionConditionMode;

    private DashboardData(Builder builder) {
        mCategory = builder.mCategory;
        mConditions = builder.mConditions;
        mSuggestions = builder.mSuggestions;
        mSuggestionConditionMode = builder.mSuggestionConditionMode;

        mItems = new ArrayList<>();

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

    public DashboardCategory getCategory() {
        return mCategory;
    }

    public List<Condition> getConditions() {
        return mConditions;
    }

    public List<Tile> getSuggestions() {
        return mSuggestions;
    }

    public int getSuggestionConditionMode() {
        return mSuggestionConditionMode;
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
     * The displayable count mainly depends on the {@link #mSuggestionConditionMode}
     * and the size of suggestions list.
     *
     * When in default mode, displayable count couldn't be larger than
     * {@link #DEFAULT_SUGGESTION_COUNT}.
     *
     * When in expanded mode, display all the suggestions.
     *
     * @return the count of suggestions to display
     */
    public int getDisplayableSuggestionCount() {
        final int suggestionSize = sizeOf(mSuggestions);
        if (mSuggestionConditionMode == HEADER_MODE_COLLAPSED) {
            return 0;
        }
        if (mSuggestionConditionMode == HEADER_MODE_DEFAULT) {
            return Math.min(DEFAULT_SUGGESTION_COUNT, suggestionSize);
        }
        return suggestionSize;
    }

    /**
     * Add item into list when {@paramref add} is true.
     *
     * @param item     maybe {@link Condition}, {@link Tile}, {@link DashboardCategory} or null
     * @param type     type of the item, and value is the layout id
     * @param stableId The stable id for this item
     * @param add      flag about whether to add item into list
     */
    private void addToItemList(Object item, int type, int stableId, boolean add) {
        if (add) {
            mItems.add(new Item(item, type, stableId));
        }
    }

    /**
     * Build the mItems list using mConditions, mSuggestions, mCategories data
     * and mIsShowingAll, mSuggestionConditionMode flag.
     */
    private void buildItemsData() {
        final boolean hasSuggestions = sizeOf(mSuggestions) > 0;
        final List<Condition> conditions = getConditionsToShow(mConditions);
        final boolean hasConditions = sizeOf(conditions) > 0;

        final List<Tile> suggestions = getSuggestionsToShow(mSuggestions);
        final int hiddenSuggestion =
                hasSuggestions ? sizeOf(mSuggestions) - sizeOf(suggestions) : 0;

        final boolean hasSuggestionAndCollapsed = hasSuggestions
                && mSuggestionConditionMode == HEADER_MODE_COLLAPSED;
        final boolean onlyHasConditionAndCollapsed = !hasSuggestions
                && hasConditions
                && mSuggestionConditionMode != HEADER_MODE_FULLY_EXPANDED;

        /* Top suggestion/condition header. This will be present when there is any suggestion
         * and the mode is collapsed */
        addToItemList(new SuggestionConditionHeaderData(conditions, hiddenSuggestion),
                R.layout.suggestion_condition_header,
                STABLE_ID_SUGGESTION_CONDITION_TOP_HEADER, hasSuggestionAndCollapsed);

        /* Use mid header if there is only condition & it's in collapsed mode */
        addToItemList(new SuggestionConditionHeaderData(conditions, hiddenSuggestion),
                R.layout.suggestion_condition_header,
                STABLE_ID_SUGGESTION_CONDITION_MIDDLE_HEADER, onlyHasConditionAndCollapsed);

        /* Suggestion container. This is the card view that contains the list of suggestions.
         * This will be added whenever the suggestion list is not empty */
        addToItemList(suggestions, R.layout.suggestion_condition_container,
                STABLE_ID_SUGGESTION_CONTAINER, sizeOf(suggestions) > 0);

        /* Second suggestion/condition header. This will be added when there is at least one
         * suggestion or condition that is not currently displayed, and the user can expand the
         * section to view more items. */
        addToItemList(new SuggestionConditionHeaderData(conditions, hiddenSuggestion),
                R.layout.suggestion_condition_header,
                STABLE_ID_SUGGESTION_CONDITION_MIDDLE_HEADER,
                mSuggestionConditionMode != HEADER_MODE_COLLAPSED
                        && mSuggestionConditionMode != HEADER_MODE_FULLY_EXPANDED
                        && (hiddenSuggestion > 0 || hasConditions && hasSuggestions));

            /* Condition container. This is the card view that contains the list of conditions.
             * This will be added whenever the condition list is not empty */
        addToItemList(conditions, R.layout.suggestion_condition_container,
                STABLE_ID_CONDITION_CONTAINER,
                hasConditions && mSuggestionConditionMode == HEADER_MODE_FULLY_EXPANDED);

            /* Suggestion/condition footer. This will be present when the section is fully expanded
             * or when there is no conditions and no hidden suggestions */
        addToItemList(null /* item */, R.layout.suggestion_condition_footer,
                STABLE_ID_SUGGESTION_CONDITION_FOOTER,
                (hasConditions || hasSuggestions)
                        && mSuggestionConditionMode == HEADER_MODE_FULLY_EXPANDED
                        || hasSuggestions
                        && !hasConditions
                        && hiddenSuggestion == 0);

        if(mCategory != null) {
            for (int j = 0; j < mCategory.tiles.size(); j++) {
                final Tile tile = mCategory.tiles.get(j);
                addToItemList(tile, R.layout.dashboard_tile, Objects.hash(tile.title),
                        true /* add */);
            }
        }
    }

    private static int sizeOf(List<?> list) {
        return list == null ? 0 : list.size();
    }

    private List<Condition> getConditionsToShow(List<Condition> conditions) {
        if (conditions == null) {
            return null;
        }
        List<Condition> result = new ArrayList<Condition>();
        final int size = conditions == null ? 0 : conditions.size();
        for (int i = 0; i < size; i++) {
            final Condition condition = conditions.get(i);
            if (condition.shouldShow()) {
                result.add(condition);
            }
        }
        return result;
    }

    private List<Tile> getSuggestionsToShow(List<Tile> suggestions) {
        if (suggestions == null || mSuggestionConditionMode == HEADER_MODE_COLLAPSED) {
            return null;
        }
        if (mSuggestionConditionMode != HEADER_MODE_DEFAULT
                || suggestions.size() <= DEFAULT_SUGGESTION_COUNT) {
            return suggestions;
        }
        return suggestions.subList(0, DEFAULT_SUGGESTION_COUNT);
    }

    /**
     * Builder used to build the ItemsData
     * <p>
     * {@link #mSuggestionConditionMode} have default value while others are not.
     */
    public static class Builder {
        @HeaderMode
        private int mSuggestionConditionMode = HEADER_MODE_DEFAULT;

        private DashboardCategory mCategory;
        private List<Condition> mConditions;
        private List<Tile> mSuggestions;

        public Builder() {
        }

        public Builder(DashboardData dashboardData) {
            mCategory = dashboardData.mCategory;
            mConditions = dashboardData.mConditions;
            mSuggestions = dashboardData.mSuggestions;
            mSuggestionConditionMode = dashboardData.mSuggestionConditionMode;
        }

        public Builder setCategory(DashboardCategory category) {
            this.mCategory = category;
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

        public Builder setSuggestionConditionMode(@HeaderMode int mode) {
            this.mSuggestionConditionMode = mode;
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

    }

    /**
     * An item contains the data needed in the DashboardData.
     */
    static class Item {
        // valid types in field type
        private static final int TYPE_DASHBOARD_TILE = R.layout.dashboard_tile;
        private static final int TYPE_SUGGESTION_CONDITION_CONTAINER =
                R.layout.suggestion_condition_container;
        private static final int TYPE_SUGGESTION_CONDITION_HEADER =
                R.layout.suggestion_condition_header;
        private static final int TYPE_SUGGESTION_CONDITION_FOOTER =
                R.layout.suggestion_condition_footer;
        private static final int TYPE_DASHBOARD_SPACER = R.layout.dashboard_spacer;

        @IntDef({TYPE_DASHBOARD_TILE, TYPE_SUGGESTION_CONDITION_CONTAINER,
                TYPE_SUGGESTION_CONDITION_HEADER, TYPE_SUGGESTION_CONDITION_FOOTER,
                TYPE_DASHBOARD_SPACER})
        @Retention(RetentionPolicy.SOURCE)
        public @interface ItemTypes {
        }

        /**
         * The main data object in item, usually is a {@link Tile}, {@link Condition}
         * object. This object can also be null when the
         * item is an divider line. Please refer to {@link #buildItemsData()} for
         * detail usage of the Item.
         */
        public final Object entity;

        /**
         * The type of item, value inside is the layout id(e.g. R.layout.dashboard_tile)
         */
        @ItemTypes
        public final int type;

        /**
         * Id of this item, used in the {@link ItemsDataDiffCallback} to identify the same item.
         */
        public final int id;

        public Item(Object entity, @ItemTypes int type, int id) {
            this.entity = entity;
            this.type = type;
            this.id = id;
        }

        /**
         * Override it to make comparision in the {@link ItemsDataDiffCallback}
         *
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
                case TYPE_DASHBOARD_TILE:
                    final Tile localTile = (Tile) entity;
                    final Tile targetTile = (Tile) targetItem.entity;

                    // Only check title and summary for dashboard tile
                    return TextUtils.equals(localTile.title, targetTile.title)
                            && TextUtils.equals(localTile.summary, targetTile.summary);
                case TYPE_SUGGESTION_CONDITION_CONTAINER:
                    // If entity is suggestion and contains remote view, force refresh
                    final List entities = (List) entity;
                    if (!entities.isEmpty()) {
                        Object firstEntity = entities.get(0);
                        if (firstEntity instanceof Tile
                                && ((Tile) firstEntity).remoteViews != null) {
                            return false;
                        }
                    }
                    // Otherwise Fall through to default
                default:
                    return entity == null ? targetItem.entity == null
                            : entity.equals(targetItem.entity);
            }
        }
    }

    /**
     * This class contains the data needed to build the suggestion/condition header. The data can
     * also be used to check the diff in DiffUtil.Callback
     */
    public static class SuggestionConditionHeaderData {
        public final List<Icon> conditionIcons;
        public final CharSequence title;
        public final int conditionCount;
        public final int hiddenSuggestionCount;

        public SuggestionConditionHeaderData(List<Condition> conditions,
                int hiddenSuggestionCount) {
            conditionCount = sizeOf(conditions);
            this.hiddenSuggestionCount = hiddenSuggestionCount;
            title = conditionCount > 0 ? conditions.get(0).getTitle() : null;
            conditionIcons = new ArrayList<Icon>();
            for (int i = 0; conditions != null && i < conditions.size(); i++) {
                final Condition condition = conditions.get(i);
                conditionIcons.add(condition.getIcon());
            }
        }
    }

}