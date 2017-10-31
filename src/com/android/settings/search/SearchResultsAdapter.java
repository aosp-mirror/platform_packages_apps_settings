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

import android.content.Context;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;

public class SearchResultsAdapter extends RecyclerView.Adapter<SearchViewHolder> {

    private final SearchFragment mFragment;
    private final List<SearchResult> mSearchResults;

    public SearchResultsAdapter(SearchFragment fragment) {
        mFragment = fragment;
        mSearchResults = new ArrayList<>();

        setHasStableIds(true);
    }

    @Override
    public SearchViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final Context context = parent.getContext();
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View view;
        switch (viewType) {
            case ResultPayload.PayloadType.INTENT:
                view = inflater.inflate(R.layout.search_intent_item, parent, false);
                return new IntentSearchViewHolder(view);
            case ResultPayload.PayloadType.INLINE_SWITCH:
                // TODO (b/62807132) replace layout InlineSwitchViewHolder and return an
                // InlineSwitchViewHolder.
                view = inflater.inflate(R.layout.search_intent_item, parent, false);
                return new IntentSearchViewHolder(view);
            case ResultPayload.PayloadType.INLINE_LIST:
                // TODO (b/62807132) build a inline-list view holder & layout.
                view = inflater.inflate(R.layout.search_intent_item, parent, false);
                return new IntentSearchViewHolder(view);
            case ResultPayload.PayloadType.SAVED_QUERY:
                view = inflater.inflate(R.layout.search_saved_query_item, parent, false);
                return new SavedQueryViewHolder(view);
            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(SearchViewHolder holder, int position) {
        holder.onBind(mFragment, mSearchResults.get(position));
    }

    @Override
    public long getItemId(int position) {
        return mSearchResults.get(position).stableId;
    }

    @Override
    public int getItemViewType(int position) {
        return mSearchResults.get(position).viewType;
    }

    @Override
    public int getItemCount() {
        return mSearchResults.size();
    }

    /**
     * Displays recent searched queries.
     */
    public void displaySavedQuery(List<? extends SearchResult> data) {
        clearResults();
        mSearchResults.addAll(data);
        notifyDataSetChanged();
    }

    public void clearResults() {
        mSearchResults.clear();
        notifyDataSetChanged();
    }

    public List<SearchResult> getSearchResults() {
        return mSearchResults;
    }

    public void postSearchResults(List<? extends SearchResult> newSearchResults) {
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                new SearchResultDiffCallback(mSearchResults, newSearchResults));
        mSearchResults.clear();
        mSearchResults.addAll(newSearchResults);
        diffResult.dispatchUpdatesTo(this);
        mFragment.onSearchResultsDisplayed(mSearchResults.size());
    }
}
