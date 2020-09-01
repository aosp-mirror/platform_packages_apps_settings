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
 * limitations under the License.
 */

package com.android.settings.wifi.slice;

import static android.net.NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL;
import static android.net.NetworkCapabilities.NET_CAPABILITY_PARTIAL_CONNECTIVITY;
import static android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED;
import static android.net.NetworkCapabilities.TRANSPORT_WIFI;

import static com.android.settings.wifi.slice.WifiSlice.DEFAULT_EXPANDED_ROW_COUNT;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.internal.util.Preconditions;
import com.android.settings.slices.SliceBackgroundWorker;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settingslib.wifi.WifiTracker;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link SliceBackgroundWorker} for Wi-Fi, used by {@link WifiSlice}.
 */
public class WifiScanWorker extends SliceBackgroundWorker<AccessPoint> implements
        WifiTracker.WifiListener {

    private static final String TAG = "WifiScanWorker";

    @VisibleForTesting
    WifiNetworkCallback mNetworkCallback;

    private final Context mContext;
    private final ConnectivityManager mConnectivityManager;
    private final WifiTracker mWifiTracker;

    private static String sClickedWifiSsid;

    public WifiScanWorker(Context context, Uri uri) {
        super(context, uri);
        mContext = context;
        mConnectivityManager = context.getSystemService(ConnectivityManager.class);
        mWifiTracker = new WifiTracker(mContext, this /* wifiListener */,
                true /* includeSaved */, true /* includeScans */);
    }

    @Override
    protected void onSlicePinned() {
        mWifiTracker.onStart();
        onAccessPointsChanged();
    }

    @Override
    protected void onSliceUnpinned() {
        mWifiTracker.onStop();
        unregisterNetworkCallback();
        clearClickedWifiOnSliceUnpinned();
    }

    @Override
    public void close() {
        mWifiTracker.onDestroy();
    }

    @Override
    public void onWifiStateChanged(int state) {
        notifySliceChange();
    }

    @Override
    public void onConnectedChanged() {
    }

    @Override
    public void onAccessPointsChanged() {
        // in case state has changed
        if (!mWifiTracker.getManager().isWifiEnabled()) {
            updateResults(null);
            return;
        }
        // AccessPoints are sorted by the WifiTracker
        final List<AccessPoint> accessPoints = mWifiTracker.getAccessPoints();
        final List<AccessPoint> resultList = new ArrayList<>();
        final int apRowCount = getApRowCount();
        for (AccessPoint ap : accessPoints) {
            if (ap.isReachable()) {
                resultList.add(clone(ap));
                if (resultList.size() >= apRowCount) {
                    break;
                }
            }
        }
        updateResults(resultList);
    }

    protected int getApRowCount() {
        return DEFAULT_EXPANDED_ROW_COUNT;
    }

    private AccessPoint clone(AccessPoint accessPoint) {
        final Bundle savedState = new Bundle();
        accessPoint.saveWifiState(savedState);
        return new AccessPoint(mContext, savedState);
    }

    @Override
    protected boolean areListsTheSame(List<AccessPoint> a, List<AccessPoint> b) {
        if (!a.equals(b)) {
            return false;
        }

        // compare access point states one by one
        final int listSize = a.size();
        for (int i = 0; i < listSize; i++) {
            if (a.get(i).getDetailedState() != b.get(i).getDetailedState()) {
                return false;
            }
        }
        return true;
    }

    static void saveClickedWifi(AccessPoint accessPoint) {
        sClickedWifiSsid = accessPoint.getSsidStr();
    }

    static void clearClickedWifi() {
        sClickedWifiSsid = null;
    }

    static boolean isWifiClicked(WifiInfo info) {
        final String ssid = WifiInfo.sanitizeSsid(info.getSSID());
        return !TextUtils.isEmpty(ssid) && TextUtils.equals(ssid, sClickedWifiSsid);
    }

    protected void clearClickedWifiOnSliceUnpinned() {
        clearClickedWifi();
    }

    protected boolean isSessionValid() {
        return true;
    }

    public void registerNetworkCallback(Network wifiNetwork) {
        if (wifiNetwork == null) {
            return;
        }

        if (mNetworkCallback != null && mNetworkCallback.isSameNetwork(wifiNetwork)) {
            return;
        }

        unregisterNetworkCallback();

        mNetworkCallback = new WifiNetworkCallback(wifiNetwork);
        mConnectivityManager.registerNetworkCallback(
                new NetworkRequest.Builder()
                        .clearCapabilities()
                        .addTransportType(TRANSPORT_WIFI)
                        .build(),
                mNetworkCallback,
                new Handler(Looper.getMainLooper()));
    }

    public void unregisterNetworkCallback() {
        if (mNetworkCallback != null) {
            try {
                mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
            } catch (RuntimeException e) {
                Log.e(TAG, "Unregistering CaptivePortalNetworkCallback failed.", e);
            }
            mNetworkCallback = null;
        }
    }

    class WifiNetworkCallback extends NetworkCallback {

        private final Network mNetwork;
        private boolean mIsCaptivePortal;
        private boolean mHasPartialConnectivity;
        private boolean mIsValidated;

        WifiNetworkCallback(Network network) {
            mNetwork = Preconditions.checkNotNull(network);
        }

        @Override
        public void onCapabilitiesChanged(Network network, NetworkCapabilities nc) {
            if (!isSameNetwork(network)) {
                return;
            }

            final boolean prevIsCaptivePortal = mIsCaptivePortal;
            final boolean prevHasPartialConnectivity = mHasPartialConnectivity;
            final boolean prevIsValidated = mIsValidated;

            mIsCaptivePortal = nc.hasCapability(NET_CAPABILITY_CAPTIVE_PORTAL);
            mHasPartialConnectivity = nc.hasCapability(NET_CAPABILITY_PARTIAL_CONNECTIVITY);
            mIsValidated = nc.hasCapability(NET_CAPABILITY_VALIDATED);

            if (prevIsCaptivePortal == mIsCaptivePortal
                    && prevHasPartialConnectivity == mHasPartialConnectivity
                    && prevIsValidated == mIsValidated) {
                return;
            }

            notifySliceChange();

            // Automatically start captive portal
            if (!prevIsCaptivePortal && mIsCaptivePortal
                    && isWifiClicked(mWifiTracker.getManager().getConnectionInfo())
                    && isSessionValid()) {
                final Intent intent = new Intent(mContext, ConnectToWifiHandler.class)
                        .putExtra(ConnectivityManager.EXTRA_NETWORK, network)
                        .addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                // Sending a broadcast in the system process needs to specify a user
                mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
            }
        }

        /**
         * Returns true if the supplied network is not null and is the same as the originally
         * supplied value.
         */
        public boolean isSameNetwork(Network network) {
            return mNetwork.equals(network);
        }
    }
}
