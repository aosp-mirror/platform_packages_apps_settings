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

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.IntDef;
import android.support.annotation.MainThread;
import android.support.annotation.VisibleForTesting;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.settings.R;
import com.android.settings.search.ranking.SearchResultsRankerCallback;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class SearchResultsAdapter extends RecyclerView.Adapter<SearchViewHolder>
        implements SearchResultsRankerCallback {
    private static final String TAG = "SearchResultsAdapter";

    @VisibleForTesting
    static final String DB_RESULTS_LOADER_KEY = DatabaseResultLoader.class.getName();

    @VisibleForTesting
    static final String APP_RESULTS_LOADER_KEY = InstalledAppResultLoader.class.getName();
    @VisibleForTesting
    static final String ACCESSIBILITY_LOADER_KEY = AccessibilityServiceResultLoader.class.getName();
    @VisibleForTesting
    static final String INPUT_DEVICE_LOADER_KEY = InputDeviceResultLoader.class.getName();

    @VisibleForTesting
    static final int MSG_RANKING_TIMED_OUT = 1;

    private final SearchFragment mFragment;
    private final Context mContext;
    private final List<SearchResult> mSearchResults;
    private final List<SearchResult> mStaticallyRankedSearchResults;
    private Map<String, Set<? extends SearchResult>> mResultsMap;
    private final SearchFeatureProvider mSearchFeatureProvider;
    private List<Pair<String, Float>> mSearchRankingScores;
    private Handler mHandler;
    private boolean mSearchResultsLoaded;
    private boolean mSearchResultsUpdated;

    @IntDef({DISABLED, PENDING_RESULTS, SUCCEEDED, FAILED, TIMED_OUT})
    @Retention(RetentionPolicy.SOURCE)
    private @interface AsyncRankingState {}
    @VisibleForTesting
    static final int DISABLED = 0;
    @VisibleForTesting
    static final int PENDING_RESULTS = 1;
    @VisibleForTesting
    static final int SUCCEEDED = 2;
    @VisibleForTesting
    static final int FAILED = 3;
    @VisibleForTesting
    static final int TIMED_OUT = 4;
    private @AsyncRankingState int mAsyncRankingState;

    public SearchResultsAdapter(SearchFragment fragment,
            SearchFeatureProvider searchFeatureProvider) {
        mFragment = fragment;
        mContext = fragment.getContext().getApplicationContext();
        mSearchResults = new ArrayList<>();
        mResultsMap = new ArrayMap<>();
        mSearchRankingScores = new ArrayList<>();
        mStaticallyRankedSearchResults = new ArrayList<>();
        mSearchFeatureProvider = searchFeatureProvider;

        setHasStableIds(true);
    }

    @Override
    public SearchViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final Context context = parent.getContext();
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View view;
        switch (viewType) {
            case ResultPayload.PayloadType.INTENT:
                view = inflater.inflate(R.layout.search_intent_item, parent, false);
                return new IntentSearchViewHolder(view);
            case ResultPayload.PayloadType.INLINE_SWITCH:
                // TODO (b/62807132) replace layout InlineSwitchViewHolder and return an
                // InlineSwitchViewHolder.
                view = inflater.inflate(R.layout.search_intent_item, parent, false);
                return new IntentSearchViewHolder(view);
            case ResultPayload.PayloadType.INLINE_LIST:
                // TODO (b/62807132) build a inline-list view holder & layout.
                view = inflater.inflate(R.layout.search_intent_item, parent, false);
                return new IntentSearchViewHolder(view);
            case ResultPayload.PayloadType.SAVED_QUERY:
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

    @MainThread
    @Override
    public void onRankingScoresAvailable(List<Pair<String, Float>> searchRankingScores) {
        // Received the scores, stop the timeout timer.
        getHandler().removeMessages(MSG_RANKING_TIMED_OUT);
        if (mAsyncRankingState == PENDING_RESULTS) {
            mAsyncRankingState = SUCCEEDED;
            mSearchRankingScores.clear();
            mSearchRankingScores.addAll(searchRankingScores);
            if (canUpdateSearchResults()) {
                updateSearchResults();
            }
        } else {
            Log.w(TAG, "Ranking scores became available in invalid state: " + mAsyncRankingState);
        }
    }

    @MainThread
    @Override
    public void onRankingFailed() {
        if (mAsyncRankingState == PENDING_RESULTS) {
            mAsyncRankingState = FAILED;
            if (canUpdateSearchResults()) {
                updateSearchResults();
            }
        } else {
            Log.w(TAG, "Ranking scores failed in invalid states: " + mAsyncRankingState);
        }
    }

   /**
     * Store the results from each of the loaders to be merged when all loaders are finished.
     *
     * @param results         the results from the loader.
     * @param loaderClassName class name of the loader.
     */
    @MainThread
    public void addSearchResults(Set<? extends SearchResult> results, String loaderClassName) {
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
     * Notifies the adapter that all the unsorted results are loaded and now the ladapter can
     * proceed with ranking the results.
     */
    @MainThread
    public void notifyResultsLoaded() {
        mSearchResultsLoaded = true;
        // static ranking is skipped only if asyc ranking is already succeeded.
        if (mAsyncRankingState != SUCCEEDED) {
            doStaticRanking();
        }
        if (canUpdateSearchResults()) {
            updateSearchResults();
        }
    }

    public void clearResults() {
        mSearchResults.clear();
        mStaticallyRankedSearchResults.clear();
        mResultsMap.clear();
        notifyDataSetChanged();
    }

    @VisibleForTesting
    public List<SearchResult> getSearchResults() {
        return mSearchResults;
    }

    @MainThread
    public void initializeSearch(String query) {
        clearResults();
        mSearchResultsLoaded = false;
        mSearchResultsUpdated = false;
        if (mSearchFeatureProvider.isSmartSearchRankingEnabled(mContext)) {
            mAsyncRankingState = PENDING_RESULTS;
            mSearchFeatureProvider.cancelPendingSearchQuery(mContext);
            final Handler handler = getHandler();
            final long timeoutMs = mSearchFeatureProvider.smartSearchRankingTimeoutMs(mContext);
            handler.sendMessageDelayed(
                    handler.obtainMessage(MSG_RANKING_TIMED_OUT), timeoutMs);
            mSearchFeatureProvider.querySearchResults(mContext, query, this);
        } else {
            mAsyncRankingState = DISABLED;
        }
    }

    @AsyncRankingState int getAsyncRankingState() {
        return mAsyncRankingState;
    }

    /**
     * Merge the results from each of the loaders into one list for the adapter.
     * Prioritizes results from the local database over installed apps.
     */
    private void doStaticRanking() {
        List<? extends SearchResult> databaseResults =
                getSortedLoadedResults(DB_RESULTS_LOADER_KEY);
        List<? extends SearchResult> installedAppResults =
                getSortedLoadedResults(APP_RESULTS_LOADER_KEY);
        List<? extends SearchResult> accessibilityResults =
                getSortedLoadedResults(ACCESSIBILITY_LOADER_KEY);
        List<? extends SearchResult> inputDeviceResults =
                getSortedLoadedResults(INPUT_DEVICE_LOADER_KEY);

        int dbSize = databaseResults.size();
        int appSize = installedAppResults.size();
        int a11ySize = accessibilityResults.size();
        int inputDeviceSize = inputDeviceResults.size();
        int dbIndex = 0;
        int appIndex = 0;
        int a11yIndex = 0;
        int inputDeviceIndex = 0;
        int rank = SearchResult.TOP_RANK;

        // TODO: We need a helper method to do k-way merge.
        mStaticallyRankedSearchResults.clear();
        while (rank <= SearchResult.BOTTOM_RANK) {
            while ((dbIndex < dbSize) && (databaseResults.get(dbIndex).rank == rank)) {
                mStaticallyRankedSearchResults.add(databaseResults.get(dbIndex++));
            }
            while ((appIndex < appSize) && (installedAppResults.get(appIndex).rank == rank)) {
                mStaticallyRankedSearchResults.add(installedAppResults.get(appIndex++));
            }
            while ((a11yIndex < a11ySize) && (accessibilityResults.get(a11yIndex).rank == rank)) {
                mStaticallyRankedSearchResults.add(accessibilityResults.get(a11yIndex++));
            }
            while (inputDeviceIndex < inputDeviceSize
                    && inputDeviceResults.get(inputDeviceIndex).rank == rank) {
                mStaticallyRankedSearchResults.add(inputDeviceResults.get(inputDeviceIndex++));
            }
            rank++;
        }

        while (dbIndex < dbSize) {
            mStaticallyRankedSearchResults.add(databaseResults.get(dbIndex++));
        }
        while (appIndex < appSize) {
            mStaticallyRankedSearchResults.add(installedAppResults.get(appIndex++));
        }
        while(a11yIndex < a11ySize) {
            mStaticallyRankedSearchResults.add(accessibilityResults.get(a11yIndex++));
        }
        while (inputDeviceIndex < inputDeviceSize) {
            mStaticallyRankedSearchResults.add(inputDeviceResults.get(inputDeviceIndex++));
        }
    }

    private void updateSearchResults() {
        switch (mAsyncRankingState) {
            case PENDING_RESULTS:
                break;
            case DISABLED:
            case FAILED:
            case TIMED_OUT:
                // When DISABLED or FAILED or TIMED_OUT, we use static ranking results.
                postSearchResults(mStaticallyRankedSearchResults, false);
                break;
            case SUCCEEDED:
                postSearchResults(doAsyncRanking(), true);
                break;
        }
    }

    private boolean canUpdateSearchResults() {
        // Results are not updated yet and db results are loaded and we are not waiting on async
        // ranking scores.
        return !mSearchResultsUpdated
                && mSearchResultsLoaded
                && mAsyncRankingState != PENDING_RESULTS;
    }

    @VisibleForTesting
    List<SearchResult> doAsyncRanking() {
        Set<? extends SearchResult> databaseResults =
                getUnsortedLoadedResults(DB_RESULTS_LOADER_KEY);
        List<? extends SearchResult> installedAppResults =
                getSortedLoadedResults(APP_RESULTS_LOADER_KEY);
        List<? extends SearchResult> accessibilityResults =
                getSortedLoadedResults(ACCESSIBILITY_LOADER_KEY);
        List<? extends SearchResult> inputDeviceResults =
                getSortedLoadedResults(INPUT_DEVICE_LOADER_KEY);
        int dbSize = databaseResults.size();
        int appSize = installedAppResults.size();
        int a11ySize = accessibilityResults.size();
        int inputDeviceSize = inputDeviceResults.size();

        final List<SearchResult> asyncRankingResults = new ArrayList<>(
                dbSize + appSize + a11ySize + inputDeviceSize);
        TreeSet<SearchResult> dbResultsSortedByScores = new TreeSet<>(
                new Comparator<SearchResult>() {
                    @Override
                    public int compare(SearchResult o1, SearchResult o2) {
                        float score1 = getRankingScoreByStableId(o1.stableId);
                        float score2 = getRankingScoreByStableId(o2.stableId);
                        if (score1 > score2) {
                            return -1;
                        } else if (score1 == score2) {
                            return 0;
                        } else {
                            return 1;
                        }
                    }
                });
        dbResultsSortedByScores.addAll(databaseResults);
        asyncRankingResults.addAll(dbResultsSortedByScores);
        // Other results are not ranked by async ranking and appended at the end of the list.
        asyncRankingResults.addAll(installedAppResults);
        asyncRankingResults.addAll(accessibilityResults);
        asyncRankingResults.addAll(inputDeviceResults);
        return asyncRankingResults;
    }

    @VisibleForTesting
    Set<? extends SearchResult> getUnsortedLoadedResults(String loaderKey) {
        return mResultsMap.containsKey(loaderKey) ? mResultsMap.get(loaderKey) : new HashSet<>();
    }

    @VisibleForTesting
    List<? extends SearchResult> getSortedLoadedResults(String loaderKey) {
        List<? extends SearchResult> sortedLoadedResults =
                new ArrayList<>(getUnsortedLoadedResults(loaderKey));
        Collections.sort(sortedLoadedResults);
        return sortedLoadedResults;
    }

    /**
     * Looks up ranking score for stableId
     * @param stableId String of stableId
     * @return the ranking score corresponding to the given stableId. If there is no score
     * available for this stableId, -Float.MAX_VALUE is returned.
     */
    @VisibleForTesting
    Float getRankingScoreByStableId(int stableId) {
        for (Pair<String, Float> rankingScore : mSearchRankingScores) {
            if (Integer.toString(stableId).compareTo(rankingScore.first) == 0) {
                return rankingScore.second;
            }
        }
        // If stableId not found in the list, we assign the minimum score so it will appear at
        // the end of the list.
        Log.w(TAG, "stableId " + stableId + " was not in the ranking scores.");
        return -Float.MAX_VALUE;
    }

    @VisibleForTesting
    Handler getHandler() {
        if (mHandler == null) {
            mHandler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    if (msg.what == MSG_RANKING_TIMED_OUT) {
                        mSearchFeatureProvider.cancelPendingSearchQuery(mContext);
                        if (mAsyncRankingState == PENDING_RESULTS) {
                            mAsyncRankingState = TIMED_OUT;
                            if (canUpdateSearchResults()) {
                                updateSearchResults();
                            }
                        } else {
                            Log.w(TAG, "Ranking scores timed out in invalid state: " +
                                    mAsyncRankingState);
                        }
                    }
                }
            };
        }
        return mHandler;
    }

    @VisibleForTesting
    public void postSearchResults(List<SearchResult> newSearchResults, boolean detectMoves) {
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                new SearchResultDiffCallback(mSearchResults, newSearchResults), detectMoves);
        mSearchResults.clear();
        mSearchResults.addAll(newSearchResults);
        diffResult.dispatchUpdatesTo(this);
        mFragment.onSearchResultsDisplayed(mSearchResults.size());
        mSearchResultsUpdated = true;
    }
}
