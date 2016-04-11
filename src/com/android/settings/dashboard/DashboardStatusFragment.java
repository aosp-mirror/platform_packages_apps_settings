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

package com.android.settings.dashboard;

import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.InstrumentedFragment;
import com.android.settings.R;
import com.android.settings.dashboard.conditional.Condition;
import com.android.settings.dashboard.conditional.ConditionAdapterUtils;
import com.android.settings.dashboard.conditional.ConditionManager;
import com.android.settings.dashboard.conditional.FocusRecyclerView;

/**
 * Dashboard fragment for showing status and suggestions.
 */
public final class DashboardStatusFragment extends InstrumentedFragment
        implements ConditionManager.ConditionListener, FocusRecyclerView.FocusListener {

    private static final String TAG = "DashboardStatus";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private ConditionManager mConditionManager;
    private DashboardStatusAdapter mAdapter;
    private FocusRecyclerView mRecyclerView;
    private GridLayoutManager mLayoutManager;

    @Override
    protected int getMetricsCategory() {
        return DASHBOARD_STATUS;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mConditionManager = ConditionManager.get(getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        final View content = inflater.inflate(R.layout.dashboard_status, parent, false);
        mRecyclerView =
                (FocusRecyclerView) content.findViewById(R.id.dashboard_status_recycler_view);
        mAdapter = new DashboardStatusAdapter(getContext());
        mAdapter.setConditions(mConditionManager.getConditions());
        mLayoutManager = new GridLayoutManager(
                getContext(), DashboardStatusAdapter.GRID_COLUMN_COUNT);
        mLayoutManager.setOrientation(GridLayoutManager.VERTICAL);
        mLayoutManager.setSpanSizeLookup(mAdapter.getSpanSizeLookup());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setListener(this);
        mRecyclerView.setAdapter(mAdapter);
        ConditionAdapterUtils.addDismiss(mRecyclerView);
        return content;
    }

    @Override
    public void onResume() {
        super.onResume();
        for (Condition c : mConditionManager.getVisibleConditions()) {
            MetricsLogger.visible(getContext(), c.getMetricsConstant());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        for (Condition c : mConditionManager.getVisibleConditions()) {
            MetricsLogger.hidden(getContext(), c.getMetricsConstant());
        }
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
    public void onConditionsChanged() {
        if (DEBUG) Log.d(TAG, "onConditionsChanged");
        mAdapter.setConditions(mConditionManager.getConditions());
    }
}
