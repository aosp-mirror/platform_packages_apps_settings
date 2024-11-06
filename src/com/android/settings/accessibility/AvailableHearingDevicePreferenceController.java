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

package com.android.settings.accessibility;

import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.bluetooth.BluetoothDeviceUpdater;
import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.search.SearchIndexableRaw;

import java.util.List;

/**
 * Controller to update the {@link androidx.preference.PreferenceCategory} for all
 * connected hearing devices, including ASHA and HAP profile.
 * Parent class {@link BaseBluetoothDevicePreferenceController} will use
 * {@link DevicePreferenceCallback} to add/remove {@link Preference}.
 */
public class AvailableHearingDevicePreferenceController extends
        BaseBluetoothDevicePreferenceController implements LifecycleObserver, OnStart, OnStop,
        BluetoothCallback {

    private static final String TAG = "AvailableHearingDevicePreferenceController";
    private static final String SEARCH_DATA_KEY_PREFIX = "a11y_available_hearing_device";

    private BluetoothDeviceUpdater mAvailableHearingDeviceUpdater;
    private final LocalBluetoothManager mLocalBluetoothManager;
    private FragmentManager mFragmentManager;

    public AvailableHearingDevicePreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey);
        mLocalBluetoothManager = com.android.settings.bluetooth.Utils.getLocalBluetoothManager(
                context);
    }

    @VisibleForTesting
    void init(AvailableHearingDeviceUpdater availableHearingDeviceUpdater) {
        if (mAvailableHearingDeviceUpdater != null) {
            throw new IllegalStateException("Should not call init() more than 1 time.");
        }
        mAvailableHearingDeviceUpdater = availableHearingDeviceUpdater;
    }

    /**
     * Initializes objects in this controller. Need to call this before onStart().
     *
     * <p>Should not call this more than 1 time.
     *
     * @param fragment The {@link DashboardFragment} uses the controller.
     */
    public void init(DashboardFragment fragment) {
        if (mAvailableHearingDeviceUpdater != null) {
            throw new IllegalStateException("Should not call init() more than 1 time.");
        }
        mAvailableHearingDeviceUpdater = new AvailableHearingDeviceUpdater(fragment.getContext(),
                this, fragment.getMetricsCategory());
        mFragmentManager = fragment.getParentFragmentManager();
    }

    @Override
    public void onStart() {
        mAvailableHearingDeviceUpdater.registerCallback();
        mAvailableHearingDeviceUpdater.refreshPreference();
        mLocalBluetoothManager.getEventManager().registerCallback(this);
    }

    @Override
    public void onStop() {
        mAvailableHearingDeviceUpdater.unregisterCallback();
        mLocalBluetoothManager.getEventManager().unregisterCallback(this);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        if (isAvailable()) {
            final Context context = screen.getContext();
            mAvailableHearingDeviceUpdater.setPrefContext(context);
            mAvailableHearingDeviceUpdater.forceUpdate();
        }
    }

    @Override
    public void onActiveDeviceChanged(CachedBluetoothDevice activeDevice, int bluetoothProfile) {
        if (activeDevice == null) {
            return;
        }

        if (bluetoothProfile == BluetoothProfile.HEARING_AID) {
            HearingAidUtils.launchHearingAidPairingDialog(mFragmentManager, activeDevice,
                    getMetricsCategory());
        }
    }

    @Override
    public void updateDynamicRawDataToIndex(List<SearchIndexableRaw> rawData) {
        if (Flags.fixA11ySettingsSearch()) {
            if (mLocalBluetoothManager == null) {
                Log.d(TAG, "Bluetooth is not supported");
                return;
            }

            for (CachedBluetoothDevice cachedDevice :
                    mLocalBluetoothManager.getCachedDeviceManager().getCachedDevicesCopy()) {

                if (!AvailableHearingDeviceUpdater.isAvailableHearingDevice(cachedDevice)) {
                    continue;
                }

                SearchIndexableRaw data = new SearchIndexableRaw(mContext);
                // Include the identity address and add prefix to ensure the key is unique and
                // distinguish from Bluetooth's connected devices.
                data.key = SEARCH_DATA_KEY_PREFIX
                        + cachedDevice.getName() + cachedDevice.getIdentityAddress();
                data.title = cachedDevice.getName();
                data.summaryOn = mContext.getString(R.string.accessibility_hearingaid_title);
                data.screenTitle = mContext.getString(R.string.accessibility_hearingaid_title);
                rawData.add(data);
            }
        } else {
            super.updateDynamicRawDataToIndex(rawData);
        }
    }
}
