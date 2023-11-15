/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHapClient;
import android.bluetooth.BluetoothHearingAid;
import android.bluetooth.BluetoothLeAudio;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.HearingAidInfo;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.Set;

/**
 * Controller that shows and updates the bluetooth device name
 */
public class AccessibilityHearingAidPreferenceController extends BasePreferenceController
        implements LifecycleObserver, OnStart, OnStop, BluetoothCallback,
        LocalBluetoothProfileManager.ServiceListener {
    private static final String TAG = "AccessibilityHearingAidPreferenceController";
    private Preference mHearingAidPreference;

    private final BroadcastReceiver mHearingAidChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateState(mHearingAidPreference);
        }
    };

    private final LocalBluetoothManager mLocalBluetoothManager;
    private final LocalBluetoothProfileManager mProfileManager;
    private final HearingAidHelper mHelper;
    private FragmentManager mFragmentManager;

    public AccessibilityHearingAidPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mLocalBluetoothManager = com.android.settings.bluetooth.Utils.getLocalBluetoothManager(
                context);
        mProfileManager = mLocalBluetoothManager.getProfileManager();
        mHelper = new HearingAidHelper(context);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mHearingAidPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public int getAvailabilityStatus() {
        return mHelper.isHearingAidSupported() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void onStart() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothHearingAid.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothHapClient.ACTION_HAP_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothLeAudio.ACTION_LE_AUDIO_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.registerReceiver(mHearingAidChangedReceiver, filter);
        mLocalBluetoothManager.getEventManager().registerCallback(this);
        // Can't get connected hearing aids when hearing aids related profiles are not ready. The
        // profiles will be ready after the services are connected. Needs to add listener and
        // updates the information when all hearing aids related services are connected.
        if (!mHelper.isAllHearingAidRelatedProfilesReady()) {
            mProfileManager.addServiceListener(this);
        }
    }

    @Override
    public void onStop() {
        mContext.unregisterReceiver(mHearingAidChangedReceiver);
        mLocalBluetoothManager.getEventManager().unregisterCallback(this);
        mProfileManager.removeServiceListener(this);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            launchHearingAidPage();
            return true;
        }
        return false;
    }

    @Override
    public CharSequence getSummary() {
        final CachedBluetoothDevice device = mHelper.getConnectedHearingAidDevice();
        if (device == null) {
            return mContext.getText(R.string.accessibility_hearingaid_not_connected_summary);
        }

        final int connectedNum = getConnectedHearingAidDeviceNum();
        final CharSequence name = device.getName();
        if (connectedNum > 1) {
            return mContext.getString(R.string.accessibility_hearingaid_more_device_summary, name);
        }

        // Check if another side of LE audio hearing aid is connected as a pair
        final Set<CachedBluetoothDevice> memberDevices = device.getMemberDevice();
        if (memberDevices.stream().anyMatch(m -> m.isConnected())) {
            return mContext.getString(
                    R.string.accessibility_hearingaid_left_and_right_side_device_summary,
                    name);
        }

        // Check if another side of ASHA hearing aid is connected as a pair
        final CachedBluetoothDevice subDevice = device.getSubDevice();
        if (subDevice != null && subDevice.isConnected()) {
            return mContext.getString(
                    R.string.accessibility_hearingaid_left_and_right_side_device_summary, name);
        }

        final int side = device.getDeviceSide();
        if (side == HearingAidInfo.DeviceSide.SIDE_LEFT_AND_RIGHT) {
            return mContext.getString(
                    R.string.accessibility_hearingaid_left_and_right_side_device_summary, name);
        } else if (side == HearingAidInfo.DeviceSide.SIDE_LEFT) {
            return mContext.getString(
                    R.string.accessibility_hearingaid_left_side_device_summary, name);
        } else if (side == HearingAidInfo.DeviceSide.SIDE_RIGHT) {
            return mContext.getString(
                    R.string.accessibility_hearingaid_right_side_device_summary, name);
        }

        // Invalid side
        return mContext.getString(
                R.string.accessibility_hearingaid_active_device_summary, name);
    }

    @Override
    public void onActiveDeviceChanged(CachedBluetoothDevice activeDevice, int bluetoothProfile) {
        if (activeDevice == null) {
            return;
        }

        if (bluetoothProfile == BluetoothProfile.HEARING_AID) {
            HearingAidUtils.launchHearingAidPairingDialog(
                    mFragmentManager, activeDevice, getMetricsCategory());
        }
    }

    @Override
    public void onServiceConnected() {
        if (mHelper.isAllHearingAidRelatedProfilesReady()) {
            updateState(mHearingAidPreference);
            mProfileManager.removeServiceListener(this);
        }
    }

    @Override
    public void onServiceDisconnected() {
        // Do nothing
    }

    public void setFragmentManager(FragmentManager fragmentManager) {
        mFragmentManager = fragmentManager;
    }

    private int getConnectedHearingAidDeviceNum() {
        return mHelper.getConnectedHearingAidDeviceList().size();
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    void setPreference(Preference preference) {
        mHearingAidPreference = preference;
    }

    private void launchHearingAidPage() {
        new SubSettingLauncher(mContext)
                .setDestination(AccessibilityHearingAidsFragment.class.getName())
                .setSourceMetricsCategory(getMetricsCategory())
                .launch();
    }
}