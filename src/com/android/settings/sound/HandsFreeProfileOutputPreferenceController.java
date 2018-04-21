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

import static android.media.AudioManager.STREAM_VOICE_CALL;
import static android.media.AudioSystem.DEVICE_OUT_USB_HEADSET;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import androidx.preference.Preference;

import com.android.internal.util.ArrayUtils;
import com.android.settings.R;
import com.android.settingslib.bluetooth.HeadsetProfile;

/**
 * This class allows switching between HFP-connected BT devices
 * while in on-call state.
 */
public class HandsFreeProfileOutputPreferenceController extends
        AudioSwitchPreferenceController {

    public HandsFreeProfileOutputPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public void updateState(Preference preference) {
        if (preference == null) {
            // In case UI is not ready.
            return;
        }

        if (!isOngoingCallStatus()) {
            // Without phone call, disable the switch entry.
            preference.setEnabled(false);
            preference.setSummary(mContext.getText(R.string.media_output_default_summary));
            return;
        }

        // Ongoing call status, list all the connected devices support hands free profile.
        // Select current active device.
        // Disable switch entry if there is no connected device.
        mConnectedDevices = null;
        BluetoothDevice activeDevice = null;

        final HeadsetProfile headsetProfile = mProfileManager.getHeadsetProfile();
        if (headsetProfile != null) {
            mConnectedDevices = headsetProfile.getConnectedDevices();
            activeDevice = headsetProfile.getActiveDevice();
        }

        final int numDevices = ArrayUtils.size(mConnectedDevices);
        if (numDevices == 0) {
            // No connected devices, disable switch entry.
            preference.setEnabled(false);
            preference.setSummary(mContext.getText(R.string.media_output_default_summary));
            return;
        }

        preference.setEnabled(true);
        CharSequence[] mediaOutputs = new CharSequence[numDevices + 1];
        CharSequence[] mediaValues = new CharSequence[numDevices + 1];

        // Setup devices entries, select active connected device
        setupPreferenceEntries(mediaOutputs, mediaValues, activeDevice);

        if (isStreamFromOutputDevice(STREAM_VOICE_CALL, DEVICE_OUT_USB_HEADSET)) {
            // If wired headset is plugged in and active, select to default device.
            mSelectedIndex = getDefaultDeviceIndex();
        }

        // Display connected devices, default device and show the active device
        setPreference(mediaOutputs, mediaValues, preference);
    }

    @Override
    public void setActiveBluetoothDevice(BluetoothDevice device) {
        if (isOngoingCallStatus()) {
            mProfileManager.getHeadsetProfile().setActiveDevice(device);
        }
    }
}
