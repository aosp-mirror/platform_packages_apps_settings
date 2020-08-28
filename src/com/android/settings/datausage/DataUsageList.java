/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static android.net.NetworkPolicyManager.POLICY_REJECT_METERED_BACKGROUND;
import static android.net.NetworkStatsHistory.FIELD_RX_BYTES;
import static android.net.NetworkStatsHistory.FIELD_TX_BYTES;
import static android.net.TrafficStats.UID_REMOVED;
import static android.net.TrafficStats.UID_TETHERING;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.settings.SettingsEnums;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStats.Bucket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkPolicy;
import android.net.NetworkTemplate;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.View.AccessibilityDelegate;
import android.view.accessibility.AccessibilityEvent;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ImageView;
import android.widget.Spinner;

import androidx.annotation.VisibleForTesting;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.Loader;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.datausage.CycleAdapter.SpinnerInterface;
import com.android.settings.network.MobileDataEnabledListener;
import com.android.settings.network.ProxySubscriptionManager;
import com.android.settings.widget.LoadingViewController;
import com.android.settingslib.AppItem;
import com.android.settingslib.net.NetworkCycleChartData;
import com.android.settingslib.net.NetworkCycleChartDataLoader;
import com.android.settingslib.net.NetworkStatsSummaryLoader;
import com.android.settingslib.net.UidDetailProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Panel showing data usage history across various networks, including options
 * to inspect based on usage cycle and control through {@link NetworkPolicy}.
 */
public class DataUsageList extends DataUsageBaseFragment
        implements MobileDataEnabledListener.Client {

    static final String EXTRA_SUB_ID = "sub_id";
    static final String EXTRA_NETWORK_TEMPLATE = "network_template";
    static final String EXTRA_NETWORK_TYPE = "network_type";

    private static final String TAG = "DataUsageList";
    private static final boolean LOGD = false;

    private static final String KEY_USAGE_AMOUNT = "usage_amount";
    private static final String KEY_CHART_DATA = "chart_data";
    private static final String KEY_APPS_GROUP = "apps_group";
    private static final String KEY_TEMPLATE = "template";
    private static final String KEY_APP = "app";
    private static final String KEY_FIELDS = "fields";

    @VisibleForTesting
    static final int LOADER_CHART_DATA = 2;
    @VisibleForTesting
    static final int LOADER_SUMMARY = 3;

    @VisibleForTesting
    MobileDataEnabledListener mDataStateListener;

    @VisibleForTesting
    NetworkTemplate mTemplate;
    @VisibleForTesting
    int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    @VisibleForTesting
    int mNetworkType;
    @VisibleForTesting
    Spinner mCycleSpinner;
    @VisibleForTesting
    LoadingViewController mLoadingViewController;

    private ChartDataUsagePreference mChart;
    private List<NetworkCycleChartData> mCycleData;
    private ArrayList<Long> mCycles;
    private UidDetailProvider mUidDetailProvider;
    private CycleAdapter mCycleAdapter;
    private Preference mUsageAmount;
    private PreferenceGroup mApps;
    private View mHeader;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DATA_USAGE_LIST;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Activity activity = getActivity();

        if (!isBandwidthControlEnabled()) {
            Log.w(TAG, "No bandwidth control; leaving");
            activity.finish();
            return;
        }

        mUidDetailProvider = new UidDetailProvider(activity);
        mUsageAmount = findPreference(KEY_USAGE_AMOUNT);
        mChart = findPreference(KEY_CHART_DATA);
        mApps = findPreference(KEY_APPS_GROUP);
        processArgument();
        mDataStateListener = new MobileDataEnabledListener(activity, this);
    }

    @Override
    public void onViewCreated(View v, Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        mHeader = setPinnedHeaderView(R.layout.apps_filter_spinner);
        mHeader.findViewById(R.id.filter_settings).setOnClickListener(btn -> {
            final Bundle args = new Bundle();
            args.putParcelable(DataUsageList.EXTRA_NETWORK_TEMPLATE, mTemplate);
            new SubSettingLauncher(getContext())
                    .setDestination(BillingCycleSettings.class.getName())
                    .setTitleRes(R.string.billing_cycle)
                    .setSourceMetricsCategory(getMetricsCategory())
                    .setArguments(args)
                    .launch();
        });
        mCycleSpinner = mHeader.findViewById(R.id.filter_spinner);
        mCycleSpinner.setVisibility(View.GONE);
        mCycleAdapter = new CycleAdapter(mCycleSpinner.getContext(), new SpinnerInterface() {
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
        }, mCycleListener);
        mCycleSpinner.setAccessibilityDelegate(new AccessibilityDelegate() {
            @Override
            public void sendAccessibilityEvent(View host, int eventType) {
                if (eventType == AccessibilityEvent.TYPE_VIEW_SELECTED) {
                    // Ignore TYPE_VIEW_SELECTED or TalkBack will speak for it at onResume.
                    return;
                }
                super.sendAccessibilityEvent(host, eventType);
            }
        });

        mLoadingViewController = new LoadingViewController(
                getView().findViewById(R.id.loading_container), getListView());
        mLoadingViewController.showLoadingViewDelayed();
    }

    @Override
    public void onResume() {
        super.onResume();
        mDataStateListener.start(mSubId);

        // kick off loader for network history
        // TODO: consider chaining two loaders together instead of reloading
        // network history when showing app detail.
        getLoaderManager().restartLoader(LOADER_CHART_DATA,
                buildArgs(mTemplate), mNetworkCycleDataCallbacks);

        updateBody();
    }

    @Override
    public void onPause() {
        super.onPause();
        mDataStateListener.stop();

        getLoaderManager().destroyLoader(LOADER_CHART_DATA);
        getLoaderManager().destroyLoader(LOADER_SUMMARY);
    }

    @Override
    public void onDestroy() {
        mUidDetailProvider.clearCache();
        mUidDetailProvider = null;

        super.onDestroy();
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.data_usage_list;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    void processArgument() {
        final Bundle args = getArguments();
        if (args != null) {
            mSubId = args.getInt(EXTRA_SUB_ID, SubscriptionManager.INVALID_SUBSCRIPTION_ID);
            mTemplate = args.getParcelable(EXTRA_NETWORK_TEMPLATE);
            mNetworkType = args.getInt(EXTRA_NETWORK_TYPE, ConnectivityManager.TYPE_MOBILE);
        }
        if (mTemplate == null && mSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            final Intent intent = getIntent();
            mSubId = intent.getIntExtra(Settings.EXTRA_SUB_ID,
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID);
            mTemplate = intent.getParcelableExtra(Settings.EXTRA_NETWORK_TEMPLATE);
        }
    }

    /**
     * Implementation of {@code MobileDataEnabledListener.Client}
     */
    public void onMobileDataEnabledChange() {
        updatePolicy();
    }

    /**
     * Update body content based on current tab. Loads network cycle data from system, and
     * binds them to visible controls.
     */
    private void updateBody() {
        if (!isAdded()) return;

        final Context context = getActivity();

        // detail mode can change visible menus, invalidate
        getActivity().invalidateOptionsMenu();

        int seriesColor = context.getColor(R.color.sim_noitification);
        if (mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            final SubscriptionInfo sir = ProxySubscriptionManager.getInstance(context)
                    .getActiveSubscriptionInfo(mSubId);

            if (sir != null) {
                seriesColor = sir.getIconTint();
            }
        }

        final int secondaryColor = Color.argb(127, Color.red(seriesColor), Color.green(seriesColor),
                Color.blue(seriesColor));
        mChart.setColors(seriesColor, secondaryColor);
    }

    private Bundle buildArgs(NetworkTemplate template) {
        final Bundle args = new Bundle();
        args.putParcelable(KEY_TEMPLATE, template);
        args.putParcelable(KEY_APP, null);
        args.putInt(KEY_FIELDS, FIELD_RX_BYTES | FIELD_TX_BYTES);
        return args;
    }

    /**
     * Update chart sweeps and cycle list to reflect {@link NetworkPolicy} for
     * current {@link #mTemplate}.
     */
    @VisibleForTesting
    void updatePolicy() {
        final NetworkPolicy policy = services.mPolicyEditor.getPolicy(mTemplate);
        final View configureButton = mHeader.findViewById(R.id.filter_settings);
        //SUB SELECT
        if (isNetworkPolicyModifiable(policy, mSubId) && isMobileDataAvailable(mSubId)) {
            mChart.setNetworkPolicy(policy);
            configureButton.setVisibility(View.VISIBLE);
            ((ImageView) configureButton).setColorFilter(android.R.color.white);
        } else {
            // controls are disabled; don't bind warning/limit sweeps
            mChart.setNetworkPolicy(null);
            configureButton.setVisibility(View.GONE);
        }

        // generate cycle list based on policy and available history
        if (mCycleAdapter.updateCycleList(mCycleData)) {
            updateDetailData();
        }
    }

    /**
     * Update details based on {@link #mChart} inspection range depending on
     * current mode. Updates {@link #mAdapter} with sorted list
     * of applications data usage.
     */
    private void updateDetailData() {
        if (LOGD) Log.d(TAG, "updateDetailData()");

        // kick off loader for detailed stats
        getLoaderManager().restartLoader(LOADER_SUMMARY, null /* args */,
                mNetworkStatsDetailCallbacks);

        final long totalBytes = mCycleData != null && !mCycleData.isEmpty()
            ? mCycleData.get(mCycleSpinner.getSelectedItemPosition()).getTotalUsage() : 0;
        final CharSequence totalPhrase = DataUsageUtils.formatDataUsage(getActivity(), totalBytes);
        mUsageAmount.setTitle(getString(R.string.data_used_template, totalPhrase));
    }

    /**
     * Bind the given {@link NetworkStats}, or {@code null} to clear list.
     */
    private void bindStats(NetworkStats stats, int[] restrictedUids) {
        mApps.removeAll();
        if (stats == null) {
            if (LOGD) {
                Log.d(TAG, "No network stats data. App list cleared.");
            }
            return;
        }

        final ArrayList<AppItem> items = new ArrayList<>();
        long largest = 0;

        final int currentUserId = ActivityManager.getCurrentUser();
        final UserManager userManager = UserManager.get(getContext());
        final List<UserHandle> profiles = userManager.getUserProfiles();
        final SparseArray<AppItem> knownItems = new SparseArray<AppItem>();

        final Bucket bucket = new Bucket();
        while (stats.hasNextBucket() && stats.getNextBucket(bucket)) {
            // Decide how to collapse items together
            final int uid = bucket.getUid();
            final int collapseKey;
            final int category;
            final int userId = UserHandle.getUserId(uid);
            if (UserHandle.isApp(uid)) {
                if (profiles.contains(new UserHandle(userId))) {
                    if (userId != currentUserId) {
                        // Add to a managed user item.
                        final int managedKey = UidDetailProvider.buildKeyForUser(userId);
                        largest = accumulate(managedKey, knownItems, bucket,
                            AppItem.CATEGORY_USER, items, largest);
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
            } else if (uid == UID_REMOVED || uid == UID_TETHERING
                    || uid == Process.OTA_UPDATE_UID) {
                collapseKey = uid;
                category = AppItem.CATEGORY_APP;
            } else {
                collapseKey = android.os.Process.SYSTEM_UID;
                category = AppItem.CATEGORY_APP;
            }
            largest = accumulate(collapseKey, knownItems, bucket, category, items, largest);
        }
        stats.close();

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
        for (int i = 0; i < items.size(); i++) {
            final int percentTotal = largest != 0 ? (int) (items.get(i).total * 100 / largest) : 0;
            final AppDataUsagePreference preference = new AppDataUsagePreference(getContext(),
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

    @VisibleForTesting
    void startAppDataUsage(AppItem item) {
        final Bundle args = new Bundle();
        args.putParcelable(AppDataUsage.ARG_APP_ITEM, item);
        args.putParcelable(AppDataUsage.ARG_NETWORK_TEMPLATE, mTemplate);
        if (mCycles == null) {
            mCycles = new ArrayList<>();
            for (NetworkCycleChartData data : mCycleData) {
                if (mCycles.isEmpty()) {
                    mCycles.add(data.getEndTime());
                }
                mCycles.add(data.getStartTime());
            }
        }
        args.putSerializable(AppDataUsage.ARG_NETWORK_CYCLES, mCycles);
        args.putLong(AppDataUsage.ARG_SELECTED_CYCLE,
            mCycleData.get(mCycleSpinner.getSelectedItemPosition()).getEndTime());

        new SubSettingLauncher(getContext())
                .setDestination(AppDataUsage.class.getName())
                .setTitleRes(R.string.data_usage_app_summary_title)
                .setArguments(args)
                .setSourceMetricsCategory(getMetricsCategory())
                .launch();
    }

    /**
     * Accumulate data usage of a network stats entry for the item mapped by the collapse key.
     * Creates the item if needed.
     *
     * @param collapseKey  the collapse key used to map the item.
     * @param knownItems   collection of known (already existing) items.
     * @param bucket       the network stats bucket to extract data usage from.
     * @param itemCategory the item is categorized on the list view by this category. Must be
     */
    private static long accumulate(int collapseKey, final SparseArray<AppItem> knownItems,
            Bucket bucket, int itemCategory, ArrayList<AppItem> items, long largest) {
        final int uid = bucket.getUid();
        AppItem item = knownItems.get(collapseKey);
        if (item == null) {
            item = new AppItem(collapseKey);
            item.category = itemCategory;
            items.add(item);
            knownItems.put(item.key, item);
        }
        item.addUid(uid);
        item.total += bucket.getRxBytes() + bucket.getTxBytes();
        return Math.max(largest, item.total);
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
            mChart.setNetworkCycleData(mCycleData.get(position));

            updateDetailData();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            // ignored
        }
    };

    @VisibleForTesting
    final LoaderCallbacks<List<NetworkCycleChartData>> mNetworkCycleDataCallbacks =
            new LoaderCallbacks<List<NetworkCycleChartData>>() {
        @Override
        public Loader<List<NetworkCycleChartData>> onCreateLoader(int id, Bundle args) {
            return NetworkCycleChartDataLoader.builder(getContext())
                    .setNetworkTemplate(mTemplate)
                    .build();
        }

        @Override
        public void onLoadFinished(Loader<List<NetworkCycleChartData>> loader,
                List<NetworkCycleChartData> data) {
            mLoadingViewController.showContent(false /* animate */);
            mCycleData = data;
            // calculate policy cycles based on available data
            updatePolicy();
            mCycleSpinner.setVisibility(View.VISIBLE);
        }

        @Override
        public void onLoaderReset(Loader<List<NetworkCycleChartData>> loader) {
            mCycleData = null;
        }
    };

    private final LoaderCallbacks<NetworkStats> mNetworkStatsDetailCallbacks =
            new LoaderCallbacks<NetworkStats>() {
        @Override
        public Loader<NetworkStats> onCreateLoader(int id, Bundle args) {
            return new NetworkStatsSummaryLoader.Builder(getContext())
                    .setStartTime(mChart.getInspectStart())
                    .setEndTime(mChart.getInspectEnd())
                    .setNetworkTemplate(mTemplate)
                    .build();
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
