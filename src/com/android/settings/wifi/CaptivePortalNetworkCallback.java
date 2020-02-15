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
package com.android.settings.wifi;

import android.net.ConnectivityManager.NetworkCallback;
import android.net.Network;
import android.net.NetworkCapabilities;

import com.android.internal.util.Preconditions;

/** Listens for changes to NetworkCapabilities to update the ConnectedAccessPointPreference. */
class CaptivePortalNetworkCallback extends NetworkCallback {

    private final ConnectedAccessPointPreference mConnectedApPreference;
    private final Network mNetwork;

    private boolean mIsCaptivePortal;

    CaptivePortalNetworkCallback(
            Network network, ConnectedAccessPointPreference connectedApPreference) {
        mNetwork = Preconditions.checkNotNull(network);
        mConnectedApPreference = Preconditions.checkNotNull(connectedApPreference);
    }

    @Override
    public final void onLost(Network network) {
        if (mNetwork.equals(network)) {
            setIsCaptivePortal(false);
        }
    }

    @Override
    public final void onCapabilitiesChanged(Network network,
            NetworkCapabilities networkCapabilities) {
        if (mNetwork.equals(network)) {
            boolean isCaptivePortal = WifiUtils.canSignIntoNetwork(networkCapabilities);
            setIsCaptivePortal(isCaptivePortal);
            mConnectedApPreference.setCaptivePortal(isCaptivePortal);
        }
    }

    /**
     * Called when captive portal capability changes for the current network. Default implementation
     * is a no-op. Use {@link CaptivePortalNetworkCallback#isCaptivePortal()} to read new
     * capability.
     */
    public void onCaptivePortalCapabilityChanged() {}

    private void setIsCaptivePortal(boolean isCaptivePortal) {
        if (isCaptivePortal == mIsCaptivePortal) {
            return;
        }
        mIsCaptivePortal = isCaptivePortal;
        onCaptivePortalCapabilityChanged();
    }

    /**
     * Returns true if the supplied network and preference are not null and are the same as the
     * originally supplied values.
     */
    public final boolean isSameNetworkAndPreference(
            Network network, ConnectedAccessPointPreference connectedApPreference) {
        return mNetwork.equals(network) && mConnectedApPreference == connectedApPreference;
    }

    /**
     * Returns true if the most recent update to the NetworkCapabilities indicates a captive portal
     * network and the Network was not lost in the interim.
     */
    public final boolean isCaptivePortal() {
        return mIsCaptivePortal;
    }

    /** Returns the currently associated network. */
    public final Network getNetwork() {
        return mNetwork;
    }
}
