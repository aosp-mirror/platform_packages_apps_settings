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

import android.content.Context;
import android.support.annotation.MainThread;
import android.support.annotation.VisibleForTesting;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.settings.R;
import com.android.settings.search2.ResultPayload.PayloadType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.android.settings.search2.SearchResult.BOTTOM_RANK;
import static com.android.settings.search2.SearchResult.TOP_RANK;

public class SearchResultsAdapter extends RecyclerView.Adapter<SearchViewHolder> {

    private final SearchFragment mFragment;

    private List<SearchResult> mSearchResults;
    private Map<String, List<? extends SearchResult>> mResultsMap;

    public SearchResultsAdapter(SearchFragment fragment) {
        mFragment = fragment;
        mSearchResults = new ArrayList<>();
        mResultsMap = new ArrayMap<>();

        setHasStableIds(true);
    }

    @Override
    public SearchViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final Context context = parent.getContext();
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View view;
        switch (viewType) {
            case PayloadType.INTENT:
                view = inflater.inflate(R.layout.search_intent_item, parent, false);
                return new IntentSearchViewHolder(view);
            case PayloadType.INLINE_SWITCH:
                view = inflater.inflate(R.layout.search_inline_switch_item, parent, false);
                return new InlineSwitchViewHolder(view, context);
            case PayloadType.SAVED_QUERY:
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
     * Store the results from each of the loaders to be merged when all loaders are finished.
     *
     * @param results         the results from the loader.
     * @param loaderClassName class name of the loader.
     */
    @MainThread
    public void addSearchResults(List<? extends SearchResult> results, String loaderClassName) {
        if (results == null) {
            return;
        }
        mResultsMap.put(loaderClassName, results);
    }

    /**
     * Displays recent searched queries.
     *
     * @return The number of saved queries to display
     */
    public int displaySavedQuery(List<? extends SearchResult> data) {
        clearResults();
        mSearchResults.addAll(data);
        notifyDataSetChanged();
        return mSearchResults.size();
    }

    /**
     * Merge the results from each of the loaders into one list for the adapter.
     * Prioritizes results from the local database over installed apps.
     *
     * @return Number of matched results
     */
    public int displaySearchResults() {
        final List<? extends SearchResult> databaseResults = mResultsMap
                .get(DatabaseResultLoader.class.getName());
        final List<? extends SearchResult> installedAppResults = mResultsMap
                .get(InstalledAppResultLoader.class.getName());
        final int dbSize = (databaseResults != null) ? databaseResults.size() : 0;
        final int appSize = (installedAppResults != null) ? installedAppResults.size() : 0;
        final List<SearchResult> newResults = new ArrayList<>(dbSize + appSize);

        int dbIndex = 0;
        int appIndex = 0;
        int rank = TOP_RANK;

        while (rank <= BOTTOM_RANK) {
            while ((dbIndex < dbSize) && (databaseResults.get(dbIndex).rank == rank)) {
                newResults.add(databaseResults.get(dbIndex++));
            }
            while ((appIndex < appSize) && (installedAppResults.get(appIndex).rank == rank)) {
                newResults.add(installedAppResults.get(appIndex++));
            }
            rank++;
        }

        while (dbIndex < dbSize) {
            newResults.add(databaseResults.get(dbIndex++));
        }
        while (appIndex < appSize) {
            newResults.add(installedAppResults.get(appIndex++));
        }

        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                new SearchResultDiffCallback(mSearchResults, newResults), false /* detectMoves */);
        mSearchResults = newResults;
        diffResult.dispatchUpdatesTo(this);

        return mSearchResults.size();
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
