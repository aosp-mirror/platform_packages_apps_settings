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

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.settings.R;

import static android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED;

public final class WifiNoInternetDialog extends AlertActivity implements
        DialogInterface.OnClickListener {
    private static final String TAG = "WifiNoInternetDialog";

    private ConnectivityManager mCM;
    private Network mNetwork;
    private String mNetworkName;
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    private CheckBox mAlwaysAllow;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        if (intent == null ||
                !intent.getAction().equals(ConnectivityManager.ACTION_PROMPT_UNVALIDATED) ||
                !"netId".equals(intent.getScheme())) {
            Log.e(TAG, "Unexpected intent " + intent + ", exiting");
            finish();
            return;
        }

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
        if (ni == null || !ni.isConnectedOrConnecting()) {
            Log.d(TAG, "Network " + mNetwork + " is not connected: " + ni);
            finish();
            return;
        }
        mNetworkName = ni.getExtraInfo();
        if (mNetworkName != null) {
            mNetworkName = mNetworkName.replaceAll("^\"|\"$", "");  // Remove double quotes
        }

        createDialog();
    }

    private void createDialog() {
        mAlert.setIcon(R.drawable.ic_settings_wireless);

        final AlertController.AlertParams ap = mAlertParams;
        ap.mTitle = mNetworkName;
        ap.mMessage = getString(R.string.no_internet_access_text);
        ap.mPositiveButtonText = getString(R.string.yes);
        ap.mNegativeButtonText = getString(R.string.no);
        ap.mPositiveButtonListener = this;
        ap.mNegativeButtonListener = this;

        final LayoutInflater inflater = LayoutInflater.from(ap.mContext);
        final View checkbox = inflater.inflate(
                com.android.internal.R.layout.always_use_checkbox, null);
        ap.mView = checkbox;

        mAlwaysAllow = (CheckBox) checkbox.findViewById(com.android.internal.R.id.alwaysUse);
        mAlwaysAllow.setText(getString(R.string.no_internet_access_remember));

        setupAlert();
    }

    @Override
    protected void onDestroy() {
        if (mNetworkCallback != null) {
            mCM.unregisterNetworkCallback(mNetworkCallback);
            mNetworkCallback = null;
        }
        super.onDestroy();
    }

    public void onClick(DialogInterface dialog, int which) {
        final boolean accept = (which == BUTTON_POSITIVE);
        final String action = (accept ? "Connect" : "Ignore");
        final boolean always = mAlwaysAllow.isChecked();

        switch (which) {
            case BUTTON_POSITIVE:
            case BUTTON_NEGATIVE:
                mCM.setAcceptUnvalidated(mNetwork, accept, always);
                Log.d(TAG, action +  " network=" + mNetwork + (always ? " and remember" : ""));
                break;
            default:
                break;
        }
    }
}
