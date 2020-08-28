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
import android.util.Log;
import android.view.View;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.android.internal.telephony.OperatorInfo;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.utils.ThreadUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * "Choose network" settings UI for the Settings app.
 */
public class NetworkSelectSettings extends DashboardFragment {

    private static final String TAG = "NetworkSelectSettings";

    private static final int EVENT_SET_NETWORK_SELECTION_MANUALLY_DONE = 1;
    private static final int EVENT_NETWORK_SCAN_RESULTS = 2;
    private static final int EVENT_NETWORK_SCAN_ERROR = 3;
    private static final int EVENT_NETWORK_SCAN_COMPLETED = 4;

    private static final String PREF_KEY_NETWORK_OPERATORS = "network_operators_preference";

    @VisibleForTesting
    PreferenceCategory mPreferenceCategory;
    @VisibleForTesting
    NetworkOperatorPreference mSelectedPreference;
    private View mProgressHeader;
    private Preference mStatusMessagePreference;
    @VisibleForTesting
    List<CellInfo> mCellInfoList;
    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    @VisibleForTesting
    TelephonyManager mTelephonyManager;
    private List<String> mForbiddenPlmns;
    private boolean mShow4GForLTE = false;
    private NetworkScanHelper mNetworkScanHelper;
    private final ExecutorService mNetworkScanExecutor = Executors.newFixedThreadPool(1);
    private MetricsFeatureProvider mMetricsFeatureProvider;
    private boolean mUseNewApi;
    private long mRequestIdManualNetworkSelect;
    private long mRequestIdManualNetworkScan;
    private long mWaitingForNumberOfScanResults;

    private static final int MIN_NUMBER_OF_SCAN_REQUIRED = 2;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mUseNewApi = getContext().getResources().getBoolean(
                com.android.internal.R.bool.config_enableNewAutoSelectNetworkUI);
        mSubId = getArguments().getInt(Settings.EXTRA_SUB_ID);

        mPreferenceCategory = findPreference(PREF_KEY_NETWORK_OPERATORS);
        mStatusMessagePreference = new Preference(getContext());
        mStatusMessagePreference.setSelectable(false);
        mSelectedPreference = null;
        mTelephonyManager = getContext().getSystemService(TelephonyManager.class)
                .createForSubscriptionId(mSubId);
        mNetworkScanHelper = new NetworkScanHelper(
                mTelephonyManager, mCallback, mNetworkScanExecutor);
        PersistableBundle bundle = ((CarrierConfigManager) getContext().getSystemService(
                Context.CARRIER_CONFIG_SERVICE)).getConfigForSubId(mSubId);
        if (bundle != null) {
            mShow4GForLTE = bundle.getBoolean(
                    CarrierConfigManager.KEY_SHOW_4G_FOR_LTE_DATA_ICON_BOOL);
        }

        mMetricsFeatureProvider = FeatureFactory
                .getFactory(getContext()).getMetricsFeatureProvider();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Activity activity = getActivity();
        if (activity != null) {
            mProgressHeader = setPinnedHeaderView(R.layout.progress_header)
                    .findViewById(R.id.progress_bar_animation);
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
    @VisibleForTesting
    void updateForbiddenPlmns() {
        final String[] forbiddenPlmns = mTelephonyManager.getForbiddenPlmns();
        mForbiddenPlmns = forbiddenPlmns != null
                ? Arrays.asList(forbiddenPlmns)
                : new ArrayList<>();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mWaitingForNumberOfScanResults <= 0) {
            stopNetworkQuery();
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference != mSelectedPreference) {
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
            getPreferenceScreen().setEnabled(false);

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
        }

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
                    getPreferenceScreen().setEnabled(true);

                    if (mSelectedPreference != null) {
                        mSelectedPreference.setSummary(isSucceed
                                ? R.string.network_connected
                                : R.string.network_could_not_connect);
                    } else {
                        Log.e(TAG, "No preference to update!");
                    }
                    break;
                case EVENT_NETWORK_SCAN_RESULTS:
                    final List<CellInfo> results = (List<CellInfo>) msg.obj;
                    if (mRequestIdManualNetworkScan < mRequestIdManualNetworkSelect) {
                        Log.d(TAG, "CellInfoList (drop): "
                                + CellInfoUtil.cellInfoListToString(new ArrayList<>(results)));
                        break;
                    }
                    mWaitingForNumberOfScanResults--;
                    if ((mWaitingForNumberOfScanResults <= 0) && (!isResumed())) {
                        stopNetworkQuery();
                    }

                    mCellInfoList = new ArrayList<>(results);
                    Log.d(TAG, "CellInfoList: " + CellInfoUtil.cellInfoListToString(mCellInfoList));
                    if (mCellInfoList != null && mCellInfoList.size() != 0) {
                        final NetworkOperatorPreference connectedPref =
                                updateAllPreferenceCategory();
                        if (connectedPref != null) {
                            // update selected preference instance into connected preference
                            if (mSelectedPreference != null) {
                                mSelectedPreference = connectedPref;
                            }
                        } else if (!getPreferenceScreen().isEnabled()) {
                            if (connectedPref == null) {
                                mSelectedPreference.setSummary(R.string.network_connecting);
                            }
                        }
                        getPreferenceScreen().setEnabled(true);
                    } else if (getPreferenceScreen().isEnabled()) {
                        addMessagePreference(R.string.empty_networks_list);
                        // keep showing progress bar, it will be stopped when error or completed
                        setProgressBarVisible(true);
                    }
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
                    if (!getPreferenceScreen().isEnabled()) {
                        clearPreferenceSummary();
                        getPreferenceScreen().setEnabled(true);
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
                    if (!getPreferenceScreen().isEnabled()) {
                        clearPreferenceSummary();
                        getPreferenceScreen().setEnabled(true);
                    } else if (mCellInfoList == null) {
                        // In case the scan timeout before getting any results
                        addMessagePreference(R.string.empty_networks_list);
                    }
                    break;
            }
            return;
        }
    };

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

    /**
     * Update the content of network operators list.
     *
     * @return preference which shows connected
     */
    @VisibleForTesting
    NetworkOperatorPreference updateAllPreferenceCategory() {
        int numberOfPreferences = mPreferenceCategory.getPreferenceCount();

        // remove unused preferences
        while (numberOfPreferences > mCellInfoList.size()) {
            numberOfPreferences--;
            mPreferenceCategory.removePreference(
                    mPreferenceCategory.getPreference(numberOfPreferences));
        }

        // update selected preference instance by index
        for (int index = 0; index < mCellInfoList.size(); index++) {
            final CellInfo cellInfo = mCellInfoList.get(index);

            if ((mSelectedPreference != null) && mSelectedPreference.isSameCell(cellInfo)) {
                mSelectedPreference = (NetworkOperatorPreference)
                        (mPreferenceCategory.getPreference(index));
            }
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
                pref = new NetworkOperatorPreference(getPrefContext(),
                        cellInfo, mForbiddenPlmns, mShow4GForLTE);
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
            for (NetworkRegistrationInfo regInfo : networkList) {
                final CellIdentity cellIdentity = regInfo.getCellIdentity();
                if (cellIdentity != null) {
                    final NetworkOperatorPreference pref = new NetworkOperatorPreference(
                            getPrefContext(), cellIdentity, mForbiddenPlmns, mShow4GForLTE);
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
    }

    /**
     * Clear all of the preference summary
     */
    private void clearPreferenceSummary() {
        int idxPreference = mPreferenceCategory.getPreferenceCount();
        while (idxPreference > 0) {
            idxPreference--;
            final NetworkOperatorPreference networkOperator = (NetworkOperatorPreference)
                    (mPreferenceCategory.getPreference(idxPreference));
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
