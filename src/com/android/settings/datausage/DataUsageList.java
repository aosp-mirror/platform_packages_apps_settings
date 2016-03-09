/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.datausage;

import android.app.ActivityManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.Loader;
import android.content.pm.UserInfo;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.INetworkStatsSession;
import android.net.NetworkPolicy;
import android.net.NetworkStats;
import android.net.NetworkStatsHistory;
import android.net.NetworkTemplate;
import android.net.TrafficStats;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settingslib.AppItem;
import com.android.settingslib.net.ChartData;
import com.android.settingslib.net.ChartDataLoader;
import com.android.settingslib.net.SummaryForAllUidLoader;
import com.android.settingslib.net.UidDetailProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.net.ConnectivityManager.TYPE_MOBILE;
import static android.net.NetworkPolicyManager.POLICY_REJECT_METERED_BACKGROUND;
import static android.net.TrafficStats.UID_REMOVED;
import static android.net.TrafficStats.UID_TETHERING;
import static android.telephony.TelephonyManager.SIM_STATE_READY;
import static com.android.settings.datausage.DataUsageSummary.TEST_RADIOS;
import static com.android.settings.datausage.DataUsageSummary.TEST_RADIOS_PROP;

/**
 * Panel showing data usage history across various networks, including options
 * to inspect based on usage cycle and control through {@link NetworkPolicy}.
 */
public class DataUsageList extends DataUsageBase {
    private static final String TAG = "DataUsage";
    private static final boolean LOGD = false;

    private static final String KEY_USAGE_AMOUNT = "usage_amount";
    private static final String KEY_CHART_DATA = "chart_data";
    private static final String KEY_APPS_GROUP = "apps_group";

    private static final int LOADER_CHART_DATA = 2;
    private static final int LOADER_SUMMARY = 3;
    public static final String EXTRA_SUB_ID = "sub_id";
    public static final String EXTRA_NETWORK_TEMPLATE = "network_template";

    private INetworkStatsSession mStatsSession;

    private ChartDataUsagePreference mChart;

    private NetworkTemplate mTemplate;
    private int mSubId;
    private ChartData mChartData;

    /** Flag used to ignore listeners during binding. */
    private boolean mBinding;

    private UidDetailProvider mUidDetailProvider;

    /**
     * Local cache of data enabled for subId, used to work around delays.
     */
    private final Map<String, Boolean> mMobileDataEnabled = new HashMap<String, Boolean>();
    private CycleAdapter mCycleAdapter;
    private Spinner mCycleSpinner;
    private Preference mUsageAmount;
    private PreferenceGroup mApps;
    private View mHeader;

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.DATA_USAGE_LIST;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Context context = getActivity();

        if (!isBandwidthControlEnabled()) {
            Log.w(TAG, "No bandwidth control; leaving");
            getActivity().finish();
        }

        try {
            mStatsSession = services.mStatsService.openSession();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }

        mUidDetailProvider = new UidDetailProvider(context);

        addPreferencesFromResource(R.xml.data_usage_list);
        mUsageAmount = findPreference(KEY_USAGE_AMOUNT);
        mChart = (ChartDataUsagePreference) findPreference(KEY_CHART_DATA);
        mApps = (PreferenceGroup) findPreference(KEY_APPS_GROUP);

        final Bundle args = getArguments();
        mSubId = args.getInt(EXTRA_SUB_ID, SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        mTemplate = args.getParcelable(EXTRA_NETWORK_TEMPLATE);
    }

    @Override
    public void onViewCreated(View v, Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        mHeader = setPinnedHeaderView(R.layout.apps_filter_spinner);
        mCycleSpinner = (Spinner) mHeader.findViewById(R.id.filter_spinner);
        mCycleAdapter = new CycleAdapter(getContext(), new CycleAdapter.SpinnerInterface() {
            @Override
            public void setAdapter(CycleAdapter cycleAdapter) {
                mCycleSpinner.setAdapter(cycleAdapter);
            }

            @Override
            public void setOnItemSelectedListener(OnItemSelectedListener listener) {
                mCycleSpinner.setOnItemSelectedListener(listener);
            }

            @Override
            public Object getSelectedItem() {
                return mCycleSpinner.getSelectedItem();
            }

            @Override
            public void setSelection(int position) {
                mCycleSpinner.setSelection(position);
            }
        }, mCycleListener, true);
        setLoading(true, false);
    }

    @Override
    public void onResume() {
        super.onResume();

        updateBody();

        // kick off background task to update stats
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    // wait a few seconds before kicking off
                    Thread.sleep(2 * DateUtils.SECOND_IN_MILLIS);
                    services.mStatsService.forceUpdate();
                } catch (InterruptedException e) {
                } catch (RemoteException e) {
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                if (isAdded()) {
                    updateBody();
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onDestroy() {
        mUidDetailProvider.clearCache();
        mUidDetailProvider = null;

        TrafficStats.closeQuietly(mStatsSession);

        super.onDestroy();
    }

    /**
     * Update body content based on current tab. Loads
     * {@link NetworkStatsHistory} and {@link NetworkPolicy} from system, and
     * binds them to visible controls.
     */
    private void updateBody() {
        mBinding = true;
        if (!isAdded()) return;

        final Context context = getActivity();

        // kick off loader for network history
        // TODO: consider chaining two loaders together instead of reloading
        // network history when showing app detail.
        getLoaderManager().restartLoader(LOADER_CHART_DATA,
                ChartDataLoader.buildArgs(mTemplate, null), mChartDataCallbacks);

        // detail mode can change visible menus, invalidate
        getActivity().invalidateOptionsMenu();

        mBinding = false;

        int seriesColor = context.getColor(R.color.sim_noitification);
        if (mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID){
            final SubscriptionInfo sir = services.mSubscriptionManager
                    .getActiveSubscriptionInfo(mSubId);

            if (sir != null) {
                seriesColor = sir.getIconTint();
            }
        }

        final int secondaryColor = Color.argb(127, Color.red(seriesColor), Color.green(seriesColor),
                Color.blue(seriesColor));
        mChart.setColors(seriesColor, secondaryColor);
    }

    /**
     * Update chart sweeps and cycle list to reflect {@link NetworkPolicy} for
     * current {@link #mTemplate}.
     */
    private void updatePolicy(boolean refreshCycle) {
        final NetworkPolicy policy = services.mPolicyEditor.getPolicy(mTemplate);
        //SUB SELECT
        if (isNetworkPolicyModifiable(policy, mSubId) && isMobileDataAvailable(mSubId)) {
            mChart.setNetworkPolicy(policy);
            mHeader.findViewById(R.id.filter_settings).setVisibility(View.VISIBLE);
            mHeader.findViewById(R.id.filter_settings).setOnClickListener(
                    new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Bundle args = new Bundle();
                    args.putParcelable(DataUsageList.EXTRA_NETWORK_TEMPLATE, mTemplate);
                    startFragment(DataUsageList.this, BillingCycleSettings.class.getName(),
                            R.string.billing_cycle, 0, args);
                }
            });
        } else {
            // controls are disabled; don't bind warning/limit sweeps
            mChart.setNetworkPolicy(null);
            mHeader.findViewById(R.id.filter_settings).setVisibility(View.GONE);
        }

        if (refreshCycle) {
            // generate cycle list based on policy and available history
            if (mCycleAdapter.updateCycleList(policy, mChartData)) {
                updateDetailData();
            }
        }
    }

    /**
     * Update details based on {@link #mChart} inspection range depending on
     * current mode. Updates {@link #mAdapter} with sorted list
     * of applications data usage.
     */
    private void updateDetailData() {
        if (LOGD) Log.d(TAG, "updateDetailData()");

        final long start = mChart.getInspectStart();
        final long end = mChart.getInspectEnd();
        final long now = System.currentTimeMillis();

        final Context context = getActivity();

        NetworkStatsHistory.Entry entry = null;
        if (mChartData != null) {
            entry = mChartData.network.getValues(start, end, now, null);
        }

        // kick off loader for detailed stats
        getLoaderManager().restartLoader(LOADER_SUMMARY,
                SummaryForAllUidLoader.buildArgs(mTemplate, start, end), mSummaryCallbacks);

        final long totalBytes = entry != null ? entry.rxBytes + entry.txBytes : 0;
        final String totalPhrase = Formatter.formatFileSize(context, totalBytes);
        mUsageAmount.setTitle(getString(R.string.data_used_template, totalPhrase));
    }

    /**
     * Bind the given {@link NetworkStats}, or {@code null} to clear list.
     */
    public void bindStats(NetworkStats stats, int[] restrictedUids) {
        ArrayList<AppItem> items = new ArrayList<>();
        long largest = 0;

        final int currentUserId = ActivityManager.getCurrentUser();
        UserManager userManager = UserManager.get(getContext());
        final List<UserHandle> profiles = userManager.getUserProfiles();
        final SparseArray<AppItem> knownItems = new SparseArray<AppItem>();

        NetworkStats.Entry entry = null;
        final int size = stats != null ? stats.size() : 0;
        for (int i = 0; i < size; i++) {
            entry = stats.getValues(i, entry);

            // Decide how to collapse items together
            final int uid = entry.uid;

            final int collapseKey;
            final int category;
            final int userId = UserHandle.getUserId(uid);
            if (UserHandle.isApp(uid)) {
                if (profiles.contains(new UserHandle(userId))) {
                    if (userId != currentUserId) {
                        // Add to a managed user item.
                        final int managedKey = UidDetailProvider.buildKeyForUser(userId);
                        largest = accumulate(managedKey, knownItems, entry, AppItem.CATEGORY_USER,
                                items, largest);
                    }
                    // Add to app item.
                    collapseKey = uid;
                    category = AppItem.CATEGORY_APP;
                } else {
                    // If it is a removed user add it to the removed users' key
                    final UserInfo info = userManager.getUserInfo(userId);
                    if (info == null) {
                        collapseKey = UID_REMOVED;
                        category = AppItem.CATEGORY_APP;
                    } else {
                        // Add to other user item.
                        collapseKey = UidDetailProvider.buildKeyForUser(userId);
                        category = AppItem.CATEGORY_USER;
                    }
                }
            } else if (uid == UID_REMOVED || uid == UID_TETHERING) {
                collapseKey = uid;
                category = AppItem.CATEGORY_APP;
            } else {
                collapseKey = android.os.Process.SYSTEM_UID;
                category = AppItem.CATEGORY_APP;
            }
            largest = accumulate(collapseKey, knownItems, entry, category, items, largest);
        }

        final int restrictedUidsMax = restrictedUids.length;
        for (int i = 0; i < restrictedUidsMax; ++i) {
            final int uid = restrictedUids[i];
            // Only splice in restricted state for current user or managed users
            if (!profiles.contains(new UserHandle(UserHandle.getUserId(uid)))) {
                continue;
            }

            AppItem item = knownItems.get(uid);
            if (item == null) {
                item = new AppItem(uid);
                item.total = -1;
                items.add(item);
                knownItems.put(item.key, item);
            }
            item.restricted = true;
        }

        Collections.sort(items);
        mApps.removeAll();
        for (int i = 0; i < items.size(); i++) {
            final int percentTotal = largest != 0 ? (int) (items.get(i).total * 100 / largest) : 0;
            AppDataUsagePreference preference = new AppDataUsagePreference(getContext(),
                    items.get(i), percentTotal, mUidDetailProvider);
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    AppDataUsagePreference pref = (AppDataUsagePreference) preference;
                    AppItem item = pref.getItem();
                    startAppDataUsage(item);
                    return true;
                }
            });
            mApps.addPreference(preference);
        }
    }

    private void startAppDataUsage(AppItem item) {
        Bundle args = new Bundle();
        args.putParcelable(AppDataUsage.ARG_APP_ITEM, item);
        args.putParcelable(AppDataUsage.ARG_NETWORK_TEMPLATE, mTemplate);
        startFragment(this, AppDataUsage.class.getName(), R.string.app_data_usage, 0, args);
    }

    /**
     * Accumulate data usage of a network stats entry for the item mapped by the collapse key.
     * Creates the item if needed.
     * @param collapseKey the collapse key used to map the item.
     * @param knownItems collection of known (already existing) items.
     * @param entry the network stats entry to extract data usage from.
     * @param itemCategory the item is categorized on the list view by this category. Must be
     */
    private static long accumulate(int collapseKey, final SparseArray<AppItem> knownItems,
            NetworkStats.Entry entry, int itemCategory, ArrayList<AppItem> items, long largest) {
        final int uid = entry.uid;
        AppItem item = knownItems.get(collapseKey);
        if (item == null) {
            item = new AppItem(collapseKey);
            item.category = itemCategory;
            items.add(item);
            knownItems.put(item.key, item);
        }
        item.addUid(uid);
        item.total += entry.rxBytes + entry.txBytes;
        return Math.max(largest, item.total);
    }

    /**
     * Test if device has a mobile data radio with SIM in ready state.
     */
    public static boolean hasReadyMobileRadio(Context context) {
        if (TEST_RADIOS) {
            return SystemProperties.get(TEST_RADIOS_PROP).contains("mobile");
        }

        final ConnectivityManager conn = ConnectivityManager.from(context);
        final TelephonyManager tele = TelephonyManager.from(context);

        final List<SubscriptionInfo> subInfoList =
                SubscriptionManager.from(context).getActiveSubscriptionInfoList();
        // No activated Subscriptions
        if (subInfoList == null) {
            if (LOGD) Log.d(TAG, "hasReadyMobileRadio: subInfoList=null");
            return false;
        }
        // require both supported network and ready SIM
        boolean isReady = true;
        for (SubscriptionInfo subInfo : subInfoList) {
            isReady = isReady & tele.getSimState(subInfo.getSimSlotIndex()) == SIM_STATE_READY;
            if (LOGD) Log.d(TAG, "hasReadyMobileRadio: subInfo=" + subInfo);
        }
        boolean retVal = conn.isNetworkSupported(TYPE_MOBILE) && isReady;
        if (LOGD) {
            Log.d(TAG, "hasReadyMobileRadio:"
                    + " conn.isNetworkSupported(TYPE_MOBILE)="
                                            + conn.isNetworkSupported(TYPE_MOBILE)
                    + " isReady=" + isReady);
        }
        return retVal;
    }

    /*
     * TODO: consider adding to TelephonyManager or SubscriptionManager.
     */
    public static boolean hasReadyMobileRadio(Context context, int subId) {
        if (TEST_RADIOS) {
            return SystemProperties.get(TEST_RADIOS_PROP).contains("mobile");
        }

        final ConnectivityManager conn = ConnectivityManager.from(context);
        final TelephonyManager tele = TelephonyManager.from(context);
        final int slotId = SubscriptionManager.getSlotId(subId);
        final boolean isReady = tele.getSimState(slotId) == SIM_STATE_READY;

        boolean retVal =  conn.isNetworkSupported(TYPE_MOBILE) && isReady;
        if (LOGD) Log.d(TAG, "hasReadyMobileRadio: subId=" + subId
                + " conn.isNetworkSupported(TYPE_MOBILE)=" + conn.isNetworkSupported(TYPE_MOBILE)
                + " isReady=" + isReady);
        return retVal;
    }

    private OnItemSelectedListener mCycleListener = new OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            final CycleAdapter.CycleItem cycle = (CycleAdapter.CycleItem)
                    mCycleSpinner.getSelectedItem();

            if (LOGD) {
                Log.d(TAG, "showing cycle " + cycle + ", start=" + cycle.start + ", end="
                        + cycle.end + "]");
            }

            // update chart to show selected cycle, and update detail data
            // to match updated sweep bounds.
            mChart.setVisibleRange(cycle.start, cycle.end);

            updateDetailData();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            // ignored
        }
    };

    private final LoaderCallbacks<ChartData> mChartDataCallbacks = new LoaderCallbacks<
            ChartData>() {
        @Override
        public Loader<ChartData> onCreateLoader(int id, Bundle args) {
            return new ChartDataLoader(getActivity(), mStatsSession, args);
        }

        @Override
        public void onLoadFinished(Loader<ChartData> loader, ChartData data) {
            setLoading(false, true);
            mChartData = data;
            mChart.setNetworkStats(mChartData.network);

            // calcuate policy cycles based on available data
            updatePolicy(true);
        }

        @Override
        public void onLoaderReset(Loader<ChartData> loader) {
            mChartData = null;
            mChart.setNetworkStats(null);
        }
    };

    private final LoaderCallbacks<NetworkStats> mSummaryCallbacks = new LoaderCallbacks<
            NetworkStats>() {
        @Override
        public Loader<NetworkStats> onCreateLoader(int id, Bundle args) {
            return new SummaryForAllUidLoader(getActivity(), mStatsSession, args);
        }

        @Override
        public void onLoadFinished(Loader<NetworkStats> loader, NetworkStats data) {
            final int[] restrictedUids = services.mPolicyManager.getUidsWithPolicy(
                    POLICY_REJECT_METERED_BACKGROUND);
            bindStats(data, restrictedUids);
            updateEmptyVisible();
        }

        @Override
        public void onLoaderReset(Loader<NetworkStats> loader) {
            bindStats(null, new int[0]);
            updateEmptyVisible();
        }

        private void updateEmptyVisible() {
            if ((mApps.getPreferenceCount() != 0) !=
                    (getPreferenceScreen().getPreferenceCount() != 0)) {
                if (mApps.getPreferenceCount() != 0) {
                    getPreferenceScreen().addPreference(mUsageAmount);
                    getPreferenceScreen().addPreference(mApps);
                } else {
                    getPreferenceScreen().removeAll();
                }
            }
        }
    };
}
