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

package com.android.settings.connecteddevice.stylus;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.ArrayList;
import java.util.List;

/** Preference controller for stylus firmware updates via USB **/
public class StylusUsbFirmwareController extends BasePreferenceController
        implements LifecycleObserver, OnStart, OnStop {
    private static final String TAG = StylusUsbFirmwareController.class.getSimpleName();
    @Nullable
    private UsbDevice mStylusUsbDevice;
    private final UsbStylusBroadcastReceiver mUsbStylusBroadcastReceiver;

    private PreferenceScreen mPreferenceScreen;
    private PreferenceCategory mPreference;

    @VisibleForTesting
    UsbStylusBroadcastReceiver.UsbStylusConnectionListener mUsbConnectionListener =
            (stylusUsbDevice, attached) -> {
                refresh();
            };

    public StylusUsbFirmwareController(Context context, String key) {
        super(context, key);
        mUsbStylusBroadcastReceiver = new UsbStylusBroadcastReceiver(context,
                mUsbConnectionListener);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mPreferenceScreen = screen;
        refresh();
        super.displayPreference(screen);
    }

    @Override
    public int getAvailabilityStatus() {
        // always available, preferences will be added or
        // removed according to the connected usb device
        return AVAILABLE;
    }

    private void refresh() {
        if (mPreferenceScreen == null) return;

        UsbDevice device = getStylusUsbDevice();
        if (device == mStylusUsbDevice) {
            return;
        }
        mStylusUsbDevice = device;
        mPreference = mPreferenceScreen.findPreference(getPreferenceKey());
        if (mPreference != null) {
            mPreferenceScreen.removePreference(mPreference);
        }
        if (hasUsbStylusFirmwareUpdateFeature(mStylusUsbDevice)) {
            StylusFeatureProvider featureProvider =
                    FeatureFactory.getFeatureFactory().getStylusFeatureProvider();
            List<Preference> preferences =
                    featureProvider.getUsbFirmwareUpdatePreferences(mContext, mStylusUsbDevice);

            if (preferences != null) {
                mPreference = new PreferenceCategory(mContext);
                mPreference.setKey(getPreferenceKey());
                mPreferenceScreen.addPreference(mPreference);

                for (Preference preference : preferences) {
                    mPreference.addPreference(preference);
                }
            }
        }
    }

    @Override
    public void onStart() {
        mUsbStylusBroadcastReceiver.register();
    }

    @Override
    public void onStop() {
        mUsbStylusBroadcastReceiver.unregister();
    }

    private UsbDevice getStylusUsbDevice() {
        UsbManager usbManager = mContext.getSystemService(UsbManager.class);

        if (usbManager == null) {
            return null;
        }

        List<UsbDevice> devices = new ArrayList<>(usbManager.getDeviceList().values());
        if (devices.isEmpty()) {
            return null;
        }

        UsbDevice usbDevice = devices.get(0);
        if (hasUsbStylusFirmwareUpdateFeature(usbDevice)) {
            return usbDevice;
        }
        return null;
    }

    static boolean hasUsbStylusFirmwareUpdateFeature(UsbDevice usbDevice) {
        if (usbDevice == null) return false;

        StylusFeatureProvider featureProvider =
                FeatureFactory.getFeatureFactory().getStylusFeatureProvider();

        return featureProvider.isUsbFirmwareUpdateEnabled(usbDevice);
    }
}
