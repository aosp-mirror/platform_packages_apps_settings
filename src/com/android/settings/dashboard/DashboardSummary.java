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
import android.app.LoaderManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.service.settings.suggestions.Suggestion;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.recyclerview.widget.LinearLayoutManager;
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
import com.android.settings.dashboard.suggestions.SuggestionFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.ActionBarShadowController;
import com.android.settingslib.drawer.CategoryKey;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.SettingsDrawerActivity;
import com.android.settingslib.drawer.SettingsDrawerActivity.CategoryListener;
import com.android.settingslib.suggestions.SuggestionControllerMixin;
import com.android.settingslib.utils.ThreadUtils;

import java.util.List;

public class DashboardSummary extends InstrumentedFragment
        implements CategoryListener, ConditionListener,
        FocusListener, SuggestionControllerMixin.SuggestionControllerHost {
    public static final boolean DEBUG = false;
    private static final boolean DEBUG_TIMING = false;
    private static final int MAX_WAIT_MILLIS = 3000;
    private static final String TAG = "DashboardSummary";

    private static final String STATE_SCROLL_POSITION = "scroll_position";
    private static final String STATE_CATEGORIES_CHANGE_CALLED = "categories_change_called";

    private final Handler mHandler = new Handler();

    private FocusRecyclerView mDashboard;
    private DashboardAdapter mAdapter;
    private SummaryLoader mSummaryLoader;
    private ConditionManager mConditionManager;
    private LinearLayoutManager mLayoutManager;
    private SuggestionControllerMixin mSuggestionControllerMixin;
    private DashboardFeatureProvider mDashboardFeatureProvider;
    @VisibleForTesting
    boolean mIsOnCategoriesChangedCalled;
    private boolean mOnConditionsChangedCalled;

    private DashboardCategory mStagingCategory;
    private List<Suggestion> mStagingSuggestions;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.DASHBOARD_SUMMARY;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(TAG, "Creating SuggestionControllerMixin");
        final SuggestionFeatureProvider suggestionFeatureProvider = FeatureFactory
                .getFactory(context)
                .getSuggestionFeatureProvider(context);
        if (suggestionFeatureProvider.isSuggestionEnabled(context)) {
            mSuggestionControllerMixin = new SuggestionControllerMixin(context, this /* host */,
                    getLifecycle(), suggestionFeatureProvider
                    .getSuggestionServiceComponent());
        }
    }

    @Override
    public LoaderManager getLoaderManager() {
        if (!isAdded()) {
            return null;
        }
        return super.getLoaderManager();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        long startTime = System.currentTimeMillis();
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Starting DashboardSummary");
        final Activity activity = getActivity();
        mDashboardFeatureProvider = FeatureFactory.getFactory(activity)
                .getDashboardFeatureProvider(activity);

        mSummaryLoader = new SummaryLoader(activity, CategoryKey.CATEGORY_HOMEPAGE);

        mConditionManager = ConditionManager.get(activity, false);
        getLifecycle().addObserver(mConditionManager);
        if (savedInstanceState != null) {
            mIsOnCategoriesChangedCalled =
                    savedInstanceState.getBoolean(STATE_CATEGORIES_CHANGE_CALLED);
        }
        if (DEBUG_TIMING) {
            Log.d(TAG, "onCreate took " + (System.currentTimeMillis() - startTime) + " ms");
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mLayoutManager == null) {
            return;
        }
        outState.putBoolean(STATE_CATEGORIES_CHANGE_CALLED, mIsOnCategoriesChangedCalled);
        outState.putInt(STATE_SCROLL_POSITION, mLayoutManager.findFirstVisibleItemPosition());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
        long startTime = System.currentTimeMillis();
        final View root = inflater.inflate(R.layout.dashboard, container, false);
        mDashboard = root.findViewById(R.id.dashboard_container);
        mLayoutManager = new LinearLayoutManager(getContext());
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        if (bundle != null) {
            int scrollPosition = bundle.getInt(STATE_SCROLL_POSITION);
            mLayoutManager.scrollToPosition(scrollPosition);
        }
        mDashboard.setLayoutManager(mLayoutManager);
        mDashboard.setHasFixedSize(true);
        mDashboard.setListener(this);
        mDashboard.setItemAnimator(new DashboardItemAnimator());
        mAdapter = new DashboardAdapter(getContext(), bundle,
                mConditionManager.getConditions(), mSuggestionControllerMixin, getLifecycle());
        mDashboard.setAdapter(mAdapter);
        mSummaryLoader.setSummaryConsumer(mAdapter);
        ActionBarShadowController.attachToRecyclerView(
                getActivity().findViewById(R.id.search_bar_container), getLifecycle(), mDashboard);
        rebuildUI();
        if (DEBUG_TIMING) {
            Log.d(TAG, "onCreateView took "
                    + (System.currentTimeMillis() - startTime) + " ms");
        }
        return root;
    }

    @VisibleForTesting
    void rebuildUI() {
        ThreadUtils.postOnBackgroundThread(() -> updateCategory());
    }

    @Override
    public void onCategoriesChanged() {
        // Bypass rebuildUI() on the first call of onCategoriesChanged, since rebuildUI() happens
        // in onViewCreated as well when app starts. But, on the subsequent calls we need to
        // rebuildUI() because there might be some changes to suggestions and categories.
        if (mIsOnCategoriesChangedCalled) {
            rebuildUI();
        }
        mIsOnCategoriesChangedCalled = true;
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
    public void onSuggestionReady(List<Suggestion> suggestions) {
        mStagingSuggestions = suggestions;
        mAdapter.setSuggestions(suggestions);
        if (mStagingCategory != null) {
            Log.d(TAG, "Category has loaded, setting category from suggestionReady");
            mHandler.removeCallbacksAndMessages(null);
            mAdapter.setCategory(mStagingCategory);
        }
    }

    @WorkerThread
    void updateCategory() {
        final DashboardCategory category = mDashboardFeatureProvider.getTilesForCategory(
                CategoryKey.CATEGORY_HOMEPAGE);
        mSummaryLoader.updateSummaryToCache(category);
        mStagingCategory = category;
        if (mSuggestionControllerMixin == null) {
            ThreadUtils.postOnMainThread(() -> mAdapter.setCategory(mStagingCategory));
            return;
        }
        if (mSuggestionControllerMixin.isSuggestionLoaded()) {
            Log.d(TAG, "Suggestion has loaded, setting suggestion/category");
            ThreadUtils.postOnMainThread(() -> {
                if (mStagingSuggestions != null) {
                    mAdapter.setSuggestions(mStagingSuggestions);
                }
                mAdapter.setCategory(mStagingCategory);
            });
        } else {
            Log.d(TAG, "Suggestion NOT loaded, delaying setCategory by " + MAX_WAIT_MILLIS + "ms");
            mHandler.postDelayed(() -> mAdapter.setCategory(mStagingCategory), MAX_WAIT_MILLIS);
        }
    }
}
