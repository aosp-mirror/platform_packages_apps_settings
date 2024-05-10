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

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.applications.SpacePreference;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.HearingAidInfo;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.ButtonPreference;

import com.google.common.annotations.VisibleForTesting;

import java.util.Set;

/**
 * This class handles button preference logic to display for hearing aid device.
 */
public class BluetoothDetailsPairOtherController extends BluetoothDetailsController {
    private static final String KEY_PAIR_OTHER = "hearing_aid_pair_other_button";
    @VisibleForTesting
    static final String KEY_SPACE = "hearing_aid_space_layout";

    private ButtonPreference mPreference;
    private SpacePreference mSpacePreference;

    public BluetoothDetailsPairOtherController(Context context,
            PreferenceFragmentCompat fragment,
            CachedBluetoothDevice device,
            Lifecycle lifecycle) {
        super(context, fragment, device, lifecycle);
        lifecycle.addObserver(this);
    }

    @Override
    public boolean isAvailable() {
        return getButtonPreferenceVisibility(mCachedDevice);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_PAIR_OTHER;
    }

    @Override
    protected void init(PreferenceScreen screen) {
        mPreference = screen.findPreference(getPreferenceKey());
        mSpacePreference = screen.findPreference(KEY_SPACE);
        updateButtonPreferenceTitle(mPreference);
        setPreferencesVisibility(getButtonPreferenceVisibility(mCachedDevice));
        mPreference.setOnClickListener(v -> launchPairingDetail());
    }

    @Override
    protected void refresh() {
        updateButtonPreferenceTitle(mPreference);
        setPreferencesVisibility(getButtonPreferenceVisibility(mCachedDevice));
    }

    private void updateButtonPreferenceTitle(ButtonPreference preference) {
        final int side = mCachedDevice.getDeviceSide();
        final int stringRes = (side == HearingAidInfo.DeviceSide.SIDE_LEFT)
                ? R.string.bluetooth_pair_right_ear_button
                : R.string.bluetooth_pair_left_ear_button;

        preference.setTitle(stringRes);
    }

    private void setPreferencesVisibility(boolean visible) {
        mPreference.setVisible(visible);
        mSpacePreference.setVisible(visible);
    }

    private boolean getButtonPreferenceVisibility(CachedBluetoothDevice cachedDevice) {
        // The device is not connected yet. Don't show the button.
        if (!cachedDevice.isConnectedHearingAidDevice()) {
            return false;
        }
        return isBinauralMode(cachedDevice) && !isOtherSideBonded(cachedDevice);
    }

    private void launchPairingDetail() {
        new SubSettingLauncher(mContext)
                .setDestination(BluetoothPairingDetail.class.getName())
                .setSourceMetricsCategory(
                        ((BluetoothDeviceDetailsFragment) mFragment).getMetricsCategory())
                .launch();
    }

    private boolean isBinauralMode(CachedBluetoothDevice cachedDevice) {
        return cachedDevice.getDeviceMode() == HearingAidInfo.DeviceMode.MODE_BINAURAL;
    }

    private boolean isOtherSideBonded(CachedBluetoothDevice cachedDevice) {
        final CachedBluetoothDevice subDevice = cachedDevice.getSubDevice();
        final boolean subDeviceBonded =
                subDevice != null && subDevice.getBondState() == BluetoothDevice.BOND_BONDED;

        final Set<CachedBluetoothDevice> memberDevice = cachedDevice.getMemberDevice();
        final boolean allMemberDevicesBonded =
                !memberDevice.isEmpty() && memberDevice.stream().allMatch(
                        device -> device.getBondState() == BluetoothDevice.BOND_BONDED);

        return subDeviceBonded || allMemberDevicesBonded;
    }
}
