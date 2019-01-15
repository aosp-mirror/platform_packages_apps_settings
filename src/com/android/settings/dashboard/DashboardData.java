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
import android.graphics.drawable.Drawable;
import android.service.settings.suggestions.Suggestion;
import androidx.annotation.VisibleForTesting;
import androidx.recyclerview.widget.DiffUtil;
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
    public static final int POSITION_NOT_FOUND = -1;
    public static final int MAX_SUGGESTION_COUNT = 2;

    // stable id for different type of items.
    @VisibleForTesting
    static final int STABLE_ID_SUGGESTION_CONTAINER = 0;
    static final int STABLE_ID_SUGGESTION_CONDITION_DIVIDER = 1;
    @VisibleForTesting
    static final int STABLE_ID_CONDITION_HEADER = 2;
    @VisibleForTesting
    static final int STABLE_ID_CONDITION_FOOTER = 3;
    @VisibleForTesting
    static final int STABLE_ID_CONDITION_CONTAINER = 4;

    private final List<Item> mItems;
    private final DashboardCategory mCategory;
    private final List<Condition> mConditions;
    private final List<Suggestion> mSuggestions;
    private final boolean mConditionExpanded;

    private DashboardData(Builder builder) {
        mCategory = builder.mCategory;
        mConditions = builder.mConditions;
        mSuggestions = builder.mSuggestions;
        mConditionExpanded = builder.mConditionExpanded;
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

    public List<Suggestion> getSuggestions() {
        return mSuggestions;
    }

    public boolean hasSuggestion() {
        return sizeOf(mSuggestions) > 0;
    }

    public boolean isConditionExpanded() {
        return mConditionExpanded;
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
     * and mIsShowingAll, mConditionExpanded flag.
     */
    private void buildItemsData() {
        final List<Condition> conditions = getConditionsToShow(mConditions);
        final boolean hasConditions = sizeOf(conditions) > 0;

        final List<Suggestion> suggestions = getSuggestionsToShow(mSuggestions);
        final boolean hasSuggestions = sizeOf(suggestions) > 0;

        /* Suggestion container. This is the card view that contains the list of suggestions.
         * This will be added whenever the suggestion list is not empty */
        addToItemList(suggestions, R.layout.suggestion_container,
            STABLE_ID_SUGGESTION_CONTAINER, hasSuggestions);

        /* Divider between suggestion and conditions if both are present. */
        addToItemList(null /* item */, R.layout.horizontal_divider,
            STABLE_ID_SUGGESTION_CONDITION_DIVIDER, hasSuggestions && hasConditions);

        /* Condition header. This will be present when there is condition and it is collapsed */
        addToItemList(new ConditionHeaderData(conditions),
            R.layout.condition_header,
            STABLE_ID_CONDITION_HEADER, hasConditions && !mConditionExpanded);

        /* Condition container. This is the card view that contains the list of conditions.
         * This will be added whenever the condition list is not empty and expanded */
        addToItemList(conditions, R.layout.condition_container,
            STABLE_ID_CONDITION_CONTAINER, hasConditions && mConditionExpanded);

        /* Condition footer. This will be present when there is condition and it is expanded */
        addToItemList(null /* item */, R.layout.condition_footer,
            STABLE_ID_CONDITION_FOOTER, hasConditions && mConditionExpanded);

        if (mCategory != null) {
            final List<Tile> tiles = mCategory.getTiles();
            for (int i = 0; i < tiles.size(); i++) {
                final Tile tile = tiles.get(i);
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
        List<Condition> result = new ArrayList<>();
        final int size = conditions == null ? 0 : conditions.size();
        for (int i = 0; i < size; i++) {
            final Condition condition = conditions.get(i);
            if (condition.shouldShow()) {
                result.add(condition);
            }
        }
        return result;
    }

    private List<Suggestion> getSuggestionsToShow(List<Suggestion> suggestions) {
        if (suggestions == null) {
            return null;
        }
        if (suggestions.size() <= MAX_SUGGESTION_COUNT) {
            return suggestions;
        }
        final List<Suggestion> suggestionsToShow = new ArrayList<>(MAX_SUGGESTION_COUNT);
        for (int i = 0; i < MAX_SUGGESTION_COUNT; i++) {
            suggestionsToShow.add(suggestions.get(i));
        }
        return suggestionsToShow;
    }

    /**
     * Builder used to build the ItemsData
     */
    public static class Builder {
        private DashboardCategory mCategory;
        private List<Condition> mConditions;
        private List<Suggestion> mSuggestions;
        private boolean mConditionExpanded;

        public Builder() {
        }

        public Builder(DashboardData dashboardData) {
            mCategory = dashboardData.mCategory;
            mConditions = dashboardData.mConditions;
            mSuggestions = dashboardData.mSuggestions;
            mConditionExpanded = dashboardData.mConditionExpanded;
        }

        public Builder setCategory(DashboardCategory category) {
            this.mCategory = category;
            return this;
        }

        public Builder setConditions(List<Condition> conditions) {
            this.mConditions = conditions;
            return this;
        }

        public Builder setSuggestions(List<Suggestion> suggestions) {
            this.mSuggestions = suggestions;
            return this;
        }

        public Builder setConditionExpanded(boolean expanded) {
            this.mConditionExpanded = expanded;
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
        private static final int TYPE_SUGGESTION_CONTAINER =
            R.layout.suggestion_container;
        private static final int TYPE_CONDITION_CONTAINER =
            R.layout.condition_container;
        private static final int TYPE_CONDITION_HEADER =
            R.layout.condition_header;
        private static final int TYPE_CONDITION_FOOTER =
            R.layout.condition_footer;
        private static final int TYPE_SUGGESTION_CONDITION_DIVIDER = R.layout.horizontal_divider;

        @IntDef({TYPE_DASHBOARD_TILE, TYPE_SUGGESTION_CONTAINER, TYPE_CONDITION_CONTAINER,
            TYPE_CONDITION_HEADER, TYPE_CONDITION_FOOTER, TYPE_SUGGESTION_CONDITION_DIVIDER})
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
                case TYPE_SUGGESTION_CONTAINER:
                case TYPE_CONDITION_CONTAINER:
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
    public static class ConditionHeaderData {
        public final List<Drawable> conditionIcons;
        public final CharSequence title;
        public final int conditionCount;

        public ConditionHeaderData(List<Condition> conditions) {
            conditionCount = sizeOf(conditions);
            title = conditionCount > 0 ? conditions.get(0).getTitle() : null;
            conditionIcons = new ArrayList<>();
            for (int i = 0; conditions != null && i < conditions.size(); i++) {
                final Condition condition = conditions.get(i);
                conditionIcons.add(condition.getIcon());
            }
        }
    }

}
