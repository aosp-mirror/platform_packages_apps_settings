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
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import com.android.settings.network.apn.ApnSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * A builder for creating a Runnable resetting network configurations.
 */
public class ResetNetworkOperationBuilder {

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
            Boolean wipped = RecoverySystem.wipeEuiccData(mContext, callerPackage);
            if (resultCallback != null) {
                resultCallback.accept(wipped);
            }
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
            Uri uri = Uri.parse(ApnSettings.RESTORE_CARRIERS_URI);

            if (SubscriptionManager.isUsableSubscriptionId(subscriptionId)) {
                uri = Uri.withAppendedPath(uri, "subId/" + String.valueOf(subscriptionId));
            }

            ContentResolver resolver = mContext.getContentResolver();
            resolver.delete(uri, null, null);
        };
        mResetSequence.add(runnable);
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
        Runnable runnable = () -> serviceAccess.accept(service);
        mResetSequence.add(runnable);
    }
}
