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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout.LayoutParams;
import android.widget.SearchView;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.overlay.FeatureFactory;

import java.util.List;

public class SearchFragment extends InstrumentedFragment implements
        SearchView.OnQueryTextListener, LoaderManager.LoaderCallbacks<List<SearchResult>> {
    private static final String TAG = "SearchFragment";

    // State values
    private static final String STATE_QUERY = "state_query";
    private static final String STATE_NEVER_ENTERED_QUERY = "state_never_entered_query";
    private static final String STATE_RESULT_CLICK_COUNT = "state_result_click_count";

    // Loader IDs
    private static final int LOADER_ID_RECENTS = 0;
    private static final int LOADER_ID_DATABASE = 1;
    private static final int LOADER_ID_INSTALLED_APPS = 2;

    // Logging
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    static final String RESULT_CLICK_COUNT = "settings_search_result_click_count";

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    String mQuery;

    private final SaveQueryRecorderCallback mSaveQueryRecorderCallback =
            new SaveQueryRecorderCallback();

    private boolean mNeverEnteredQuery = true;
    private int mResultClickCount;
    private MetricsFeatureProvider mMetricsFeatureProvider;
    private SearchFeatureProvider mSearchFeatureProvider;

    private SearchResultsAdapter mSearchAdapter;
    private RecyclerView mResultsRecyclerView;
    private SearchView mSearchView;

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.DASHBOARD_SEARCH_RESULTS;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mSearchFeatureProvider = FeatureFactory.getFactory(context).getSearchFeatureProvider();
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mSearchAdapter = new SearchResultsAdapter(this);
        final LoaderManager loaderManager = getLoaderManager();
        if (savedInstanceState != null) {
            mQuery = savedInstanceState.getString(STATE_QUERY);
            mNeverEnteredQuery = savedInstanceState.getBoolean(STATE_NEVER_ENTERED_QUERY);
            mResultClickCount = savedInstanceState.getInt(STATE_RESULT_CLICK_COUNT);
            loaderManager.initLoader(LOADER_ID_DATABASE, null, this);
            loaderManager.initLoader(LOADER_ID_INSTALLED_APPS, null, this);
        } else {
            loaderManager.initLoader(LOADER_ID_RECENTS, null, this);
        }

        final Activity activity = getActivity();
        final ActionBar actionBar = activity.getActionBar();
        mSearchView = makeSearchView(actionBar, mQuery);
        actionBar.setCustomView(mSearchView);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);

        // Run the Index update only if we have some space
        if (!Utils.isLowStorage(activity)) {
            mSearchFeatureProvider.updateIndex(activity);
        } else {
            Log.w(TAG, "Cannot update the Indexer as we are running low on storage space!");
        }
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
    public void onStop() {
        super.onStop();
        final Activity activity = getActivity();
        if (activity != null && activity.isFinishing()) {
            mMetricsFeatureProvider.histogram(activity, RESULT_CLICK_COUNT, mResultClickCount);
            if (mNeverEnteredQuery) {
                mMetricsFeatureProvider.action(activity,
                        MetricsProto.MetricsEvent.ACTION_LEAVE_SEARCH_RESULT_WITHOUT_QUERY);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_QUERY, mQuery);
        outState.putBoolean(STATE_NEVER_ENTERED_QUERY, mNeverEnteredQuery);
        outState.putInt(STATE_RESULT_CLICK_COUNT, mResultClickCount);
    }

    @Override
    public boolean onQueryTextChange(String query) {
        if (TextUtils.equals(query, mQuery)) {
            return true;
        }
        mResultClickCount = 0;
        mNeverEnteredQuery = false;
        mQuery = query;
        mSearchAdapter.clearResults();

        if (TextUtils.isEmpty(mQuery)) {
            final LoaderManager loaderManager = getLoaderManager();
            loaderManager.destroyLoader(LOADER_ID_DATABASE);
            loaderManager.destroyLoader(LOADER_ID_INSTALLED_APPS);
            loaderManager.restartLoader(LOADER_ID_RECENTS, null /* args */, this /* callback */);
        } else {
            restartLoaders();
        }

        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        // Save submitted query.
        getLoaderManager().restartLoader(SaveQueryRecorderCallback.LOADER_ID_SAVE_QUERY_TASK, null,
                mSaveQueryRecorderCallback);

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
            case LOADER_ID_RECENTS:
                return mSearchFeatureProvider.getSavedQueryLoader(activity);
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

    public void onSearchResultClicked() {
        mResultClickCount++;
    }

    public void onSavedQueryClicked(CharSequence query) {
        final String queryString = query.toString();
        mSearchView.setQuery(queryString, false /* submit */);
        onQueryTextChange(queryString);
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

    private class SaveQueryRecorderCallback implements LoaderManager.LoaderCallbacks<Void> {
        // TODO: make a generic background task manager to handle one-off tasks like this one.

        private static final int LOADER_ID_SAVE_QUERY_TASK = 0;

        @Override
        public Loader<Void> onCreateLoader(int id, Bundle args) {
            return new SavedQueryRecorder(getActivity(), mQuery);
        }

        @Override
        public void onLoadFinished(Loader<Void> loader, Void data) {

        }

        @Override
        public void onLoaderReset(Loader<Void> loader) {

        }
    }
}
