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
package com.android.settings.connecteddevice;

import static com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.isExternalDisplaySettingsPageEnabled;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.input.InputManager;
import android.util.FeatureFlagUtils;
import android.util.Log;
import android.view.InputDevice;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.bluetooth.BluetoothDeviceUpdater;
import com.android.settings.bluetooth.ConnectedBluetoothDeviceUpdater;
import com.android.settings.bluetooth.Utils;
import com.android.settings.connecteddevice.display.ExternalDisplayUpdater;
import com.android.settings.connecteddevice.dock.DockUpdater;
import com.android.settings.connecteddevice.stylus.StylusDeviceUpdater;
import com.android.settings.connecteddevice.usb.ConnectedUsbDeviceUpdater;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.flags.FeatureFlags;
import com.android.settings.flags.FeatureFlagsImpl;
import com.android.settings.flags.Flags;
import com.android.settings.overlay.DockUpdaterFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.bluetooth.BluetoothDeviceFilter;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.search.SearchIndexableRaw;

import java.util.List;

/**
 * Controller to maintain the {@link androidx.preference.PreferenceGroup} for all
 * connected devices. It uses {@link DevicePreferenceCallback} to add/remove {@link Preference}
 */
public class ConnectedDeviceGroupController extends BasePreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnStart, OnStop,
        DevicePreferenceCallback {

    private static final String KEY = "connected_device_list";
    private static final String TAG = "ConnectedDeviceGroupController";

    @VisibleForTesting
    PreferenceGroup mPreferenceGroup;
    @Nullable
    private ExternalDisplayUpdater mExternalDisplayUpdater;
    private BluetoothDeviceUpdater mBluetoothDeviceUpdater;
    private ConnectedUsbDeviceUpdater mConnectedUsbDeviceUpdater;
    private DockUpdater mConnectedDockUpdater;
    private StylusDeviceUpdater mStylusDeviceUpdater;
    private final PackageManager mPackageManager;
    private final InputManager mInputManager;
    private final LocalBluetoothManager mLocalBluetoothManager;
    @NonNull
    private final FeatureFlags mFeatureFlags = new FeatureFlagsImpl();

    public ConnectedDeviceGroupController(Context context) {
        super(context, KEY);
        mPackageManager = context.getPackageManager();
        mInputManager = context.getSystemService(InputManager.class);
        mLocalBluetoothManager = Utils.getLocalBluetoothManager(context);
    }

    @Override
    public void onStart() {
        if (mExternalDisplayUpdater != null) {
            mExternalDisplayUpdater.registerCallback();
        }

        if (mBluetoothDeviceUpdater != null) {
            mBluetoothDeviceUpdater.registerCallback();
            mBluetoothDeviceUpdater.refreshPreference();
        }

        if (mConnectedUsbDeviceUpdater != null) {
            mConnectedUsbDeviceUpdater.registerCallback();
        }

        if (mConnectedDockUpdater != null) {
            mConnectedDockUpdater.registerCallback();
        }

        if (mStylusDeviceUpdater != null) {
            mStylusDeviceUpdater.registerCallback();
        }
    }

    @Override
    public void onStop() {
        if (mExternalDisplayUpdater != null) {
            mExternalDisplayUpdater.unregisterCallback();
        }

        if (mBluetoothDeviceUpdater != null) {
            mBluetoothDeviceUpdater.unregisterCallback();
        }

        if (mConnectedUsbDeviceUpdater != null) {
            mConnectedUsbDeviceUpdater.unregisterCallback();
        }

        if (mConnectedDockUpdater != null) {
            mConnectedDockUpdater.unregisterCallback();
        }

        if (mStylusDeviceUpdater != null) {
            mStylusDeviceUpdater.unregisterCallback();
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreferenceGroup = screen.findPreference(KEY);
        mPreferenceGroup.setVisible(false);

        if (isAvailable()) {
            final Context context = screen.getContext();
            if (mExternalDisplayUpdater != null) {
                mExternalDisplayUpdater.initPreference(context);
            }

            if (mBluetoothDeviceUpdater != null) {
                mBluetoothDeviceUpdater.setPrefContext(context);
                mBluetoothDeviceUpdater.forceUpdate();
            }

            if (mConnectedUsbDeviceUpdater != null) {
                mConnectedUsbDeviceUpdater.initUsbPreference(context);
            }

            if (mConnectedDockUpdater != null) {
                mConnectedDockUpdater.setPreferenceContext(context);
                mConnectedDockUpdater.forceUpdate();
            }

            if (mStylusDeviceUpdater != null) {
                mStylusDeviceUpdater.setPreferenceContext(context);
                mStylusDeviceUpdater.forceUpdate();
            }
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return (hasExternalDisplayFeature()
                || hasBluetoothFeature()
                || hasUsbFeature()
                || hasUsiStylusFeature()
                || mConnectedDockUpdater != null)
                ? AVAILABLE_UNSEARCHABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void onDeviceAdded(Preference preference) {
        if (mPreferenceGroup.getPreferenceCount() == 0) {
            mPreferenceGroup.setVisible(true);
        }
        mPreferenceGroup.addPreference(preference);
    }

    @Override
    public void onDeviceRemoved(Preference preference) {
        mPreferenceGroup.removePreference(preference);
        if (mPreferenceGroup.getPreferenceCount() == 0) {
            mPreferenceGroup.setVisible(false);
        }
    }

    @VisibleForTesting
    void init(@Nullable ExternalDisplayUpdater externalDisplayUpdater,
            BluetoothDeviceUpdater bluetoothDeviceUpdater,
            ConnectedUsbDeviceUpdater connectedUsbDeviceUpdater,
            DockUpdater connectedDockUpdater,
            StylusDeviceUpdater connectedStylusDeviceUpdater) {

        mExternalDisplayUpdater = externalDisplayUpdater;
        mBluetoothDeviceUpdater = bluetoothDeviceUpdater;
        mConnectedUsbDeviceUpdater = connectedUsbDeviceUpdater;
        mConnectedDockUpdater = connectedDockUpdater;
        mStylusDeviceUpdater = connectedStylusDeviceUpdater;
    }

    public void init(DashboardFragment fragment) {
        final Context context = fragment.getContext();
        DockUpdaterFeatureProvider dockUpdaterFeatureProvider =
                FeatureFactory.getFeatureFactory().getDockUpdaterFeatureProvider();
        final DockUpdater connectedDockUpdater =
                dockUpdaterFeatureProvider.getConnectedDockUpdater(context, this);
        init(hasExternalDisplayFeature()
                        ? new ExternalDisplayUpdater(this, fragment.getMetricsCategory())
                        : null,
                hasBluetoothFeature()
                        ? new ConnectedBluetoothDeviceUpdater(context, this,
                        fragment.getMetricsCategory())
                        : null,
                hasUsbFeature()
                        ? new ConnectedUsbDeviceUpdater(context, fragment, this)
                        : null,
                connectedDockUpdater,
                hasUsiStylusFeature()
                        ? new StylusDeviceUpdater(context, fragment, this)
                        : null);
    }

    /**
     * @return trunk stable feature flags.
     */
    @VisibleForTesting
    @NonNull
    public FeatureFlags getFeatureFlags() {
        return mFeatureFlags;
    }

    private boolean hasExternalDisplayFeature() {
        return isExternalDisplaySettingsPageEnabled(getFeatureFlags());
    }

    private boolean hasBluetoothFeature() {
        return mPackageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
    }

    private boolean hasUsbFeature() {
        return mPackageManager.hasSystemFeature(PackageManager.FEATURE_USB_ACCESSORY)
                || mPackageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST);
    }

    private boolean hasUsiStylusFeature() {
        if (!FeatureFlagUtils.isEnabled(mContext,
                FeatureFlagUtils.SETTINGS_SHOW_STYLUS_PREFERENCES)) {
            return false;
        }

        for (int deviceId : mInputManager.getInputDeviceIds()) {
            InputDevice device = mInputManager.getInputDevice(deviceId);
            if (device != null
                    && device.supportsSource(InputDevice.SOURCE_STYLUS)
                    && !device.isExternal()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void updateDynamicRawDataToIndex(List<SearchIndexableRaw> rawData) {
        if (!Flags.enableBondedBluetoothDeviceSearchable()) {
            return;
        }
        if (mLocalBluetoothManager == null) {
            Log.d(TAG, "Bluetooth is not supported");
            return;
        }
        for (CachedBluetoothDevice cachedDevice :
                mLocalBluetoothManager.getCachedDeviceManager().getCachedDevicesCopy()) {
            if (!BluetoothDeviceFilter.BONDED_DEVICE_FILTER.matches(cachedDevice.getDevice())) {
                continue;
            }
            if (BluetoothUtils.isExclusivelyManagedBluetoothDevice(mContext,
                    cachedDevice.getDevice())) {
                continue;
            }
            SearchIndexableRaw data = new SearchIndexableRaw(mContext);
            // Include the identity address as well to ensure the key is unique.
            data.key = cachedDevice.getName() + cachedDevice.getIdentityAddress();
            data.title = cachedDevice.getName();
            data.summaryOn = mContext.getString(R.string.connected_devices_dashboard_title);
            rawData.add(data);
        }
    }
}
