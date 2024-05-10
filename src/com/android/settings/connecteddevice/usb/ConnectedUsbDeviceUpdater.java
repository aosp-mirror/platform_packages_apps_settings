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
package com.android.settings.connecteddevice.usb;

import static android.hardware.usb.UsbPortStatus.DATA_ROLE_DEVICE;
import static android.hardware.usb.UsbPortStatus.POWER_ROLE_SINK;
import static android.hardware.usb.UsbPortStatus.POWER_ROLE_SOURCE;

import static com.android.settingslib.RestrictedLockUtilsInternal.checkIfUsbDataSignalingIsDisabled;

import android.content.Context;
import android.hardware.usb.UsbManager;
import android.os.UserHandle;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

/**
 * Controller to maintain connected usb device
 */
public class ConnectedUsbDeviceUpdater {

    private static final String PREF_KEY = "connected_usb";

    private final MetricsFeatureProvider mMetricsFeatureProvider;

    private DashboardFragment mFragment;
    private UsbBackend mUsbBackend;
    private DevicePreferenceCallback mDevicePreferenceCallback;
    @VisibleForTesting
    RestrictedPreference mUsbPreference;
    @VisibleForTesting
    UsbConnectionBroadcastReceiver mUsbReceiver;

    @VisibleForTesting
    UsbConnectionBroadcastReceiver.UsbConnectionListener mUsbConnectionListener =
            (connected, functions, powerRole, dataRole, isUsbConfigured) -> {
                if (connected) {
                    mUsbPreference.setSummary(getSummary(dataRole == DATA_ROLE_DEVICE
                                    ? functions : UsbManager.FUNCTION_NONE, powerRole));
                    mDevicePreferenceCallback.onDeviceAdded(mUsbPreference);
                } else {
                    mDevicePreferenceCallback.onDeviceRemoved(mUsbPreference);
                }
            };

    public ConnectedUsbDeviceUpdater(Context context, DashboardFragment fragment,
            DevicePreferenceCallback devicePreferenceCallback) {
        this(context, fragment, devicePreferenceCallback, new UsbBackend(context));
    }

    @VisibleForTesting
    ConnectedUsbDeviceUpdater(Context context, DashboardFragment fragment,
            DevicePreferenceCallback devicePreferenceCallback, UsbBackend usbBackend) {
        mFragment = fragment;
        mDevicePreferenceCallback = devicePreferenceCallback;
        mUsbBackend = usbBackend;
        mUsbReceiver = new UsbConnectionBroadcastReceiver(context,
                mUsbConnectionListener, mUsbBackend);
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    public void registerCallback() {
        // This method could handle multiple register
        mUsbReceiver.register();
    }

    public void unregisterCallback() {
        mUsbReceiver.unregister();
    }

    public void initUsbPreference(Context context) {
        mUsbPreference = new RestrictedPreference(context, null /* AttributeSet */);
        mUsbPreference.setTitle(R.string.usb_pref);
        mUsbPreference.setIcon(R.drawable.ic_usb);
        mUsbPreference.setKey(PREF_KEY);
        mUsbPreference.setDisabledByAdmin(
                checkIfUsbDataSignalingIsDisabled(context, UserHandle.myUserId()));
        mUsbPreference.setOnPreferenceClickListener((Preference p) -> {
            mMetricsFeatureProvider.logClickedPreference(p, mFragment.getMetricsCategory());
            // New version - uses a separate screen.
            new SubSettingLauncher(mFragment.getContext())
                    .setDestination(UsbDetailsFragment.class.getName())
                    .setTitleRes(R.string.usb_preference)
                    .setSourceMetricsCategory(mFragment.getMetricsCategory())
                    .launch();
            return true;
        });

        forceUpdate();
    }

    private void forceUpdate() {
        // Register so we can get the connection state from sticky intent.
        //TODO(b/70336520): Use an API to get data instead of sticky intent
        mUsbReceiver.register();
    }

    public static int getSummary(long functions, int power) {
        switch (power) {
            case POWER_ROLE_SINK:
                if (functions == UsbManager.FUNCTION_MTP) {
                    return R.string.usb_summary_file_transfers;
                } else if (functions == UsbManager.FUNCTION_RNDIS) {
                    return R.string.usb_summary_tether;
                } else if (functions == UsbManager.FUNCTION_PTP) {
                    return R.string.usb_summary_photo_transfers;
                } else if (functions == UsbManager.FUNCTION_MIDI) {
                    return R.string.usb_summary_MIDI;
                } else if (functions == UsbManager.FUNCTION_UVC) {
                    return R.string.usb_summary_UVC;
                } else {
                    return R.string.usb_summary_charging_only;
                }
            case POWER_ROLE_SOURCE:
                if (functions == UsbManager.FUNCTION_MTP) {
                    return R.string.usb_summary_file_transfers_power;
                } else if (functions == UsbManager.FUNCTION_RNDIS) {
                    return R.string.usb_summary_tether_power;
                } else if (functions == UsbManager.FUNCTION_PTP) {
                    return R.string.usb_summary_photo_transfers_power;
                } else if (functions == UsbManager.FUNCTION_MIDI) {
                    return R.string.usb_summary_MIDI_power;
                } else if (functions == UsbManager.FUNCTION_UVC) {
                    return R.string.usb_summary_UVC_power;
                } else {
                    return R.string.usb_summary_power_only;
                }
            default:
                return R.string.usb_summary_charging_only;
        }
    }
}
