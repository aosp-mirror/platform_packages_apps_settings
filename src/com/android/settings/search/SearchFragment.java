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

import android.app.Activity;
import android.app.LoaderManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toolbar;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.ActionBarShadowController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
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
        LoaderManager.LoaderCallbacks<Set<? extends SearchResult>>, IndexingCallback {
    private static final String TAG = "SearchFragment";

    // State values
    private static final String STATE_QUERY = "state_query";
    private static final String STATE_SHOWING_SAVED_QUERY = "state_showing_saved_query";
    private static final String STATE_NEVER_ENTERED_QUERY = "state_never_entered_query";
    private static final String STATE_RESULT_CLICK_COUNT = "state_result_click_count";

    static final class SearchLoaderId {
        // Search Query IDs
        public static final int DATABASE = 1;
        public static final int INSTALLED_APPS = 2;
        public static final int ACCESSIBILITY_SERVICES = 3;
        public static final int INPUT_DEVICES = 4;

        // Saved Query IDs
        public static final int SAVE_QUERY_TASK = 5;
        public static final int REMOVE_QUERY_TASK = 6;
        public static final int SAVED_QUERIES = 7;
    }


    private static final int NUM_QUERY_LOADERS = 4;

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

    @VisibleForTesting
    SearchFeatureProvider mSearchFeatureProvider;

    @VisibleForTesting
    SearchResultsAdapter mSearchAdapter;

    @VisibleForTesting
    RecyclerView mResultsRecyclerView;
    @VisibleForTesting
    SearchView mSearchView;
    @VisibleForTesting
    LinearLayout mNoResultsView;

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
        return MetricsEvent.DASHBOARD_SEARCH_RESULTS;
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
        long startTime = System.currentTimeMillis();
        setHasOptionsMenu(true);

        final LoaderManager loaderManager = getLoaderManager();
        mSearchAdapter = new SearchResultsAdapter(this, mSearchFeatureProvider);
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
        // Run the Index update only if we have some space
        if (!Utils.isLowStorage(activity)) {
            mSearchFeatureProvider.updateIndexAsync(activity, this /* indexingCallback */);
        } else {
            Log.w(TAG, "Cannot update the Indexer as we are running low on storage space!");
        }
        if (SettingsSearchIndexablesProvider.DEBUG) {
            Log.d(TAG, "onCreate spent " + (System.currentTimeMillis() - startTime) + " ms");
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        mSavedQueryController.buildMenuItem(menu);
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

        Toolbar toolbar = view.findViewById(R.id.search_toolbar);
        getActivity().setActionBar(toolbar);
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);

        mSearchView = toolbar.findViewById(R.id.search_view);
        mSearchView.setQuery(mQuery, false /* submitQuery */);
        mSearchView.setOnQueryTextListener(this);
        mSearchView.requestFocus();

        // Updating internal views inside SearchView was the easiest way to get this too look right.
        // Instead of grabbing the TextView directly, we grab it as a view and do an instanceof
        // check. This ensures if we return, say, a LinearLayout in the tests, they won't fail.
        View searchText = mSearchView.findViewById(com.android.internal.R.id.search_src_text);
        if (searchText instanceof TextView) {
            TextView searchTextView = (TextView) searchText;
            searchTextView.setTextColor(getContext().getColorStateList(
                    com.android.internal.R.color.text_color_primary));
            searchTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimension(R.dimen.search_bar_text_size));

        }
        View editFrame = mSearchView.findViewById(com.android.internal.R.id.search_edit_frame);
        if (editFrame != null) {
            ViewGroup.MarginLayoutParams params =
                    (ViewGroup.MarginLayoutParams) editFrame.getLayoutParams();
            params.setMarginStart(0);
            editFrame.setLayoutParams(params);
        }
        ActionBarShadowController.attachToRecyclerView(
                view.findViewById(R.id.search_bar_container), getLifecycle(), mResultsRecyclerView);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Context appContext = getContext().getApplicationContext();
        if (mSearchFeatureProvider.isSmartSearchRankingEnabled(appContext)) {
            mSearchFeatureProvider.searchRankingWarmup(appContext);
        }
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
                        MetricsEvent.ACTION_LEAVE_SEARCH_RESULT_WITHOUT_QUERY);
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
        if (mQuery != null
                && mNoResultsView.getVisibility() == View.VISIBLE
                && query.length() < mQuery.length()) {
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
            loaderManager.destroyLoader(SearchLoaderId.DATABASE);
            loaderManager.destroyLoader(SearchLoaderId.INSTALLED_APPS);
            loaderManager.destroyLoader(SearchLoaderId.ACCESSIBILITY_SERVICES);
            loaderManager.destroyLoader(SearchLoaderId.INPUT_DEVICES);
            mShowingSavedQuery = true;
            mSavedQueryController.loadSavedQueries();
            mSearchFeatureProvider.hideFeedbackButton();
        } else {
            mSearchAdapter.initializeSearch(mQuery);
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
    public Loader<Set<? extends SearchResult>> onCreateLoader(int id, Bundle args) {
        final Activity activity = getActivity();

        switch (id) {
            case SearchLoaderId.DATABASE:
                return mSearchFeatureProvider.getDatabaseSearchLoader(activity, mQuery);
            case SearchLoaderId.INSTALLED_APPS:
                return mSearchFeatureProvider.getInstalledAppSearchLoader(activity, mQuery);
            case SearchLoaderId.ACCESSIBILITY_SERVICES:
                return mSearchFeatureProvider.getAccessibilityServiceResultLoader(activity, mQuery);
            case SearchLoaderId.INPUT_DEVICES:
                return mSearchFeatureProvider.getInputDeviceResultLoader(activity, mQuery);
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Set<? extends SearchResult>> loader,
            Set<? extends SearchResult> data) {
        mSearchAdapter.addSearchResults(data, loader.getClass().getName());
        if (mUnfinishedLoadersCount.decrementAndGet() != 0) {
            return;
        }

        mSearchAdapter.notifyResultsLoaded();
    }

    @Override
    public void onLoaderReset(Loader<Set<? extends SearchResult>> loader) {
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
            loaderManager.initLoader(SearchLoaderId.DATABASE, null /* args */, this /* callback */);
            loaderManager.initLoader(
                    SearchLoaderId.INSTALLED_APPS, null /* args */, this /* callback */);
            loaderManager.initLoader(
                    SearchLoaderId.ACCESSIBILITY_SERVICES, null /* args */, this /* callback */);
            loaderManager.initLoader(
                    SearchLoaderId.INPUT_DEVICES, null /* args */, this /* callback */);
        }

        requery();
    }

    public void onSearchResultClicked(SearchViewHolder resultViewHolder, SearchResult result,
            Pair<Integer, Object>... logTaggedData) {
        logSearchResultClicked(resultViewHolder, result, logTaggedData);
        mSearchFeatureProvider.searchResultClicked(getContext(), mQuery, result);
        mSavedQueryController.saveQuery(mQuery);
        mResultClickCount++;
    }

    public void onSearchResultsDisplayed(int resultCount) {
        if (resultCount == 0) {
            mNoResultsView.setVisibility(View.VISIBLE);
            mMetricsFeatureProvider.visible(getContext(), getMetricsCategory(),
                    MetricsEvent.SETTINGS_SEARCH_NO_RESULT);
        } else {
            mNoResultsView.setVisibility(View.GONE);
            mResultsRecyclerView.scrollToPosition(0);
        }
        mSearchFeatureProvider.showFeedbackButton(this, getView());
    }

    public void onSavedQueryClicked(CharSequence query) {
        final String queryString = query.toString();
        mMetricsFeatureProvider.action(getContext(),
                MetricsEvent.ACTION_CLICK_SETTINGS_SEARCH_SAVED_QUERY);
        mSearchView.setQuery(queryString, false /* submit */);
        onQueryTextChange(queryString);
    }

    private void restartLoaders() {
        mShowingSavedQuery = false;
        final LoaderManager loaderManager = getLoaderManager();
        mUnfinishedLoadersCount.set(NUM_QUERY_LOADERS);
        loaderManager.restartLoader(
                SearchLoaderId.DATABASE, null /* args */, this /* callback */);
        loaderManager.restartLoader(
                SearchLoaderId.INSTALLED_APPS, null /* args */, this /* callback */);
        loaderManager.restartLoader(
                SearchLoaderId.ACCESSIBILITY_SERVICES, null /* args */, this /* callback */);
        loaderManager.restartLoader(
                SearchLoaderId.INPUT_DEVICES, null /* args */, this /* callback */);
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

    private void logSearchResultClicked(SearchViewHolder resultViewHolder, SearchResult result,
            Pair<Integer, Object>... logTaggedData) {
        final Intent intent = result.payload.getIntent();
        if (intent == null) {
            Log.w(TAG, "Skipped logging click on search result because of null intent, which can " +
                    "happen on saved query results.");
            return;
        }
        final ComponentName cn = intent.getComponent();
        String resultName = intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT);
        if (TextUtils.isEmpty(resultName) && cn != null) {
            resultName = cn.flattenToString();
        }
        final List<Pair<Integer, Object>> taggedData = new ArrayList<>();
        if (logTaggedData != null) {
            taggedData.addAll(Arrays.asList(logTaggedData));
        }
        taggedData.add(Pair.create(
                MetricsEvent.FIELD_SETTINGS_SEARCH_RESULT_COUNT,
                mSearchAdapter.getItemCount()));
        taggedData.add(Pair.create(
                MetricsEvent.FIELD_SETTINGS_SEARCH_RESULT_RANK,
                resultViewHolder.getAdapterPosition()));
        taggedData.add(Pair.create(
                MetricsEvent.FIELD_SETTINGS_SEARCH_RESULT_ASYNC_RANKING_STATE,
                mSearchAdapter.getAsyncRankingState()));
        taggedData.add(Pair.create(
                MetricsEvent.FIELD_SETTINGS_SEARCH_QUERY_LENGTH,
                TextUtils.isEmpty(mQuery) ? 0 : mQuery.length()));

        mMetricsFeatureProvider.action(getContext(),
                resultViewHolder.getClickActionMetricName(),
                resultName,
                taggedData.toArray(new Pair[0]));
    }
}
