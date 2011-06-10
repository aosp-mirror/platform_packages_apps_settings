/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings;

import static android.net.NetworkPolicyManager.computeLastCycleBoundary;
import static android.net.NetworkPolicyManager.computeNextCycleBoundary;
import static android.net.TrafficStats.TEMPLATE_MOBILE_3G_LOWER;
import static android.net.TrafficStats.TEMPLATE_MOBILE_4G;
import static android.net.TrafficStats.TEMPLATE_MOBILE_ALL;
import static android.net.TrafficStats.TEMPLATE_WIFI;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.INetworkPolicyManager;
import android.net.INetworkStatsService;
import android.net.NetworkPolicy;
import android.net.NetworkStats;
import android.net.NetworkStatsHistory;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabHost.TabSpec;
import android.widget.TabWidget;
import android.widget.TextView;

import com.android.settings.widget.DataUsageChartView;
import com.android.settings.widget.DataUsageChartView.DataUsageChartListener;
import com.google.android.collect.Lists;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

public class DataUsageSummary extends Fragment {
    private static final String TAG = "DataUsage";
    private static final boolean LOGD = true;

    private static final int TEMPLATE_INVALID = -1;

    private static final String TAB_3G = "3g";
    private static final String TAB_4G = "4g";
    private static final String TAB_MOBILE = "mobile";
    private static final String TAB_WIFI = "wifi";

    private static final long KB_IN_BYTES = 1024;
    private static final long MB_IN_BYTES = KB_IN_BYTES * 1024;
    private static final long GB_IN_BYTES = MB_IN_BYTES * 1024;

    private INetworkStatsService mStatsService;
    private INetworkPolicyManager mPolicyService;

    private TabHost mTabHost;
    private TabWidget mTabWidget;
    private ListView mListView;
    private DataUsageAdapter mAdapter;

    private View mHeader;
    private LinearLayout mSwitches;

    private CheckBoxPreference mDataEnabled;
    private CheckBoxPreference mDisableAtLimit;
    private View mDataEnabledView;
    private View mDisableAtLimitView;

    private DataUsageChartView mChart;

    private Spinner mCycleSpinner;
    private CycleAdapter mCycleAdapter;

    private boolean mSplit4G = false;
    private boolean mShowWifi = false;

    private int mTemplate = TEMPLATE_INVALID;

    private NetworkPolicy mPolicy;
    private NetworkStatsHistory mHistory;

    // TODO: policy service should always provide valid stub policy

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mStatsService = INetworkStatsService.Stub.asInterface(
                ServiceManager.getService(Context.NETWORK_STATS_SERVICE));
        mPolicyService = INetworkPolicyManager.Stub.asInterface(
                ServiceManager.getService(Context.NETWORK_POLICY_SERVICE));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        final Context context = inflater.getContext();
        final View view = inflater.inflate(R.layout.data_usage_summary, container, false);

        mTabHost = (TabHost) view.findViewById(android.R.id.tabhost);
        mTabWidget = (TabWidget) view.findViewById(android.R.id.tabs);
        mListView = (ListView) view.findViewById(android.R.id.list);

        mTabHost.setup();
        mTabHost.setOnTabChangedListener(mTabListener);

        mHeader = inflater.inflate(R.layout.data_usage_header, mListView, false);
        mListView.addHeaderView(mHeader, null, false);

        mDataEnabled = new CheckBoxPreference(context);
        mDisableAtLimit = new CheckBoxPreference(context);

        // kick refresh once to force-create views
        refreshPreferenceViews();

        // TODO: remove once thin preferences are supported (48dip)
        mDataEnabledView.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, 72));
        mDisableAtLimitView.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, 72));

        mDataEnabledView.setOnClickListener(mDataEnabledListener);
        mDisableAtLimitView.setOnClickListener(mDisableAtLimitListener);

        mSwitches = (LinearLayout) mHeader.findViewById(R.id.switches);
        mSwitches.addView(mDataEnabledView);
        mSwitches.addView(mDisableAtLimitView);

        mCycleSpinner = (Spinner) mHeader.findViewById(R.id.cycles);
        mCycleAdapter = new CycleAdapter(context);
        mCycleSpinner.setAdapter(mCycleAdapter);
        mCycleSpinner.setOnItemSelectedListener(mCycleListener);

        mChart = new DataUsageChartView(context);
        mChart.setListener(mChartListener);
        mChart.setLayoutParams(new AbsListView.LayoutParams(MATCH_PARENT, 350));
        mListView.addHeaderView(mChart, null, false);

        mAdapter = new DataUsageAdapter();
        mListView.setOnItemClickListener(mListListener);
        mListView.setAdapter(mAdapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        // this kicks off chain reaction which creates tabs, binds the body to
        // selected network, and binds chart, cycles and detail list.
        updateTabs();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.data_usage, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO: persist checked-ness of options to restore tabs later

        switch (item.getItemId()) {
            case R.id.action_split_4g: {
                mSplit4G = !item.isChecked();
                item.setChecked(mSplit4G);
                updateTabs();
                return true;
            }
            case R.id.action_show_wifi: {
                mShowWifi = !item.isChecked();
                item.setChecked(mShowWifi);
                updateTabs();
                return true;
            }
        }
        return false;
    }

    /**
     * Rebuild all tabs based on {@link #mSplit4G} and {@link #mShowWifi},
     * hiding the tabs entirely when applicable. Selects first tab, and kicks
     * off a full rebind of body contents.
     */
    private void updateTabs() {
        // TODO: persist/restore if user wants mobile split, or wifi visibility

        final boolean tabsVisible = mSplit4G || mShowWifi;
        mTabWidget.setVisibility(tabsVisible ? View.VISIBLE : View.GONE);
        mTabHost.clearAllTabs();

        if (mSplit4G) {
            mTabHost.addTab(buildTabSpec(TAB_3G, R.string.data_usage_tab_3g));
            mTabHost.addTab(buildTabSpec(TAB_4G, R.string.data_usage_tab_4g));
        }

        if (mShowWifi) {
            if (!mSplit4G) {
                mTabHost.addTab(buildTabSpec(TAB_MOBILE, R.string.data_usage_tab_mobile));
            }
            mTabHost.addTab(buildTabSpec(TAB_WIFI, R.string.data_usage_tab_wifi));
        }

        if (mTabWidget.getTabCount() > 0) {
            // select first tab, which will kick off updateBody()
            mTabHost.setCurrentTab(0);
        } else {
            // no tabs shown; update body manually
            updateBody();
        }
    }

    /**
     * Factory that provide empty {@link View} to make {@link TabHost} happy.
     */
    private TabContentFactory mEmptyTabContent = new TabContentFactory() {
        /** {@inheritDoc} */
        public View createTabContent(String tag) {
            return new View(mTabHost.getContext());
        }
    };

    /**
     * Build {@link TabSpec} with thin indicator, and empty content.
     */
    private TabSpec buildTabSpec(String tag, int titleRes) {
        final LayoutInflater inflater = LayoutInflater.from(mTabWidget.getContext());
        final View indicator = inflater.inflate(
                R.layout.tab_indicator_thin_holo, mTabWidget, false);
        final TextView title = (TextView) indicator.findViewById(android.R.id.title);
        title.setText(titleRes);
        return mTabHost.newTabSpec(tag).setIndicator(indicator).setContent(mEmptyTabContent);
    }

    private OnTabChangeListener mTabListener = new OnTabChangeListener() {
        /** {@inheritDoc} */
        public void onTabChanged(String tabId) {
            // user changed tab; update body
            updateBody();
        }
    };

    /**
     * Update body content based on current tab. Loads
     * {@link NetworkStatsHistory} and {@link NetworkPolicy} from system, and
     * binds them to visible controls.
     */
    private void updateBody() {
        final String tabTag = mTabHost.getCurrentTabTag();
        final String currentTab = tabTag != null ? tabTag : TAB_MOBILE;

        if (LOGD) Log.d(TAG, "updateBody() with currentTab=" + currentTab);

        if (TAB_WIFI.equals(currentTab)) {
            // wifi doesn't have any controls
            mDataEnabledView.setVisibility(View.GONE);
            mDisableAtLimitView.setVisibility(View.GONE);
            mTemplate = TEMPLATE_WIFI;

        } else {
            // make sure we show for non-wifi
            mDataEnabledView.setVisibility(View.VISIBLE);
            mDisableAtLimitView.setVisibility(View.VISIBLE);
        }

        if (TAB_MOBILE.equals(currentTab)) {
            mDataEnabled.setTitle(R.string.data_usage_enable_mobile);
            mDisableAtLimit.setTitle(R.string.data_usage_disable_mobile_limit);
            mTemplate = TEMPLATE_MOBILE_ALL;

        } else if (TAB_3G.equals(currentTab)) {
            mDataEnabled.setTitle(R.string.data_usage_enable_3g);
            mDisableAtLimit.setTitle(R.string.data_usage_disable_3g_limit);
            mTemplate = TEMPLATE_MOBILE_3G_LOWER;

        } else if (TAB_4G.equals(currentTab)) {
            mDataEnabled.setTitle(R.string.data_usage_enable_4g);
            mDisableAtLimit.setTitle(R.string.data_usage_disable_4g_limit);
            mTemplate = TEMPLATE_MOBILE_4G;

        }

        // TODO: populate checkbox based on radio preferences
        mDataEnabled.setChecked(true);

        try {
            // load policy and stats for current template
            mPolicy = mPolicyService.getNetworkPolicy(mTemplate, null);
            mHistory = mStatsService.getHistoryForNetwork(mTemplate);
        } catch (RemoteException e) {
            // since we can't do much without policy or history, and we don't
            // want to leave with half-baked UI, we bail hard.
            throw new RuntimeException("problem reading network policy or stats", e);
        }

        // TODO: eventually service will always provide stub policy
        if (mPolicy == null) {
            mPolicy = new NetworkPolicy(1, 4 * GB_IN_BYTES, -1);
        }

        // bind chart to historical stats
        mChart.bindNetworkPolicy(mPolicy);
        mChart.bindNetworkStats(mHistory);

        // generate cycle list based on policy and available history
        updateCycleList();

        // reflect policy limit in checkbox
        mDisableAtLimit.setChecked(mPolicy.limitBytes != -1);

        // force scroll to top of body
        mListView.smoothScrollToPosition(0);

        // kick preference views so they rebind from changes above
        refreshPreferenceViews();
    }

    /**
     * Return full time bounds (earliest and latest time recorded) of the given
     * {@link NetworkStatsHistory}.
     */
    private static long[] getHistoryBounds(NetworkStatsHistory history) {
        final long currentTime = System.currentTimeMillis();

        long start = currentTime;
        long end = currentTime;
        if (history.bucketCount > 0) {
            start = history.bucketStart[0];
            end = history.bucketStart[history.bucketCount - 1];
        }

        return new long[] { start, end };
    }

    /**
     * Rebuild {@link #mCycleAdapter} based on {@link NetworkPolicy#cycleDay}
     * and available {@link NetworkStatsHistory} data. Always selects the newest
     * item, updating the inspection range on {@link #mChart}.
     */
    private void updateCycleList() {
        mCycleAdapter.clear();

        final Context context = mCycleSpinner.getContext();

        final long[] bounds = getHistoryBounds(mHistory);
        final long historyStart = bounds[0];
        final long historyEnd = bounds[1];

        // find the next cycle boundary
        long cycleEnd = computeNextCycleBoundary(historyEnd, mPolicy);

        int guardCount = 0;

        // walk backwards, generating all valid cycle ranges
        while (cycleEnd > historyStart) {
            final long cycleStart = computeLastCycleBoundary(cycleEnd, mPolicy);
            Log.d(TAG, "generating cs=" + cycleStart + " to ce=" + cycleEnd + " waiting for hs="
                    + historyStart);
            mCycleAdapter.add(new CycleItem(context, cycleStart, cycleEnd));
            cycleEnd = cycleStart;

            // TODO: remove this guard once we have better testing
            if (guardCount++ > 50) {
                Log.wtf(TAG, "stuck generating ranges for bounds=" + Arrays.toString(bounds)
                        + " and policy=" + mPolicy);
            }
        }

        // one last cycle entry to change date
        mCycleAdapter.add(new CycleChangeItem(context));

        // force pick the current cycle (first item)
        mCycleSpinner.setSelection(0);
        mCycleListener.onItemSelected(mCycleSpinner, null, 0, 0);
    }

    /**
     * Force rebind of hijacked {@link Preference} views.
     */
    private void refreshPreferenceViews() {
        mDataEnabledView = mDataEnabled.getView(mDataEnabledView, mListView);
        mDisableAtLimitView = mDisableAtLimit.getView(mDisableAtLimitView, mListView);
    }

    private OnClickListener mDataEnabledListener = new OnClickListener() {
        /** {@inheritDoc} */
        public void onClick(View v) {
            mDataEnabled.setChecked(!mDataEnabled.isChecked());
            refreshPreferenceViews();

            // TODO: wire up to telephony to enable/disable radios
        }
    };

    private OnClickListener mDisableAtLimitListener = new OnClickListener() {
        /** {@inheritDoc} */
        public void onClick(View v) {
            final boolean disableAtLimit = !mDisableAtLimit.isChecked();
            mDisableAtLimit.setChecked(disableAtLimit);
            refreshPreferenceViews();

            // TODO: push updated policy to service
            // TODO: show interstitial warning dialog to user
            final long limitBytes = disableAtLimit ? 5 * GB_IN_BYTES : -1;
            mPolicy = new NetworkPolicy(mPolicy.cycleDay, mPolicy.warningBytes, limitBytes);
            mChart.bindNetworkPolicy(mPolicy);
        }
    };

    private OnItemClickListener mListListener = new OnItemClickListener() {
        /** {@inheritDoc} */
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final Object object = parent.getItemAtPosition(position);

            // TODO: show app details
            Log.d(TAG, "showing app details for " + object);
        }
    };

    private OnItemSelectedListener mCycleListener = new OnItemSelectedListener() {
        /** {@inheritDoc} */
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            final CycleItem cycle = (CycleItem) parent.getItemAtPosition(position);
            if (cycle instanceof CycleChangeItem) {
                // TODO: show "define cycle" dialog
                // also reset back to first cycle
                Log.d(TAG, "CHANGE CYCLE DIALOG!!");

            } else {
                if (LOGD) Log.d(TAG, "shoiwng cycle " + cycle);

                // update chart to show selected cycle, and update detail data
                // to match updated sweep bounds.
                final long[] bounds = getHistoryBounds(mHistory);
                mChart.setVisibleRange(cycle.start, cycle.end, bounds[1]);

                updateDetailData();
            }
        }

        /** {@inheritDoc} */
        public void onNothingSelected(AdapterView<?> parent) {
            // ignored
        }
    };

    /**
     * Update {@link #mAdapter} with sorted list of applications data usage,
     * based on current inspection from {@link #mChart}.
     */
    private void updateDetailData() {
        if (LOGD) Log.d(TAG, "updateDetailData()");

        try {
            final long[] range = mChart.getInspectRange();
            final NetworkStats stats = mStatsService.getSummaryForAllUid(
                    range[0], range[1], mTemplate);
            mAdapter.bindStats(stats);
        } catch (RemoteException e) {
            Log.w(TAG, "problem reading stats");
        }
    }

    private DataUsageChartListener mChartListener = new DataUsageChartListener() {
        /** {@inheritDoc} */
        public void onInspectRangeChanged() {
            if (LOGD) Log.d(TAG, "onInspectRangeChanged()");
            updateDetailData();
        }

        /** {@inheritDoc} */
        public void onLimitsChanged() {
            if (LOGD) Log.d(TAG, "onLimitsChanged()");

            // redefine policy and persist into service
            // TODO: kick this onto background thread, since service touches disk

            // TODO: remove this mPolicy null check, since later service will
            // always define baseline value.
            final int cycleDay = mPolicy != null ? mPolicy.cycleDay : 1;
            final long warningBytes = mChart.getWarningBytes();
            final long limitBytes = mDisableAtLimit.isChecked() ? -1 : mChart.getLimitBytes();

            mPolicy = new NetworkPolicy(cycleDay, warningBytes, limitBytes);
            if (LOGD) Log.d(TAG, "persisting policy=" + mPolicy);

            try {
                mPolicyService.setNetworkPolicy(mTemplate, null, mPolicy);
            } catch (RemoteException e) {
                Log.w(TAG, "problem persisting policy", e);
            }
        }
    };


    /**
     * List item that reflects a specific data usage cycle.
     */
    public static class CycleItem {
        public CharSequence label;
        public long start;
        public long end;

        private static final StringBuilder sBuilder = new StringBuilder(50);
        private static final java.util.Formatter sFormatter = new java.util.Formatter(
                sBuilder, Locale.getDefault());

        CycleItem(CharSequence label) {
            this.label = label;
        }

        public CycleItem(Context context, long start, long end) {
            this.label = formatDateRangeUtc(context, start, end);
            this.start = start;
            this.end = end;
        }

        private static String formatDateRangeUtc(Context context, long start, long end) {
            synchronized (sBuilder) {
                sBuilder.setLength(0);
                return DateUtils.formatDateRange(context, sFormatter, start, end,
                        DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH,
                        Time.TIMEZONE_UTC).toString();
            }
        }

        @Override
        public String toString() {
            return label.toString();
        }
    }

    /**
     * Special-case data usage cycle that triggers dialog to change
     * {@link NetworkPolicy#cycleDay}.
     */
    public static class CycleChangeItem extends CycleItem {
        public CycleChangeItem(Context context) {
            super(context.getString(R.string.data_usage_change_cycle));
        }
    }

    public static class CycleAdapter extends ArrayAdapter<CycleItem> {
        public CycleAdapter(Context context) {
            super(context, android.R.layout.simple_spinner_item);
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }
    }

    /**
     * Adapter of applications, sorted by total usage descending.
     */
    public static class DataUsageAdapter extends BaseAdapter {
        private ArrayList<AppUsageItem> mItems = Lists.newArrayList();

        private static class AppUsageItem implements Comparable<AppUsageItem> {
            public int uid;
            public long total;

            /** {@inheritDoc} */
            public int compareTo(AppUsageItem another) {
                return Long.compare(another.total, total);
            }
        }

        public void bindStats(NetworkStats stats) {
            mItems.clear();

            for (int i = 0; i < stats.length(); i++) {
                final AppUsageItem item = new AppUsageItem();
                item.uid = stats.uid[i];
                item.total = stats.rx[i] + stats.tx[i];
                mItems.add(item);
            }

            Collections.sort(mItems);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public Object getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(
                        android.R.layout.simple_list_item_2, parent, false);
            }

            final Context context = parent.getContext();
            final PackageManager pm = context.getPackageManager();

            final TextView text1 = (TextView) convertView.findViewById(android.R.id.text1);
            final TextView text2 = (TextView) convertView.findViewById(android.R.id.text2);

            final AppUsageItem item = mItems.get(position);
            text1.setText(pm.getNameForUid(item.uid));
            text2.setText(Formatter.formatFileSize(context, item.total));

            return convertView;
        }

    }


}
