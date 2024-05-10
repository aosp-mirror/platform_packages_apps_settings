/*
 * Copyright (C) 2022 The Android Open Source Project
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
import android.hardware.BatteryState;
import android.hardware.input.InputManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.InputDevice;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller to maintain available USI stylus devices. Listens to bluetooth
 * stylus connection to determine whether to show the USI preference.
 */
public class StylusDeviceUpdater implements InputManager.InputDeviceListener,
        InputManager.InputDeviceBatteryListener {

    private static final String TAG = "StylusDeviceUpdater";
    private static final String PREF_KEY = "stylus_usi_device";
    private static final String INPUT_ID_ARG = "device_input_id";

    private final DevicePreferenceCallback mDevicePreferenceCallback;
    private final List<Integer> mRegisteredBatteryCallbackIds;
    private final DashboardFragment mFragment;
    private final InputManager mInputManager;
    private final MetricsFeatureProvider mMetricsFeatureProvider;

    private Context mContext;

    @VisibleForTesting
    Integer mLastDetectedUsiId;
    BatteryState mLastBatteryState;

    @VisibleForTesting
    Preference mUsiPreference;


    public StylusDeviceUpdater(Context context, DashboardFragment fragment,
            DevicePreferenceCallback devicePreferenceCallback) {
        mFragment = fragment;
        mRegisteredBatteryCallbackIds = new ArrayList<>();
        mDevicePreferenceCallback = devicePreferenceCallback;
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
        mContext = context;
        mInputManager = context.getSystemService(InputManager.class);
    }

    /**
     * Register the stylus event callback and update the list
     */
    public void registerCallback() {
        for (int deviceId : mInputManager.getInputDeviceIds()) {
            onInputDeviceAdded(deviceId);
        }
        mInputManager.registerInputDeviceListener(this, new Handler(Looper.myLooper()));
        forceUpdate();
    }

    /**
     * Unregister the stylus event callback
     */
    public void unregisterCallback() {
        for (int deviceId : mRegisteredBatteryCallbackIds) {
            mInputManager.removeInputDeviceBatteryListener(deviceId, this);
        }
        mInputManager.unregisterInputDeviceListener(this);
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
        InputDevice inputDevice = mInputManager.getInputDevice(deviceId);
        if (inputDevice == null) return;

        if (inputDevice.supportsSource(InputDevice.SOURCE_STYLUS)
                && !inputDevice.isExternal()) {
            try {
                mInputManager.addInputDeviceBatteryListener(deviceId,
                        mContext.getMainExecutor(), this);
                mRegisteredBatteryCallbackIds.add(deviceId);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, e.getMessage());
            }
        }
        forceUpdate();
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        Log.d(TAG, String.format("Input device removed %d", deviceId));
        forceUpdate();
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {
        InputDevice inputDevice = mInputManager.getInputDevice(deviceId);
        if (inputDevice == null) return;

        if (inputDevice.supportsSource(InputDevice.SOURCE_STYLUS)) {
            forceUpdate();
        }
    }


    @Override
    public void onBatteryStateChanged(int deviceId, long eventTimeMillis,
            @NonNull BatteryState batteryState) {
        mLastBatteryState = batteryState;
        mLastDetectedUsiId = deviceId;
        forceUpdate();
    }

    /**
     * Set the context to generate the {@link Preference}, so it could get the correct theme.
     */
    public void setPreferenceContext(Context context) {
        mContext = context;
    }

    /**
     * Force update to add or remove stylus preference
     */
    public void forceUpdate() {
        if (shouldShowUsiPreference()) {
            addOrUpdateUsiPreference();
        } else {
            removeUsiPreference();
        }
    }

    private synchronized void addOrUpdateUsiPreference() {
        if (mUsiPreference == null) {
            mUsiPreference = new Preference(mContext);
            mDevicePreferenceCallback.onDeviceAdded(mUsiPreference);
        }
        mUsiPreference.setKey(PREF_KEY);
        mUsiPreference.setTitle(R.string.stylus_connected_devices_title);
        mUsiPreference.setIcon(R.drawable.ic_stylus);
        mUsiPreference.setOnPreferenceClickListener((Preference p) -> {
            mMetricsFeatureProvider.logClickedPreference(p, mFragment.getMetricsCategory());
            launchDeviceDetails();
            return true;
        });
    }

    private synchronized void removeUsiPreference() {
        if (mUsiPreference != null) {
            mDevicePreferenceCallback.onDeviceRemoved(mUsiPreference);
            mUsiPreference = null;
        }
    }

    private boolean shouldShowUsiPreference() {
        return isUsiBatteryValid() && !hasConnectedBluetoothStylusDevice();
    }

    @VisibleForTesting
    public Preference getPreference() {
        return mUsiPreference;
    }

    @VisibleForTesting
    boolean hasConnectedBluetoothStylusDevice() {
        for (int deviceId : mInputManager.getInputDeviceIds()) {
            InputDevice device = mInputManager.getInputDevice(deviceId);
            if (device == null) continue;

            if (device.supportsSource(InputDevice.SOURCE_STYLUS)
                    && mInputManager.getInputDeviceBluetoothAddress(deviceId) != null) {
                return true;
            }
        }

        return false;
    }

    @VisibleForTesting
    boolean isUsiBatteryValid() {
        return mLastBatteryState != null
                && mLastBatteryState.isPresent() && mLastBatteryState.getCapacity() > 0f;
    }

    private void launchDeviceDetails() {
        final Bundle args = new Bundle();
        args.putInt(INPUT_ID_ARG, mLastDetectedUsiId);

        new SubSettingLauncher(mFragment.getContext())
                .setDestination(StylusUsiDetailsFragment.class.getName())
                .setArguments(args)
                .setSourceMetricsCategory(mFragment.getMetricsCategory()).launch();
    }
}
