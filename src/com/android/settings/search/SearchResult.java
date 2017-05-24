/*
 * Copyright (C) 2017 The Android Open Source Project
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
 *
 */

package com.android.settings.search;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;

/**
 * Data class as an interface for all Search Results.
 */
public class SearchResult implements Comparable<SearchResult> {

    private static final String TAG = "SearchResult";

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
    public final int stableId;

    protected SearchResult(Builder builder) {
        stableId = builder.mStableId;
        title = builder.mTitle;
        summary = builder.mSummary;
        breadcrumbs = builder.mBreadcrumbs;
        rank = builder.mRank;
        icon = builder.mIcon;
        payload = builder.mResultPayload;
        viewType = payload.getType();
    }

    @Override
    public int compareTo(SearchResult searchResult) {
        if (searchResult == null) {
            return -1;
        }
        return this.rank - searchResult.rank;
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (!(that instanceof SearchResult)) {
            return false;
        }
        return this.stableId == ((SearchResult) that).stableId;
    }

    @Override
    public int hashCode() {
        return stableId;
    }

    public static class Builder {
        protected CharSequence mTitle;
        protected CharSequence mSummary;
        protected List<String> mBreadcrumbs;
        protected int mRank = 42;
        protected ResultPayload mResultPayload;
        protected Drawable mIcon;
        protected int mStableId;

        public Builder setTitle(CharSequence title) {
            mTitle = title;
            return this;
        }

        public Builder setSummary(CharSequence summary) {
            mSummary = summary;
            return this;
        }

        public Builder addBreadcrumbs(List<String> breadcrumbs) {
            mBreadcrumbs = breadcrumbs;
            return this;
        }

        public Builder setRank(int rank) {
            if (rank >= 0 && rank <= 9) {
                mRank = rank;
            }
            return this;
        }

        public Builder setIcon(Drawable icon) {
            mIcon = icon;
            return this;
        }

        public Builder setPayload(ResultPayload payload) {
            mResultPayload = payload;
            return this;
        }

        public Builder setStableId(int stableId) {
            mStableId = stableId;
            return this;
        }

        public SearchResult build() {
            // Check that all of the mandatory fields are set.
            if (TextUtils.isEmpty(mTitle)) {
                throw new IllegalStateException("SearchResult missing title argument");
            } else if (mStableId == 0) {
                Log.v(TAG, "No stable ID on SearchResult with title: " + mTitle);
                throw new IllegalStateException("SearchResult missing stableId argument");
            } else if (mResultPayload == null) {
                throw new IllegalStateException("SearchResult missing Payload argument");
            }
            return new SearchResult(this);
        }
    }
}