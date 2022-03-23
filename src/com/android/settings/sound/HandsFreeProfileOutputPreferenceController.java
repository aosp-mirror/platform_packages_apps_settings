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

package com.android.settings.sound;

import static android.bluetooth.IBluetoothHearingAid.HI_SYNC_ID_INVALID;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.text.TextUtils;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settingslib.Utils;
import com.android.settingslib.bluetooth.HeadsetProfile;
import com.android.settingslib.bluetooth.HearingAidProfile;

/**
 * This class allows switching between HFP-connected & HAP-connected BT devices
 * while in on-call state.
 */
public class HandsFreeProfileOutputPreferenceController extends AudioSwitchPreferenceController
        implements Preference.OnPreferenceChangeListener {

    private static final int INVALID_INDEX = -1;

    public HandsFreeProfileOutputPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final String address = (String) newValue;
        if (!(preference instanceof ListPreference)) {
            return false;
        }

        final CharSequence defaultSummary = mContext.getText(R.string.media_output_default_summary);
        final ListPreference listPreference = (ListPreference) preference;
        if (TextUtils.equals(address, defaultSummary)) {
            // Switch to default device which address is device name
            mSelectedIndex = getDefaultDeviceIndex();
            setActiveBluetoothDevice(null);
            listPreference.setSummary(defaultSummary);
        } else {
            // Switch to BT device which address is hardware address
            final int connectedDeviceIndex = getConnectedDeviceIndex(address);
            if (connectedDeviceIndex == INVALID_INDEX) {
                return false;
            }
            final BluetoothDevice btDevice = mConnectedDevices.get(connectedDeviceIndex);
            mSelectedIndex = connectedDeviceIndex;
            setActiveBluetoothDevice(btDevice);
            listPreference.setSummary(btDevice.getAlias());
        }
        return true;
    }

    private int getConnectedDeviceIndex(String hardwareAddress) {
        if (mConnectedDevices != null) {
            for (int i = 0, size = mConnectedDevices.size(); i < size; i++) {
                final BluetoothDevice btDevice = mConnectedDevices.get(i);
                if (TextUtils.equals(btDevice.getAddress(), hardwareAddress)) {
                    return i;
                }
            }
        }
        return INVALID_INDEX;
    }

    @Override
    public void updateState(Preference preference) {
        if (preference == null) {
            // In case UI is not ready.
            return;
        }

        if (!Utils.isAudioModeOngoingCall(mContext)) {
            // Without phone call, disable the switch entry.
            mPreference.setVisible(false);
            preference.setSummary(mContext.getText(R.string.media_output_default_summary));
            return;
        }

        // Ongoing call status, list all the connected devices support hands free profile.
        // Select current active device.
        // Disable switch entry if there is no connected device.
        mConnectedDevices.clear();
        mConnectedDevices.addAll(getConnectedHfpDevices());
        mConnectedDevices.addAll(getConnectedHearingAidDevices());

        final int numDevices = mConnectedDevices.size();
        if (numDevices == 0) {
            // No connected devices, disable switch entry.
            mPreference.setVisible(false);
            final CharSequence summary = mContext.getText(R.string.media_output_default_summary);
            final CharSequence[] defaultMediaOutput = new CharSequence[]{summary};
            mSelectedIndex = getDefaultDeviceIndex();
            preference.setSummary(summary);
            setPreference(defaultMediaOutput, defaultMediaOutput, preference);
            return;
        }

        mPreference.setVisible(true);
        CharSequence[] mediaOutputs = new CharSequence[numDevices + 1];
        CharSequence[] mediaValues = new CharSequence[numDevices + 1];

        // Setup devices entries, select active connected device
        setupPreferenceEntries(mediaOutputs, mediaValues, findActiveDevice());

        // Display connected devices, default device and show the active device
        setPreference(mediaOutputs, mediaValues, preference);
    }

    int getDefaultDeviceIndex() {
        // Default device is after all connected devices.
        return mConnectedDevices.size();
    }

    void setupPreferenceEntries(CharSequence[] mediaOutputs, CharSequence[] mediaValues,
            BluetoothDevice activeDevice) {
        // default to current device
        mSelectedIndex = getDefaultDeviceIndex();
        // default device is after all connected devices.
        final CharSequence defaultSummary = mContext.getText(R.string.media_output_default_summary);
        mediaOutputs[mSelectedIndex] = defaultSummary;
        // use default device name as address
        mediaValues[mSelectedIndex] = defaultSummary;
        for (int i = 0, size = mConnectedDevices.size(); i < size; i++) {
            final BluetoothDevice btDevice = mConnectedDevices.get(i);
            mediaOutputs[i] = btDevice.getAlias();
            mediaValues[i] = btDevice.getAddress();
            if (btDevice.equals(activeDevice)) {
                // select the active connected device.
                mSelectedIndex = i;
            }
        }
    }

    void setPreference(CharSequence[] mediaOutputs, CharSequence[] mediaValues,
            Preference preference) {
        final ListPreference listPreference = (ListPreference) preference;
        listPreference.setEntries(mediaOutputs);
        listPreference.setEntryValues(mediaValues);
        listPreference.setValueIndex(mSelectedIndex);
        listPreference.setSummary(mediaOutputs[mSelectedIndex]);
        mAudioSwitchPreferenceCallback.onPreferenceDataChanged(listPreference);
    }

    public void setActiveBluetoothDevice(BluetoothDevice device) {
        if (!Utils.isAudioModeOngoingCall(mContext)) {
            return;
        }
        final HearingAidProfile hapProfile = mProfileManager.getHearingAidProfile();
        final HeadsetProfile hfpProfile = mProfileManager.getHeadsetProfile();
        if (hapProfile != null && hfpProfile != null && device == null) {
            hfpProfile.setActiveDevice(null);
            hapProfile.setActiveDevice(null);
        } else if (hapProfile != null && device != null
                && hapProfile.getHiSyncId(device) != HI_SYNC_ID_INVALID) {
            hapProfile.setActiveDevice(device);
        } else if (hfpProfile != null) {
            hfpProfile.setActiveDevice(device);
        }
    }

    @Override
    public BluetoothDevice findActiveDevice() {
        BluetoothDevice activeDevice = findActiveHearingAidDevice();
        final HeadsetProfile headsetProfile = mProfileManager.getHeadsetProfile();

        if (activeDevice == null && headsetProfile != null) {
            activeDevice = headsetProfile.getActiveDevice();
        }
        return activeDevice;
    }
}
