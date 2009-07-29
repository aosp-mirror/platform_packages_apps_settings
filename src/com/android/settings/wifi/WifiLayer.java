/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.settings.wifi;

import static android.net.wifi.WifiManager.WIFI_STATE_DISABLED;
import static android.net.wifi.WifiManager.WIFI_STATE_ENABLED;

import com.android.settings.R;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkInfo.State;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Config;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Helper class for abstracting Wi-Fi.
 * <p>
 * Client must call {@link #onCreate()}, {@link #onCreatedCallback()},
 * {@link #onPause()}, {@link #onResume()}.
 */
public class WifiLayer {
    
    private static final String TAG = "SettingsWifiLayer";
    static final boolean LOGV = false || Config.LOGV;
    
    //============================
    // Other member variables
    //============================
    
    private Context mContext;
    private Callback mCallback;

    static final int MESSAGE_ATTEMPT_SCAN = 1;
    private Handler mHandler = new MyHandler();
    
    //============================
    // Wifi member variables
    //============================
    
    private WifiManager mWifiManager;
    private IntentFilter mIntentFilter;
    private List<AccessPointState> mApScanList = new ArrayList<AccessPointState>();
    private List<AccessPointState> mApOtherList = new ArrayList<AccessPointState>();
    private AccessPointState mCurrentPrimaryAp;
    
    /** The last access point that we were authenticating with. */
    private AccessPointState mLastAuthenticatingAp;
    
    /** The delay between scans when we're continually scanning. */
    private static final int CONTINUOUS_SCAN_DELAY_MS = 6000; 
    /** On failure, the maximum retries for scanning. */
    private static final int SCAN_MAX_RETRY = 5;
    /** On failure, the delay between each scan retry. */
    private static final int SCAN_RETRY_DELAY_MS = 1000;
    /** On failure, the number of retries so far. */
    private int mScanRetryCount = 0;
    /**
     * Whether we're currently obtaining an address. Continuous scanning will be
     * disabled in this state.
     */
    private boolean mIsObtainingAddress;

    /**
     * See {@link android.provider.Settings.Secure#WIFI_NUM_OPEN_NETWORKS_KEPT}.
     */
    private int WIFI_NUM_OPEN_NETWORKS_KEPT;
    /**
     * Once the highest priority exceeds this value, all networks will be
     * wrapped around starting at 0. This is so another client of the Wi-Fi
     * API can have access points that aren't managed by us. (If the other
     * client wants lower-priority access points than ours, it can use negative
     * priority.)
     */
    private static final int HIGHEST_PRIORITY_MAX_VALUE = 99999;
    /**
     * Never access directly, only the related methods should.
     */
    private int mNextHighestPriority;
    
    /**
     * This is used to track when the user wants to connect to a specific AP. We
     * disable all other APs, set this to true, and let wpa_supplicant connect.
     * Once we get a network state change, we re-enable the rest of them.
     */
    private boolean mReenableApsOnNetworkStateChange = false;

    /**
     * The current supplicant state, as broadcasted.
     */
    private SupplicantState mCurrentSupplicantState;
    
    //============================
    // Inner classes
    //============================
    
    interface Callback {
        void onError(int messageResId);
        
        /**
         * Called when an AP is added or removed.
         * 
         * @param ap The AP.
         * @param added {@code true} if added, {@code false} if removed.
         */
        void onAccessPointSetChanged(AccessPointState ap, boolean added);
        
        /**
         * Called when the scanning status changes.
         * 
         * @param started {@code true} if the scanning just started,
         *            {@code false} if it just ended.
         */
        void onScanningStatusChanged(boolean started);

        /**
         * Called when the access points should be enabled or disabled. This is
         * called from both wpa_supplicant being connected/disconnected and Wi-Fi
         * being enabled/disabled.
         * 
         * @param enabled {@code true} if they should be enabled, {@code false}
         *            if they should be disabled.
         */
        void onAccessPointsStateChanged(boolean enabled);   
        
        /**
         * Called when there is trouble authenticating and the retry-password
         * dialog should be shown.
         * 
         * @param ap The access point.
         */
        void onRetryPassword(AccessPointState ap);
    }
    
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                handleNetworkStateChanged(
                        (NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO),
                        intent.getStringExtra(WifiManager.EXTRA_BSSID));
            } else if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                handleScanResultsAvailable();
            } else if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
                handleSupplicantConnectionChanged(
                        intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false));
            } else if (action.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
                handleSupplicantStateChanged(
                        (SupplicantState) intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE),
                        intent.hasExtra(WifiManager.EXTRA_SUPPLICANT_ERROR),
                        intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, 0));
            } else if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                handleWifiStateChanged(intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                        WifiManager.WIFI_STATE_UNKNOWN));
            } else if (action.equals(WifiManager.RSSI_CHANGED_ACTION)) {
                handleSignalChanged(intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, 0));
            } else if (action.equals(WifiManager.NETWORK_IDS_CHANGED_ACTION)) {
                handleNetworkIdsChanged();
            }
        }
    };
    
    /**
     * If using this class, make sure to call the callbacks of this class, such
     * as {@link #onCreate()}, {@link #onCreatedCallback()},
     * {@link #onPause()}, {@link #onResume()}.
     * 
     * @param context The context.
     * @param callback The interface that will be invoked when events from this
     *            class are generated.
     */
    public WifiLayer(Context context, Callback callback) {
        mContext = context;
        mCallback = callback;
    }
    
    //============================
    // Lifecycle
    //============================

    /**
     * The client MUST call this.
     * <p>
     * This shouldn't have any dependency on the callback.
     */
    public void onCreate() {
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mIntentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        mIntentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
        
        WIFI_NUM_OPEN_NETWORKS_KEPT = Settings.Secure.getInt(mContext.getContentResolver(),
            Settings.Secure.WIFI_NUM_OPEN_NETWORKS_KEPT, 10);
    }
    
    /**
     * The client MUST call this.
     * <p>
     * Callback is ready, this can do whatever it wants with it.
     */
    public void onCreatedCallback() {
        if (isWifiEnabled()) {
            refreshAll(false);
        }
    }
    
    /**
     * The client MUST call this.
     * 
     * @see android.app.Activity#onResume
     */
    public void onResume() {
        mContext.registerReceiver(mReceiver, mIntentFilter);
        
        if (isWifiEnabled()) {
            // Kick start the continual scan
            queueContinuousScan();
        }
    }

    /**
     * The client MUST call this.
     * 
     * @see android.app.Activity#onPause
     */
    public void onPause() {
        mContext.unregisterReceiver(mReceiver);
        
        attemptReenableAllAps();
        
        removeFutureScans();
    }
    
    //============================
    // "Public" API
    //============================

    /**
     * Returns an AccessPointState instance (that we track locally in WifiLayer)
     * for the given state. First, we check if we track the given instance. If
     * not, we find an equal AccessPointState instance that we track.
     * 
     * @param state An AccessPointState instance that does not necessarily have
     *            to be one that this WifiLayer class tracks. For example, it
     *            could be the result of unparceling.
     * @return An AccessPointState instance that this WifiLayer class tracks.
     */
    public AccessPointState getWifiLayerApInstance(AccessPointState state) {
        synchronized (this) {
            
            if (hasApInstanceLocked(state)) {
                return state;
            }
            
            return findApLocked(state.networkId, state.bssid, state.ssid, state.security);
        }
    }
    
    /**
     * Connects to the network, and creates the Wi-Fi API config if necessary.
     * 
     * @param state The state of the network to connect to. This MUST be an
     *            instance that was given to you by this class. If you
     *            constructed the instance yourself (for example, after
     *            unparceling it), you should use
     *            {@link #getWifiLayerApInstance(AccessPointState)}.
     * @return Whether the operation was successful.
     */
    public boolean connectToNetwork(AccessPointState state) {
        if (LOGV) {
            Log.v(TAG, "Connecting to " + state);
        }
        
        // Need WifiConfiguration for the AP
        WifiConfiguration config = findConfiguredNetwork(state);

        if (LOGV) {
            Log.v(TAG, " Found configured network " + config);
        }
        
        if (config == null) {
            /*
             * Connecting for the first time, need to create it. We will enable
             * and save it below (when we set priority).
             */
            config = addConfiguration(state, 0);

            if (config == null) {
                Log.e(TAG, "Config is still null, even after attempting to add it.");
                error(R.string.error_connecting);
                return false;
            }

            /*
             * We could reload the configured networks, but instead just
             * shortcut and add this state to our list in memory.
             */
            ensureTrackingState(state);
        } else {
            // Make sure the configuration has the latest from the state
            state.updateWifiConfiguration(config);
        }

        // Enable this network before we save to storage
        if (!managerEnableNetwork(state, false)) {
            Log.e(TAG, "Could not enable network ID " + state.networkId);
            error(R.string.error_connecting);
            return false;
        }
        
        /*
         * Give it highest priority, this could cause a network ID change, so do
         * it after any modifications to the network we're connecting to
         */
        setHighestPriorityStateAndSave(state, config);
        
        /*
         * We force supplicant to connect to this network by disabling the
         * others. We do this AFTER we save above so this disabled flag isn't
         * persisted.
         */
        mReenableApsOnNetworkStateChange = true;
        if (!managerEnableNetwork(state, true)) {
            Log.e(TAG, "Could not enable network ID " + state.networkId);
            error(R.string.error_connecting);
            return false;
        }

        if (LOGV) {
            Log.v(TAG, " Enabled network " + state.networkId);
        }

        if (mCurrentSupplicantState == SupplicantState.DISCONNECTED ||
                mCurrentSupplicantState == SupplicantState.SCANNING) {
            mWifiManager.reconnect();
        }
        
        // Check for too many configured open networks
        if (!state.hasSecurity()) {
            checkForExcessOpenNetworks();
        }
        
        return true;
    }
    
    /**
     * Saves a network, and creates the Wi-Fi API config if necessary.
     * 
     * @param state The state of the network to save. If you constructed the
     *            instance yourself (for example, after unparceling it), you
     *            should use {@link #getWifiLayerApInstance(AccessPointState)}.
     * @return Whether the operation was successful.
     */
    public boolean saveNetwork(AccessPointState state) {
        WifiConfiguration config = findConfiguredNetwork(state);
        
        if (config == null) {
            // if the user is adding a new network, assume that it is hidden
            state.setHiddenSsid(true);

            config = addConfiguration(state, ADD_CONFIGURATION_ENABLE);

            if (config == null) {
                Log.e(TAG, "Could not save configuration, call to addConfiguration failed.");
                error(R.string.error_saving);
                return false;
            }
            
        } else {
            state.updateWifiConfiguration(config);
            if (mWifiManager.updateNetwork(config) == -1) {
                Log.e(TAG, "Could not update configuration, call to WifiManager failed.");
                error(R.string.error_saving);
                return false;
            }
        }
        
        // Successfully added network, go ahead and persist
        if (!managerSaveConfiguration()) {
            Log.e(TAG, "Could not save configuration, call to WifiManager failed.");
            error(R.string.error_saving);
            return false;
        }
        
        /*
         * It's necessary to update the network id of this state because the network id
         * could have changed after the configuration is saved. For example, if there are
         * more than 10 saved open-networks, some older open-networks will have been be forgotten.
         */
        state.setNetworkId(AccessPointState.NETWORK_ID_ANY);
        config = findConfiguredNetwork(state);
        if (config != null) {
            state.setNetworkId(config.networkId);
        }

        /*
         * We could reload the configured networks, but instead just shortcut
         * and add this state to our list in memory
         */
        ensureTrackingState(state);
        
        return true;
    }
    
    /**
     * Forgets a network.
     * 
     * @param state The state of the network to forget. If you constructed the
     *            instance yourself (for example, after unparceling it), you
     *            should use {@link #getWifiLayerApInstance(AccessPointState)}.
     * @return Whether the operation was succesful.
     */
    public boolean forgetNetwork(AccessPointState state) {
        if (!state.configured) {
            Log.w(TAG, "Inconsistent state:  Forgetting a network that is not configured.");
            return true;
        }
        
        int oldNetworkId = state.networkId;
        state.forget();
        
        if (!state.seen) {
            // If it is not seen, it should be removed from the UI
            removeApFromUi(state);
        }
                    
        synchronized (this) {
            mApOtherList.remove(state);
            // It should not be removed from the scan list, since if it was
            // there that means it's still seen
        }

        if (!mWifiManager.removeNetwork(oldNetworkId)) {
            Log.e(TAG, "Removing network " + state.ssid + " (network ID " + oldNetworkId +
                    ") failed.");
            return false;
        }
        
        if (!managerSaveConfiguration()) {
            error(R.string.error_saving);
            return false;
        }

        return true;
    }
    
    /**
     * This ensures this class is tracking the given state. This means it is in
     * our list of access points, either in the scanned list or in the
     * remembered list.
     * 
     * @param state The state that will be checked for tracking, and if not
     *            tracking will be added to the remembered list in memory.
     */
    private void ensureTrackingState(AccessPointState state) {
        synchronized (this) {
            if (hasApInstanceLocked(state)) {
                return;
            }
            
            mApOtherList.add(state);
        }
    }
    
    /**
     * Attempts to scan networks.  This has a retry mechanism.
     */
    public void attemptScan() {
        
        // Remove any future scans since we're scanning right now
        removeFutureScans();
        
        if (!mWifiManager.isWifiEnabled()) return;
        
        if (!mWifiManager.startScanActive()) {
            postAttemptScan();
        } else {
            mScanRetryCount = 0;
        }
    }

    private void queueContinuousScan() {
        mHandler.removeMessages(MESSAGE_ATTEMPT_SCAN);
        
        if (!mIsObtainingAddress) {
            // Don't do continuous scan while in obtaining IP state
            mHandler.sendEmptyMessageDelayed(MESSAGE_ATTEMPT_SCAN, CONTINUOUS_SCAN_DELAY_MS);
        }
    }
    
    private void removeFutureScans() {
        mHandler.removeMessages(MESSAGE_ATTEMPT_SCAN);
    }
    
    public boolean isWifiEnabled() {
        return mWifiManager.isWifiEnabled();
    }
    
    public void error(int messageResId) {
        Log.e(TAG, mContext.getResources().getString(messageResId));
        
        if (mCallback != null) {
            mCallback.onError(messageResId);
        }
    }
    
    //============================
    // Wifi logic
    //============================

    private void refreshAll(boolean attemptScan) {
        loadConfiguredAccessPoints();
        refreshStatus();
        
        if (attemptScan) {
            attemptScan();
        }
    }
    
    private void postAttemptScan() {
        onScanningStarted();

        if (++mScanRetryCount < SCAN_MAX_RETRY) {
            // Just in case, remove previous ones first
            removeFutureScans();
            mHandler.sendEmptyMessageDelayed(MESSAGE_ATTEMPT_SCAN, SCAN_RETRY_DELAY_MS);
        } else {
            // Show an error once we run out of attempts
            error(R.string.error_scanning);
            onScanningEnded();
        }
    }
    
    private void onScanningStarted() {
        if (mCallback != null) {
            mCallback.onScanningStatusChanged(true);
        }
    }
    
    private void onScanningEnded() {
        queueContinuousScan();
        
        if (mCallback != null) {
            mCallback.onScanningStatusChanged(false);
        }
    }
    
    private void clearApLists() {
        List<AccessPointState> accessPoints = new ArrayList<AccessPointState>();
        
        synchronized(this) {
            // Clear the logic's list of access points
            accessPoints.addAll(mApScanList);
            accessPoints.addAll(mApOtherList);
            mApScanList.clear();
            mApOtherList.clear();
        }
        
        for (int i = accessPoints.size() - 1; i >= 0; i--) {
            removeApFromUi(accessPoints.get(i));
        }
    }
    
    private boolean managerSaveConfiguration() {
        boolean retValue = mWifiManager.saveConfiguration();
        
        /*
         * We need to assume the network IDs have changed, so handle this. Note:
         * we also have a receiver on the broadcast intent in case another wifi
         * framework client caused the change. In this case, we will handle the
         * possible network ID change twice (but it's not too costly).
         */
        handleNetworkIdsChanged();
        
        return retValue;
    }
    
    private boolean managerEnableNetwork(AccessPointState state, boolean disableOthers) {
        if (!mWifiManager.enableNetwork(state.networkId, disableOthers)) {
            return false;
        }
        
        // Enabling was successful, make sure the state is not disabled
        state.setDisabled(false);
        
        return true;
    }
    
    private static final int ADD_CONFIGURATION_ENABLE = 1;
    private static final int ADD_CONFIGURATION_SAVE = 2;
    private WifiConfiguration addConfiguration(AccessPointState state, int flags) {
        // Create and add
        WifiConfiguration config = new WifiConfiguration();
        
        state.updateWifiConfiguration(config);
        
        final int networkId = mWifiManager.addNetwork(config);
        if (networkId == -1) {
            return null;
        }
        
        state.setNetworkId(networkId);
        state.setConfigured(true);
        
        // If we should, then enable it, since it comes disabled by default
        if ((flags & ADD_CONFIGURATION_ENABLE) != 0
                && !managerEnableNetwork(state, false)) {
            return null;
        }

        // If we should, then save it
        if ((flags & ADD_CONFIGURATION_SAVE) != 0 && !managerSaveConfiguration()) {
            return null;
        }

        if (mCallback != null) {
            mCallback.onAccessPointSetChanged(state, true);
        }

        return config;
    }
    
    private WifiConfiguration findConfiguredNetwork(AccessPointState state) {
        final List<WifiConfiguration> wifiConfigs = getConfiguredNetworks();
        
        for (int i = wifiConfigs.size() - 1; i >= 0; i--) {
            final WifiConfiguration wifiConfig = wifiConfigs.get(i); 
            if (state.matchesWifiConfiguration(wifiConfig) >= AccessPointState.MATCH_WEAK) {
                return wifiConfig;
            }
        }
        
        return null;
    }
    
    private List<WifiConfiguration> getConfiguredNetworks() {
        final List<WifiConfiguration> wifiConfigs = mWifiManager.getConfiguredNetworks();
        return wifiConfigs;
    }
    
    /**
     * Must call while holding the lock for the list, which is usually the
     * WifiLayer instance.
     */
    private static AccessPointState findApLocked(List<AccessPointState> list, int networkId,
            String bssid, String ssid, String security) {
        AccessPointState ap;
        for (int i = list.size() - 1; i >= 0; i--) {
            ap = list.get(i);
            if (ap.matches(networkId, bssid, ssid, security) >= AccessPointState.MATCH_WEAK) {
                return ap;
            }
        }

        return null;
    }
    
    /**
     * Must call while holding the lock for the lists, which is usually this
     * WifiLayer instance.
     */
    private AccessPointState findApLocked(int networkId, String bssid, String ssid,
            String security) {
        AccessPointState ap = findApLocked(mApScanList, networkId, bssid, ssid, security);
        if (ap == null) {
            ap = findApLocked(mApOtherList, networkId, bssid, ssid, security);
        }
        return ap;
    }

    /**
     * Returns whether we have the exact instance of the access point state
     * given. This is useful in cases where an AccessPointState has been
     * parceled by the client and the client is attempting to use it to
     * connect/forget/save.
     * <p>
     * Must call while holding the lock for the lists, which is usually this
     * WifiLayer instance.
     */
    private boolean hasApInstanceLocked(AccessPointState state) {
        
        for (int i = mApScanList.size() - 1; i >= 0; i--) {
            if (mApScanList.get(i) == state) {
                return true;
            }
        }

        for (int i = mApOtherList.size() - 1; i >= 0; i--) {
            if (mApOtherList.get(i) == state) {
                return true;
            }
        }
        
        return false;
    }
    
    private void loadConfiguredAccessPoints() {
        final List<WifiConfiguration> configs = getConfiguredNetworks();
        
        for (int i = configs.size() - 1; i >= 0; i--) {
            final WifiConfiguration config = configs.get(i);
            
            AccessPointState ap;
            synchronized(this) {
                ap = findApLocked(config.networkId, config.BSSID, config.SSID,
                        AccessPointState.getWifiConfigurationSecurity(config));
                
                if (ap != null) {
                    // We already know about this one
                    continue;
                }
    
                ap = new AccessPointState(mContext);
                ap.updateFromWifiConfiguration(config);
                if (LOGV) Log.v(TAG, "Created " + ap + " in loadConfiguredAccessPoints");
                mApOtherList.add(ap);
            }

            // Make sure our next highest priority is greater than this
            checkNextHighestPriority(ap.priority);
            
            if (mCallback != null) {
                mCallback.onAccessPointSetChanged(ap, true);
            }
        }
    }

    private AccessPointState getCurrentAp() {
        final WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        
        String ssid = wifiInfo.getSSID();
        if (ssid != null) {
            /*
             * We pass null for security since we have a network ID (i.e., it's
             * not a wildcard), and rely on it matching.
             */
            return findApLocked(wifiInfo.getNetworkId(), wifiInfo.getBSSID(), ssid, null);
        } else {
            return null;
        }
    }
    
    private void setPrimaryAp(AccessPointState ap) {
        synchronized (this) {
            // Unset other
            if (mCurrentPrimaryAp != null) {
                mCurrentPrimaryAp.setPrimary(false);
            }

            mCurrentPrimaryAp = ap;
        }

        if (ap != null) {
            ap.setPrimary(true);
        }
    }

    private void attemptReenableAllAps() {
        if (mReenableApsOnNetworkStateChange) {
            mReenableApsOnNetworkStateChange = false;
            enableAllAps();
        }
    }
    
    private void enableAllAps() {
        synchronized(this) {
            if (LOGV) {
                Log.v(TAG, " Enabling all APs");
            }
            
            enableApsLocked(mApOtherList);
            enableApsLocked(mApScanList);
        }
    }
    
    private void enableApsLocked(List<AccessPointState> apList) {
        for (int i = apList.size() - 1; i >= 0; i--) {
            AccessPointState state = apList.get(i);
            int networkId = state.networkId;
            if (networkId != AccessPointState.NETWORK_ID_NOT_SET &&
                    networkId != AccessPointState.NETWORK_ID_ANY) {
                managerEnableNetwork(state, false);
            }
        }
    }
    
    private void removeApFromUi(AccessPointState ap) {
        if (mCallback != null) {
            mCallback.onAccessPointSetChanged(ap, false);
        }
    }
    
    /**
     * Sets the access point state to the highest priority.
     * <p>
     * If you have a list of configured networks from WifiManager, you probably
     * shouldn't call this until you're done traversing the list.
     * 
     * @param state The state to set as the highest priority.
     * @param reusableConfiguration An optional WifiConfiguration that will be
     *            given to the WifiManager as updated data for the network ID.
     *            This will be filled with the new priority.
     * @return Whether the operation was successful.
     */
    private boolean setHighestPriorityStateAndSave(AccessPointState state,
            WifiConfiguration reusableConfiguration) {

        if (!isConsideredForHighestPriority(state)) {
            Log.e(TAG,
                    "Could not set highest priority on state because state is not being considered.");
            return false;
        }
        
        if (reusableConfiguration == null) {
            reusableConfiguration = new WifiConfiguration();
        }
        
        int oldPriority = reusableConfiguration.priority;
        reusableConfiguration.priority = getNextHighestPriority();
        reusableConfiguration.networkId = state.networkId;
        
        if (mWifiManager.updateNetwork(reusableConfiguration) == -1) {
            // Rollback priority
            reusableConfiguration.priority = oldPriority;
            Log.e(TAG,
                    "Could not set highest priority on state because updating the supplicant network failed.");
            return false;
        }

        if (!managerSaveConfiguration()) {
            reusableConfiguration.priority = oldPriority;
            Log.e(TAG,
                    "Could not set highest priority on state because saving config failed.");
            return false;
        }
        
        state.priority = reusableConfiguration.priority;
        
        if (LOGV) {
            Log.v(TAG, " Set highest priority to "
                    + state.priority + " from " + oldPriority);
        }
        
        return true;
    }

    /**
     * Makes sure the next highest priority is larger than the given priority.
     */
    private void checkNextHighestPriority(int priority) {
        if (priority > HIGHEST_PRIORITY_MAX_VALUE || priority < 0) {
            // This is a priority that we aren't managing
            return;
        }
        
        if (mNextHighestPriority <= priority) {
            mNextHighestPriority = priority + 1;
        }
    }

    /**
     * Checks if there are too many open networks, and removes the excess ones.
     */
    private void checkForExcessOpenNetworks() {
        synchronized(this) {
            ArrayList<AccessPointState> allAps = getApsSortedByPriorityLocked();

            // Walk from highest to lowest priority
            int openConfiguredCount = 0;
            for (int i = allAps.size() - 1; i >= 0; i--) {
                AccessPointState state = allAps.get(i);
                if (state.configured && !state.hasSecurity()) {
                    openConfiguredCount++;
                    if (openConfiguredCount > WIFI_NUM_OPEN_NETWORKS_KEPT) {
                        // Remove this network
                        forgetNetwork(state);
                    }
                }
            }
        }
    }
    
    private boolean isConsideredForHighestPriority(AccessPointState state) {
        return state.configured && state.networkId != AccessPointState.NETWORK_ID_ANY &&
                state.networkId != AccessPointState.NETWORK_ID_NOT_SET;
    }
    
    /**
     * Gets the next highest priority. If this value is larger than the max,
     * shift all the priorities so the lowest starts at 0.
     * <p>
     * Only
     * {@link #setHighestPriorityStateAndSave(AccessPointState, WifiConfiguration)}
     * should call this.
     * 
     * @return The next highest priority to use.
     */
    private int getNextHighestPriority() {
        if (mNextHighestPriority > HIGHEST_PRIORITY_MAX_VALUE) {
            shiftPriorities();
        }
        
        return mNextHighestPriority++;
    }

    /**
     * Shift all the priorities so the lowest starts at 0.
     * 
     * @return Whether the operation was successful.
     */
    private boolean shiftPriorities() {
        synchronized(this) {

            ArrayList<AccessPointState> allAps = getApsSortedByPriorityLocked();

            // Re-usable WifiConfiguration for setting priority
            WifiConfiguration updatePriorityConfig = new WifiConfiguration();
            
            // Set new priorities
            mNextHighestPriority = 0;
            int size = allAps.size();
            for (int i = 0; i < size; i++) {
                AccessPointState state = allAps.get(i);
                
                if (!isConsideredForHighestPriority(state)) {
                    continue;
                }
                
                if (!setHighestPriorityStateAndSave(state, updatePriorityConfig)) {
                    Log.e(TAG,
                            "Could not shift priorities because setting the new priority failed.");
                    return false;
                }
            }
            
            return true;
        }
    }

    private ArrayList<AccessPointState> getApsSortedByPriorityLocked() {
        // Get all of the access points we have
        ArrayList<AccessPointState> allAps = new ArrayList<AccessPointState>(mApScanList.size()
                + mApOtherList.size());
        allAps.addAll(mApScanList);
        allAps.addAll(mApOtherList);
        
        // Sort them based on priority
        Collections.sort(allAps, new Comparator<AccessPointState>() {
            public int compare(AccessPointState object1, AccessPointState object2) {
                return object1.priority - object2.priority;
            }
        });

        return allAps;
    }
    
    //============================
    // Status related
    //============================

    private void refreshStatus() {
        refreshStatus(null, null);
    }
    
    private void refreshStatus(AccessPointState ap, NetworkInfo.DetailedState detailedState) {
        final WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        if (detailedState == null) {
            detailedState = WifiInfo.getDetailedStateOf(wifiInfo.getSupplicantState());
        }

        if (ap == null && WifiStatus.isLiveConnection(detailedState)) {
            /*
             * We pass null for security since we have a network ID (i.e., it's
             * not a wildcard), and rely on it matching.
             */
            ap = findApLocked(wifiInfo.getNetworkId(), wifiInfo.getBSSID(), wifiInfo
                    .getSSID(), null);
        }

        if (ap != null) {
            ap.blockRefresh();

            // Let the AP get the latest info from the WifiInfo 
            ap.updateFromWifiInfo(wifiInfo, detailedState);

            // The detailed state from the Intent has more states than the WifiInfo's detailed
            // state can have (for example, DHCP completion).  Set the status using
            // the Intent's detailed state.
            ap.setStatus(detailedState);
            ap.unblockRefresh();
        }
    }
    
    //============================
    // Wifi callbacks
    //============================
    
    private void handleNetworkStateChanged(NetworkInfo info, String bssid) {
        final AccessPointState ap = getCurrentAp();
        NetworkInfo.DetailedState detailedState = info.getDetailedState(); 

        if (LOGV) {
            Log.v(TAG, "State change received " + info.toString() + ", or "
                    + detailedState + " on " + bssid + " matched to " + ap);
        }

        handleDisablingScanWhileObtainingAddress(detailedState);
        
        // This will update the AP with its new info
        refreshStatus(ap, detailedState);
        
        boolean isDisconnected = info.getState().equals(State.DISCONNECTED);
        if (ap != null && info.isConnectedOrConnecting()) {
            setPrimaryAp(ap);

            if (LOGV) {
                Log.v(TAG, " Updated " + ap + " to be primary");
            }
            
        } else if (isDisconnected) {
            
            /*
             * When we drop off a network (for example, the router is powered
             * down when we were connected), we received a DISCONNECT event
             * without a BSSID. We should not have a primary AP anymore.
             */
            setPrimaryAp(null);
            
            if (LOGV) {
                Log.v(TAG, " Cleared primary");
            }
            
        } else if (detailedState.equals(DetailedState.FAILED)) {

            /*
             * Doh, failed for whatever reason. Unset the primary AP, but set
             * failed status on the AP that failed.
             */
            setPrimaryAp(null);
            ap.setStatus(DetailedState.FAILED);
            
            // Bring up error dialog
            error(R.string.wifi_generic_connection_error);
            
        } else if (LOGV) {
            Log.v(TAG, " Did not update any AP to primary, could have updated "
                    + ap + " but we aren't connected or connecting");
        }

        if ((ap != null) && (info.isConnected()
                    || (detailedState == DetailedState.OBTAINING_IPADDR))) {
            /*
             * Sometimes the scan results do not contain the AP even though it's
             * clearly connected. This may be because we do passive background
             * scanning that isn't as 'strong' as active scanning, so even
             * though a network is nearby, it won't be seen by the passive
             * scanning. If we are connected (or obtaining IP) then we know it
             * is seen.
             */
            ap.setSeen(true);
        }

        attemptReenableAllAps();
    }

    private void handleDisablingScanWhileObtainingAddress(DetailedState detailedState) {
        
        if (detailedState == DetailedState.OBTAINING_IPADDR) {
            mIsObtainingAddress = true;

            // We will not scan while obtaining an IP address
            removeFutureScans();
            
        } else {
            mIsObtainingAddress = false;
            
            // Start continuous scan
            queueContinuousScan();
        }
    }
    
    private void handleScanResultsAvailable() {
        synchronized(this) {
            // In the end, we'll moved the ones no longer seen into the mApOtherList
            List<AccessPointState> oldScanList = mApScanList;
            List<AccessPointState> newScanList =
                    new ArrayList<AccessPointState>(oldScanList.size());

            List<ScanResult> list = mWifiManager.getScanResults();
            if (list != null) {
                for (int i = list.size() - 1; i >= 0; i--) {
                    final ScanResult scanResult = list.get(i);
                    
                    if (LOGV) {
//                        Log.v(TAG, "    " + scanResult);
                    }
                    
                    if (scanResult == null) {
                        continue;
                    }
                    
                    /*
                     * Ignore adhoc, enterprise-secured, or hidden networks.
                     * Hidden networks show up with empty SSID.
                     */
                    if (AccessPointState.isAdhoc(scanResult)
                            || TextUtils.isEmpty(scanResult.SSID)) {
                        continue;
                    }
                    
                    final String ssid = AccessPointState.convertToQuotedString(scanResult.SSID);
                    String security = AccessPointState.getScanResultSecurity(scanResult);
                    
                    // See if this AP is part of a group of APs (e.g., any large
                    // wifi network has many APs, we'll only show one) that we've
                    // seen in this scan
                    AccessPointState ap = findApLocked(newScanList, AccessPointState.NETWORK_ID_ANY,
                                                 AccessPointState.BSSID_ANY, ssid, security);

                    // Yup, we've seen this network.
                    if (ap != null) {
                        // Use the better signal
                        if (WifiManager.compareSignalLevel(scanResult.level, ap.signal) > 0) {
                            ap.setSignal(scanResult.level);
                        }
                        
                        if (LOGV) {
//                            Log.v(TAG, "         Already seen, continuing..");
                        }
                        
                        continue;
                    }

                    // Find the AP in either our old scan list, or our non-seen
                    // configured networks list
                    ap = findApLocked(AccessPointState.NETWORK_ID_ANY, AccessPointState.BSSID_ANY,
                                ssid, security);

                    if (ap != null) {
                        // Remove the AP from both (no harm if one doesn't contain it)
                        oldScanList.remove(ap);
                        mApOtherList.remove(ap);
                    } else {
                        ap = new AccessPointState(mContext);
//                        if (LOGV) Log.v(TAG, "Created " + ap);
                    }

                    // Give it the latest state
                    ap.updateFromScanResult(scanResult);

                    if (mCallback != null) {
                        mCallback.onAccessPointSetChanged(ap, true);
                    }

                    newScanList.add(ap);
                }
            }
            
            // oldScanList contains the ones no longer seen
            List<AccessPointState> otherList = mApOtherList;
            for (int i = oldScanList.size() - 1; i >= 0; i--) {
                final AccessPointState ap = oldScanList.get(i);
                
                if (ap.configured) {
                    
                    // Keep it around, since it is configured
                    ap.setSeen(false);
                    otherList.add(ap);
                    
                } else {
                    
                    // Remove it since it is not configured and not seen
                    removeApFromUi(ap);
                }
            }
            
            mApScanList = newScanList;
        }

        onScanningEnded();
    }
    
    private void handleSupplicantConnectionChanged(boolean connected) {
        if (mCallback != null) {
            mCallback.onAccessPointsStateChanged(connected);
        }
        
        if (connected) {
            refreshAll(true);
        }
    }

    private void handleWifiStateChanged(int wifiState) {
        
        if (wifiState == WIFI_STATE_ENABLED) {
            loadConfiguredAccessPoints();
            attemptScan();

        } else if (wifiState == WIFI_STATE_DISABLED) {
            removeFutureScans();
            if (LOGV) Log.v(TAG, "Clearing AP lists because wifi is disabled");
            clearApLists();
        }
        
        if (mCallback != null) {
            mCallback.onAccessPointsStateChanged(wifiState == WIFI_STATE_ENABLED);
        }
    }
    
    private void handleSignalChanged(int rssi) {
        
        if (mCurrentPrimaryAp != null) {
            mCurrentPrimaryAp.setSignal(rssi);
        }
    }

    private void handleSupplicantStateChanged(SupplicantState state, boolean hasError, int error) {
        mCurrentSupplicantState = state;
        
        if (SupplicantState.FOUR_WAY_HANDSHAKE.equals(state)) {
            mLastAuthenticatingAp = getCurrentAp();
        }
        
        if (hasError) {
            handleSupplicantStateError(error);
        }
    }
    
    private void handleSupplicantStateError(int supplicantError) {
        if (supplicantError == WifiManager.ERROR_AUTHENTICATING) {
            if (mCallback != null) {
                if (mLastAuthenticatingAp != null) {
                    mCallback.onRetryPassword(mLastAuthenticatingAp);
                }
            }
        }
    }
    
    private void handleNetworkIdsChanged() {
        synchronized (this) {
            final List<WifiConfiguration> configs = getConfiguredNetworks();
            
            for (int i = configs.size() - 1; i >= 0; i--) {
                final WifiConfiguration config = configs.get(i);
                
                AccessPointState ap;
                // Since network IDs have changed, we can't use it to find our previous AP state
                ap = findApLocked(AccessPointState.NETWORK_ID_ANY, config.BSSID, config.SSID,
                        AccessPointState.getWifiConfigurationSecurity(config));
                
                if (ap == null) {
                    continue;
                }

                ap.setNetworkId(config.networkId);
            }
        }
    }
    
    private class MyHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_ATTEMPT_SCAN:
                    attemptScan();
                    break;
            }
        }
    }
    
}
