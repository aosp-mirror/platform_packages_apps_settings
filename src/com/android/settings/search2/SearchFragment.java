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

import android.app.ActionBar;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout.LayoutParams;
import android.widget.SearchView;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.overlay.FeatureFactory;

import java.util.List;

public class SearchFragment extends InstrumentedFragment implements
        SearchView.OnQueryTextListener, LoaderManager.LoaderCallbacks<List<SearchResult>> {

    // State values
    static final String STATE_QUERY = "query";

    // Loader IDs
    private static final int LOADER_ID_DATABASE = 0;
    private static final int LOADER_ID_INSTALLED_APPS = 1;

    @VisibleForTesting
    String mQuery;

    private SearchFeatureProvider mSearchFeatureProvider;

    private SearchResultsAdapter mSearchAdapter;
    private RecyclerView mResultsRecyclerView;

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.DASHBOARD_SEARCH_RESULTS;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mSearchFeatureProvider = FeatureFactory.getFactory(context)
                .getSearchFeatureProvider(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mSearchAdapter = new SearchResultsAdapter(this);
        if (savedInstanceState != null) {
            mQuery = savedInstanceState.getString(STATE_QUERY);
            final LoaderManager loaderManager = getLoaderManager();
            loaderManager.initLoader(LOADER_ID_DATABASE, null, this);
            loaderManager.initLoader(LOADER_ID_INSTALLED_APPS, null, this);
        }
        final ActionBar actionBar = getActivity().getActionBar();
        actionBar.setCustomView(makeSearchView(actionBar, mQuery));
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.search_panel_2, container, false);
        mResultsRecyclerView = (RecyclerView) view.findViewById(R.id.list_results);
        mResultsRecyclerView.setAdapter(mSearchAdapter);
        mResultsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_QUERY, mQuery);
    }

    @Override
    public boolean onQueryTextChange(String query) {
        if (TextUtils.equals(query, mQuery)) {
            return true;
        }
        mQuery = query;
        mSearchAdapter.clearResults();

        if (TextUtils.isEmpty(mQuery)) {
            getLoaderManager().destroyLoader(LOADER_ID_DATABASE);
        } else {
            restartLoaders();
        }

        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return true;
    }

    @Override
    public Loader<List<SearchResult>> onCreateLoader(int id, Bundle args) {
        final Activity activity = getActivity();

        switch (id) {
            case LOADER_ID_DATABASE:
                return mSearchFeatureProvider.getDatabaseSearchLoader(activity, mQuery);
            case LOADER_ID_INSTALLED_APPS:
                return mSearchFeatureProvider.getInstalledAppSearchLoader(activity, mQuery);
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<List<SearchResult>> loader, List<SearchResult> data) {
        mSearchAdapter.mergeResults(data, loader.getClass().getName());
    }

    @Override
    public void onLoaderReset(Loader<List<SearchResult>> loader) {
    }

    private void restartLoaders() {
        final LoaderManager loaderManager = getLoaderManager();
        loaderManager.restartLoader(LOADER_ID_DATABASE, null /* args */, this /* callback */);
        loaderManager.restartLoader(LOADER_ID_INSTALLED_APPS, null /* args */, this /* callback */);
    }

    private SearchView makeSearchView(ActionBar actionBar, String query) {
        final SearchView searchView = new SearchView(actionBar.getThemedContext());
        searchView.setIconifiedByDefault(false);
        searchView.setQuery(query, false /* submitQuery */);
        searchView.setOnQueryTextListener(this);
        final LayoutParams lp =
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        searchView.setLayoutParams(lp);
        return searchView;
    }
}
