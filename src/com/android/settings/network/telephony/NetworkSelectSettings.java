/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.network.telephony;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.telephony.AccessNetworkConstants;
import android.telephony.CarrierConfigManager;
import android.telephony.CellIdentity;
import android.telephony.CellInfo;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.satellite.SatelliteManager;
import android.util.Log;
import android.view.View;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.android.internal.annotations.Initializer;
import com.android.internal.telephony.OperatorInfo;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.utils.ThreadUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * "Choose network" settings UI for the Settings app.
 */
@Keep
public class NetworkSelectSettings extends DashboardFragment {

    private static final String TAG = "NetworkSelectSettings";

    private static final int EVENT_SET_NETWORK_SELECTION_MANUALLY_DONE = 1;
    private static final int EVENT_NETWORK_SCAN_RESULTS = 2;
    private static final int EVENT_NETWORK_SCAN_ERROR = 3;
    private static final int EVENT_NETWORK_SCAN_COMPLETED = 4;

    private static final String PREF_KEY_NETWORK_OPERATORS = "network_operators_preference";
    private static final int MIN_NUMBER_OF_SCAN_REQUIRED = 2;

    private PreferenceCategory mPreferenceCategory;
    @VisibleForTesting
    NetworkOperatorPreference mSelectedPreference;
    private View mProgressHeader;
    private Preference mStatusMessagePreference;
    @VisibleForTesting
    List<CellInfo> mCellInfoList;
    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    private TelephonyManager mTelephonyManager;
    private SatelliteManager mSatelliteManager;
    private CarrierConfigManager mCarrierConfigManager;
    private List<String> mForbiddenPlmns;
    private boolean mShow4GForLTE = false;
    private NetworkScanHelper mNetworkScanHelper;
    private final ExecutorService mNetworkScanExecutor = Executors.newFixedThreadPool(1);
    private MetricsFeatureProvider mMetricsFeatureProvider;
    private boolean mUseNewApi;
    private long mRequestIdManualNetworkSelect;
    private long mRequestIdManualNetworkScan;
    private long mWaitingForNumberOfScanResults;
    @VisibleForTesting
    boolean mIsAggregationEnabled = false;
    private CarrierConfigManager.CarrierConfigChangeListener mCarrierConfigChangeListener;
    private AtomicBoolean mShouldFilterOutSatellitePlmn = new AtomicBoolean();

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        onCreateInitialization();
    }

    @Keep
    @VisibleForTesting
    @Initializer
    protected void onCreateInitialization() {
        Context context = getContext();
        mUseNewApi = enableNewAutoSelectNetworkUI(context);
        mSubId = getSubId();

        mPreferenceCategory = getPreferenceCategory(PREF_KEY_NETWORK_OPERATORS);
        mStatusMessagePreference = new Preference(context);
        mStatusMessagePreference.setSelectable(false);
        mSelectedPreference = null;
        mTelephonyManager = getTelephonyManager(context, mSubId);
        mSatelliteManager = getSatelliteManager(context);
        mCarrierConfigManager = getCarrierConfigManager(context);
        mNetworkScanHelper = new NetworkScanHelper(
                mTelephonyManager, mCallback, mNetworkScanExecutor);
        PersistableBundle bundle = mCarrierConfigManager.getConfigForSubId(mSubId,
                CarrierConfigManager.KEY_SHOW_4G_FOR_LTE_DATA_ICON_BOOL,
                CarrierConfigManager.KEY_REMOVE_SATELLITE_PLMN_IN_MANUAL_NETWORK_SCAN_BOOL);
        mShow4GForLTE = bundle.getBoolean(CarrierConfigManager.KEY_SHOW_4G_FOR_LTE_DATA_ICON_BOOL,
                false);
        mShouldFilterOutSatellitePlmn.set(bundle.getBoolean(
                CarrierConfigManager.KEY_REMOVE_SATELLITE_PLMN_IN_MANUAL_NETWORK_SCAN_BOOL,
                true));

        mMetricsFeatureProvider = getMetricsFeatureProvider(context);
        mIsAggregationEnabled = enableAggregation(context);
        Log.d(TAG, "init: mUseNewApi:" + mUseNewApi
                + " ,mIsAggregationEnabled:" + mIsAggregationEnabled + " ,mSubId:" + mSubId);

        mCarrierConfigChangeListener =
                (slotIndex, subId, carrierId, specificCarrierId) -> handleCarrierConfigChanged(
                        subId);
        mCarrierConfigManager.registerCarrierConfigChangeListener(mNetworkScanExecutor,
                mCarrierConfigChangeListener);

    }

    @Keep
    @VisibleForTesting
    protected boolean enableNewAutoSelectNetworkUI(Context context) {
        return context.getResources().getBoolean(
                com.android.internal.R.bool.config_enableNewAutoSelectNetworkUI);
    }

    @Keep
    @VisibleForTesting
    protected boolean enableAggregation(Context context) {
        return context.getResources().getBoolean(
                R.bool.config_network_selection_list_aggregation_enabled);
    }

    @Keep
    @VisibleForTesting
    protected PreferenceCategory getPreferenceCategory(String preferenceKey) {
        return findPreference(preferenceKey);
    }

    @Keep
    @VisibleForTesting
    protected TelephonyManager getTelephonyManager(Context context, int subscriptionId) {
        return context.getSystemService(TelephonyManager.class)
                .createForSubscriptionId(subscriptionId);
    }

    @Keep
    @VisibleForTesting
    protected CarrierConfigManager getCarrierConfigManager(Context context) {
        return context.getSystemService(CarrierConfigManager.class);
    }

    @Keep
    @VisibleForTesting
    protected MetricsFeatureProvider getMetricsFeatureProvider(Context context) {
        return FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    @Keep
    @VisibleForTesting
    protected SatelliteManager getSatelliteManager(Context context) {
        return context.getSystemService(SatelliteManager.class);
    }

    @Keep
    @VisibleForTesting
    protected boolean isPreferenceScreenEnabled() {
        return getPreferenceScreen().isEnabled();
    }

    @Keep
    @VisibleForTesting
    protected void enablePreferenceScreen(boolean enable) {
        getPreferenceScreen().setEnabled(enable);
    }

    @Keep
    @VisibleForTesting
    protected int getSubId() {
        int subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        Intent intent = getActivity().getIntent();
        if (intent != null) {
            subId = intent.getIntExtra(Settings.EXTRA_SUB_ID,
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        }
        return subId;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Activity activity = getActivity();
        if (activity != null) {
            mProgressHeader = setPinnedHeaderView(
                    com.android.settingslib.widget.progressbar.R.layout.progress_header)
                    .findViewById(com.android.settingslib.widget.progressbar.R.id.progress_bar_animation);
            setProgressBarVisible(false);
        }
        forceUpdateConnectedPreferenceCategory();
    }

    @Override
    public void onStart() {
        super.onStart();

        updateForbiddenPlmns();
        if (isProgressBarVisible()) {
            return;
        }
        if (mWaitingForNumberOfScanResults <= 0) {
            startNetworkQuery();
        }
    }

    /**
     * Update forbidden PLMNs from the USIM App
     */
    @Keep
    @VisibleForTesting
    protected void updateForbiddenPlmns() {
        final String[] forbiddenPlmns = mTelephonyManager.getForbiddenPlmns();
        mForbiddenPlmns = forbiddenPlmns != null
                ? Arrays.asList(forbiddenPlmns)
                : new ArrayList<>();
    }

    @Override
    public void onStop() {
        if (mWaitingForNumberOfScanResults <= 0) {
            stopNetworkQuery();
        }
        super.onStop();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mSelectedPreference) {
            Log.d(TAG, "onPreferenceTreeClick: preference is mSelectedPreference. Do nothing.");
            return true;
        }
        if (!(preference instanceof NetworkOperatorPreference)) {
            Log.d(TAG, "onPreferenceTreeClick: preference is not the NetworkOperatorPreference.");
            return false;
        }

        stopNetworkQuery();

        // Refresh the last selected item in case users reselect network.
        clearPreferenceSummary();
        if (mSelectedPreference != null) {
            // Set summary as "Disconnected" to the previously connected network
            mSelectedPreference.setSummary(R.string.network_disconnected);
        }

        mSelectedPreference = (NetworkOperatorPreference) preference;
        mSelectedPreference.setSummary(R.string.network_connecting);

        mMetricsFeatureProvider.action(getContext(),
                SettingsEnums.ACTION_MOBILE_NETWORK_MANUAL_SELECT_NETWORK);

        setProgressBarVisible(true);
        // Disable the screen until network is manually set
        enablePreferenceScreen(false);

        mRequestIdManualNetworkSelect = getNewRequestId();
        mWaitingForNumberOfScanResults = MIN_NUMBER_OF_SCAN_REQUIRED;
        final OperatorInfo operator = mSelectedPreference.getOperatorInfo();
        ThreadUtils.postOnBackgroundThread(() -> {
            final Message msg = mHandler.obtainMessage(
                    EVENT_SET_NETWORK_SELECTION_MANUALLY_DONE);
            msg.obj = mTelephonyManager.setNetworkSelectionModeManual(
                    operator, true /* persistSelection */);
            msg.sendToTarget();
        });

        return true;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.choose_network;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.MOBILE_NETWORK_SELECT;
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_SET_NETWORK_SELECTION_MANUALLY_DONE:
                    final boolean isSucceed = (boolean) msg.obj;
                    stopNetworkQuery();
                    setProgressBarVisible(false);
                    enablePreferenceScreen(true);

                    if (mSelectedPreference != null) {
                        mSelectedPreference.setSummary(isSucceed
                                ? R.string.network_connected
                                : R.string.network_could_not_connect);
                    } else {
                        Log.e(TAG, "No preference to update!");
                    }
                    break;
                case EVENT_NETWORK_SCAN_RESULTS:
                    scanResultHandler((List<CellInfo>) msg.obj);
                    break;

                case EVENT_NETWORK_SCAN_ERROR:
                    stopNetworkQuery();
                    Log.i(TAG, "Network scan failure " + msg.arg1 + ":"
                            + " scan request 0x" + Long.toHexString(mRequestIdManualNetworkScan)
                            + ", waiting for scan results = " + mWaitingForNumberOfScanResults
                            + ", select request 0x"
                            + Long.toHexString(mRequestIdManualNetworkSelect));
                    if (mRequestIdManualNetworkScan < mRequestIdManualNetworkSelect) {
                        break;
                    }
                    if (!isPreferenceScreenEnabled()) {
                        clearPreferenceSummary();
                        enablePreferenceScreen(true);
                    } else {
                        addMessagePreference(R.string.network_query_error);
                    }
                    break;

                case EVENT_NETWORK_SCAN_COMPLETED:
                    stopNetworkQuery();
                    Log.d(TAG, "Network scan complete:"
                            + " scan request 0x" + Long.toHexString(mRequestIdManualNetworkScan)
                            + ", waiting for scan results = " + mWaitingForNumberOfScanResults
                            + ", select request 0x"
                            + Long.toHexString(mRequestIdManualNetworkSelect));
                    if (mRequestIdManualNetworkScan < mRequestIdManualNetworkSelect) {
                        break;
                    }
                    if (!isPreferenceScreenEnabled()) {
                        clearPreferenceSummary();
                        enablePreferenceScreen(true);
                    } else if (mCellInfoList == null) {
                        // In case the scan timeout before getting any results
                        addMessagePreference(R.string.empty_networks_list);
                    }
                    break;
            }
            return;
        }
    };

    @VisibleForTesting
    List<CellInfo> doAggregation(List<CellInfo> cellInfoListInput) {
        if (!mIsAggregationEnabled) {
            Log.d(TAG, "no aggregation");
            return new ArrayList<>(cellInfoListInput);
        }
        ArrayList<CellInfo> aggregatedList = new ArrayList<>();
        for (CellInfo cellInfo : cellInfoListInput) {
            String plmn = CellInfoUtil.getNetworkTitle(cellInfo.getCellIdentity());
            Class className = cellInfo.getClass();

            Optional<CellInfo> itemInTheList = aggregatedList.stream().filter(
                    item -> {
                        String itemPlmn = CellInfoUtil.getNetworkTitle(item.getCellIdentity());
                        return itemPlmn.equals(plmn) && item.getClass().equals(className);
                    })
                    .findFirst();
            if (itemInTheList.isPresent()) {
                if (cellInfo.isRegistered() && !itemInTheList.get().isRegistered()) {
                    // Adding the registered cellinfo item into list. If there are two registered
                    // cellinfo items, then select first one from source list.
                    aggregatedList.set(aggregatedList.indexOf(itemInTheList.get()), cellInfo);
                }
                continue;
            }
            aggregatedList.add(cellInfo);
        }

        return filterOutSatellitePlmn(aggregatedList);
    }

    /* We do not want to expose carrier satellite plmns to the user when manually scan the
       cellular network. Therefore, it is needed to filter out satellite plmns from current cell
       info list  */
    private List<CellInfo> filterOutSatellitePlmn(List<CellInfo> cellInfoList) {
        List<String> aggregatedSatellitePlmn = getSatellitePlmnsForCarrierWrapper();
        if (!mShouldFilterOutSatellitePlmn.get() || aggregatedSatellitePlmn.isEmpty()) {
            return cellInfoList;
        }
        return cellInfoList.stream()
                .filter(cellInfo -> !aggregatedSatellitePlmn.contains(
                        CellInfoUtil.getOperatorNumeric(cellInfo.getCellIdentity())))
                .collect(Collectors.toList());
    }

    /**
     * Serves as a wrapper method for {@link SatelliteManager#getSatellitePlmnsForCarrier(int)}.
     * Since SatelliteManager is final, this wrapper enables mocking or spying of
     * {@link SatelliteManager#getSatellitePlmnsForCarrier(int)} for unit testing purposes.
     */
    @VisibleForTesting
    protected List<String> getSatellitePlmnsForCarrierWrapper() {
        return mSatelliteManager.getSatellitePlmnsForCarrier(mSubId);
    }

    private void handleCarrierConfigChanged(int subId) {
        PersistableBundle config = mCarrierConfigManager.getConfigForSubId(subId,
                CarrierConfigManager.KEY_REMOVE_SATELLITE_PLMN_IN_MANUAL_NETWORK_SCAN_BOOL);
        boolean shouldFilterSatellitePlmn = config.getBoolean(
                CarrierConfigManager.KEY_REMOVE_SATELLITE_PLMN_IN_MANUAL_NETWORK_SCAN_BOOL,
                true);
        if (shouldFilterSatellitePlmn != mShouldFilterOutSatellitePlmn.get()) {
            mShouldFilterOutSatellitePlmn.set(shouldFilterSatellitePlmn);
        }
    }

    private final NetworkScanHelper.NetworkScanCallback mCallback =
            new NetworkScanHelper.NetworkScanCallback() {
                public void onResults(List<CellInfo> results) {
                    final Message msg = mHandler.obtainMessage(EVENT_NETWORK_SCAN_RESULTS, results);
                    msg.sendToTarget();
                }

                public void onComplete() {
                    final Message msg = mHandler.obtainMessage(EVENT_NETWORK_SCAN_COMPLETED);
                    msg.sendToTarget();
                }

                public void onError(int error) {
                    final Message msg = mHandler.obtainMessage(EVENT_NETWORK_SCAN_ERROR, error,
                            0 /* arg2 */);
                    msg.sendToTarget();
                }
            };

    @Keep
    @VisibleForTesting
    protected void scanResultHandler(List<CellInfo> results) {
        if (mRequestIdManualNetworkScan < mRequestIdManualNetworkSelect) {
            Log.d(TAG, "CellInfoList (drop): "
                    + CellInfoUtil.cellInfoListToString(new ArrayList<>(results)));
            return;
        }
        mWaitingForNumberOfScanResults--;
        if ((mWaitingForNumberOfScanResults <= 0) && (!isResumed())) {
            stopNetworkQuery();
        }

        mCellInfoList = doAggregation(results);
        Log.d(TAG, "CellInfoList: " + CellInfoUtil.cellInfoListToString(mCellInfoList));
        if (mCellInfoList != null && mCellInfoList.size() != 0) {
            final NetworkOperatorPreference connectedPref = updateAllPreferenceCategory();
            if (connectedPref != null) {
                // update selected preference instance into connected preference
                if (mSelectedPreference != null) {
                    mSelectedPreference = connectedPref;
                }
            } else if (!isPreferenceScreenEnabled()) {
                mSelectedPreference.setSummary(R.string.network_connecting);
            }
            enablePreferenceScreen(true);
        } else if (isPreferenceScreenEnabled()) {
            addMessagePreference(R.string.empty_networks_list);
            // keep showing progress bar, it will be stopped when error or completed
            setProgressBarVisible(true);
        }
    }

    @Keep
    @VisibleForTesting
    protected NetworkOperatorPreference createNetworkOperatorPreference(CellInfo cellInfo) {
        if (mForbiddenPlmns == null) {
            updateForbiddenPlmns();
        }
        NetworkOperatorPreference preference =
                new NetworkOperatorPreference(getPrefContext(), mForbiddenPlmns, mShow4GForLTE);
        preference.updateCell(cellInfo);
        return preference;
    }

    /**
     * Update the content of network operators list.
     *
     * @return preference which shows connected
     */
    @Nullable
    private NetworkOperatorPreference updateAllPreferenceCategory() {
        int numberOfPreferences = mPreferenceCategory.getPreferenceCount();

        // remove unused preferences
        while (numberOfPreferences > mCellInfoList.size()) {
            numberOfPreferences--;
            mPreferenceCategory.removePreference(
                    mPreferenceCategory.getPreference(numberOfPreferences));
        }

        // update the content of preference
        NetworkOperatorPreference connectedPref = null;
        for (int index = 0; index < mCellInfoList.size(); index++) {
            final CellInfo cellInfo = mCellInfoList.get(index);

            NetworkOperatorPreference pref = null;
            if (index < numberOfPreferences) {
                final Preference rawPref = mPreferenceCategory.getPreference(index);
                if (rawPref instanceof NetworkOperatorPreference) {
                    // replace existing preference
                    pref = (NetworkOperatorPreference) rawPref;
                    pref.updateCell(cellInfo);
                } else {
                    mPreferenceCategory.removePreference(rawPref);
                }
            }
            if (pref == null) {
                // add new preference
                pref = createNetworkOperatorPreference(cellInfo);
                pref.setOrder(index);
                mPreferenceCategory.addPreference(pref);
            }
            pref.setKey(pref.getOperatorName());

            if (mCellInfoList.get(index).isRegistered()) {
                pref.setSummary(R.string.network_connected);
                connectedPref = pref;
            } else {
                pref.setSummary(null);
            }
        }

        // update selected preference instance by index
        for (int index = 0; index < mCellInfoList.size(); index++) {
            final CellInfo cellInfo = mCellInfoList.get(index);

            if ((mSelectedPreference != null) && mSelectedPreference.isSameCell(cellInfo)) {
                mSelectedPreference = (NetworkOperatorPreference)
                        (mPreferenceCategory.getPreference(index));
            }
        }

        return connectedPref;
    }

    /**
     * Config the network operator list when the page was created. When user get
     * into this page, the device might or might not have data connection.
     * - If the device has data:
     * 1. use {@code ServiceState#getNetworkRegistrationInfoList()} to get the currently
     * registered cellIdentity, wrap it into a CellInfo;
     * 2. set the signal strength level as strong;
     * 3. get the title of the previously connected network operator, since the CellIdentity
     * got from step 1 only has PLMN.
     * - If the device has no data, we will remove the connected network operators list from the
     * screen.
     */
    private void forceUpdateConnectedPreferenceCategory() {
        if (mTelephonyManager.getDataState() == mTelephonyManager.DATA_CONNECTED) {
            // Try to get the network registration states
            final ServiceState ss = mTelephonyManager.getServiceState();
            if (ss == null) {
                return;
            }
            final List<NetworkRegistrationInfo> networkList =
                    ss.getNetworkRegistrationInfoListForTransportType(
                            AccessNetworkConstants.TRANSPORT_TYPE_WWAN);
            if (networkList == null || networkList.size() == 0) {
                return;
            }
            // Due to the aggregation of cell between carriers, it's possible to get CellIdentity
            // containing forbidden PLMN.
            // Getting current network from ServiceState is no longer a good idea.
            // Add an additional rule to avoid from showing forbidden PLMN to the user.
            if (mForbiddenPlmns == null) {
                updateForbiddenPlmns();
            }
            for (NetworkRegistrationInfo regInfo : networkList) {
                final CellIdentity cellIdentity = regInfo.getCellIdentity();
                if (cellIdentity == null) {
                    continue;
                }
                final NetworkOperatorPreference pref = new NetworkOperatorPreference(
                        getPrefContext(), mForbiddenPlmns, mShow4GForLTE);
                pref.updateCell(null, cellIdentity);
                if (pref.isForbiddenNetwork()) {
                    continue;
                }
                pref.setSummary(R.string.network_connected);
                // Update the signal strength icon, since the default signalStrength value
                // would be zero
                // (it would be quite confusing why the connected network has no signal)
                pref.setIcon(SignalStrength.NUM_SIGNAL_STRENGTH_BINS - 1);
                mPreferenceCategory.addPreference(pref);
                break;
            }
        }
    }

    /**
     * Clear all of the preference summary
     */
    private void clearPreferenceSummary() {
        int idxPreference = mPreferenceCategory.getPreferenceCount();
        while (idxPreference > 0) {
            idxPreference--;
            final Preference networkOperator = mPreferenceCategory.getPreference(idxPreference);
            networkOperator.setSummary(null);
        }
    }

    private long getNewRequestId() {
        return Math.max(mRequestIdManualNetworkSelect,
                mRequestIdManualNetworkScan) + 1;
    }

    private boolean isProgressBarVisible() {
        if (mProgressHeader == null) {
            return false;
        }
        return (mProgressHeader.getVisibility() == View.VISIBLE);
    }

    protected void setProgressBarVisible(boolean visible) {
        if (mProgressHeader != null) {
            mProgressHeader.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void addMessagePreference(int messageId) {
        setProgressBarVisible(false);
        mStatusMessagePreference.setTitle(messageId);
        mPreferenceCategory.removeAll();
        mPreferenceCategory.addPreference(mStatusMessagePreference);
    }

    private void startNetworkQuery() {
        setProgressBarVisible(true);
        if (mNetworkScanHelper != null) {
            mRequestIdManualNetworkScan = getNewRequestId();
            mWaitingForNumberOfScanResults = MIN_NUMBER_OF_SCAN_REQUIRED;
            mNetworkScanHelper.startNetworkScan(
                    mUseNewApi
                            ? NetworkScanHelper.NETWORK_SCAN_TYPE_INCREMENTAL_RESULTS
                            : NetworkScanHelper.NETWORK_SCAN_TYPE_WAIT_FOR_ALL_RESULTS);
        }
    }

    private void stopNetworkQuery() {
        setProgressBarVisible(false);
        if (mNetworkScanHelper != null) {
            mWaitingForNumberOfScanResults = 0;
            mNetworkScanHelper.stopNetworkQuery();
        }
    }

    @Override
    public void onDestroy() {
        stopNetworkQuery();
        mNetworkScanExecutor.shutdown();
        super.onDestroy();
    }
}
