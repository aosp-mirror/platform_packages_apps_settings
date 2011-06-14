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

import static android.net.NetworkPolicy.LIMIT_DISABLED;
import static android.net.NetworkPolicyManager.computeLastCycleBoundary;
import static android.net.NetworkPolicyManager.computeNextCycleBoundary;
import static android.net.TrafficStats.TEMPLATE_MOBILE_3G_LOWER;
import static android.net.TrafficStats.TEMPLATE_MOBILE_4G;
import static android.net.TrafficStats.TEMPLATE_MOBILE_ALL;
import static android.net.TrafficStats.TEMPLATE_WIFI;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.preference.PreferenceActivity;
import android.preference.SwitchPreference;
import android.telephony.TelephonyManager;
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
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabHost.TabSpec;
import android.widget.TabWidget;
import android.widget.TextView;

import com.android.settings.net.NetworkPolicyModifier;
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

    private static final String TAG_CONFIRM_LIMIT = "confirmLimit";
    private static final String TAG_CYCLE_EDITOR = "cycleEditor";

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

    private SwitchPreference mDataEnabled;
    private CheckBoxPreference mDisableAtLimit;
    private View mDataEnabledView;
    private View mDisableAtLimitView;

    private DataUsageChartView mChart;

    private Spinner mCycleSpinner;
    private CycleAdapter mCycleAdapter;

    // TODO: persist show wifi flag
    private boolean mShowWifi = false;

    private int mTemplate = TEMPLATE_INVALID;

    private NetworkPolicyModifier mPolicyModifier;
    private NetworkStatsHistory mHistory;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mStatsService = INetworkStatsService.Stub.asInterface(
                ServiceManager.getService(Context.NETWORK_STATS_SERVICE));
        mPolicyService = INetworkPolicyManager.Stub.asInterface(
                ServiceManager.getService(Context.NETWORK_POLICY_SERVICE));

        final Context context = getActivity();
        final String subscriberId = getActiveSubscriberId(context);
        mPolicyModifier = new NetworkPolicyModifier(mPolicyService, subscriberId);
        mPolicyModifier.read();

        setHasOptionsMenu(true);
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

        mDataEnabled = new SwitchPreference(context);
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
    public void onPrepareOptionsMenu(Menu menu) {
        final MenuItem split4g = menu.findItem(R.id.action_split_4g);
        split4g.setChecked(mPolicyModifier.isMobilePolicySplit());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_split_4g: {
                final boolean mobileSplit = !item.isChecked();
                mPolicyModifier.setMobilePolicySplit(mobileSplit);
                item.setChecked(mPolicyModifier.isMobilePolicySplit());
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mDataEnabledView = null;
        mDisableAtLimitView = null;
    }

    /**
     * Rebuild all tabs based on {@link NetworkPolicyModifier} and
     * {@link #mShowWifi}, hiding the tabs entirely when applicable. Selects
     * first tab, and kicks off a full rebind of body contents.
     */
    private void updateTabs() {
        final boolean mobileSplit = mPolicyModifier.isMobilePolicySplit();
        final boolean tabsVisible = mobileSplit || mShowWifi;
        mTabWidget.setVisibility(tabsVisible ? View.VISIBLE : View.GONE);
        mTabHost.clearAllTabs();

        if (mobileSplit) {
            mTabHost.addTab(buildTabSpec(TAB_3G, R.string.data_usage_tab_3g));
            mTabHost.addTab(buildTabSpec(TAB_4G, R.string.data_usage_tab_4g));
        }

        if (mShowWifi) {
            if (!mobileSplit) {
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
            // load stats for current template
            mHistory = mStatsService.getHistoryForNetwork(mTemplate);
        } catch (RemoteException e) {
            // since we can't do much without policy or history, and we don't
            // want to leave with half-baked UI, we bail hard.
            throw new RuntimeException("problem reading network policy or stats", e);
        }

        // bind chart to historical stats
        mChart.bindNetworkStats(mHistory);

        updatePolicy(true);

        // force scroll to top of body
        mListView.smoothScrollToPosition(0);

        // kick preference views so they rebind from changes above
        refreshPreferenceViews();
    }

    private void setPolicyCycleDay(int cycleDay) {
        if (LOGD) Log.d(TAG, "setPolicyCycleDay()");
        mPolicyModifier.setPolicyCycleDay(mTemplate, cycleDay);
        updatePolicy(true);
    }

    private void setPolicyWarningBytes(long warningBytes) {
        if (LOGD) Log.d(TAG, "setPolicyWarningBytes()");
        mPolicyModifier.setPolicyWarningBytes(mTemplate, warningBytes);
        updatePolicy(false);
    }

    private void setPolicyLimitBytes(long limitBytes) {
        if (LOGD) Log.d(TAG, "setPolicyLimitBytes()");
        mPolicyModifier.setPolicyLimitBytes(mTemplate, limitBytes);
        updatePolicy(false);
    }

    /**
     * Update chart sweeps and cycle list to reflect {@link NetworkPolicy} for
     * current {@link #mTemplate}.
     */
    private void updatePolicy(boolean refreshCycle) {
        final NetworkPolicy policy = mPolicyModifier.getPolicy(mTemplate);

        // reflect policy limit in checkbox
        mDisableAtLimit.setChecked(policy != null && policy.limitBytes != LIMIT_DISABLED);
        mChart.bindNetworkPolicy(policy);

        if (refreshCycle) {
            // generate cycle list based on policy and available history
            updateCycleList(policy);
        }

        // kick preference views so they rebind from changes above
        refreshPreferenceViews();
    }

    /**
     * Return full time bounds (earliest and latest time recorded) of the given
     * {@link NetworkStatsHistory}.
     */
    public static long[] getHistoryBounds(NetworkStatsHistory history) {
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
    private void updateCycleList(NetworkPolicy policy) {
        mCycleAdapter.clear();

        final Context context = mCycleSpinner.getContext();

        final long[] bounds = getHistoryBounds(mHistory);
        final long historyStart = bounds[0];
        final long historyEnd = bounds[1];

        if (policy != null) {
            // find the next cycle boundary
            long cycleEnd = computeNextCycleBoundary(historyEnd, policy);

            int guardCount = 0;

            // walk backwards, generating all valid cycle ranges
            while (cycleEnd > historyStart) {
                final long cycleStart = computeLastCycleBoundary(cycleEnd, policy);
                Log.d(TAG, "generating cs=" + cycleStart + " to ce=" + cycleEnd + " waiting for hs="
                        + historyStart);
                mCycleAdapter.add(new CycleItem(context, cycleStart, cycleEnd));
                cycleEnd = cycleStart;

                // TODO: remove this guard once we have better testing
                if (guardCount++ > 50) {
                    Log.wtf(TAG, "stuck generating ranges for bounds=" + Arrays.toString(bounds)
                            + " and policy=" + policy);
                }
            }

            // one last cycle entry to modify policy cycle day
            mCycleAdapter.add(new CycleChangeItem(context));

        } else {
            // no valid cycle; show all data
            // TODO: offer simple ranges like "last week" etc
            mCycleAdapter.add(new CycleItem(context, historyStart, historyEnd));

        }

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
            if (disableAtLimit) {
                // enabling limit; show confirmation dialog which eventually
                // calls setPolicyLimitBytes() once user confirms.
                ConfirmLimitFragment.show(DataUsageSummary.this);
            } else {
                setPolicyLimitBytes(LIMIT_DISABLED);
            }
        }
    };

    private OnItemClickListener mListListener = new OnItemClickListener() {
        /** {@inheritDoc} */
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final AppUsageItem app = (AppUsageItem) parent.getItemAtPosition(position);

            final Bundle args = new Bundle();
            args.putInt(Intent.EXTRA_UID, app.uid);

            final PreferenceActivity activity = (PreferenceActivity) getActivity();
            activity.startPreferencePanel(DataUsageAppDetail.class.getName(), args,
                    R.string.data_usage_summary_title, null, null, 0);
        }
    };

    private OnItemSelectedListener mCycleListener = new OnItemSelectedListener() {
        /** {@inheritDoc} */
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            final CycleItem cycle = (CycleItem) parent.getItemAtPosition(position);
            if (cycle instanceof CycleChangeItem) {
                // show cycle editor; will eventually call setPolicyCycleDay()
                // when user finishes editing.
                CycleEditorFragment.show(DataUsageSummary.this);

                // reset spinner to something other than "change cycle..."
                mCycleSpinner.setSelection(0);

            } else {
                if (LOGD) Log.d(TAG, "showing cycle " + cycle);

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

    private static String getActiveSubscriberId(Context context) {
        final TelephonyManager telephony = (TelephonyManager) context.getSystemService(
                Context.TELEPHONY_SERVICE);
        return telephony.getSubscriberId();
    }

    private DataUsageChartListener mChartListener = new DataUsageChartListener() {
        /** {@inheritDoc} */
        public void onInspectRangeChanged() {
            if (LOGD) Log.d(TAG, "onInspectRangeChanged()");
            updateDetailData();
        }

        /** {@inheritDoc} */
        public void onWarningChanged() {
            setPolicyWarningBytes(mChart.getWarningBytes());
        }

        /** {@inheritDoc} */
        public void onLimitChanged() {
            setPolicyLimitBytes(mChart.getLimitBytes());
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

    private static class AppUsageItem implements Comparable<AppUsageItem> {
        public int uid;
        public long total;

        /** {@inheritDoc} */
        public int compareTo(AppUsageItem another) {
            return Long.compare(another.total, total);
        }
    }

    /**
     * Adapter of applications, sorted by total usage descending.
     */
    public static class DataUsageAdapter extends BaseAdapter {
        private ArrayList<AppUsageItem> mItems = Lists.newArrayList();

        public void bindStats(NetworkStats stats) {
            mItems.clear();

            for (int i = 0; i < stats.size; i++) {
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

    /**
     * Dialog to request user confirmation before setting
     * {@link NetworkPolicy#limitBytes}.
     */
    public static class ConfirmLimitFragment extends DialogFragment {
        public static final String EXTRA_MESSAGE_ID = "messageId";
        public static final String EXTRA_LIMIT_BYTES = "limitBytes";

        public static void show(DataUsageSummary parent) {
            final Bundle args = new Bundle();

            // TODO: customize default limits based on network template
            switch (parent.mTemplate) {
                case TEMPLATE_MOBILE_3G_LOWER: {
                    args.putInt(EXTRA_MESSAGE_ID, R.string.data_usage_limit_dialog_3g);
                    args.putLong(EXTRA_LIMIT_BYTES, 5 * GB_IN_BYTES);
                    break;
                }
                case TEMPLATE_MOBILE_4G: {
                    args.putInt(EXTRA_MESSAGE_ID, R.string.data_usage_limit_dialog_4g);
                    args.putLong(EXTRA_LIMIT_BYTES, 5 * GB_IN_BYTES);
                    break;
                }
                case TEMPLATE_MOBILE_ALL: {
                    args.putInt(EXTRA_MESSAGE_ID, R.string.data_usage_limit_dialog_mobile);
                    args.putLong(EXTRA_LIMIT_BYTES, 5 * GB_IN_BYTES);
                    break;
                }
            }

            final ConfirmLimitFragment dialog = new ConfirmLimitFragment();
            dialog.setArguments(args);
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), TAG_CONFIRM_LIMIT);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();

            final int messageId = getArguments().getInt(EXTRA_MESSAGE_ID);
            final long limitBytes = getArguments().getLong(EXTRA_LIMIT_BYTES);

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.data_usage_limit_dialog_title);
            builder.setMessage(messageId);

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    final DataUsageSummary target = (DataUsageSummary) getTargetFragment();
                    if (target != null) {
                        target.setPolicyLimitBytes(limitBytes);
                    }
                }
            });

            return builder.create();
        }
    }

    /**
     * Dialog to edit {@link NetworkPolicy#cycleDay}.
     */
    public static class CycleEditorFragment extends DialogFragment {
        public static final String EXTRA_CYCLE_DAY = "cycleDay";

        public static void show(DataUsageSummary parent) {
            final NetworkPolicy policy = parent.mPolicyModifier.getPolicy(parent.mTemplate);
            final Bundle args = new Bundle();
            args.putInt(CycleEditorFragment.EXTRA_CYCLE_DAY, policy.cycleDay);

            final CycleEditorFragment dialog = new CycleEditorFragment();
            dialog.setArguments(args);
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), TAG_CYCLE_EDITOR);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            final LayoutInflater dialogInflater = LayoutInflater.from(builder.getContext());

            final View view = dialogInflater.inflate(R.layout.data_usage_cycle_editor, null, false);
            final NumberPicker cycleDayPicker = (NumberPicker) view.findViewById(R.id.cycle_day);

            final int oldCycleDay = getArguments().getInt(EXTRA_CYCLE_DAY, 1);

            cycleDayPicker.setMinValue(1);
            cycleDayPicker.setMaxValue(31);
            cycleDayPicker.setValue(oldCycleDay);
            cycleDayPicker.setWrapSelectorWheel(true);

            builder.setTitle(R.string.data_usage_cycle_editor_title);
            builder.setView(view);

            builder.setPositiveButton(R.string.data_usage_cycle_editor_positive,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            final int cycleDay = cycleDayPicker.getValue();
                            final DataUsageSummary target = (DataUsageSummary) getTargetFragment();
                            if (target != null) {
                                target.setPolicyCycleDay(cycleDay);
                            }
                        }
                    });

            return builder.create();
        }
    }

}
