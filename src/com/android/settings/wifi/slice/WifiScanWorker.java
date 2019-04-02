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

import static com.android.settings.wifi.slice.WifiSlice.DEFAULT_EXPANDED_ROW_COUNT;

import android.content.Context;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;

import com.android.settings.slices.SliceBackgroundWorker;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settingslib.wifi.WifiTracker;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link SliceBackgroundWorker} for Wi-Fi, used by WifiSlice.
 */
public class WifiScanWorker extends SliceBackgroundWorker<AccessPoint>
        implements WifiTracker.WifiListener {

    private final Context mContext;

    private WifiTracker mWifiTracker;

    public WifiScanWorker(Context context, Uri uri) {
        super(context, uri);
        mContext = context;
    }

    @Override
    protected void onSlicePinned() {
        if (mWifiTracker == null) {
            mWifiTracker = new WifiTracker(mContext, this /* wifiListener */,
                    true /* includeSaved */, true /* includeScans */);
        }
        mWifiTracker.onStart();
        onAccessPointsChanged();
    }

    @Override
    protected void onSliceUnpinned() {
        mWifiTracker.onStop();
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
}