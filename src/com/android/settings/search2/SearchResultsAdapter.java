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

public class SearchResultsAdapter extends Adapter<SearchViewHolder> {
    private ArrayList<SearchResult> mSearchResults;
    private HashMap<String, List<SearchResult>> mResultsMap;

    public SearchResultsAdapter() {
        mSearchResults = new ArrayList<>();
        mResultsMap = new HashMap<>();

        setHasStableIds(true);
    }

    public void mergeResults(List<SearchResult> freshResults, String loaderClassName) {
        if (freshResults == null) {
            return;
        }
        mResultsMap.put(loaderClassName, freshResults);
        mSearchResults = mergeMappedResults();
        notifyDataSetChanged();
    }

    private ArrayList<SearchResult> mergeMappedResults() {
        ArrayList<SearchResult> mergedResults = new ArrayList<>();
        for(String key : mResultsMap.keySet()) {
            mergedResults.addAll(mResultsMap.get(key));
        }
        return mergedResults;
    }

    @Override
    public SearchViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch(viewType) {
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
        SearchResult result = mSearchResults.get(position);
        holder.onBind(result);
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    @Override
    public int getItemViewType(int position) {
        return mSearchResults.get(position).viewType;
    }

    @Override
    public int getItemCount() {
        return mSearchResults.size();
    }

    @VisibleForTesting
    public ArrayList<SearchResult> getSearchResults() {
        return mSearchResults;
    }
}
