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
import static android.media.AudioManager.STREAM_MUSIC;
import static android.media.AudioSystem.DEVICE_OUT_REMOTE_SUBMIX;
import static android.media.AudioSystem.DEVICE_OUT_USB_HEADSET;

import com.android.settingslib.Utils;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.media.AudioManager;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settingslib.bluetooth.A2dpProfile;
import com.android.settingslib.bluetooth.HearingAidProfile;

/**
 * This class which allows switching between A2dp-connected & HAP-connected BT devices.
 * A few conditions will disable this switcher:
 * - No available BT device(s)
 * - Media stream captured by cast device
 * - During a call.
 */
public class MediaOutputPreferenceController extends AudioSwitchPreferenceController {

    public MediaOutputPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public void updateState(Preference preference) {
        if (preference == null) {
            // In case UI is not ready.
            return;
        }

        if (isStreamFromOutputDevice(STREAM_MUSIC, DEVICE_OUT_REMOTE_SUBMIX)) {
            // In cast mode, disable switch entry.
            mPreference.setVisible(false);
            preference.setSummary(mContext.getText(R.string.media_output_summary_unavailable));
            return;
        }

        if (Utils.isAudioModeOngoingCall(mContext)) {
            // Ongoing call status, switch entry for media will be disabled.
            mPreference.setVisible(false);
            preference.setSummary(
                    mContext.getText(R.string.media_out_summary_ongoing_call_state));
            return;
        }

        mConnectedDevices.clear();
        // Otherwise, list all of the A2DP connected device and display the active device.
        if (mAudioManager.getMode() == AudioManager.MODE_NORMAL) {
            mConnectedDevices.addAll(getConnectedA2dpDevices());
            mConnectedDevices.addAll(getConnectedHearingAidDevices());
        }

        final int numDevices = mConnectedDevices.size();
        if (numDevices == 0) {
            // Disable switch entry if there is no connected devices.
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
        setupPreferenceEntries(mediaOutputs, mediaValues, findActiveDevice(STREAM_MUSIC));

        if (isStreamFromOutputDevice(STREAM_MUSIC, DEVICE_OUT_USB_HEADSET)) {
            // If wired headset is plugged in and active, select to default device.
            mSelectedIndex = getDefaultDeviceIndex();
        }

        // Display connected devices, default device and show the active device
        setPreference(mediaOutputs, mediaValues, preference);
    }

    @Override
    public void setActiveBluetoothDevice(BluetoothDevice device) {
        if (mAudioManager.getMode() != AudioManager.MODE_NORMAL) {
            return;
        }
        final HearingAidProfile hapProfile = mProfileManager.getHearingAidProfile();
        final A2dpProfile a2dpProfile = mProfileManager.getA2dpProfile();
        if (hapProfile != null && a2dpProfile != null && device == null) {
            hapProfile.setActiveDevice(null);
            a2dpProfile.setActiveDevice(null);
        } else if (hapProfile != null && hapProfile.getHiSyncId(device) != HI_SYNC_ID_INVALID) {
            hapProfile.setActiveDevice(device);
        } else if (a2dpProfile != null) {
            a2dpProfile.setActiveDevice(device);
        }
    }
}
