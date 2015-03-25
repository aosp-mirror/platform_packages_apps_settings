/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.app.Fragment;
import android.content.Context;
import android.net.IConnectivityManager;
import android.net.NetworkPolicy;
import android.net.NetworkPolicyManager;
import android.net.NetworkTemplate;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.internal.net.VpnConfig;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.settings.net.NetworkPolicyEditor;

import java.util.ArrayList;
import java.util.List;

/**
 * Confirm and execute a reset of the network settings to a clean "just out of the box"
 * state.  Multiple confirmations are required: first, a general "are you sure
 * you want to do this?" prompt, followed by a keyguard pattern trace if the user
 * has defined one, followed by a final strongly-worded "THIS WILL RESET EVERYTHING"
 * prompt.  If at any time the phone is allowed to go to sleep, is
 * locked, et cetera, then the confirmation sequence is abandoned.
 *
 * This is the confirmation screen.
 */
public class ResetNetworkConfirm extends Fragment {

    private View mContentView;
    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

    /**
     * The user has gone through the multiple confirmation, so now we go ahead
     * and reset the network settings to its factory-default state.
     */
    private Button.OnClickListener mFinalClickListener = new Button.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (Utils.isMonkeyRunning()) {
                return;
            }
            // TODO maybe show a progress dialog if this ends up taking a while

            IConnectivityManager connectivityService = IConnectivityManager.Stub.asInterface(
                    ServiceManager.getService(Context.CONNECTIVITY_SERVICE));
            WifiManager wifiManager = (WifiManager)
                    getActivity().getSystemService(Context.WIFI_SERVICE);
            TelephonyManager telephonyManager = (TelephonyManager)
                    getActivity().getSystemService(Context.TELEPHONY_SERVICE);
            NetworkPolicyManager policyManager = NetworkPolicyManager.from(getActivity());
            NetworkPolicyEditor policyEditor = new NetworkPolicyEditor(policyManager);
            policyEditor.read();

            // Turn airplane mode off
            try {
                connectivityService.setAirplaneMode(false);
            } catch (RemoteException e) {
                // Well, we tried
            }

            // Turn wifi on
            wifiManager.setWifiEnabled(true);

            // Delete all Wifi SSIDs
            List<WifiConfiguration> networks = wifiManager.getConfiguredNetworks();
            if (networks != null) {
                for (WifiConfiguration config : networks) {
                    wifiManager.removeNetwork(config.networkId);
                }
            }

            // Turn mobile hotspot off
            wifiManager.setWifiApEnabled(null, false);

            // Un-tether
            try {
                for (String tether : connectivityService.getTetheredIfaces()) {
                    connectivityService.untether(tether);
                }
            } catch (RemoteException e) {
                // Well, we tried
            }

            // Turn VPN off
            try {
                VpnConfig vpnConfig = connectivityService.getVpnConfig();
                if (vpnConfig != null) {
                    if (vpnConfig.legacy) {
                        connectivityService.prepareVpn(VpnConfig.LEGACY_VPN, VpnConfig.LEGACY_VPN);
                    } else {
                        // Prevent this app from initiating VPN connections in the future without
                        // user intervention.
                        connectivityService.setVpnPackageAuthorization(false);
                        connectivityService.prepareVpn(vpnConfig.user, VpnConfig.LEGACY_VPN);
                    }
                }
            } catch (RemoteException e) {
                // Well, we tried
            }

            if (SubscriptionManager.isUsableSubIdValue(mSubId)) {
                // Turn mobile data on
                telephonyManager.setDataEnabled(mSubId, true);

                // Set mobile network selection mode to automatic
                // TODO set network selection mode to automatic
                // phone.setNetworkSelectionModeAutomatic(null);

                // Set preferred mobile network type to manufacturer's recommended
                // int networkType = ; // TODO get manufacturer's default
                // telephonyManager.setPreferredNetworkType(networkType);

                // Turn roaming to manufacturer's default
                // boolean enabled = ; // TODO get manufacturer's default
                // SubscriptionManager.from(getContext()).setDataRoaming(enabled, mSubId);

                String subscriberId = telephonyManager.getSubscriberId(mSubId);
                NetworkTemplate template = NetworkTemplate.buildTemplateMobileAll(subscriberId);
                // Turn mobile data limit off
                policyEditor.setPolicyLimitBytes(template, NetworkPolicy.LIMIT_DISABLED);
            }

            // Turn restrict background data off
            policyManager.setRestrictBackground(false);

            // Remove app's "restrict background data" flag
            for (int uid : policyManager.getUidsWithPolicy(
                    NetworkPolicyManager.POLICY_REJECT_METERED_BACKGROUND)) {
                policyManager.setUidPolicy(uid, NetworkPolicyManager.POLICY_NONE);
            }

            Toast.makeText(getActivity(), R.string.reset_network_complete_toast, Toast.LENGTH_SHORT)
                    .show();
        }
    };

    /**
     * Configure the UI for the final confirmation interaction
     */
    private void establishFinalConfirmationState() {
        mContentView.findViewById(R.id.execute_reset_network)
                .setOnClickListener(mFinalClickListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.reset_network_confirm, null);
        establishFinalConfirmationState();
        return mContentView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            mSubId = args.getInt(PhoneConstants.SUBSCRIPTION_KEY,
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        }
    }
}
