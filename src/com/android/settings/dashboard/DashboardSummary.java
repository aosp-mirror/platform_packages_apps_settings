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
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.InstrumentedFragment;
import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.SettingsActivity;
import com.android.settings.dashboard.conditional.Condition;
import com.android.settings.dashboard.conditional.ConditionAdapterUtils;
import com.android.settings.dashboard.conditional.ConditionManager;
import com.android.settings.dashboard.conditional.FocusRecyclerView;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.SuggestionParser;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.SettingsDrawerActivity;
import com.android.settingslib.drawer.Tile;

import java.util.List;

public class DashboardSummary extends InstrumentedFragment
        implements SettingsDrawerActivity.CategoryListener, ConditionManager.ConditionListener,
        FocusRecyclerView.FocusListener {
    public static final boolean DEBUG = false;
    private static final boolean DEBUG_TIMING = false;
    private static final String TAG = "DashboardSummary";

    public static final String[] INITIAL_ITEMS = new String[] {
            Settings.WifiSettingsActivity.class.getName(),
            Settings.BluetoothSettingsActivity.class.getName(),
            Settings.DataUsageSummaryActivity.class.getName(),
            Settings.PowerUsageSummaryActivity.class.getName(),
            Settings.ManageApplicationsActivity.class.getName(),
            Settings.StorageSettingsActivity.class.getName(),
    };

    private static final String SUGGESTIONS = "suggestions";

    private static final String EXTRA_SCROLL_POSITION = "scroll_position";

    private FocusRecyclerView mDashboard;
    private DashboardAdapter mAdapter;
    private SummaryLoader mSummaryLoader;
    private ConditionManager mConditionManager;
    private SuggestionParser mSuggestionParser;
    private LinearLayoutManager mLayoutManager;
    private SuggestionsChecks mSuggestionsChecks;

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.DASHBOARD_SUMMARY;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        long startTime = System.currentTimeMillis();
        super.onCreate(savedInstanceState);

        List<DashboardCategory> categories =
                ((SettingsActivity) getActivity()).getDashboardCategories();
        mSummaryLoader = new SummaryLoader(getActivity(), categories);
        Context context = getContext();
        mConditionManager = ConditionManager.get(context, false);
        mSuggestionParser = new SuggestionParser(context,
                context.getSharedPreferences(SUGGESTIONS, 0), R.xml.suggestion_ordering);
        mSuggestionsChecks = new SuggestionsChecks(getContext());
        if (DEBUG_TIMING) Log.d(TAG, "onCreate took " + (System.currentTimeMillis() - startTime)
                + " ms");
    }

    @Override
    public void onDestroy() {
        mSummaryLoader.release();
        super.onDestroy();
    }

    @Override
    public void onStart() {
        long startTime = System.currentTimeMillis();
        super.onStart();

        ((SettingsDrawerActivity) getActivity()).addCategoryListener(this);
        mSummaryLoader.setListening(true);
        for (Condition c : mConditionManager.getConditions()) {
            if (c.shouldShow()) {
                MetricsLogger.visible(getContext(), c.getMetricsConstant());
            }
        }
        if (mAdapter.getSuggestions() != null) {
            for (Tile suggestion : mAdapter.getSuggestions()) {
                MetricsLogger.action(getContext(), MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                        DashboardAdapter.getSuggestionIdentifier(getContext(), suggestion));
            }
        }
        if (DEBUG_TIMING) Log.d(TAG, "onStart took " + (System.currentTimeMillis() - startTime)
                + " ms");
    }

    @Override
    public void onStop() {
        super.onStop();

        ((SettingsDrawerActivity) getActivity()).remCategoryListener(this);
        mSummaryLoader.setListening(false);
        for (Condition c : mConditionManager.getConditions()) {
            if (c.shouldShow()) {
                MetricsLogger.hidden(getContext(), c.getMetricsConstant());
            }
        }
        if (mAdapter.getSuggestions() == null) {
            return;
        }
        for (Tile suggestion : mAdapter.getSuggestions()) {
            MetricsLogger.action(getContext(), MetricsEvent.ACTION_HIDE_SETTINGS_SUGGESTION,
                    DashboardAdapter.getSuggestionIdentifier(getContext(), suggestion));
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        long startTime = System.currentTimeMillis();
        if (hasWindowFocus) {
            mConditionManager.addListener(this);
            mConditionManager.refreshAll();
        } else {
            mConditionManager.remListener(this);
        }
        if (DEBUG_TIMING) Log.d(TAG, "onWindowFocusChanged took "
                + (System.currentTimeMillis() - startTime) + " ms");
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
        mDashboard = (FocusRecyclerView) view.findViewById(R.id.dashboard_container);
        mLayoutManager = new LinearLayoutManager(getContext());
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        if (bundle != null) {
            int scrollPosition = bundle.getInt(EXTRA_SCROLL_POSITION);
            mLayoutManager.scrollToPosition(scrollPosition);
        }
        mDashboard.setLayoutManager(mLayoutManager);
        mDashboard.setHasFixedSize(true);
        mDashboard.setListener(this);
        mDashboard.addItemDecoration(new DashboardDecorator(getContext()));
        mAdapter = new DashboardAdapter(getContext(), mSuggestionParser, bundle,
                mConditionManager.getConditions());
        mDashboard.setAdapter(mAdapter);
        mSummaryLoader.setAdapter(mAdapter);
        ConditionAdapterUtils.addDismiss(mDashboard);
        if (DEBUG_TIMING) Log.d(TAG, "onViewCreated took "
                + (System.currentTimeMillis() - startTime) + " ms");
        rebuildUI();
    }

    private void rebuildUI() {
        if (!isAdded()) {
            Log.w(TAG, "Cannot build the DashboardSummary UI yet as the Fragment is not added");
            return;
        }

        // recheck to see if any suggestions have been changed.
        new SuggestionLoader().execute();
    }

    @Override
    public void onCategoriesChanged() {
        rebuildUI();
    }

    @Override
    public void onConditionsChanged() {
        Log.d(TAG, "onConditionsChanged");
        mAdapter.setConditions(mConditionManager.getConditions());
    }

    private class SuggestionLoader extends AsyncTask<Void, Void, List<Tile>> {

        @Override
        protected List<Tile> doInBackground(Void... params) {
            List<Tile> suggestions = mSuggestionParser.getSuggestions();
            for (int i = 0; i < suggestions.size(); i++) {
                if (mSuggestionsChecks.isSuggestionComplete(suggestions.get(i))) {
                    mAdapter.disableSuggestion(suggestions.get(i));
                    suggestions.remove(i--);
                }
            }
            return suggestions;
        }

        @Override
        protected void onPostExecute(List<Tile> tiles) {
            final Activity activity = getActivity();
            if (activity == null) {
                return;
            }
            List<DashboardCategory> categories =
                    ((SettingsActivity) activity).getDashboardCategories();
            mAdapter.setCategoriesAndSuggestions(categories, tiles);
        }
    }
}
