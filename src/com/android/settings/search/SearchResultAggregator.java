package com.android.settings.search;

import android.annotation.NonNull;
import android.content.Context;
import android.util.Log;
import android.util.SparseArray;

import com.android.settings.overlay.FeatureFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Collects the sorted list of all setting search results.
 *
 * TODO (b/64939692) Convert the timing logs to metrics
 */
public class SearchResultAggregator {

    private static final String TAG = "SearchResultAggregator";

    /**
     * Timeout for first task. Allows for longer delay.
     */
    private static final long LONG_CHECK_TASK_TIMEOUT_MS = 500;

    /**
     * Timeout for subsequent tasks to allow for fast returning tasks.
     */
    private static final long SHORT_CHECK_TASK_TIMEOUT_MS = 150;

    private static SearchResultAggregator sResultAggregator;

    // TODO (b/33577327) Merge the other loaders into a single dynamic loader
    static final class ResultLoaderId {
        static final int STATIC_RESULTS = 1;
        static final int INSTALLED_RESULTS = 2;
        static final int INPUT_RESULTS = 3;
        static final int ACCESSIBILITY_RESULTS = 4;
    }

    private SearchResultAggregator() {
    }

    public static SearchResultAggregator getInstance() {
        if (sResultAggregator == null) {
            sResultAggregator = new SearchResultAggregator();
        }

        return sResultAggregator;
    }

    @NonNull
    public synchronized List<? extends SearchResult> fetchResults(Context context, String query) {
        SearchFeatureProvider mFeatureProvider = FeatureFactory.getFactory(
                context).getSearchFeatureProvider();
        ExecutorService executorService = mFeatureProvider.getExecutorService();

        final DatabaseResultLoader staticResultsTask =
                mFeatureProvider.getStaticSearchResultTask(context, query);
        final InstalledAppResultLoader installedAppTask =
                mFeatureProvider.getInstalledAppSearchTask(context, query);
        final InputDeviceResultLoader inputDevicesTask =
                mFeatureProvider.getInputDeviceResultTask(context, query);
        final AccessibilityServiceResultLoader accessibilityServicesTask =
                mFeatureProvider.getAccessibilityServiceResultTask(context,
                        query);

        executorService.execute(staticResultsTask);
        executorService.execute(installedAppTask);
        executorService.execute(inputDevicesTask);
        executorService.execute(accessibilityServicesTask);

        SparseArray<List<? extends SearchResult>> resultsArray = new SparseArray<>();
        List<? extends SearchResult> EMPTY_LIST = new ArrayList<>();

        long allTasksStart = System.currentTimeMillis();
        try {
            resultsArray.put(ResultLoaderId.INPUT_RESULTS,
                    inputDevicesTask.get(SHORT_CHECK_TASK_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            Log.d(TAG, "Could not retrieve input devices results in time: " + e);
            resultsArray.put(ResultLoaderId.INPUT_RESULTS, EMPTY_LIST);
        }

        try {
            resultsArray.put(ResultLoaderId.ACCESSIBILITY_RESULTS,
                    accessibilityServicesTask.get(SHORT_CHECK_TASK_TIMEOUT_MS,
                            TimeUnit.MILLISECONDS));
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            Log.d(TAG, "Could not retrieve accessibility results in time: " + e);
            resultsArray.put(ResultLoaderId.ACCESSIBILITY_RESULTS, EMPTY_LIST);
        }

        try {
            resultsArray.put(ResultLoaderId.STATIC_RESULTS,
                    staticResultsTask.get(LONG_CHECK_TASK_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            Log.d(TAG, "Could not retrieve static results: " + e);
            resultsArray.put(ResultLoaderId.STATIC_RESULTS, EMPTY_LIST);
        }

        try {
            resultsArray.put(ResultLoaderId.INSTALLED_RESULTS,
                    installedAppTask.get(SHORT_CHECK_TASK_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            Log.d(TAG, "Could not retrieve installed app results in time: " + e);

            resultsArray.put(ResultLoaderId.INSTALLED_RESULTS, EMPTY_LIST);
        }

        long mergeStartTime = System.currentTimeMillis();
        Log.i(TAG, "Total result loader time: " + (mergeStartTime - allTasksStart));
        List<? extends SearchResult> mergedResults = mergeSearchResults(resultsArray);
        Log.i(TAG, "Total merge time: " + (System.currentTimeMillis() - mergeStartTime));
        Log.i(TAG, "Total aggregator time: " + (System.currentTimeMillis() - allTasksStart));

        return mergedResults;
    }

    // TODO (b/68255021) scale the dynamic search results ranks and do a k-way merge
    private List<? extends SearchResult> mergeSearchResults(
            SparseArray<List<? extends SearchResult>> resultsArray) {
        List<? extends SearchResult> staticResults = resultsArray.get(
                ResultLoaderId.STATIC_RESULTS);
        List<? extends SearchResult> installedAppResults = resultsArray.get(
                ResultLoaderId.INSTALLED_RESULTS);
        List<? extends SearchResult> accessibilityResults = resultsArray.get(
                ResultLoaderId.ACCESSIBILITY_RESULTS);
        List<? extends SearchResult> inputDeviceResults = resultsArray.get(
                ResultLoaderId.INPUT_RESULTS);
        List<SearchResult> searchResults;

        int staticSize = staticResults.size();
        int appSize = installedAppResults.size();
        int a11ySize = accessibilityResults.size();
        int inputDeviceSize = inputDeviceResults.size();
        int appIndex = 0;
        int a11yIndex = 0;
        int inputDeviceIndex = 0;
        int rank = SearchResult.TOP_RANK;

        // TODO: We need a helper method to do k-way merge.
        searchResults = new ArrayList<>(staticSize + appSize + a11ySize + inputDeviceSize);
        searchResults.addAll(resultsArray.get(ResultLoaderId.STATIC_RESULTS));

        while (rank <= SearchResult.BOTTOM_RANK) {
            while ((appIndex < appSize) && (installedAppResults.get(appIndex).rank == rank)) {
                searchResults.add(installedAppResults.get(appIndex++));
            }
            while ((a11yIndex < a11ySize) && (accessibilityResults.get(a11yIndex).rank == rank)) {
                searchResults.add(accessibilityResults.get(a11yIndex++));
            }
            while (inputDeviceIndex < inputDeviceSize
                    && inputDeviceResults.get(inputDeviceIndex).rank == rank) {
                searchResults.add(inputDeviceResults.get(inputDeviceIndex++));
            }
            rank++;
        }

        while (appIndex < appSize) {
            searchResults.add(installedAppResults.get(appIndex++));
        }
        while (a11yIndex < a11ySize) {
            searchResults.add(accessibilityResults.get(a11yIndex++));
        }
        while (inputDeviceIndex < inputDeviceSize) {
            searchResults.add(inputDeviceResults.get(inputDeviceIndex++));
        }

        return searchResults;
    }
}
