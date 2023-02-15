/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.bluetooth;

import static android.os.UserManager.DISALLOW_CONFIG_BLUETOOTH;

import static com.android.settings.bluetooth.BluetoothDeviceDetailsFragment.KEY_DEVICE_ADDRESS;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.dashboard.RestrictedDashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.search.SearchIndexable;

/** Settings fragment containing bluetooth audio routing. */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class BluetoothDetailsAudioRoutingFragment extends RestrictedDashboardFragment {

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.bluetooth_audio_routing_fragment);
    private static final String TAG = "BluetoothDetailsAudioRoutingFragment";
    @VisibleForTesting
    CachedBluetoothDevice mCachedDevice;

    public BluetoothDetailsAudioRoutingFragment() {
        super(DISALLOW_CONFIG_BLUETOOTH);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        final LocalBluetoothManager localBtMgr = Utils.getLocalBtManager(context);
        final CachedBluetoothDeviceManager cachedDeviceMgr = localBtMgr.getCachedDeviceManager();
        final BluetoothDevice bluetoothDevice = localBtMgr.getBluetoothAdapter().getRemoteDevice(
                getArguments().getString(KEY_DEVICE_ADDRESS));

        mCachedDevice = cachedDeviceMgr.findDevice(bluetoothDevice);
        if (mCachedDevice == null) {
            // Close this page if device is null with invalid device mac address
            Log.w(TAG, "onAttach() CachedDevice is null! Can not find address: "
                    + bluetoothDevice.getAnonymizedAddress());
            finish();
            return;
        }

        use(HearingDeviceRingtoneRoutingPreferenceController.class).init(mCachedDevice);
        use(HearingDeviceCallRoutingPreferenceController.class).init(mCachedDevice);
        use(HearingDeviceMediaRoutingPreferenceController.class).init(mCachedDevice);
        use(HearingDeviceSystemSoundsRoutingPreferenceController.class).init(mCachedDevice);
    }

    @Override
    public int getMetricsCategory() {
        // TODO(b/262839191): To be updated settings_enums.proto
        return 0;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.bluetooth_audio_routing_fragment;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }
}
