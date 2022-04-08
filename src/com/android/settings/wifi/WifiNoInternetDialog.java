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

package com.android.settings.wifi;

import static android.net.ConnectivityManager.ACTION_PROMPT_LOST_VALIDATION;
import static android.net.ConnectivityManager.ACTION_PROMPT_PARTIAL_CONNECTIVITY;
import static android.net.ConnectivityManager.ACTION_PROMPT_UNVALIDATED;
import static android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.settings.R;

public final class WifiNoInternetDialog extends AlertActivity implements
        DialogInterface.OnClickListener {
    private static final String TAG = "WifiNoInternetDialog";

    private ConnectivityManager mCM;
    private Network mNetwork;
    private String mNetworkName;
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    private CheckBox mAlwaysAllow;
    private String mAction;
    private boolean mButtonClicked;

    private boolean isKnownAction(Intent intent) {
        return ACTION_PROMPT_UNVALIDATED.equals(intent.getAction())
                || ACTION_PROMPT_LOST_VALIDATION.equals(intent.getAction())
                || ACTION_PROMPT_PARTIAL_CONNECTIVITY.equals(intent.getAction());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        if (intent == null || !isKnownAction(intent) || !"netId".equals(intent.getScheme())) {
            Log.e(TAG, "Unexpected intent " + intent + ", exiting");
            finish();
            return;
        }

        mAction = intent.getAction();

        try {
            mNetwork = new Network(Integer.parseInt(intent.getData().getSchemeSpecificPart()));
        } catch (NullPointerException|NumberFormatException e) {
            mNetwork = null;
        }

        if (mNetwork == null) {
            Log.e(TAG, "Can't determine network from '" + intent.getData() + "' , exiting");
            finish();
            return;
        }

        // TODO: add a registerNetworkCallback(Network network, NetworkCallback networkCallback) and
        // simplify this.
        final NetworkRequest request = new NetworkRequest.Builder().clearCapabilities().build();
        mNetworkCallback = new NetworkCallback() {
            @Override
            public void onLost(Network network) {
                // Close the dialog if the network disconnects.
                if (mNetwork.equals(network)) {
                    Log.d(TAG, "Network " + mNetwork + " disconnected");
                    finish();
                }
            }
            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities nc) {
                // Close the dialog if the network validates.
                if (mNetwork.equals(network) && nc.hasCapability(NET_CAPABILITY_VALIDATED)) {
                    Log.d(TAG, "Network " + mNetwork + " validated");
                    finish();
                }
            }
        };

        mCM = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        mCM.registerNetworkCallback(request, mNetworkCallback);

        final NetworkInfo ni = mCM.getNetworkInfo(mNetwork);
        final NetworkCapabilities nc = mCM.getNetworkCapabilities(mNetwork);
        if (ni == null || !ni.isConnectedOrConnecting() || nc == null) {
            Log.d(TAG, "Network " + mNetwork + " is not connected: " + ni);
            finish();
            return;
        }
        mNetworkName = nc.getSsid();
        if (mNetworkName != null) {
            mNetworkName = WifiInfo.sanitizeSsid(mNetworkName);
        }

        createDialog();
    }

    private void createDialog() {
        mAlert.setIcon(R.drawable.ic_settings_wireless);

        final AlertController.AlertParams ap = mAlertParams;
        if (ACTION_PROMPT_UNVALIDATED.equals(mAction)) {
            ap.mTitle = mNetworkName;
            ap.mMessage = getString(R.string.no_internet_access_text);
            ap.mPositiveButtonText = getString(R.string.yes);
            ap.mNegativeButtonText = getString(R.string.no);
        } else if (ACTION_PROMPT_PARTIAL_CONNECTIVITY.equals(mAction)) {
            ap.mTitle = mNetworkName;
            ap.mMessage = getString(R.string.partial_connectivity_text);
            ap.mPositiveButtonText = getString(R.string.yes);
            ap.mNegativeButtonText = getString(R.string.no);
        } else {
            ap.mTitle = getString(R.string.lost_internet_access_title);
            ap.mMessage = getString(R.string.lost_internet_access_text);
            ap.mPositiveButtonText = getString(R.string.lost_internet_access_switch);
            ap.mNegativeButtonText = getString(R.string.lost_internet_access_cancel);
        }
        ap.mPositiveButtonListener = this;
        ap.mNegativeButtonListener = this;

        final LayoutInflater inflater = LayoutInflater.from(ap.mContext);
        final View checkbox = inflater.inflate(
                com.android.internal.R.layout.always_use_checkbox, null);
        ap.mView = checkbox;
        mAlwaysAllow = (CheckBox) checkbox.findViewById(com.android.internal.R.id.alwaysUse);

        if (ACTION_PROMPT_UNVALIDATED.equals(mAction)
                || ACTION_PROMPT_PARTIAL_CONNECTIVITY.equals(mAction)) {
            mAlwaysAllow.setText(getString(R.string.no_internet_access_remember));
        } else {
            mAlwaysAllow.setText(getString(R.string.lost_internet_access_persist));
        }

        setupAlert();
    }

    @Override
    protected void onDestroy() {
        if (mNetworkCallback != null) {
            mCM.unregisterNetworkCallback(mNetworkCallback);
            mNetworkCallback = null;
        }

        // If the user exits the no Internet or partial connectivity dialog without taking any
        // action, disconnect the network, because once the dialog has been dismissed there is no
        // way to use the network.
        //
        // Unfortunately, AlertDialog does not seem to offer any good way to get an onCancel or
        // onDismiss callback. So we implement this ourselves.
        if (isFinishing() && !mButtonClicked) {
            if (ACTION_PROMPT_PARTIAL_CONNECTIVITY.equals(mAction)) {
                mCM.setAcceptPartialConnectivity(mNetwork, false /* accept */, false /* always */);
            } else if (ACTION_PROMPT_UNVALIDATED.equals(mAction)) {
                mCM.setAcceptUnvalidated(mNetwork, false /* accept */, false /* always */);
            }
        }
        super.onDestroy();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which != BUTTON_NEGATIVE && which != BUTTON_POSITIVE) return;
        final boolean always = mAlwaysAllow.isChecked();
        final String what, action;

        mButtonClicked = true;

        if (ACTION_PROMPT_UNVALIDATED.equals(mAction)) {
            what = "NO_INTERNET";
            final boolean accept = (which == BUTTON_POSITIVE);
            action = (accept ? "Connect" : "Ignore");
            mCM.setAcceptUnvalidated(mNetwork, accept, always);
        } else if (ACTION_PROMPT_PARTIAL_CONNECTIVITY.equals(mAction)) {
            what = "PARTIAL_CONNECTIVITY";
            final boolean accept = (which == BUTTON_POSITIVE);
            action = (accept ? "Connect" : "Ignore");
            mCM.setAcceptPartialConnectivity(mNetwork, accept, always);
        } else {
            what = "LOST_INTERNET";
            final boolean avoid = (which == BUTTON_POSITIVE);
            action = (avoid ? "Switch away" : "Get stuck");
            if (always) {
                Settings.Global.putString(mAlertParams.mContext.getContentResolver(),
                        Settings.Global.NETWORK_AVOID_BAD_WIFI, avoid ? "1" : "0");
            } else if (avoid) {
                mCM.setAvoidUnvalidated(mNetwork);
            }
        }
        Log.d(TAG, what + ": " + action +  " network=" + mNetwork +
                (always ? " and remember" : ""));
    }
}
