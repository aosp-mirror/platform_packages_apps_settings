/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.dashboard;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.dashboard.conditional.Condition;
import com.android.settings.dashboard.conditional.ConditionManager;
import com.android.settings.dashboard.conditional.ConditionManager.ConditionListener;
import com.android.settings.dashboard.conditional.FocusRecyclerView;
import com.android.settings.dashboard.conditional.FocusRecyclerView.FocusListener;
import com.android.settings.dashboard.suggestions.SuggestionDismissController;
import com.android.settings.dashboard.suggestions.SuggestionFeatureProvider;
import com.android.settings.dashboard.suggestions.SuggestionsChecks;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.ActionBarShadowController;
import com.android.settingslib.drawer.CategoryKey;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.SettingsDrawerActivity;
import com.android.settingslib.drawer.SettingsDrawerActivity.CategoryListener;
import com.android.settingslib.drawer.Tile;
import com.android.settingslib.suggestions.SuggestionList;
import com.android.settingslib.suggestions.SuggestionParser;

import java.util.ArrayList;
import java.util.List;

public class DashboardSummary extends InstrumentedFragment
        implements CategoryListener, ConditionListener,
        FocusListener, SuggestionDismissController.Callback {
    public static final boolean DEBUG = false;
    private static final boolean DEBUG_TIMING = false;
    private static final int MAX_WAIT_MILLIS = 700;
    private static final String TAG = "DashboardSummary";


    private static final String EXTRA_SCROLL_POSITION = "scroll_position";

    private final Handler mHandler = new Handler();

    private FocusRecyclerView mDashboard;
    private DashboardAdapter mAdapter;
    private SummaryLoader mSummaryLoader;
    private ConditionManager mConditionManager;
    private SuggestionParser mSuggestionParser;
    private LinearLayoutManager mLayoutManager;
    private SuggestionsChecks mSuggestionsChecks;
    private DashboardFeatureProvider mDashboardFeatureProvider;
    private SuggestionFeatureProvider mSuggestionFeatureProvider;
    private boolean isOnCategoriesChangedCalled;
    private boolean mOnConditionsChangedCalled;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.DASHBOARD_SUMMARY;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        long startTime = System.currentTimeMillis();
        super.onCreate(savedInstanceState);
        final Activity activity = getActivity();
        mDashboardFeatureProvider = FeatureFactory.getFactory(activity)
                .getDashboardFeatureProvider(activity);
        mSuggestionFeatureProvider = FeatureFactory.getFactory(activity)
                .getSuggestionFeatureProvider(activity);

        mSummaryLoader = new SummaryLoader(activity, CategoryKey.CATEGORY_HOMEPAGE);

        mConditionManager = ConditionManager.get(activity, false);
        getLifecycle().addObserver(mConditionManager);
        if (mSuggestionFeatureProvider.isSuggestionEnabled(activity)) {
            mSuggestionParser = new SuggestionParser(activity,
                    mSuggestionFeatureProvider.getSharedPrefs(activity), R.xml.suggestion_ordering);
            mSuggestionsChecks = new SuggestionsChecks(getContext());
        }
        if (DEBUG_TIMING) {
            Log.d(TAG, "onCreate took " + (System.currentTimeMillis() - startTime)
                    + " ms");
        }
    }

    @Override
    public void onDestroy() {
        mSummaryLoader.release();
        super.onDestroy();
    }

    @Override
    public void onResume() {
        long startTime = System.currentTimeMillis();
        super.onResume();

        ((SettingsDrawerActivity) getActivity()).addCategoryListener(this);
        mSummaryLoader.setListening(true);
        final int metricsCategory = getMetricsCategory();
        for (Condition c : mConditionManager.getConditions()) {
            if (c.shouldShow()) {
                mMetricsFeatureProvider.visible(getContext(), metricsCategory,
                        c.getMetricsConstant());
            }
        }
        if (DEBUG_TIMING) {
            Log.d(TAG, "onResume took " + (System.currentTimeMillis() - startTime) + " ms");
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        ((SettingsDrawerActivity) getActivity()).remCategoryListener(this);
        mSummaryLoader.setListening(false);
        for (Condition c : mConditionManager.getConditions()) {
            if (c.shouldShow()) {
                mMetricsFeatureProvider.hidden(getContext(), c.getMetricsConstant());
            }
        }
        if (!getActivity().isChangingConfigurations()) {
            mAdapter.onPause();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        long startTime = System.currentTimeMillis();
        if (hasWindowFocus) {
            Log.d(TAG, "Listening for condition changes");
            mConditionManager.addListener(this);
            Log.d(TAG, "conditions refreshed");
            mConditionManager.refreshAll();
        } else {
            Log.d(TAG, "Stopped listening for condition changes");
            mConditionManager.remListener(this);
        }
        if (DEBUG_TIMING) {
            Log.d(TAG, "onWindowFocusChanged took "
                    + (System.currentTimeMillis() - startTime) + " ms");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dashboard, container, false);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mLayoutManager == null) return;
        outState.putInt(EXTRA_SCROLL_POSITION, mLayoutManager.findFirstVisibleItemPosition());
        if (mAdapter != null) {
            mAdapter.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        long startTime = System.currentTimeMillis();
        mDashboard = view.findViewById(R.id.dashboard_container);
        mLayoutManager = new LinearLayoutManager(getContext());
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        if (bundle != null) {
            int scrollPosition = bundle.getInt(EXTRA_SCROLL_POSITION);
            mLayoutManager.scrollToPosition(scrollPosition);
        }
        mDashboard.setLayoutManager(mLayoutManager);
        mDashboard.setHasFixedSize(true);
        mDashboard.setListener(this);
        mAdapter = new DashboardAdapter(getContext(), bundle, mConditionManager.getConditions(),
            mSuggestionParser, this /* SuggestionDismissController.Callback */);
        mDashboard.setAdapter(mAdapter);
        mDashboard.setItemAnimator(new DashboardItemAnimator());
        mSummaryLoader.setSummaryConsumer(mAdapter);
        ActionBarShadowController.attachToRecyclerView(
                getActivity().findViewById(R.id.search_bar_container), getLifecycle(), mDashboard);

        if (DEBUG_TIMING) {
            Log.d(TAG, "onViewCreated took "
                    + (System.currentTimeMillis() - startTime) + " ms");
        }
        rebuildUI();
    }

    @VisibleForTesting
    void rebuildUI() {
        if (!mSuggestionFeatureProvider.isSuggestionEnabled(getContext())) {
            Log.d(TAG, "Suggestion feature is disabled, skipping suggestion entirely");
            updateCategoryAndSuggestion(null /* tiles */);
        } else {
            new SuggestionLoader().execute();
            // Set categories on their own if loading suggestions takes too long.
            mHandler.postDelayed(() -> {
                updateCategoryAndSuggestion(null /* tiles */);
            }, MAX_WAIT_MILLIS);
        }
    }

    @Override
    public void onCategoriesChanged() {
        // Bypass rebuildUI() on the first call of onCategoriesChanged, since rebuildUI() happens
        // in onViewCreated as well when app starts. But, on the subsequent calls we need to
        // rebuildUI() because there might be some changes to suggestions and categories.
        if (isOnCategoriesChangedCalled) {
            rebuildUI();
        }
        isOnCategoriesChangedCalled = true;
    }

    @Override
    public void onConditionsChanged() {
        Log.d(TAG, "onConditionsChanged");
        // Bypass refreshing the conditions on the first call of onConditionsChanged.
        // onConditionsChanged is called immediately everytime we start listening to the conditions
        // change when we gain window focus. Since the conditions are passed to the adapter's
        // constructor when we create the view, the first handling is not necessary.
        // But, on the subsequent calls we need to handle it because there might be real changes to
        // conditions.
        if (mOnConditionsChangedCalled) {
            final boolean scrollToTop =
                    mLayoutManager.findFirstCompletelyVisibleItemPosition() <= 1;
            mAdapter.setConditions(mConditionManager.getConditions());
            if (scrollToTop) {
                mDashboard.scrollToPosition(0);
            }
        } else {
            mOnConditionsChangedCalled = true;
        }
    }

    @Override
    public Tile getSuggestionForPosition(int position) {
        return mAdapter.getSuggestion(position);
    }

    @Override
    public void onSuggestionDismissed(Tile suggestion) {
        mAdapter.onSuggestionDismissed(suggestion);
    }

    private class SuggestionLoader extends AsyncTask<Void, Void, List<Tile>> {
        @Override
        protected List<Tile> doInBackground(Void... params) {
            final Context context = getContext();
            boolean isSmartSuggestionEnabled =
                    mSuggestionFeatureProvider.isSmartSuggestionEnabled(context);
            final SuggestionList sl = mSuggestionParser.getSuggestions(isSmartSuggestionEnabled);
            final List<Tile> suggestions = sl.getSuggestions();

            if (isSmartSuggestionEnabled) {
                List<String> suggestionIds = new ArrayList<>(suggestions.size());
                for (Tile suggestion : suggestions) {
                    suggestionIds.add(mSuggestionFeatureProvider.getSuggestionIdentifier(
                            context, suggestion));
                }
                // TODO: create a Suggestion class to maintain the id and other info
                mSuggestionFeatureProvider.rankSuggestions(suggestions, suggestionIds);
            }
            for (int i = 0; i < suggestions.size(); i++) {
                Tile suggestion = suggestions.get(i);
                if (mSuggestionsChecks.isSuggestionComplete(suggestion)) {
                    suggestions.remove(i--);
                }
            }
            if (sl.isExclusiveSuggestionCategory()) {
                mSuggestionFeatureProvider.filterExclusiveSuggestions(suggestions);
            }
            return suggestions;
        }

        @Override
        protected void onPostExecute(List<Tile> tiles) {
            // tell handler that suggestions were loaded quickly enough
            mHandler.removeCallbacksAndMessages(null);
            updateCategoryAndSuggestion(tiles);
        }
    }

    @VisibleForTesting
    void updateCategoryAndSuggestion(List<Tile> suggestions) {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        final DashboardCategory category = mDashboardFeatureProvider.getTilesForCategory(
                CategoryKey.CATEGORY_HOMEPAGE);
        mSummaryLoader.updateSummaryToCache(category);
        if (suggestions != null) {
            mAdapter.setCategoriesAndSuggestions(category, suggestions);
        } else {
            mAdapter.setCategory(category);
        }
    }

}
