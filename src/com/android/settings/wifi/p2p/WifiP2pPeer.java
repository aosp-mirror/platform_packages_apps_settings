/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings.wifi.p2p;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.text.TextUtils;
import android.widget.ImageView;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

public class WifiP2pPeer extends Preference {

    private static final int FIXED_RSSI = 60;
    private static final int[] STATE_SECURED = {com.android.settingslib.R.attr.state_encrypted};
    public WifiP2pDevice device;

    @VisibleForTesting final int mRssi;
    private ImageView mSignal;

    @VisibleForTesting
    static final int SIGNAL_LEVELS = 4;

    public WifiP2pPeer(Context context, WifiP2pDevice dev) {
        super(context);
        device = dev;
        setWidgetLayoutResource(R.layout.preference_widget_wifi_signal);
        mRssi = FIXED_RSSI; //TODO: fix
        if (TextUtils.isEmpty(device.deviceName)) {
            setTitle(device.deviceAddress);
        } else {
            setTitle(device.deviceName);
        }
        String[] statusArray = context.getResources().getStringArray(R.array.wifi_p2p_status);
        setSummary(statusArray[device.status]);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        mSignal = (ImageView) view.findViewById(R.id.signal);
        if (mRssi == Integer.MAX_VALUE) {
            mSignal.setImageDrawable(null);
        } else {
            mSignal.setImageResource(R.drawable.wifi_signal);
            mSignal.setImageState(STATE_SECURED,  true);
        }
        mSignal.setImageLevel(getLevel());
    }

    @Override
    public int compareTo(Preference preference) {
        if (!(preference instanceof WifiP2pPeer)) {
            return 1;
        }
        WifiP2pPeer other = (WifiP2pPeer) preference;

        // devices go in the order of the status
        if (device.status != other.device.status) {
            return device.status < other.device.status ? -1 : 1;
        }

        // Sort by name/address
        if (device.deviceName != null) {
            return device.deviceName.compareToIgnoreCase(other.device.deviceName);
        }

        return device.deviceAddress.compareToIgnoreCase(other.device.deviceAddress);
    }

    int getLevel() {
        if (mRssi == Integer.MAX_VALUE) {
            return -1;
        }
        return WifiManager.calculateSignalLevel(mRssi, SIGNAL_LEVELS);
    }
}
