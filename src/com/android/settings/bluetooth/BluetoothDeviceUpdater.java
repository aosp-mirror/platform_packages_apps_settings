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
package com.android.settings.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.GearPreference;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Update the bluetooth devices. It gets bluetooth event from {@link LocalBluetoothManager} using
 * {@link BluetoothCallback}. It notifies the upper level whether to add/remove the preference
 * through {@link DevicePreferenceCallback}
 *
 * In {@link BluetoothDeviceUpdater}, it uses {@link #isFilterMatched(CachedBluetoothDevice)} to
 * detect whether the {@link CachedBluetoothDevice} is relevant.
 */
public abstract class BluetoothDeviceUpdater implements BluetoothCallback,
        LocalBluetoothProfileManager.ServiceListener {
    protected final MetricsFeatureProvider mMetricsFeatureProvider;
    protected final DevicePreferenceCallback mDevicePreferenceCallback;
    protected final Map<BluetoothDevice, Preference> mPreferenceMap;
    protected Context mContext;
    protected Context mPrefContext;
    @VisibleForTesting
    protected LocalBluetoothManager mLocalManager;
    protected int mMetricsCategory;

    protected static final String TAG = "BluetoothDeviceUpdater";
    private static final boolean DBG = Log.isLoggable(TAG, Log.DEBUG);

    @VisibleForTesting
    final GearPreference.OnGearClickListener mDeviceProfilesListener = pref -> {
        launchDeviceDetails(pref);
    };

    public BluetoothDeviceUpdater(Context context,
            DevicePreferenceCallback devicePreferenceCallback, int metricsCategory) {
        this(context, devicePreferenceCallback, Utils.getLocalBtManager(context), metricsCategory);
    }

    @VisibleForTesting
    BluetoothDeviceUpdater(Context context,
            DevicePreferenceCallback devicePreferenceCallback, LocalBluetoothManager localManager,
            int metricsCategory) {
        mContext = context;
        mDevicePreferenceCallback = devicePreferenceCallback;
        mPreferenceMap = new HashMap<>();
        mLocalManager = localManager;
        mMetricsCategory = metricsCategory;
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    /**
     * Register the bluetooth event callback and update the list
     */
    public void registerCallback() {
        if (mLocalManager == null) {
            Log.e(getLogTag(), "registerCallback() Bluetooth is not supported on this device");
            return;
        }
        mLocalManager.setForegroundActivity(mContext);
        mLocalManager.getEventManager().registerCallback(this);
        mLocalManager.getProfileManager().addServiceListener(this);
        forceUpdate();
    }

    /**
     * Unregister the bluetooth event callback
     */
    public void unregisterCallback() {
        if (mLocalManager == null) {
            Log.e(getLogTag(), "unregisterCallback() Bluetooth is not supported on this device");
            return;
        }
        mLocalManager.setForegroundActivity(null);
        mLocalManager.getEventManager().unregisterCallback(this);
        mLocalManager.getProfileManager().removeServiceListener(this);
    }

    /**
     * Force to update the list of bluetooth devices
     */
    public void forceUpdate() {
        if (mLocalManager == null) {
            Log.e(getLogTag(), "forceUpdate() Bluetooth is not supported on this device");
            return;
        }
        if (BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            final Collection<CachedBluetoothDevice> cachedDevices =
                    mLocalManager.getCachedDeviceManager().getCachedDevicesCopy();
            for (CachedBluetoothDevice cachedBluetoothDevice : cachedDevices) {
                update(cachedBluetoothDevice);
            }
        } else {
            removeAllDevicesFromPreference();
        }
    }

    public void removeAllDevicesFromPreference() {
        if (mLocalManager == null) {
            Log.e(getLogTag(),
                    "removeAllDevicesFromPreference() BT is not supported on this device");
            return;
        }
        final Collection<CachedBluetoothDevice> cachedDevices =
                mLocalManager.getCachedDeviceManager().getCachedDevicesCopy();
        for (CachedBluetoothDevice cachedBluetoothDevice : cachedDevices) {
            removePreference(cachedBluetoothDevice);
        }
    }

    @Override
    public void onBluetoothStateChanged(int bluetoothState) {
        if (BluetoothAdapter.STATE_ON == bluetoothState) {
            forceUpdate();
        } else if (BluetoothAdapter.STATE_OFF == bluetoothState) {
            removeAllDevicesFromPreference();
        }
    }

    @Override
    public void onDeviceAdded(CachedBluetoothDevice cachedDevice) {
        update(cachedDevice);
    }

    @Override
    public void onDeviceDeleted(CachedBluetoothDevice cachedDevice) {
        // Used to combine the hearing aid entries just after pairing. Once both the hearing aids
        // get connected and their hiSyncId gets populated, this gets called for one of the
        // 2 hearing aids so that only one entry in the connected devices list will be seen.
        removePreference(cachedDevice);
    }

    @Override
    public void onDeviceBondStateChanged(CachedBluetoothDevice cachedDevice, int bondState) {
        update(cachedDevice);
    }

    @Override
    public void onProfileConnectionStateChanged(CachedBluetoothDevice cachedDevice, int state,
            int bluetoothProfile) {
        if (DBG) {
            Log.d(getLogTag(), "onProfileConnectionStateChanged() device: " + cachedDevice.getName()
                    + ", state: " + state + ", bluetoothProfile: " + bluetoothProfile);
        }
        update(cachedDevice);
    }

    @Override
    public void onAclConnectionStateChanged(CachedBluetoothDevice cachedDevice, int state) {
        Log.d(getLogTag(), "onAclConnectionStateChanged() device: " + cachedDevice.getName()
                + ", state: " + state);
        update(cachedDevice);
    }

    @Override
    public void onServiceConnected() {
        // When bluetooth service connected update the UI
        forceUpdate();
    }

    @Override
    public void onServiceDisconnected() {

    }

    /**
     * Set the context to generate the {@link Preference}, so it could get the correct theme.
     */
    public void setPrefContext(Context context) {
        mPrefContext = context;
    }

    /**
     * Return {@code true} if {@code cachedBluetoothDevice} matches this
     * {@link BluetoothDeviceUpdater} and should stay in the list, otherwise return {@code false}
     */
    public abstract boolean isFilterMatched(CachedBluetoothDevice cachedBluetoothDevice);

    /**
     * Return a preference key for logging
     */
    protected abstract String getPreferenceKey();

    /**
     * Update whether to show {@link CachedBluetoothDevice} in the list.
     */
    protected void update(CachedBluetoothDevice cachedBluetoothDevice) {
        if (isFilterMatched(cachedBluetoothDevice)) {
            // Add the preference if it is new one
            addPreference(cachedBluetoothDevice);
        } else {
            removePreference(cachedBluetoothDevice);
        }
    }

    /**
     * Add the {@link Preference} that represents the {@code cachedDevice}
     */
    protected void addPreference(CachedBluetoothDevice cachedDevice) {
        addPreference(cachedDevice, BluetoothDevicePreference.SortType.TYPE_DEFAULT);
    }

    /**
     * Add the {@link Preference} with {@link BluetoothDevicePreference.SortType} that
     * represents the {@code cachedDevice}
     */
    protected void addPreference(CachedBluetoothDevice cachedDevice,
            @BluetoothDevicePreference.SortType int type) {
        final BluetoothDevice device = cachedDevice.getDevice();
        if (!mPreferenceMap.containsKey(device)) {
            BluetoothDevicePreference btPreference =
                    new BluetoothDevicePreference(mPrefContext, cachedDevice,
                            true /* showDeviceWithoutNames */,
                            type);
            btPreference.setKey(getPreferenceKey());
            btPreference.setOnGearClickListener(mDeviceProfilesListener);
            if (this instanceof Preference.OnPreferenceClickListener) {
                btPreference.setOnPreferenceClickListener(
                        (Preference.OnPreferenceClickListener) this);
            }
            mPreferenceMap.put(device, btPreference);
            mDevicePreferenceCallback.onDeviceAdded(btPreference);
        }
    }

    /**
     * Remove the {@link Preference} that represents the {@code cachedDevice}
     */
    protected void removePreference(CachedBluetoothDevice cachedDevice) {
        final BluetoothDevice device = cachedDevice.getDevice();
        final CachedBluetoothDevice subCachedDevice = cachedDevice.getSubDevice();
        if (mPreferenceMap.containsKey(device)) {
            removePreference(device);
        } else if (subCachedDevice != null) {
            // When doing remove, to check if preference maps to sub device.
            // This would happen when connection state is changed in detail page that there is no
            // callback from SettingsLib.
            final BluetoothDevice subDevice = subCachedDevice.getDevice();
            removePreference(subDevice);
        }
    }

    private void removePreference(BluetoothDevice device) {
        if (mPreferenceMap.containsKey(device)) {
            mDevicePreferenceCallback.onDeviceRemoved(mPreferenceMap.get(device));
            mPreferenceMap.remove(device);
        }
    }

    /**
     * Get {@link CachedBluetoothDevice} from {@link Preference} and it is used to init
     * {@link SubSettingLauncher} to launch {@link BluetoothDeviceDetailsFragment}
     */
    protected void launchDeviceDetails(Preference preference) {
        mMetricsFeatureProvider.logClickedPreference(preference, mMetricsCategory);
        final CachedBluetoothDevice device =
                ((BluetoothDevicePreference) preference).getBluetoothDevice();
        if (device == null) {
            return;
        }
        final Bundle args = new Bundle();
        args.putString(BluetoothDeviceDetailsFragment.KEY_DEVICE_ADDRESS,
                device.getDevice().getAddress());

        new SubSettingLauncher(mContext)
                .setDestination(BluetoothDeviceDetailsFragment.class.getName())
                .setArguments(args)
                .setTitleRes(R.string.device_details_title)
                .setSourceMetricsCategory(mMetricsCategory)
                .launch();
    }

    /**
     * @return {@code true} if {@code cachedBluetoothDevice} is connected
     * and the bond state is bonded.
     */
    public boolean isDeviceConnected(CachedBluetoothDevice cachedDevice) {
        if (cachedDevice == null) {
            return false;
        }
        final BluetoothDevice device = cachedDevice.getDevice();
        if (DBG) {
            Log.d(getLogTag(), "isDeviceConnected() device name : " + cachedDevice.getName()
                    + ", is connected : " + device.isConnected() + " , is profile connected : "
                    + cachedDevice.isConnected());
        }
        return device.getBondState() == BluetoothDevice.BOND_BONDED && device.isConnected();
    }

    /**
     * Update the attributes of {@link Preference}.
     */
    public void refreshPreference() {
        List<BluetoothDevice> removeList = new ArrayList<>();
        mPreferenceMap.forEach((key, preference) -> {
            if (isDeviceOfMapInCachedDevicesList(key)) {
                ((BluetoothDevicePreference) preference).onPreferenceAttributesChanged();
            } else {
                // If the BluetoothDevice of preference is not in the CachedDevices List, then
                // remove this preference.
                removeList.add(key);
            }
        });

        for (BluetoothDevice bluetoothDevice : removeList) {
            Log.d(getLogTag(), "removePreference key: " + bluetoothDevice.getAnonymizedAddress());
            removePreference(bluetoothDevice);
        }
    }

    protected boolean isDeviceInCachedDevicesList(CachedBluetoothDevice cachedDevice) {
        return mLocalManager.getCachedDeviceManager().getCachedDevicesCopy().contains(cachedDevice);
    }

    private boolean isDeviceOfMapInCachedDevicesList(BluetoothDevice inputBluetoothDevice) {
        Collection<CachedBluetoothDevice> cachedDevices =
                mLocalManager.getCachedDeviceManager().getCachedDevicesCopy();
        if (cachedDevices == null || cachedDevices.isEmpty()) {
            return false;
        }
        return cachedDevices.stream()
                .anyMatch(cachedBluetoothDevice -> cachedBluetoothDevice.getDevice() != null
                        && cachedBluetoothDevice.getDevice().equals(inputBluetoothDevice));
    }

    protected String getLogTag() {
        return TAG;
    }
}
