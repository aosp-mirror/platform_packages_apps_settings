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
import static android.media.AudioManager.STREAM_VOICE_CALL;
import static android.media.AudioSystem.DEVICE_OUT_USB_HEADSET;

import com.android.settingslib.Utils;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.v7.preference.Preference;

import com.android.settings.R;
import com.android.settingslib.bluetooth.HeadsetProfile;
import com.android.settingslib.bluetooth.HearingAidProfile;

/**
 * This class allows switching between HFP-connected & HAP-connected BT devices
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
        setupPreferenceEntries(mediaOutputs, mediaValues, findActiveDevice(STREAM_VOICE_CALL));

        if (isStreamFromOutputDevice(STREAM_VOICE_CALL, DEVICE_OUT_USB_HEADSET)) {
            // If wired headset is plugged in and active, select to default device.
            mSelectedIndex = getDefaultDeviceIndex();
        }

        // Display connected devices, default device and show the active device
        setPreference(mediaOutputs, mediaValues, preference);
    }

    @Override
    public void setActiveBluetoothDevice(BluetoothDevice device) {
        if (!Utils.isAudioModeOngoingCall(mContext)) {
            return;
        }
        final HearingAidProfile hapProfile = mProfileManager.getHearingAidProfile();
        final HeadsetProfile hfpProfile = mProfileManager.getHeadsetProfile();
        if (hapProfile != null && hfpProfile != null && device == null) {
            hfpProfile.setActiveDevice(null);
            hapProfile.setActiveDevice(null);
            return;
        }
        if (hapProfile != null && hapProfile.getHiSyncId(device) != HI_SYNC_ID_INVALID) {
            hapProfile.setActiveDevice(device);
        }
        if (hfpProfile != null) {
            hfpProfile.setActiveDevice(device);
        }
    }
}
