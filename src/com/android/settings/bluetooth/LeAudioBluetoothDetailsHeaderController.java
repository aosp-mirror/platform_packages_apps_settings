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

package com.android.settings.bluetooth;

import android.bluetooth.BluetoothLeAudio;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.fuelgauge.BatteryMeterView;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LeAudioProfile;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnDestroy;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.widget.LayoutPreference;

import java.util.List;

/**
 * This class adds a header with device name and status (connected/disconnected, etc.).
 */
public class LeAudioBluetoothDetailsHeaderController extends BasePreferenceController implements
        LifecycleObserver, OnStart, OnStop, OnDestroy, CachedBluetoothDevice.Callback {
    private static final String TAG = "LeAudioBtHeaderCtrl";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    @VisibleForTesting
    static final int LEFT_DEVICE_ID =
            BluetoothLeAudio.AUDIO_LOCATION_FRONT_LEFT
                    | BluetoothLeAudio.AUDIO_LOCATION_BACK_LEFT
                    | BluetoothLeAudio.AUDIO_LOCATION_FRONT_LEFT_OF_CENTER
                    | BluetoothLeAudio.AUDIO_LOCATION_SIDE_LEFT
                    | BluetoothLeAudio.AUDIO_LOCATION_TOP_FRONT_LEFT
                    | BluetoothLeAudio.AUDIO_LOCATION_TOP_BACK_LEFT
                    | BluetoothLeAudio.AUDIO_LOCATION_TOP_SIDE_LEFT
                    | BluetoothLeAudio.AUDIO_LOCATION_BOTTOM_FRONT_LEFT
                    | BluetoothLeAudio.AUDIO_LOCATION_FRONT_LEFT_WIDE
                    | BluetoothLeAudio.AUDIO_LOCATION_LEFT_SURROUND;

    @VisibleForTesting
    static final int RIGHT_DEVICE_ID =
            BluetoothLeAudio.AUDIO_LOCATION_FRONT_RIGHT
                    | BluetoothLeAudio.AUDIO_LOCATION_BACK_RIGHT
                    | BluetoothLeAudio.AUDIO_LOCATION_FRONT_RIGHT_OF_CENTER
                    | BluetoothLeAudio.AUDIO_LOCATION_SIDE_RIGHT
                    | BluetoothLeAudio.AUDIO_LOCATION_TOP_FRONT_RIGHT
                    | BluetoothLeAudio.AUDIO_LOCATION_TOP_BACK_RIGHT
                    | BluetoothLeAudio.AUDIO_LOCATION_TOP_SIDE_RIGHT
                    | BluetoothLeAudio.AUDIO_LOCATION_BOTTOM_FRONT_RIGHT
                    | BluetoothLeAudio.AUDIO_LOCATION_FRONT_RIGHT_WIDE
                    | BluetoothLeAudio.AUDIO_LOCATION_RIGHT_SURROUND;

    @VisibleForTesting
    static final int INVALID_RESOURCE_ID = -1;

    @VisibleForTesting
    LayoutPreference mLayoutPreference;
    LocalBluetoothManager mManager;
    private CachedBluetoothDevice mCachedDevice;
    private List<CachedBluetoothDevice> mAllOfCachedDevices;
    @VisibleForTesting
    Handler mHandler = new Handler(Looper.getMainLooper());
    @VisibleForTesting
    boolean mIsRegisterCallback = false;

    private LocalBluetoothProfileManager mProfileManager;

    public LeAudioBluetoothDetailsHeaderController(Context context, String prefKey) {
        super(context, prefKey);
    }

    @Override
    public int getAvailabilityStatus() {
        if (mCachedDevice == null || mProfileManager == null) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        boolean hasLeAudio = mCachedDevice.getConnectableProfiles()
                .stream()
                .anyMatch(profile -> profile.getProfileId() == BluetoothProfile.LE_AUDIO);

        return !BluetoothUtils.isAdvancedDetailsHeader(mCachedDevice.getDevice()) && hasLeAudio
                ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mLayoutPreference = screen.findPreference(getPreferenceKey());
        mLayoutPreference.setVisible(isAvailable());
    }

    @Override
    public void onStart() {
        if (!isAvailable()) {
            return;
        }
        mIsRegisterCallback = true;
        for (CachedBluetoothDevice item : mAllOfCachedDevices) {
            item.registerCallback(this);
        }
        refresh();
    }

    @Override
    public void onStop() {
        if (!mIsRegisterCallback) {
            return;
        }
        for (CachedBluetoothDevice item : mAllOfCachedDevices) {
            item.unregisterCallback(this);
        }

        mIsRegisterCallback = false;
    }

    @Override
    public void onDestroy() {
    }

    public void init(CachedBluetoothDevice cachedBluetoothDevice,
            LocalBluetoothManager bluetoothManager) {
        mCachedDevice = cachedBluetoothDevice;
        mManager = bluetoothManager;
        mProfileManager = bluetoothManager.getProfileManager();
        mAllOfCachedDevices = Utils.getAllOfCachedBluetoothDevices(mManager, mCachedDevice);
    }

    @VisibleForTesting
    void refresh() {
        if (mLayoutPreference == null || mCachedDevice == null) {
            return;
        }
        final ImageView imageView = mLayoutPreference.findViewById(R.id.entity_header_icon);
        if (imageView != null) {
            final Pair<Drawable, String> pair =
                    BluetoothUtils.getBtRainbowDrawableWithDescription(mContext, mCachedDevice);
            imageView.setImageDrawable(pair.first);
            imageView.setContentDescription(pair.second);
        }

        final TextView title = mLayoutPreference.findViewById(R.id.entity_header_title);
        if (title != null) {
            title.setText(mCachedDevice.getName());
        }
        final TextView summary = mLayoutPreference.findViewById(R.id.entity_header_summary);
        if (summary != null) {
            summary.setText(mCachedDevice.getConnectionSummary(true /* shortSummary */));
        }

        if (!mCachedDevice.isConnected() || mCachedDevice.isBusy()) {
            hideAllOfBatteryLayouts();
            return;
        }

        updateBatteryLayout();
    }

    @VisibleForTesting
    Drawable createBtBatteryIcon(Context context, int level) {
        final BatteryMeterView.BatteryMeterDrawable drawable =
                new BatteryMeterView.BatteryMeterDrawable(context,
                        context.getColor(com.android.settingslib.R.color.meter_background_color),
                        context.getResources().getDimensionPixelSize(
                                R.dimen.advanced_bluetooth_battery_meter_width),
                        context.getResources().getDimensionPixelSize(
                                R.dimen.advanced_bluetooth_battery_meter_height));
        drawable.setBatteryLevel(level);
        drawable.setColorFilter(new PorterDuffColorFilter(
                com.android.settings.Utils.getColorAttrDefaultColor(context,
                        android.R.attr.colorControlNormal),
                PorterDuff.Mode.SRC));
        return drawable;
    }

    private int getBatterySummaryResource(int containerId) {
        if (containerId == R.id.bt_battery_case) {
            return R.id.bt_battery_case_summary;
        } else if (containerId == R.id.bt_battery_left) {
            return R.id.bt_battery_left_summary;
        } else if (containerId == R.id.bt_battery_right) {
            return R.id.bt_battery_right_summary;
        }
        Log.d(TAG, "No summary resource id. The containerId is " + containerId);
        return INVALID_RESOURCE_ID;
    }

    private void hideAllOfBatteryLayouts() {
        // hide the case
        updateBatteryLayout(R.id.bt_battery_case, BluetoothUtils.META_INT_ERROR);
        // hide the left
        updateBatteryLayout(R.id.bt_battery_left, BluetoothUtils.META_INT_ERROR);
        // hide the right
        updateBatteryLayout(R.id.bt_battery_right, BluetoothUtils.META_INT_ERROR);
    }

    private void updateBatteryLayout() {
        // Init the battery layouts.
        hideAllOfBatteryLayouts();
        LeAudioProfile leAudioProfile = mProfileManager.getLeAudioProfile();
        if (mAllOfCachedDevices.isEmpty()) {
            Log.e(TAG, "There is no LeAudioProfile.");
            return;
        }

        if (!leAudioProfile.isEnabled(mCachedDevice.getDevice())) {
            Log.d(TAG, "Show the legacy battery style if the LeAudio is not enabled.");
            final TextView summary = mLayoutPreference.findViewById(R.id.entity_header_summary);
            if (summary != null) {
                summary.setText(mCachedDevice.getConnectionSummary());
            }
            return;
        }

        for (CachedBluetoothDevice cachedDevice : mAllOfCachedDevices) {
            int deviceId = leAudioProfile.getAudioLocation(cachedDevice.getDevice());
            Log.d(TAG, "LeAudioDevices:" + cachedDevice.getDevice().getAnonymizedAddress()
                    + ", deviceId:" + deviceId);

            if (deviceId == BluetoothLeAudio.AUDIO_LOCATION_INVALID) {
                Log.d(TAG, "The device does not support the AUDIO_LOCATION.");
                return;
            }
            boolean isLeft = (deviceId & LEFT_DEVICE_ID) != 0;
            boolean isRight = (deviceId & RIGHT_DEVICE_ID) != 0;
            boolean isLeftRight = isLeft && isRight;
            // The LE device updates the BatteryLayout
            if (isLeftRight) {
                Log.d(TAG, "Show the legacy battery style if the device id is left+right.");
                final TextView summary = mLayoutPreference.findViewById(R.id.entity_header_summary);
                if (summary != null) {
                    summary.setText(mCachedDevice.getConnectionSummary());
                }
            } else if (isLeft) {
                updateBatteryLayout(R.id.bt_battery_left, cachedDevice.getBatteryLevel());
            } else if (isRight) {
                updateBatteryLayout(R.id.bt_battery_right, cachedDevice.getBatteryLevel());
            } else {
                Log.d(TAG, "The device id is other Audio Location. Do nothing.");
            }
        }
    }

    private void updateBatteryLayout(int resId, int batteryLevel) {
        final View batteryView = mLayoutPreference.findViewById(resId);
        if (batteryView == null) {
            Log.e(TAG, "updateBatteryLayout: No View");
            return;
        }
        if (batteryLevel != BluetoothUtils.META_INT_ERROR) {
            batteryView.setVisibility(View.VISIBLE);
            final TextView batterySummaryView =
                    batteryView.requireViewById(getBatterySummaryResource(resId));
            final String batteryLevelPercentageString =
                    com.android.settings.Utils.formatPercentage(batteryLevel);
            batterySummaryView.setText(batteryLevelPercentageString);
            batterySummaryView.setContentDescription(mContext.getString(
                    com.android.settingslib.R.string.bluetooth_battery_level,
                    batteryLevelPercentageString));
            batterySummaryView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    createBtBatteryIcon(mContext, batteryLevel), /* top */ null,
                    /* end */ null, /* bottom */ null);
        } else {
            Log.d(TAG, "updateBatteryLayout: Hide it if it doesn't have battery information.");
            batteryView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDeviceAttributesChanged() {
        for (CachedBluetoothDevice item : mAllOfCachedDevices) {
            item.unregisterCallback(this);
        }
        mAllOfCachedDevices = Utils.getAllOfCachedBluetoothDevices(mManager, mCachedDevice);
        for (CachedBluetoothDevice item : mAllOfCachedDevices) {
            item.registerCallback(this);
        }

        if (!mAllOfCachedDevices.isEmpty()) {
            refresh();
        }
    }
}
