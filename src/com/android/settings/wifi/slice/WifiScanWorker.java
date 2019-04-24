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

import static android.net.NetworkCapabilities.TRANSPORT_WIFI;
import static com.android.settings.wifi.slice.WifiSlice.DEFAULT_EXPANDED_ROW_COUNT;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
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
import com.android.settings.wifi.WifiUtils;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settingslib.wifi.WifiTracker;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link SliceBackgroundWorker} for Wi-Fi, used by WifiSlice.
 */
public class WifiScanWorker extends SliceBackgroundWorker<AccessPoint> implements
        WifiTracker.WifiListener {

    private static final String TAG = "WifiScanWorker";

    @VisibleForTesting
    CaptivePortalNetworkCallback mCaptivePortalNetworkCallback;

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
        unregisterCaptivePortalNetworkCallback();
        clearClickedWifi();
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
        for (AccessPoint ap : accessPoints) {
            if (ap.isReachable()) {
                resultList.add(clone(ap));
                if (resultList.size() >= DEFAULT_EXPANDED_ROW_COUNT) {
                    break;
                }
            }
        }
        updateResults(resultList);
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
            if (getState(a.get(i)) != getState(b.get(i))) {
                return false;
            }
        }
        return true;
    }

    private NetworkInfo.State getState(AccessPoint accessPoint) {
        final NetworkInfo networkInfo = accessPoint.getNetworkInfo();
        if (networkInfo != null) {
            return networkInfo.getState();
        }
        return null;
    }

    static void saveClickedWifi(AccessPoint accessPoint) {
        sClickedWifiSsid = accessPoint.getSsidStr();
    }

    static void clearClickedWifi() {
        sClickedWifiSsid = null;
    }

    static boolean isWifiClicked(WifiInfo info) {
        final String ssid = WifiInfo.removeDoubleQuotes(info.getSSID());
        return !TextUtils.isEmpty(ssid) && TextUtils.equals(ssid, sClickedWifiSsid);
    }

    public void registerCaptivePortalNetworkCallback(Network wifiNetwork) {
        if (wifiNetwork == null) {
            return;
        }

        if (mCaptivePortalNetworkCallback != null
                && mCaptivePortalNetworkCallback.isSameNetwork(wifiNetwork)) {
            return;
        }

        unregisterCaptivePortalNetworkCallback();

        mCaptivePortalNetworkCallback = new CaptivePortalNetworkCallback(wifiNetwork);
        mConnectivityManager.registerNetworkCallback(
                new NetworkRequest.Builder()
                        .clearCapabilities()
                        .addTransportType(TRANSPORT_WIFI)
                        .build(),
                mCaptivePortalNetworkCallback,
                new Handler(Looper.getMainLooper()));
    }

    public void unregisterCaptivePortalNetworkCallback() {
        if (mCaptivePortalNetworkCallback != null) {
            try {
                mConnectivityManager.unregisterNetworkCallback(mCaptivePortalNetworkCallback);
            } catch (RuntimeException e) {
                Log.e(TAG, "Unregistering CaptivePortalNetworkCallback failed.", e);
            }
            mCaptivePortalNetworkCallback = null;
        }
    }

    class CaptivePortalNetworkCallback extends NetworkCallback {

        private final Network mNetwork;
        private boolean mIsCaptivePortal;

        CaptivePortalNetworkCallback(Network network) {
            mNetwork = Preconditions.checkNotNull(network);
        }

        @Override
        public void onCapabilitiesChanged(Network network,
                NetworkCapabilities networkCapabilities) {
            if (!isSameNetwork(network)) {
                return;
            }

            final boolean isCaptivePortal = WifiUtils.canSignIntoNetwork(networkCapabilities);
            if (mIsCaptivePortal == isCaptivePortal) {
                return;
            }

            mIsCaptivePortal = isCaptivePortal;
            notifySliceChange();

            // Automatically start captive portal
            if (mIsCaptivePortal) {
                if (!isWifiClicked(mWifiTracker.getManager().getConnectionInfo())) {
                    return;
                }

                final Intent intent = new Intent(mContext, ConnectToWifiHandler.class)
                        .putExtra(ConnectivityManager.EXTRA_NETWORK, network)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                // Starting activity in the system process needs to specify a user
                mContext.startActivityAsUser(intent, UserHandle.CURRENT);
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
