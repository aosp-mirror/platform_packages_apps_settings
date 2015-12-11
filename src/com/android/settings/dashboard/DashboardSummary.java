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

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.internal.logging.MetricsLogger;
import com.android.settings.HelpUtils;
import com.android.settings.InstrumentedFragment;
import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.SettingsActivity;
import com.android.settings.dashboard.conditional.ConditionAdapterUtils;
import com.android.settings.dashboard.conditional.ConditionManager;
import com.android.settings.dashboard.conditional.FocusRecyclerView;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.SettingsDrawerActivity;

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

    private FocusRecyclerView mDashboard;
    private DashboardAdapter mAdapter;
    private SummaryLoader mSummaryLoader;
    private ConditionManager mConditionManager;

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DASHBOARD_SUMMARY;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        long startTime = System.currentTimeMillis();
        List<DashboardCategory> categories =
                ((SettingsActivity) getActivity()).getDashboardCategories();
        mSummaryLoader = new SummaryLoader(getActivity(), categories);
        setHasOptionsMenu(true);
        if (DEBUG_TIMING) Log.d(TAG, "onCreate took " + (System.currentTimeMillis() - startTime)
                + " ms");
        mConditionManager = ConditionManager.get(getContext());
    }

    @Override
    public void onDestroy() {
        mSummaryLoader.release();
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (getActivity() == null) return;
        HelpUtils.prepareHelpMenuItem(getActivity(), menu, R.string.help_uri_dashboard,
                getClass().getName());
    }

    @Override
    public void onResume() {
        super.onResume();

        ((SettingsDrawerActivity) getActivity()).addCategoryListener(this);
        mSummaryLoader.setListening(true);
        Log.d(TAG, "onResume");
    }

    @Override
    public void onPause() {
        super.onPause();

        ((SettingsDrawerActivity) getActivity()).remCategoryListener(this);
        mSummaryLoader.setListening(false);
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        if (hasWindowFocus) {
            mConditionManager.addListener(this);
            mConditionManager.refreshAll();
        } else {
            mConditionManager.remListener(this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dashboard, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        mDashboard = (FocusRecyclerView) view.findViewById(R.id.dashboard_container);
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        mDashboard.setLayoutManager(llm);
        mDashboard.setHasFixedSize(true);
        mDashboard.setListener(this);
        mAdapter = new DashboardAdapter(getContext());
        mAdapter.setConditions(mConditionManager.getConditions());
        mSummaryLoader.setAdapter(mAdapter);
        ConditionAdapterUtils.addDismiss(mDashboard);

        rebuildUI();
    }

    private void rebuildUI() {
        if (!isAdded()) {
            Log.w(TAG, "Cannot build the DashboardSummary UI yet as the Fragment is not added");
            return;
        }

        long start = System.currentTimeMillis();
        // TODO: Cache summaries from old categories somehow.
        List<DashboardCategory> categories =
                ((SettingsActivity) getActivity()).getDashboardCategories();
        mAdapter.setCategories(categories);
        mDashboard.setAdapter(mAdapter);

        long delta = System.currentTimeMillis() - start;
        Log.d(TAG, "rebuildUI took: " + delta + " ms");
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
}
