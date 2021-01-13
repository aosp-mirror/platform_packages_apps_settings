/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.network;


import static android.app.slice.Slice.EXTRA_TOGGLE_STATE;

import static com.android.settings.slices.CustomSliceRegistry.PROVIDER_MODEL_SLICE_URI;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;

import com.android.settings.R;
import com.android.settings.SubSettings;
import com.android.settings.network.telephony.MobileNetworkUtils;
import com.android.settings.network.telephony.NetworkProviderWorker;
import com.android.settings.slices.CustomSliceable;
import com.android.settings.slices.SliceBackgroundWorker;
import com.android.settings.slices.SliceBuilderUtils;
import com.android.settings.wifi.slice.WifiSlice;
import com.android.settings.wifi.slice.WifiSliceItem;
import com.android.wifitrackerlib.WifiEntry;

import java.util.List;
import java.util.stream.Collectors;

/**
 * {@link CustomSliceable} for Wi-Fi and mobile data connection, used by generic clients.
 */
// ToDo If the provider model become default design in the future, the code needs to refactor
// the whole structure and use new "data object", and then split provider model out of old design.
public class ProviderModelSlice extends WifiSlice {

    private static final String TAG = "ProviderModelSlice";
    private final ProviderModelSliceHelper mHelper;

    public ProviderModelSlice(Context context) {
        super(context);
        mHelper = getHelper();
    }

    @Override
    public Uri getUri() {
        return PROVIDER_MODEL_SLICE_URI;
    }

    private static void log(String s) {
        Log.d(TAG, s);
    }

    protected boolean isApRowCollapsed() {
        return false;
    }

    @Override
    public Slice getSlice() {
        // The provider model slice step:
        // First section:  Add a Wi-Fi item which state is connected.
        // Second section:  Add a carrier item.
        // Third section:  Add the Wi-Fi items which are not connected.
        // Fourth section:  If device has connection problem, this row show the message for user.
        final ListBuilder listBuilder = mHelper.createListBuilder(getUri());
        if (mHelper.isAirplaneModeEnabled() && !mWifiManager.isWifiEnabled()) {
            log("Airplane mode is enabled.");
            listBuilder.setHeader(mHelper.createHeader(Settings.ACTION_AIRPLANE_MODE_SETTINGS));
            listBuilder.addGridRow(mHelper.createMessageGridRow(R.string.condition_airplane_title,
                    Settings.ACTION_AIRPLANE_MODE_SETTINGS));
            return listBuilder.build();
        }

        int maxListSize = 0;
        List<WifiSliceItem> wifiList = null;
        final NetworkProviderWorker worker = getWorker();
        if (worker != null) {
            // get Wi-Fi list.
            wifiList = worker.getResults();
            maxListSize = worker.getApRowCount();
        } else {
            log("network provider worker is null.");
        }

        final boolean hasCarrier = mHelper.hasCarrier();
        log("hasCarrier: " + hasCarrier);

        // First section:  Add a Wi-Fi item which state is connected.
        final WifiSliceItem connectedWifiItem = mHelper.getConnectedWifiItem(wifiList);
        if (connectedWifiItem != null) {
            log("get Wi-Fi item witch is connected");
            listBuilder.addRow(getWifiSliceItemRow(connectedWifiItem));
            maxListSize--;
        }

        // Second section:  Add a carrier item.
        if (hasCarrier) {
            mHelper.updateTelephony();
            listBuilder.addRow(mHelper.createCarrierRow());
            maxListSize--;
        }

        // Third section:  Add the Wi-Fi items which are not connected.
        if (wifiList != null) {
            log("get Wi-Fi items which are not connected");
            final List<WifiSliceItem> disconnectedWifiList = wifiList.stream()
                    .filter(wifiSliceItem -> wifiSliceItem.getConnectedState()
                            != WifiEntry.CONNECTED_STATE_CONNECTED)
                    .limit(maxListSize)
                    .collect(Collectors.toList());
            for (WifiSliceItem item : disconnectedWifiList) {
                listBuilder.addRow(getWifiSliceItemRow(item));
            }
        }

        // Fourth section:  If device has connection problem, this row show the message for user.
        // 1) show non_carrier_network_unavailable:
        //    - while no wifi item
        // 2) show all_network_unavailable:
        //    - while no wifi item + no carrier
        //    - while no wifi item + no data capability
        if (worker == null || wifiList == null) {
            log("wifiList is null");
            int resId = R.string.non_carrier_network_unavailable;
            if (!hasCarrier || !mHelper.isDataSimActive()) {
                log("No carrier item or no carrier data.");
                resId = R.string.all_network_unavailable;
            }

            if (!hasCarrier) {
                // If there is no item in ProviderModelItem, slice needs a header.
                listBuilder.setHeader(mHelper.createHeader(
                        NetworkProviderSettings.ACTION_NETWORK_PROVIDER_SETTINGS));
            }
            listBuilder.addGridRow(
                    mHelper.createMessageGridRow(resId,
                            NetworkProviderSettings.ACTION_NETWORK_PROVIDER_SETTINGS));
        }

        return listBuilder.build();
    }

    /**
     * Update the current carrier's mobile data status.
     */
    @Override
    public void onNotifyChange(Intent intent) {
        final SubscriptionManager subscriptionManager = mHelper.getSubscriptionManager();
        if (subscriptionManager == null) {
            return;
        }
        final int defaultSubId = subscriptionManager.getDefaultDataSubscriptionId();
        log("defaultSubId:" + defaultSubId);
        if (!SubscriptionManager.isUsableSubscriptionId(defaultSubId)) {
            return; // No subscription - do nothing.
        }
        boolean requestConnectCarrier = !intent.hasExtra(EXTRA_TOGGLE_STATE);
        // Enable the mobile data always if the user requests to connect to the carrier network.
        boolean newState = requestConnectCarrier ? true
                : intent.getBooleanExtra(EXTRA_TOGGLE_STATE, mHelper.isMobileDataEnabled());

        MobileNetworkUtils.setMobileDataEnabled(mContext, defaultSubId, newState,
                false /* disableOtherSubscriptions */);

        final NetworkProviderWorker worker = getWorker();
        if (worker == null) {
            return;
        }
        if (requestConnectCarrier) {
            worker.connectCarrierNetwork();
        } else {
            worker.setCarrierNetworkEnabled(newState);
        }
    }

    @Override
    public Intent getIntent() {
        final String screenTitle = mContext.getText(R.string.provider_internet_settings).toString();
        return SliceBuilderUtils.buildSearchResultPageIntent(mContext,
                NetworkProviderSettings.class.getName(), "" /* key */, screenTitle,
                SettingsEnums.SLICE)
                .setClassName(mContext.getPackageName(), SubSettings.class.getName())
                .setData(getUri());
    }

    @Override
    public Class getBackgroundWorkerClass() {
        return NetworkProviderWorker.class;
    }

    @VisibleForTesting
    ProviderModelSliceHelper getHelper() {
        return new ProviderModelSliceHelper(mContext, this);
    }

    @VisibleForTesting
    NetworkProviderWorker getWorker() {
        return SliceBackgroundWorker.getInstance(getUri());
    }
}
