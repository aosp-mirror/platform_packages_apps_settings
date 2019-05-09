/*
 * Copyright (C) 2019 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.network.telephony;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

/** A helper class to listen to a few different kinds of connectivity changes that could be relevant
 *  to changes in which network is active, and whether the active network has internet data
 *  connectivity. */
public class DataConnectivityListener extends ConnectivityManager.NetworkCallback {
    private Context mContext;
    private ConnectivityManager mConnectivityManager;
    private final NetworkRequest mNetworkRequest;
    private Client mClient;

    public interface Client {
        void onDataConnectivityChange();
    }

    public DataConnectivityListener(Context context, Client client) {
        mContext = context;
        mConnectivityManager = mContext.getSystemService(ConnectivityManager.class);
        mClient = client;
        mNetworkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
    }

    public void start() {
        mConnectivityManager.registerNetworkCallback(mNetworkRequest, this,
                mContext.getMainThreadHandler());
    }

    public void stop() {
        mConnectivityManager.unregisterNetworkCallback(this);
    }

    @Override
    public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
        final Network activeNetwork = mConnectivityManager.getActiveNetwork();
        if (activeNetwork != null && activeNetwork.equals(network)) {
            mClient.onDataConnectivityChange();
        }
    }

    @Override
    public void onLosing(Network network, int maxMsToLive) {
        mClient.onDataConnectivityChange();
    }

    @Override
    public void onLost(Network network) {
        mClient.onDataConnectivityChange();
    }
}
