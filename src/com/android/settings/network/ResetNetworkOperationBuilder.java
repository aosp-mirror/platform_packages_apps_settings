/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.network;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ContentResolver;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkPolicyManager;
import android.net.Uri;
import android.net.VpnManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Looper;
import android.os.RecoverySystem;
import android.os.SystemClock;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.ResetNetworkRequest;
import com.android.settings.network.apn.ApnSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * A builder for creating a Runnable resetting network configurations.
 */
public class ResetNetworkOperationBuilder {

    private static final String TAG = "ResetNetworkOpBuilder";

    private static final boolean DRY_RUN = false;

    // TelephonyContentProvider method to restart phone process
    @VisibleForTesting
    static final String METHOD_RESTART_PHONE_PROCESS = "restartPhoneProcess";
    // TelephonyContentProvider method to restart RILD
    @VisibleForTesting
    static final String METHOD_RESTART_RILD = "restartRild";

    private Context mContext;
    private List<Runnable> mResetSequence = new ArrayList<Runnable>();

    /**
     * Constructor of builder.
     *
     * @param context Context
     */
    public ResetNetworkOperationBuilder(Context context) {
        mContext = context;
    }

    /**
     * Append a step of resetting ConnectivityManager.
     * @return this
     */
    public ResetNetworkOperationBuilder resetConnectivityManager() {
        attachSystemServiceWork(Context.CONNECTIVITY_SERVICE,
                (Consumer<ConnectivityManager>) cm -> {
                        cm.factoryReset();
                });
        return this;
    }

    /**
     * Append a step of resetting VpnManager.
     * @return this
     */
    public ResetNetworkOperationBuilder resetVpnManager() {
        attachSystemServiceWork(Context.VPN_MANAGEMENT_SERVICE,
                (Consumer<VpnManager>) vpnManager -> {
                        vpnManager.factoryReset();
                });
        return this;
    }

    /**
     * Append a step of resetting WifiManager.
     * @return this
     */
    public ResetNetworkOperationBuilder resetWifiManager() {
        attachSystemServiceWork(Context.WIFI_SERVICE,
                (Consumer<WifiManager>) wifiManager -> {
                        wifiManager.factoryReset();
                });
        return this;
    }

    /**
     * Append a step of resetting WifiP2pManager.
     * @param callbackLooper looper to support callback from WifiP2pManager
     * @return this
     */
    public ResetNetworkOperationBuilder resetWifiP2pManager(Looper callbackLooper) {
        attachSystemServiceWork(Context.WIFI_P2P_SERVICE,
                (Consumer<WifiP2pManager>) wifiP2pManager -> {
                        WifiP2pManager.Channel channel = wifiP2pManager.initialize(
                                mContext, callbackLooper, null /* listener */);
                        if (channel != null) {
                            wifiP2pManager.factoryReset(channel, null /* listener */);
                        }
                });
        return this;
    }

    /**
     * Append a step of resetting E-SIM.
     * @param callerPackage package name of caller
     * @return this
     */
    public ResetNetworkOperationBuilder resetEsim(String callerPackage) {
        resetEsim(callerPackage, null);
        return this;
    }

    /**
     * Append a step of resetting E-SIM.
     * @param callerPackage package name of caller
     * @param resultCallback a Consumer<Boolean> dealing with result of resetting eSIM
     * @return this
     */
    public ResetNetworkOperationBuilder resetEsim(String callerPackage,
            Consumer<Boolean> resultCallback) {
        Runnable runnable = () -> {
            long startTime = SystemClock.elapsedRealtime();

            if (!DRY_RUN) {
                Boolean wipped = RecoverySystem.wipeEuiccData(mContext, callerPackage);
                if (resultCallback != null) {
                    resultCallback.accept(wipped);
                }
            }

            long endTime = SystemClock.elapsedRealtime();
            Log.i(TAG, "Reset eSIM, takes " + (endTime - startTime) + " ms");
        };
        mResetSequence.add(runnable);
        return this;
    }

    /**
     * Append a step of resetting TelephonyManager and .
     * @param subscriptionId of a SIM card
     * @return this
     */
    public ResetNetworkOperationBuilder resetTelephonyAndNetworkPolicyManager(
            int subscriptionId) {
        final AtomicReference<String> subscriberId = new AtomicReference<String>();
        attachSystemServiceWork(Context.TELEPHONY_SERVICE,
                (Consumer<TelephonyManager>) tm -> {
                        TelephonyManager subIdTm = tm.createForSubscriptionId(subscriptionId);
                        subscriberId.set(subIdTm.getSubscriberId());
                        subIdTm.resetSettings();
                });
        attachSystemServiceWork(Context.NETWORK_POLICY_SERVICE,
                (Consumer<NetworkPolicyManager>) policyManager -> {
                        policyManager.factoryReset(subscriberId.get());
                });
        return this;
    }

    /**
     * Append a step of resetting BluetoothAdapter.
     * @return this
     */
    public ResetNetworkOperationBuilder resetBluetoothManager() {
        attachSystemServiceWork(Context.BLUETOOTH_SERVICE,
                (Consumer<BluetoothManager>) btManager -> {
                        BluetoothAdapter btAdapter = btManager.getAdapter();
                        if (btAdapter != null) {
                            btAdapter.clearBluetooth();
                        }
                });
        return this;
    }

    /**
     * Append a step of resetting APN configurations.
     * @param subscriptionId of a SIM card
     * @return this
     */
    public ResetNetworkOperationBuilder resetApn(int subscriptionId) {
        Runnable runnable = () -> {
            long startTime = SystemClock.elapsedRealtime();

            Uri uri = Uri.parse(ApnSettings.RESTORE_CARRIERS_URI);

            if (SubscriptionManager.isUsableSubscriptionId(subscriptionId)) {
                uri = Uri.withAppendedPath(uri, "subId/" + String.valueOf(subscriptionId));
            }

            if (!DRY_RUN) {
                ContentResolver resolver = mContext.getContentResolver();
                resolver.delete(uri, null, null);
            }

            long endTime = SystemClock.elapsedRealtime();
            Log.i(TAG, "Reset " + uri + ", takes " + (endTime - startTime) + " ms");
        };
        mResetSequence.add(runnable);
        return this;
    }

    /**
     * Append a step of resetting IMS stack.
     *
     * @return this
     */
    public ResetNetworkOperationBuilder resetIms(int subId) {
        attachSystemServiceWork(Context.TELEPHONY_SERVICE,
                (Consumer<TelephonyManager>) tm -> {
                    if (subId == ResetNetworkRequest.INVALID_SUBSCRIPTION_ID) {
                        // Do nothing
                        return;
                    }
                    if (subId == ResetNetworkRequest.ALL_SUBSCRIPTION_ID) {
                        // Reset IMS for all slots
                        for (int slotIndex = 0; slotIndex < tm.getActiveModemCount(); slotIndex++) {
                            tm.resetIms(slotIndex);
                            Log.i(TAG, "IMS was reset for slot " + slotIndex);
                        }
                    } else {
                        // Reset IMS for the slot specified by the sucriptionId.
                        final int slotIndex = SubscriptionManager.getSlotIndex(subId);
                        tm.resetIms(slotIndex);
                        Log.i(TAG, "IMS was reset for slot " + slotIndex);
                    }
                });
        return this;
    }

    /**
     * Append a step to restart phone process by the help of TelephonyContentProvider.
     * It's a no-op if TelephonyContentProvider doesn't exist.
     * @return this
     */
    public ResetNetworkOperationBuilder restartPhoneProcess() {
        try {
            mContext.getContentResolver().call(
                    getResetTelephonyContentProviderAuthority(),
                    METHOD_RESTART_PHONE_PROCESS,
                    /* arg= */ null,
                    /* extras= */ null);
            Log.i(TAG, "Phone process was restarted.");
        } catch (IllegalArgumentException iae) {
            Log.w(TAG, "Fail to restart phone process: " + iae);
        }
        return this;
    }

    /**
     * Append a step to restart RILD by the help of TelephonyContentProvider.
     * It's a no-op if TelephonyContentProvider doesn't exist.
     * @return this
     */
    public ResetNetworkOperationBuilder restartRild() {
        try {
            mContext.getContentResolver().call(
                    getResetTelephonyContentProviderAuthority(),
                    METHOD_RESTART_RILD,
                    /* arg= */ null,
                    /* extras= */ null);
            Log.i(TAG, "RILD was restarted.");
        } catch (IllegalArgumentException iae) {
            Log.w(TAG, "Fail to restart RILD: " + iae);
        }
        return this;
    }

    /**
     * Construct a Runnable containing all operations appended.
     * @return Runnable
     */
    public Runnable build() {
        return () -> mResetSequence.forEach(runnable -> runnable.run());
    }

    protected <T> void attachSystemServiceWork(String serviceName, Consumer<T> serviceAccess) {
        T service = (T) mContext.getSystemService(serviceName);
        if (service == null) {
            return;
        }
        Runnable runnable = () -> {
            long startTime = SystemClock.elapsedRealtime();
            if (!DRY_RUN) {
                serviceAccess.accept(service);
            }
            long endTime = SystemClock.elapsedRealtime();
            Log.i(TAG, "Reset " + serviceName + ", takes " + (endTime - startTime) + " ms");
        };
        mResetSequence.add(runnable);
    }

    /**
     * @return the authority of the telephony content provider that support methods
     * resetPhoneProcess and resetRild.
     */
    @VisibleForTesting
    String getResetTelephonyContentProviderAuthority() {
        return mContext.getResources().getString(
                R.string.reset_telephony_stack_content_provider_authority);
    }
}
