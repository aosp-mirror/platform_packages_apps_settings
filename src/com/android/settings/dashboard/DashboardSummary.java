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
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.dashboard.conditional.Condition;
import com.android.settings.dashboard.conditional.ConditionAdapterUtils;
import com.android.settings.dashboard.conditional.ConditionManager;
import com.android.settings.dashboard.conditional.FocusRecyclerView;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.SuggestionParser;
import com.android.settingslib.drawer.CategoryKey;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.SettingsDrawerActivity;
import com.android.settingslib.drawer.Tile;

import java.util.ArrayList;
import java.util.List;

public class DashboardSummary extends InstrumentedPreferenceFragment
        implements SettingsDrawerActivity.CategoryListener, ConditionManager.ConditionListener,
        FocusRecyclerView.FocusListener {
    public static final boolean DEBUG = false;
    private static final boolean DEBUG_TIMING = false;
    private static final int MAX_WAIT_MILLIS = 700;
    private static final String TAG = "DashboardSummary";

    private static final String SUGGESTIONS = "suggestions";

    private static final String EXTRA_SCROLL_POSITION = "scroll_position";
    private static final String EXTRA_SUGGESTION_SHOWN_LOGGED = "suggestions_shown_logged";
    private static final String EXTRA_SUGGESTION_HIDDEN_LOGGED = "suggestions_hidden_logged";

    private final Handler mHandler = new Handler();

    private FocusRecyclerView mDashboard;
    private DashboardAdapter mAdapter;
    private SummaryLoader mSummaryLoader;
    private ConditionManager mConditionManager;
    private SuggestionParser mSuggestionParser;
    private LinearLayoutManager mLayoutManager;
    private SuggestionsChecks mSuggestionsChecks;
    private ArrayList<String> mSuggestionsShownLogged;
    private ArrayList<String> mSuggestionsHiddenLogged;
    private DashboardFeatureProvider mDashboardFeatureProvider;

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

        if (mDashboardFeatureProvider.isEnabled()) {
            mSummaryLoader = new SummaryLoader(activity, CategoryKey.CATEGORY_HOMEPAGE);
        } else {
            mSummaryLoader = new SummaryLoader(activity,
                    ((SettingsActivity) getActivity()).getDashboardCategories());
        }

        Context context = getContext();
        mConditionManager = ConditionManager.get(context, false);
        mSuggestionParser = new SuggestionParser(context,
                context.getSharedPreferences(SUGGESTIONS, 0), R.xml.suggestion_ordering);
        mSuggestionsChecks = new SuggestionsChecks(getContext());
        if (savedInstanceState == null) {
            mSuggestionsShownLogged = new ArrayList<>();
            mSuggestionsHiddenLogged = new ArrayList<>();
        } else {
            mSuggestionsShownLogged =
                    savedInstanceState.getStringArrayList(EXTRA_SUGGESTION_SHOWN_LOGGED);
            mSuggestionsHiddenLogged =
                    savedInstanceState.getStringArrayList(EXTRA_SUGGESTION_HIDDEN_LOGGED);
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
        for (Condition c : mConditionManager.getConditions()) {
            if (c.shouldShow()) {
                mMetricsFeatureProvider.visible(getContext(), c.getMetricsConstant());
            }
        }
        if (DEBUG_TIMING) {
            Log.d(TAG, "onResume took " + (System.currentTimeMillis() - startTime)
                    + " ms");
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
        if (mAdapter.getSuggestions() == null) {
            return;
        }
        if (!getActivity().isChangingConfigurations()) {
            for (Tile suggestion : mAdapter.getSuggestions()) {
                String id = DashboardAdapter.getSuggestionIdentifier(getContext(), suggestion);
                if (!mSuggestionsHiddenLogged.contains(id)) {
                    mSuggestionsHiddenLogged.add(id);
                    mMetricsFeatureProvider.action(getContext(),
                            MetricsEvent.ACTION_HIDE_SETTINGS_SUGGESTION, id);
                }
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dashboard, container, false);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(EXTRA_SUGGESTION_HIDDEN_LOGGED, mSuggestionsHiddenLogged);
        outState.putStringArrayList(EXTRA_SUGGESTION_SHOWN_LOGGED, mSuggestionsShownLogged);
        if (mLayoutManager == null) return;
        outState.putInt(EXTRA_SCROLL_POSITION, mLayoutManager.findFirstVisibleItemPosition());
        if (mAdapter != null) {
            mAdapter.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        long startTime = System.currentTimeMillis();
        mDashboard = (FocusRecyclerView) view.findViewById(R.id.dashboard_container);
        mLayoutManager = new LinearLayoutManager(getContext());
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        if (bundle != null) {
            int scrollPosition = bundle.getInt(EXTRA_SCROLL_POSITION);
            mLayoutManager.scrollToPosition(scrollPosition);
        }
        mDashboard.setLayoutManager(mLayoutManager);
        mDashboard.setHasFixedSize(true);
        mDashboard.addItemDecoration(new DashboardDecorator(getContext()));
        mDashboard.setListener(this);
        Log.d(TAG, "adapter created");
        mAdapter = new DashboardAdapter(getContext(), mSuggestionParser, mMetricsFeatureProvider,
                bundle, mConditionManager.getConditions());
        mDashboard.setAdapter(mAdapter);
        mDashboard.setItemAnimator(new DashboardItemAnimator());
        mSummaryLoader.setSummaryConsumer(mAdapter);
        ConditionAdapterUtils.addDismiss(mDashboard);
        if (DEBUG_TIMING) {
            Log.d(TAG, "onViewCreated took "
                    + (System.currentTimeMillis() - startTime) + " ms");
        }
        rebuildUI();
    }

    private void rebuildUI() {
        if (!isAdded()) {
            Log.w(TAG, "Cannot build the DashboardSummary UI yet as the Fragment is not added");
            return;
        }

        // recheck to see if any suggestions have been changed.
        new SuggestionLoader().execute();
        // Set categories on their own if loading suggestions takes too long.
        mHandler.postDelayed(() -> {
            final Activity activity = getActivity();
            if (activity != null) {
                mAdapter.setCategoriesAndSuggestions(
                        ((SettingsActivity) activity).getDashboardCategories(), null);
            }
        }, MAX_WAIT_MILLIS);
    }

    @Override
    public void onCategoriesChanged() {
        rebuildUI();
    }

    @Override
    public void onConditionsChanged() {
        Log.d(TAG, "onConditionsChanged");
        mAdapter.setConditions(mConditionManager.getConditions());
        mDashboard.scrollToPosition(0);
    }

    private class SuggestionLoader extends AsyncTask<Void, Void, List<Tile>> {

        @Override
        protected List<Tile> doInBackground(Void... params) {
            final Context context = getContext();
            List<Tile> suggestions = mSuggestionParser.getSuggestions();
            for (int i = 0; i < suggestions.size(); i++) {
                Tile suggestion = suggestions.get(i);
                if (mSuggestionsChecks.isSuggestionComplete(suggestion)) {
                    mAdapter.disableSuggestion(suggestion);
                    suggestions.remove(i--);
                } else if (context != null) {
                    String id = DashboardAdapter.getSuggestionIdentifier(context, suggestion);
                    if (!mSuggestionsShownLogged.contains(id)) {
                        mSuggestionsShownLogged.add(id);
                        mMetricsFeatureProvider.action(context,
                                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION, id);
                    }
                }
            }
            return suggestions;
        }

        @Override
        protected void onPostExecute(List<Tile> tiles) {
            // tell handler that suggestions were loaded quickly enough
            mHandler.removeCallbacksAndMessages(null);

            final Activity activity = getActivity();
            if (activity == null) {
                return;
            }

            if (mDashboardFeatureProvider.isEnabled()) {
                // Temporary hack to wrap homepage category into a list. Soon we will create adapter
                // API that takes a single category.
                List<DashboardCategory> categories = new ArrayList<>();
                categories.add(mDashboardFeatureProvider.getTilesForCategory(
                        CategoryKey.CATEGORY_HOMEPAGE));
                mAdapter.setCategoriesAndSuggestions(categories, tiles);
            } else {
                mAdapter.setCategoriesAndSuggestions(
                        ((SettingsActivity) activity).getDashboardCategories(), tiles);
            }
        }
    }
}
