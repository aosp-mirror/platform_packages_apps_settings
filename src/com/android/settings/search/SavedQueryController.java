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

import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;

import com.android.settings.overlay.FeatureFactory;

import java.util.List;

public class SavedQueryController implements LoaderManager.LoaderCallbacks {

    // TODO: make a generic background task manager to handle one-off tasks like this one.

    private static final int LOADER_ID_SAVE_QUERY_TASK = 0;
    private static final int LOADER_ID_REMOVE_QUERY_TASK = 1;
    private static final int LOADER_ID_SAVED_QUERIES = 2;
    private static final String ARG_QUERY = "remove_query";

    private final Context mContext;
    private final LoaderManager mLoaderManager;
    private final SearchFeatureProvider mSearchFeatureProvider;
    private final SearchResultsAdapter mResultAdapter;

    public SavedQueryController(Context context, LoaderManager loaderManager,
            SearchResultsAdapter resultsAdapter) {
        mContext = context;
        mLoaderManager = loaderManager;
        mResultAdapter = resultsAdapter;
        mSearchFeatureProvider = FeatureFactory.getFactory(context)
                .getSearchFeatureProvider();
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID_SAVE_QUERY_TASK:
                return new SavedQueryRecorder(mContext, args.getString(ARG_QUERY));
            case LOADER_ID_REMOVE_QUERY_TASK:
                return new SavedQueryRemover(mContext, args.getString(ARG_QUERY));
            case LOADER_ID_SAVED_QUERIES:
                return mSearchFeatureProvider.getSavedQueryLoader(mContext);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader loader, Object data) {
        switch (loader.getId()) {
            case LOADER_ID_REMOVE_QUERY_TASK:
                mLoaderManager.restartLoader(LOADER_ID_SAVED_QUERIES, null, this);
                break;
            case LOADER_ID_SAVED_QUERIES:
                mResultAdapter.displaySavedQuery((List<SearchResult>) data);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {

    }

    public void saveQuery(String query) {
        final Bundle args = new Bundle();
        args.putString(ARG_QUERY, query);
        mLoaderManager.restartLoader(LOADER_ID_SAVE_QUERY_TASK, args, this);
    }

    public void removeQuery(String query) {
        final Bundle args = new Bundle();
        args.putString(ARG_QUERY, query);
        mLoaderManager.restartLoader(LOADER_ID_REMOVE_QUERY_TASK, args, this);
    }

    public void loadSavedQueries() {
        mLoaderManager.restartLoader(LOADER_ID_SAVED_QUERIES, null, this);
    }
}
