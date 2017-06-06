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
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.SearchView;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.IndexingCallback;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This fragment manages the lifecycle of indexing and searching.
 *
 * In onCreate, the indexing process is initiated in DatabaseIndexingManager.
 * While the indexing is happening, loaders are blocked from accessing the database, but the user
 * is free to start typing their query.
 *
 * When the indexing is complete, the fragment gets a callback to initialize the loaders and search
 * the query if the user has entered text.
 */
public class SearchFragment extends InstrumentedFragment implements SearchView.OnQueryTextListener,
        LoaderManager.LoaderCallbacks<List<? extends SearchResult>>, IndexingCallback {
    private static final String TAG = "SearchFragment";

    @VisibleForTesting
    static final int SEARCH_TAG = "SearchViewTag".hashCode();

    // State values
    private static final String STATE_QUERY = "state_query";
    private static final String STATE_SHOWING_SAVED_QUERY = "state_showing_saved_query";
    private static final String STATE_NEVER_ENTERED_QUERY = "state_never_entered_query";
    private static final String STATE_RESULT_CLICK_COUNT = "state_result_click_count";

    // Loader IDs
    @VisibleForTesting
    static final int LOADER_ID_DATABASE = 1;
    @VisibleForTesting
    static final int LOADER_ID_INSTALLED_APPS = 2;

    private static final int NUM_QUERY_LOADERS = 2;

    @VisibleForTesting
    AtomicInteger mUnfinishedLoadersCount = new AtomicInteger(NUM_QUERY_LOADERS);

    // Logging
    @VisibleForTesting
    static final String RESULT_CLICK_COUNT = "settings_search_result_click_count";

    @VisibleForTesting
    String mQuery;

    private boolean mNeverEnteredQuery = true;
    @VisibleForTesting
    boolean mShowingSavedQuery;
    private int mResultClickCount;
    private MetricsFeatureProvider mMetricsFeatureProvider;
    @VisibleForTesting
    SavedQueryController mSavedQueryController;

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    SearchFeatureProvider mSearchFeatureProvider;

    private SearchResultsAdapter mSearchAdapter;

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    RecyclerView mResultsRecyclerView;
    @VisibleForTesting
    SearchView mSearchView;
    private LinearLayout mNoResultsView;

    @VisibleForTesting
    final RecyclerView.OnScrollListener mScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            if (dy != 0) {
                hideKeyboard();
            }
        }
    };

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

        final LoaderManager loaderManager = getLoaderManager();
        mSearchAdapter = new SearchResultsAdapter(this);
        mSavedQueryController = new SavedQueryController(
                getContext(), loaderManager, mSearchAdapter);
        mSearchFeatureProvider.initFeedbackButton();

        if (savedInstanceState != null) {
            mQuery = savedInstanceState.getString(STATE_QUERY);
            mNeverEnteredQuery = savedInstanceState.getBoolean(STATE_NEVER_ENTERED_QUERY);
            mResultClickCount = savedInstanceState.getInt(STATE_RESULT_CLICK_COUNT);
            mShowingSavedQuery = savedInstanceState.getBoolean(STATE_SHOWING_SAVED_QUERY);
        } else {
            mShowingSavedQuery = true;
        }

        final Activity activity = getActivity();
        final ActionBar actionBar = activity.getActionBar();
        mSearchView = makeSearchView(actionBar, mQuery);
        actionBar.setCustomView(mSearchView);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        mSearchView.requestFocus();

        // Run the Index update only if we have some space
        if (!Utils.isLowStorage(activity)) {
            mSearchFeatureProvider.updateIndex(activity, this /* indexingCallback */);
        } else {
            Log.w(TAG, "Cannot update the Indexer as we are running low on storage space!");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.search_panel, container, false);
        mResultsRecyclerView = view.findViewById(R.id.list_results);
        mResultsRecyclerView.setAdapter(mSearchAdapter);
        mResultsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mResultsRecyclerView.addOnScrollListener(mScrollListener);

        mNoResultsView = view.findViewById(R.id.no_results_layout);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        requery();
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
        outState.putBoolean(STATE_SHOWING_SAVED_QUERY, mShowingSavedQuery);
        outState.putInt(STATE_RESULT_CLICK_COUNT, mResultClickCount);
    }

    @Override
    public boolean onQueryTextChange(String query) {
        if (TextUtils.equals(query, mQuery)) {
            return true;
        }

        final boolean isEmptyQuery = TextUtils.isEmpty(query);

        // Hide no-results-view when the new query is not a super-string of the previous
        if ((mQuery != null) && (mNoResultsView.getVisibility() == View.VISIBLE)
                && (query.length() < mQuery.length())) {
            mNoResultsView.setVisibility(View.GONE);
        }

        mResultClickCount = 0;
        mNeverEnteredQuery = false;
        mQuery = query;

        // If indexing is not finished, register the query text, but don't search.
        if (!mSearchFeatureProvider.isIndexingComplete(getActivity())) {
            return true;
        }

        if (isEmptyQuery) {
            final LoaderManager loaderManager = getLoaderManager();
            loaderManager.destroyLoader(LOADER_ID_DATABASE);
            loaderManager.destroyLoader(LOADER_ID_INSTALLED_APPS);
            mShowingSavedQuery = true;
            mSavedQueryController.loadSavedQueries();
            mSearchFeatureProvider.hideFeedbackButton();
        } else {
            restartLoaders();
        }

        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        // Save submitted query.
        mSavedQueryController.saveQuery(mQuery);
        hideKeyboard();
        return true;
    }

    @Override
    public Loader<List<? extends SearchResult>> onCreateLoader(int id, Bundle args) {
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
    public void onLoadFinished(Loader<List<? extends SearchResult>> loader,
            List<? extends SearchResult> data) {
        mSearchAdapter.addSearchResults(data, loader.getClass().getName());
        if (mUnfinishedLoadersCount.decrementAndGet() != 0) {
            return;
        }
        final int resultCount = mSearchAdapter.displaySearchResults();

        if (resultCount == 0) {
            mNoResultsView.setVisibility(View.VISIBLE);
        } else {
            mNoResultsView.setVisibility(View.GONE);
            mResultsRecyclerView.scrollToPosition(0);
        }
        mSearchFeatureProvider.showFeedbackButton(this, getView());
    }

    @Override
    public void onLoaderReset(Loader<List<? extends SearchResult>> loader) {
    }

    /**
     * Gets called when Indexing is completed.
     */
    @Override
    public void onIndexingFinished() {
        if (getActivity() == null) {
            return;
        }
        if (mShowingSavedQuery) {
            mSavedQueryController.loadSavedQueries();
        } else {
            final LoaderManager loaderManager = getLoaderManager();
            loaderManager.initLoader(LOADER_ID_DATABASE, null, this);
            loaderManager.initLoader(LOADER_ID_INSTALLED_APPS, null, this);
        }

        requery();
    }

    public void onSearchResultClicked() {
        mSavedQueryController.saveQuery(mQuery);
        mResultClickCount++;
    }

    public void onSavedQueryClicked(CharSequence query) {
        final String queryString = query.toString();
        mMetricsFeatureProvider.action(getContext(),
                MetricsProto.MetricsEvent.ACTION_CLICK_SETTINGS_SEARCH_SAVED_QUERY);
        mSearchView.setQuery(queryString, false /* submit */);
        onQueryTextChange(queryString);
    }

    public void onRemoveSavedQueryClicked(CharSequence title) {
        mSavedQueryController.removeQuery(title.toString());
    }

    private void restartLoaders() {
        mShowingSavedQuery = false;
        final LoaderManager loaderManager = getLoaderManager();
        mUnfinishedLoadersCount.set(NUM_QUERY_LOADERS);
        loaderManager.restartLoader(LOADER_ID_DATABASE, null /* args */, this /* callback */);
        loaderManager.restartLoader(LOADER_ID_INSTALLED_APPS, null /* args */, this /* callback */);
    }

    public String getQuery() {
        return mQuery;
    }

    public List<SearchResult> getSearchResults() {
        return mSearchAdapter.getSearchResults();
    }

    private void requery() {
        if (TextUtils.isEmpty(mQuery)) {
            return;
        }
        final String query = mQuery;
        mQuery = "";
        onQueryTextChange(query);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    SearchView makeSearchView(ActionBar actionBar, String query) {
        final SearchView searchView = new SearchView(actionBar.getThemedContext());
        searchView.setIconifiedByDefault(false);
        searchView.setQuery(query, false /* submitQuery */);
        searchView.setOnQueryTextListener(this);
        searchView.setTag(SEARCH_TAG, searchView);
        final LayoutParams lp =
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        searchView.setLayoutParams(lp);
        return searchView;
    }

    private void hideKeyboard() {
        final Activity activity = getActivity();
        if (activity != null) {
            View view = activity.getCurrentFocus();
            InputMethodManager imm = (InputMethodManager)
                    activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        if (mResultsRecyclerView != null) {
            mResultsRecyclerView.requestFocus();
        }
    }
}