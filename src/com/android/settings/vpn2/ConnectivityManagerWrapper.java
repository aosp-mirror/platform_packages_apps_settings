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

/**
 * This interface replicates a subset of the android.net.ConnectivityManager (CM). The interface
 * exists so that we can use a thin wrapper around the CM in production code and a mock in tests.
 * We cannot directly mock or shadow the CM, because some of the methods we rely on are marked as
 * hidden and are thus invisible to Robolectric.
 */
public interface ConnectivityManagerWrapper {

    /**
     * Returns the real ConnectivityManager object wrapped by this wrapper.
     */
    public ConnectivityManager getConnectivityManager();

    /**
     * Calls {@code ConnectivityManager.getAlwaysOnVpnPackageForUser()}.
     *
     * @see android.net.ConnectivityManager#getAlwaysOnVpnPackageForUser
     */
   String getAlwaysOnVpnPackageForUser(int userId);

    /**
     * Calls {@code ConnectivityManager.getGlobalProxy()}.
     *
     * @see android.net.ConnectivityManager#getGlobalProxy
     */
   ProxyInfo getGlobalProxy();

    /**
     * Calls {@code ConnectivityManager.registerNetworkCallback()}.
     *
     * This is part of the ConnectivityManager public API in SDK 26 or above, but is not yet visible
     * to the robolectric tests, which currently build with SDK 23.
     * TODO: delete this once the robolectric tests build with SDK 26 or above.
     *
     * @see android.net.ConnectivityManager#registerNetworkCallback(NetworkRequest,NetworkCallback,Handler)
     */
    public void registerNetworkCallback(NetworkRequest request, NetworkCallback callback,
            Handler handler);

    /**
     * Calls {@code ConnectivityManager.startCaptivePortalApp()}.
     *
     * This is part of the ConnectivityManager public API in SDK 26 or above, but is not yet visible
     * to the robolectric tests, which currently build with SDK 23.
     * TODO: delete this once the robolectric tests build with SDK 26 or above.
     *
     * @see android.net.ConnectivityManager#startCaptivePortalApp(Network)
     */
    public void startCaptivePortalApp(Network network);
}
