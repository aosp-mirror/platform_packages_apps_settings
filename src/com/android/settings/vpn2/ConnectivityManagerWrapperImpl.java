/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.vpn2;

import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Handler;
import android.net.ProxyInfo;

public class ConnectivityManagerWrapperImpl implements ConnectivityManagerWrapper {

    private final ConnectivityManager mCm;

    public ConnectivityManagerWrapperImpl(ConnectivityManager cm) {
        mCm = cm;
    }

    @Override
    public ConnectivityManager getConnectivityManager() {
        return mCm;
    }

    @Override
    public String getAlwaysOnVpnPackageForUser(int userId) {
        return mCm.getAlwaysOnVpnPackageForUser(userId);
    }

    @Override
    public ProxyInfo getGlobalProxy() {
        return mCm.getGlobalProxy();
    }

    @Override
    public void registerNetworkCallback(NetworkRequest request, NetworkCallback callback,
            Handler handler) {
        mCm.registerNetworkCallback(request, callback, handler);
    }

    @Override
    public void startCaptivePortalApp(Network network) {
        mCm.startCaptivePortalApp(network);
    }
}
