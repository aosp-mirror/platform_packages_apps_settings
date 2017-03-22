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

package com.android.settings.search2;

import android.graphics.drawable.Drawable;

import java.util.List;
import java.util.Objects;

/**
 * Data class as an interface for all Search Results.
 */
public class SearchResult implements Comparable<SearchResult> {

    /**
     * Defines the lowest rank for a search result to be considered as ranked. Results with ranks
     * higher than this have no guarantee for sorting order.
     */
    public static final int BOTTOM_RANK = 10;

    /**
     * Defines the highest rank for a search result. Used for special search results only.
     */
    public static final int TOP_RANK = 0;

    /**
     * The title of the result and main text displayed.
     * Intent Results: Displays as the primary
     */
    public final CharSequence title;

    /**
     * Summary / subtitle text
     * Intent Results: Displays the text underneath the title
     */
    final public CharSequence summary;

    /**
     * An ordered list of the information hierarchy.
     * Intent Results: Displayed a hierarchy of selections to reach the setting from the home screen
     */
    public final List<String> breadcrumbs;

    /**
     * A suggestion for the ranking of the result.
     * Based on Settings Rank:
     * 1 is a near perfect match
     * 9 is the weakest match
     * TODO subject to change
     */
    public final int rank;

    /**
     * Identifier for the recycler view adapter.
     */
    @ResultPayload.PayloadType
    public final int viewType;

    /**
     * Metadata for the specific result types.
     */
    public final ResultPayload payload;

    /**
     * Result's icon.
     */
    public final Drawable icon;

    /**
     * Stable id for this object.
     */
    public final long stableId;

    protected SearchResult(Builder builder) {
        title = builder.mTitle;
        summary = builder.mSummary;
        breadcrumbs = builder.mBreadcrumbs;
        rank = builder.mRank;
        icon = builder.mIcon;
        payload = builder.mResultPayload;
        viewType = payload.getType();
        stableId = Objects.hash(title, summary, breadcrumbs, rank, viewType);
    }

    @Override
    public int compareTo(SearchResult searchResult) {
        if (searchResult == null) {
            return -1;
        }
        return this.rank - searchResult.rank;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SearchResult)) {
            return false;
        }
        return this.stableId == ((SearchResult) obj).stableId;
    }

    @Override
    public int hashCode() {
        return (int) stableId;
    }

    public static class Builder {
        protected CharSequence mTitle;
        protected CharSequence mSummary;
        protected List<String> mBreadcrumbs;
        protected int mRank = 42;
        protected ResultPayload mResultPayload;
        protected Drawable mIcon;

        public Builder addTitle(CharSequence title) {
            mTitle = title;
            return this;
        }

        public Builder addSummary(CharSequence summary) {
            mSummary = summary;
            return this;
        }

        public Builder addBreadcrumbs(List<String> breadcrumbs) {
            mBreadcrumbs = breadcrumbs;
            return this;
        }

        public Builder addRank(int rank) {
            if (rank >= 0 && rank <= 9) {
                mRank = rank;
            }
            return this;
        }

        public Builder addIcon(Drawable icon) {
            mIcon = icon;
            return this;
        }

        public Builder addPayload(ResultPayload payload) {
            mResultPayload = payload;
            return this;
        }

        public SearchResult build() {
            // Check that all of the mandatory fields are set.
            if (mTitle == null) {
                throw new IllegalArgumentException("SearchResult missing title argument");
            } else if (mResultPayload == null) {
                throw new IllegalArgumentException("SearchResult missing Payload argument");
            }
            return new SearchResult(this);
        }
    }
}