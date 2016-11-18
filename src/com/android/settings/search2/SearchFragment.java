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

import android.app.Activity;
import android.content.Loader;
import android.os.Bundle;
import android.app.LoaderManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.SearchView;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.core.InstrumentedFragment;

import java.util.List;

public class SearchFragment extends InstrumentedFragment implements
        SearchView.OnQueryTextListener, MenuItem.OnActionExpandListener,
        LoaderManager.LoaderCallbacks<List<SearchResult>>  {

    private static final int DATABASE_LOADER_ID = 0;

    private SearchResultsAdapter mSearchAdapter;

    private DatabaseResultLoader mSearchLoader;

    private RecyclerView mResultsRecyclerView;
    private SearchView mSearchView;
    private MenuItem mSearchMenuItem;

    private String mQuery;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mSearchAdapter = new SearchResultsAdapter();

        final LoaderManager loaderManager = getLoaderManager();
        loaderManager.initLoader(DATABASE_LOADER_ID, null, this);
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.search_options_menu, menu);


        mSearchMenuItem = menu.findItem(R.id.search);

        mSearchView = (SearchView) mSearchMenuItem.getActionView();
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setMaxWidth(Integer.MAX_VALUE);
        mSearchMenuItem.expandActionView();
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        // Return false to prevent the search box from collapsing.
        return false;
    }

    @Override
    public boolean onQueryTextChange(String query) {
        if (query == null || query.equals(mQuery)) {
            return false;
        }

        mQuery = query;
        clearLoaders();

        final LoaderManager loaderManager = getLoaderManager();
        loaderManager.restartLoader(DATABASE_LOADER_ID, null, this);

        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public Loader<List<SearchResult>> onCreateLoader(int id, Bundle args) {
        final Activity activity = getActivity();

        switch (id) {
            case DATABASE_LOADER_ID:
                mSearchLoader = new DatabaseResultLoader(activity, mQuery);
                return mSearchLoader;
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<List<SearchResult>> loader, List<SearchResult> data) {
        if (data == null) {
            return;
        }

        mSearchAdapter.mergeResults(data, loader.getClass().getName());
    }

    @Override
    public void onLoaderReset(Loader<List<SearchResult>> loader) { }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.DASHBOARD_SEARCH_RESULTS;
    }

    private void clearLoaders() {
        if (mSearchLoader != null) {
            mSearchLoader.cancelLoad();
            mSearchLoader = null;
        }
    }
}
