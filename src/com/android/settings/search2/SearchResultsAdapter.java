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

import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.RecyclerView.Adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.settings.R;
import com.android.settings.search2.ResultPayload.PayloadType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchResultsAdapter extends Adapter<SearchViewHolder> {
    private final List<SearchResult> mSearchResults;
    private final Map<String, List<SearchResult>> mResultsMap;
    private final SearchFragment mFragment;

    public SearchResultsAdapter(SearchFragment fragment) {
        mFragment = fragment;
        mSearchResults = new ArrayList<>();
        mResultsMap = new HashMap<>();

        setHasStableIds(true);
    }

    @Override
    public SearchViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case PayloadType.INTENT:
                View view = inflater.inflate(R.layout.search_intent_item, parent, false);
                return new IntentSearchViewHolder(view);
            case PayloadType.INLINE_SLIDER:
                return null;
            case PayloadType.INLINE_SWITCH:
                return null;
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

    public void mergeResults(List<SearchResult> freshResults, String loaderClassName) {
        if (freshResults == null) {
            return;
        }
        mResultsMap.put(loaderClassName, freshResults);
        final int oldSize = mSearchResults.size();
        mSearchResults.addAll(freshResults);
        final int newSize = mSearchResults.size();
        notifyItemRangeInserted(oldSize, newSize - oldSize);
    }

    public void clearResults() {
        mSearchResults.clear();
        mResultsMap.clear();
        notifyDataSetChanged();
    }

    @VisibleForTesting
    public List<SearchResult> getSearchResults() {
        return mSearchResults;
    }
}
