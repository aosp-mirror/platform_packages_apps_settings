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
package com.android.settings.mobilenetwork;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.SearchIndexableResource;
import android.telephony.AccessNetworkConstants;
import android.telephony.CellIdentity;
import android.telephony.CellInfo;
import android.telephony.NetworkRegistrationState;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.telephony.OperatorInfo;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.utils.ThreadUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * "Choose network" settings UI for the Phone app.
 */
//TODO(b/115429509): Add test for this file once b/115429509 is not blocked anymore
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class NetworkSelectSettings extends DashboardFragment {

    private static final String TAG = "NetworkSelectSetting";
    private static final boolean DBG = true;

    public static final String KEY_SUBSCRIPTION_ID = "subscription_id";

    private static final int EVENT_SET_NETWORK_SELECTION_MANUALLY_DONE = 1;
    private static final int EVENT_NETWORK_SCAN_RESULTS = 2;
    private static final int EVENT_NETWORK_SCAN_ERROR = 3;
    private static final int EVENT_NETWORK_SCAN_COMPLETED = 4;

    private static final String PREF_KEY_CONNECTED_NETWORK_OPERATOR =
            "connected_network_operator_preference";
    private static final String PREF_KEY_NETWORK_OPERATORS = "network_operators_preference";

    // used to add/remove NetworkOperatorsPreference.
    private PreferenceCategory mNetworkOperatorsPreferences;
    // used to add/remove connected NetworkOperatorPreference.
    private PreferenceCategory mConnectedNetworkOperatorsPreference;
    // manage the progress bar on the top of the page.
    private View mProgressHeader;
    private Preference mStatusMessagePreference;
    private List<CellInfo> mCellInfoList;
    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    private NetworkOperatorPreference mSelectedNetworkOperatorPreference;
    private TelephonyManager mTelephonyManager;
    private List<String> mForbiddenPlmns;
    //Flag indicating whether we have called bind on the service.
    private boolean mShouldUnbind;

    private final Runnable mUpdateNetworkOperatorsRunnable = () -> {
        updateNetworkOperatorsPreferenceCategory();
    };

    /**
     * Create a new instance of this fragment.
     */
    public static NetworkSelectSettings newInstance(int subId) {
        Bundle args = new Bundle();
        args.putInt(KEY_SUBSCRIPTION_ID, subId);
        NetworkSelectSettings
                fragment = new NetworkSelectSettings();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle icicle) {
        if (DBG) logd("onCreate");
        super.onCreate(icicle);

        mSubId = getArguments().getInt(KEY_SUBSCRIPTION_ID);

        mConnectedNetworkOperatorsPreference =
                (PreferenceCategory) findPreference(PREF_KEY_CONNECTED_NETWORK_OPERATOR);
        mNetworkOperatorsPreferences =
                (PreferenceCategory) findPreference(PREF_KEY_NETWORK_OPERATORS);
        mStatusMessagePreference = new Preference(getContext());
        mSelectedNetworkOperatorPreference = null;
        mTelephonyManager = TelephonyManager.from(getContext()).createForSubscriptionId(mSubId);
        setRetainInstance(true);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mProgressHeader = setPinnedHeaderView(R.layout.wifi_progress_header)
                .findViewById(R.id.progress_bar_animation);
        setProgressBarVisible(false);
        forceConfigConnectedNetworkOperatorsPreferenceCategory();
    }

    @Override
    public void onStart() {
        super.onStart();
        new AsyncTask<Void, Void, List<String>>() {
            @Override
            protected List<String> doInBackground(Void... voids) {
                String[] forbiddenPlmns = mTelephonyManager.getForbiddenPlmns();
                return forbiddenPlmns != null ? Arrays.asList(forbiddenPlmns) : null;
            }

            @Override
            protected void onPostExecute(List<String> result) {
                mForbiddenPlmns = result;
                bindNetworkQueryService();
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Invoked on each preference click in this hierarchy, overrides
     * PreferenceActivity's implementation.  Used to make sure we track the
     * preference click events.
     * Since the connected network operator is either faked (when no data connection) or already
     * connected, we do not allow user to click the connected network operator.
     */
    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (DBG) logd("User clicked the screen");
        stopNetworkQuery();
        setProgressBarVisible(false);
        if (preference instanceof NetworkOperatorPreference) {
            // Refresh the last selected item in case users reselect network.
            if (mSelectedNetworkOperatorPreference != null) {
                mSelectedNetworkOperatorPreference.setSummary("");
            }

            mSelectedNetworkOperatorPreference = (NetworkOperatorPreference) preference;
            CellInfo cellInfo = mSelectedNetworkOperatorPreference.getCellInfo();
            if (DBG) logd("User click a NetworkOperatorPreference: " + cellInfo.toString());

            // Send metrics event
            MetricsLogger.action(getContext(),
                    MetricsEvent.ACTION_MOBILE_NETWORK_MANUAL_SELECT_NETWORK);

            // Connect to the network
            if (SubscriptionManager.isValidSubscriptionId(mSubId)) {
                if (DBG) {
                    logd("Connect to the network: " + CellInfoUtil.getNetworkTitle(cellInfo));
                }
                // Set summary as "Connecting" to the selected network.
                mSelectedNetworkOperatorPreference.setSummary(R.string.network_connecting);

                // Set summary as "Disconnected" to the previously connected network
                if (mConnectedNetworkOperatorsPreference.getPreferenceCount() > 0) {
                    NetworkOperatorPreference connectedNetworkOperator = (NetworkOperatorPreference)
                            (mConnectedNetworkOperatorsPreference.getPreference(0));
                    if (!CellInfoUtil.getNetworkTitle(cellInfo).equals(
                            CellInfoUtil.getNetworkTitle(connectedNetworkOperator.getCellInfo()))) {
                        connectedNetworkOperator.setSummary(R.string.network_disconnected);
                    }
                }

                OperatorInfo operatorInfo = CellInfoUtil.getOperatorInfoFromCellInfo(cellInfo);
                if (DBG) logd("manually selected network operator: " + operatorInfo.toString());

                ThreadUtils.postOnBackgroundThread(() -> {
                    Message msg = mHandler.obtainMessage(EVENT_SET_NETWORK_SELECTION_MANUALLY_DONE);
                    msg.obj = mTelephonyManager.setNetworkSelectionModeManual(
                            operatorInfo.getOperatorNumeric(), true /* persistSelection */);
                    msg.sendToTarget();
                });

                setProgressBarVisible(true);
                return true;
            } else {
                loge("Error selecting network. Subscription Id is invalid.");
                mSelectedNetworkOperatorPreference = null;
                return false;
            }

        } else {
            getPreferenceScreen().setEnabled(false);
            return false;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (DBG) logd("onStop");
        getView().removeCallbacks(mUpdateNetworkOperatorsRunnable);
        stopNetworkQuery();
        // Unbind the NetworkQueryService
        unbindNetworkQueryService();
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
        //TODO(b/114749736): add metrics id for this page
        return 0;
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_SET_NETWORK_SELECTION_MANUALLY_DONE:
                    if (DBG) logd("network selection done: hide the progress header");
                    setProgressBarVisible(false);

                    boolean isSuccessed = (boolean) msg.obj;
                    if (isSuccessed) {
                        if (DBG) logd("manual network selection: succeeded! ");
                        // Set summary as "Connected" to the selected network.
                        mSelectedNetworkOperatorPreference.setSummary(R.string.network_connected);
                    } else {
                        if (DBG) logd("manual network selection: failed! ");
                        updateNetworkSelection();
                        // Set summary as "Couldn't connect" to the selected network.
                        mSelectedNetworkOperatorPreference.setSummary(
                                R.string.network_could_not_connect);
                    }
                    break;

                case EVENT_NETWORK_SCAN_RESULTS:
                    List<CellInfo> results = aggregateCellInfoList((List<CellInfo>) msg.obj);
                    mCellInfoList = new ArrayList<>(results);
                    if (DBG) logd("after aggregate: " + mCellInfoList.toString());
                    if (mCellInfoList != null && mCellInfoList.size() != 0) {
                        updateNetworkOperators();
                    } else {
                        addMessagePreference(R.string.empty_networks_list);
                    }

                    break;

                case EVENT_NETWORK_SCAN_ERROR:
                    int error = msg.arg1;
                    if (DBG) logd("error while querying available networks " + error);
                    stopNetworkQuery();
                    addMessagePreference(R.string.network_query_error);
                    break;

                case EVENT_NETWORK_SCAN_COMPLETED:
                    stopNetworkQuery();
                    if (DBG) logd("scan complete");
                    if (mCellInfoList == null) {
                        // In case the scan timeout before getting any results
                        addMessagePreference(R.string.empty_networks_list);
                    }
                    break;
            }
            return;
        }
    };

    private void updateNetworkOperators() {
        if (DBG) logd("updateNetworkOperators");
        if (getActivity() != null) {
            final View view = getView();
            final Handler handler = view.getHandler();
            if (handler != null && handler.hasCallbacks(mUpdateNetworkOperatorsRunnable)) {
                return;
            }
            view.post(mUpdateNetworkOperatorsRunnable);
        }
    }

    /**
     * Update the currently available network operators list, which only contains the unregistered
     * network operators. So if the device has no data and the network operator in the connected
     * network operator category shows "Disconnected", it will also exist in the available network
     * operator category for user to select. On the other hand, if the device has data and the
     * network operator in the connected network operator category shows "Connected", it will not
     * exist in the available network category.
     */
    private void updateNetworkOperatorsPreferenceCategory() {
        mNetworkOperatorsPreferences.removeAll();

        configConnectedNetworkOperatorsPreferenceCategory();
        for (int index = 0; index < mCellInfoList.size(); index++) {
            if (!mCellInfoList.get(index).isRegistered()) {
                NetworkOperatorPreference pref = new NetworkOperatorPreference(
                        mCellInfoList.get(index), getContext(), mForbiddenPlmns);
                pref.setKey(CellInfoUtil.getNetworkTitle(mCellInfoList.get(index)));
                pref.setOrder(index);
                mNetworkOperatorsPreferences.addPreference(pref);
            }
        }
    }

    /**
     * Config the connected network operator preference when the page was created. When user get
     * into this page, the device might or might not have data connection.
     * - If the device has data:
     * 1. use {@code ServiceState#getNetworkRegistrationStates()} to get the currently
     * registered cellIdentity, wrap it into a CellInfo;
     * 2. set the signal strength level as strong;
     * 3. use {@link TelephonyManager#getNetworkOperatorName()} to get the title of the
     * previously connected network operator, since the CellIdentity got from step 1 only has
     * PLMN.
     * - If the device has no data, we will remove the connected network operators list from the
     * screen.
     */
    private void forceConfigConnectedNetworkOperatorsPreferenceCategory() {
        if (DBG) logd("Force config ConnectedNetworkOperatorsPreferenceCategory");
        if (mTelephonyManager.getDataState() == mTelephonyManager.DATA_CONNECTED) {
            // Try to get the network registration states
            ServiceState ss = mTelephonyManager.getServiceState();
            List<NetworkRegistrationState> networkList =
                    ss.getNetworkRegistrationStates(AccessNetworkConstants.TransportType.WWAN);
            if (networkList == null || networkList.size() == 0) {
                loge("getNetworkRegistrationStates return null");
                // Remove the connected network operators category
                removeConnectedNetworkOperatorPreference();
                return;
            }
            CellIdentity cellIdentity = networkList.get(0).getCellIdentity();
            CellInfo cellInfo = CellInfoUtil.wrapCellInfoWithCellIdentity(cellIdentity);
            if (cellInfo != null) {
                if (DBG) logd("Currently registered cell: " + cellInfo.toString());
                NetworkOperatorPreference pref =
                        new NetworkOperatorPreference(cellInfo, getContext(), mForbiddenPlmns);
                pref.setTitle(mTelephonyManager.getNetworkOperatorName());
                pref.setSummary(R.string.network_connected);
                // Update the signal strength icon, since the default signalStrength value would be
                // zero (it would be quite confusing why the connected network has no signal)
                pref.setIcon(NetworkOperatorPreference.NUMBER_OF_LEVELS - 1);

                mConnectedNetworkOperatorsPreference.addPreference(pref);
            } else {
                loge("Invalid CellIfno: " + cellInfo.toString());
                // Remove the connected network operators category
                removeConnectedNetworkOperatorPreference();
            }
        } else {
            if (DBG) logd("No currently registered cell");
            // Remove the connected network operators category
            removeConnectedNetworkOperatorPreference();
        }
    }

    /**
     * Configure the ConnectedNetworkOperatorsPreferenceCategory. The category only need to be
     * configured if the category is currently empty or the operator network title of the previous
     * connected network is different from the new one.
     */
    private void configConnectedNetworkOperatorsPreferenceCategory() {
        if (DBG) logd("config ConnectedNetworkOperatorsPreferenceCategory");
        // Remove the category if the CellInfo list is empty or does not have registered cell.
        if (mCellInfoList.size() == 0) {
            if (DBG) logd("empty cellinfo list");
            removeConnectedNetworkOperatorPreference();
        }
        CellInfo connectedNetworkOperator = null;
        for (CellInfo cellInfo : mCellInfoList) {
            if (cellInfo.isRegistered()) {
                connectedNetworkOperator = cellInfo;
                break;
            }
        }
        if (connectedNetworkOperator == null) {
            if (DBG) logd("no registered network");
            removeConnectedNetworkOperatorPreference();
            return;
        }

        // config the category if it is empty.
        if (mConnectedNetworkOperatorsPreference.getPreferenceCount() == 0) {
            if (DBG) logd("ConnectedNetworkSelectList is empty, add one");
            addConnectedNetworkOperatorPreference(connectedNetworkOperator);
            return;
        }
        NetworkOperatorPreference previousConnectedNetworkOperator = (NetworkOperatorPreference)
                (mConnectedNetworkOperatorsPreference.getPreference(0));

        // config the category if the network title of the previous connected network is different
        // from the new one.
        String cTitle = CellInfoUtil.getNetworkTitle(connectedNetworkOperator);
        String pTitle = CellInfoUtil.getNetworkTitle(
                previousConnectedNetworkOperator.getCellInfo());
        if (!cTitle.equals(pTitle)) {
            if (DBG) logd("reconfig the category: connected network changed");
            addConnectedNetworkOperatorPreference(connectedNetworkOperator);
            return;
        }
        if (DBG) logd("same network operator is connected, only refresh the connected network");
        // Otherwise same network operator is connected, only refresh the connected network
        // operator preference (first and the only one in this category).
        ((NetworkOperatorPreference) mConnectedNetworkOperatorsPreference.getPreference(0))
                .refresh();
        return;
    }

    /**
     * Creates a Preference for the given {@link CellInfo} and adds it to the
     * {@link #mConnectedNetworkOperatorsPreference}.
     */
    private void addConnectedNetworkOperatorPreference(CellInfo cellInfo) {
        if (DBG) logd("addConnectedNetworkOperatorPreference");
        // Remove the current ConnectedNetworkOperatorsPreference
        removeConnectedNetworkOperatorPreference();
        final NetworkOperatorPreference pref =
                new NetworkOperatorPreference(cellInfo, getContext(), mForbiddenPlmns);
        pref.setSummary(R.string.network_connected);
        mConnectedNetworkOperatorsPreference.addPreference(pref);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.addPreference(mConnectedNetworkOperatorsPreference);
    }

    /** Removes all preferences and hide the {@link #mConnectedNetworkOperatorsPreference}. */
    private void removeConnectedNetworkOperatorPreference() {
        mConnectedNetworkOperatorsPreference.removeAll();
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.removePreference(mConnectedNetworkOperatorsPreference);
    }

    protected void setProgressBarVisible(boolean visible) {
        if (mProgressHeader != null) {
            mProgressHeader.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void addMessagePreference(int messageId) {
        if (DBG) logd("remove callback");
        getView().removeCallbacks(mUpdateNetworkOperatorsRunnable);
        setProgressBarVisible(false);
        if (DBG) logd("addMessagePreference");
        mStatusMessagePreference.setTitle(messageId);
        removeConnectedNetworkOperatorPreference();
        mNetworkOperatorsPreferences.removeAll();
        mNetworkOperatorsPreferences.addPreference(mStatusMessagePreference);
    }

    /**
     * The Scan results may contains several cell infos with different radio technologies and signal
     * strength for one network operator. Aggregate the CellInfoList by retaining only the cell info
     * with the strongest signal strength.
     */
    private List<CellInfo> aggregateCellInfoList(List<CellInfo> cellInfoList) {
        if (DBG) logd("before aggregate: " + cellInfoList.toString());
        Map<String, CellInfo> map = new HashMap<>();
        for (CellInfo cellInfo : cellInfoList) {
            String plmn = CellInfoUtil.getOperatorInfoFromCellInfo(cellInfo).getOperatorNumeric();
            if (cellInfo.isRegistered() || !map.containsKey(plmn)) {
                map.put(plmn, cellInfo);
            } else {
                if (map.get(plmn).isRegistered()
                        || map.get(plmn).getCellSignalStrength().getLevel()
                        > cellInfo.getCellSignalStrength().getLevel()) {
                    // Skip if the stored cellInfo is registered or has higher signal strength level
                    continue;
                }
                // Otherwise replace it with the new CellInfo
                map.put(plmn, cellInfo);
            }
        }
        return new ArrayList<>(map.values());
    }

    private void loadNetworksList() {
        if (DBG) logd("load networks list...");
        setProgressBarVisible(true);
        //TODO(b/114749736): load network list once b/115401728 is done
    }

    private void bindNetworkQueryService() {
        if (DBG) logd("bindNetworkQueryService");
        //TODO(b/114749736): bind service/manager once b/115401728 is done
        mShouldUnbind = true;
    }

    private void unbindNetworkQueryService() {
        if (DBG) logd("unbindNetworkQueryService");
        if (mShouldUnbind) {
            if (DBG) logd("mShouldUnbind is true");
            // unbind the service.
            //TODO(b/114749736): unbind service/manager once b/115401728 is done
            mShouldUnbind = false;
        }
    }

    private void updateNetworkSelection() {
        if (DBG) logd("Update notification about no service of user selected operator");
        //TODO(b/114749736): update network selection once b/115429509 is done
    }

    private void stopNetworkQuery() {
        // Stop the network query process
        //TODO(b/114749736): stop service/manager query once b/115401728 is done
    }

    private void logd(String msg) {
        Log.d(TAG, msg);
    }

    private void loge(String msg) {
        Log.e(TAG, msg);
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    final ArrayList<SearchIndexableResource> result = new ArrayList<>();

                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.choose_network;
                    result.add(sir);
                    return result;
                }
            };
}
