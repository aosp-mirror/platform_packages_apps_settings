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

package com.android.settings.wifi.slice;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkInfo.State;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.slices.CustomSliceRegistry;
import com.android.settings.slices.CustomSliceable;
import com.android.settingslib.wifi.AccessPoint;

/**
 * {@link CustomSliceable} for Wi-Fi, used by contextual homepage.
 */
public class ContextualWifiSlice extends WifiSlice {

    @VisibleForTesting
    static final String CONTEXTUAL_WIFI_EXPANDABLE = "contextual_wifi_expandable";

    @VisibleForTesting
    static final int COLLAPSED_ROW_COUNT = 0;

    @VisibleForTesting
    static long sActiveUiSession = -1000;
    @VisibleForTesting
    static boolean sApRowCollapsed;

    public ContextualWifiSlice(Context context) {
        super(context);
    }

    @Override
    public Uri getUri() {
        return CustomSliceRegistry.CONTEXTUAL_WIFI_SLICE_URI;
    }

    @Override
    public Slice getSlice() {
        if (isExpandable()) {
            final long currentUiSession = FeatureFactory.getFactory(mContext)
                    .getSlicesFeatureProvider().getUiSessionToken();
            if (currentUiSession != sActiveUiSession) {
                sActiveUiSession = currentUiSession;
                sApRowCollapsed = hasWorkingNetwork();
            } else if (!mWifiManager.isWifiEnabled()) {
                sApRowCollapsed = false;
            }
        } else {
            sApRowCollapsed = true;
        }
        return super.getSlice();
    }

    static int getApRowCount() {
        return sApRowCollapsed ? COLLAPSED_ROW_COUNT : DEFAULT_EXPANDED_ROW_COUNT;
    }

    @Override
    protected boolean isApRowCollapsed() {
        return sApRowCollapsed;
    }

    @Override
    protected ListBuilder.RowBuilder getHeaderRow(boolean isWifiEnabled, AccessPoint accessPoint) {
        final ListBuilder.RowBuilder builder = super.getHeaderRow(isWifiEnabled, accessPoint);
        builder.setTitleItem(getHeaderIcon(isWifiEnabled, accessPoint), ListBuilder.ICON_IMAGE);
        if (sApRowCollapsed && isWifiEnabled) {
            builder.setSubtitle(getSubtitle(accessPoint));
        }
        return builder;
    }

    private boolean isExpandable() {
        // Return whether this slice can be expandable.
        return Settings.Global.getInt(mContext.getContentResolver(), CONTEXTUAL_WIFI_EXPANDABLE, 0)
                != 0;
    }

    private IconCompat getHeaderIcon(boolean isWifiEnabled, AccessPoint accessPoint) {
        final Drawable drawable;
        final int tint;
        if (!isWifiEnabled) {
            drawable = mContext.getDrawable(R.drawable.ic_wifi_off);
            tint = Utils.getDisabled(mContext, Utils.getColorAttrDefaultColor(mContext,
                    android.R.attr.colorControlNormal));
        } else {
            // get icon of medium signal strength
            drawable = mContext.getDrawable(com.android.settingslib.Utils.getWifiIconResource(2));
            if (isNetworkConnected(accessPoint)) {
                tint = Utils.getColorAccentDefaultColor(mContext);
            } else {
                tint = Utils.getColorAttrDefaultColor(mContext, android.R.attr.colorControlNormal);
            }
        }
        drawable.setTint(tint);
        return Utils.createIconWithDrawable(drawable);
    }

    private boolean isNetworkConnected(AccessPoint accessPoint) {
        if (accessPoint == null) {
            return false;
        }

        final NetworkInfo networkInfo = accessPoint.getNetworkInfo();
        if (networkInfo == null) {
            return false;
        }

        return networkInfo.getState() == State.CONNECTED;
    }

    private CharSequence getSubtitle(AccessPoint accessPoint) {
        if (isCaptivePortal()) {
            final int id = mContext.getResources()
                    .getIdentifier("network_available_sign_in", "string", "android");
            return mContext.getText(id);
        }

        if (accessPoint == null) {
            return mContext.getText(R.string.disconnected);
        }

        final NetworkInfo networkInfo = accessPoint.getNetworkInfo();
        if (networkInfo == null) {
            return mContext.getText(R.string.disconnected);
        }

        final State state = networkInfo.getState();
        DetailedState detailedState;
        if (state == State.CONNECTING) {
            detailedState = DetailedState.CONNECTING;
        } else if (state == State.CONNECTED) {
            detailedState = DetailedState.CONNECTED;
        } else {
            detailedState = networkInfo.getDetailedState();
        }

        final String[] formats = mContext.getResources().getStringArray(
                R.array.wifi_status_with_ssid);
        final int index = detailedState.ordinal();
        return String.format(formats[index], accessPoint.getTitle());
    }

    private boolean hasWorkingNetwork() {
        return !TextUtils.equals(getActiveSSID(), WifiManager.UNKNOWN_SSID) && hasInternetAccess();
    }

    private String getActiveSSID() {
        if (mWifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLED) {
            return WifiManager.UNKNOWN_SSID;
        }
        return WifiInfo.sanitizeSsid(mWifiManager.getConnectionInfo().getSSID());
    }

    private boolean hasInternetAccess() {
        final NetworkCapabilities nc = mConnectivityManager.getNetworkCapabilities(
                mWifiManager.getCurrentNetwork());
        return nc != null
                && !nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL)
                && !nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_PARTIAL_CONNECTIVITY)
                && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    @Override
    public Class getBackgroundWorkerClass() {
        return ContextualWifiScanWorker.class;
    }
}
