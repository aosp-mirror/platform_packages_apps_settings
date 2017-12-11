package com.android.settings.search;

import com.android.settingslib.utils.AsyncLoader;

import android.content.Context;

import java.util.List;

/**
 * Loads a sorted list of Search results for a given query.
 */
public class SearchResultLoader extends AsyncLoader<List<? extends SearchResult>> {

    private final String mQuery;

    public SearchResultLoader(Context context, String query) {
        super(context);
        mQuery = query;
    }

    @Override
    public List<? extends SearchResult> loadInBackground() {
        SearchResultAggregator aggregator = SearchResultAggregator.getInstance();
        return aggregator.fetchResults(getContext(), mQuery);
    }

    @Override
    protected void onDiscardResult(List<? extends SearchResult> result) {
    }
}
