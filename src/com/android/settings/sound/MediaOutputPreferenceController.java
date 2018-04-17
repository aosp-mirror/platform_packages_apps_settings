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
import static android.media.MediaRouter.ROUTE_TYPE_REMOTE_DISPLAY;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaRouter;
import android.support.v7.preference.Preference;

import com.android.internal.util.ArrayUtils;
import com.android.settings.R;
import com.android.settingslib.bluetooth.A2dpProfile;


/**
 * This class which allows switching between a2dp-connected BT devices.
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

        if (mAudioManager.isMusicActiveRemotely() || isCastDevice(mMediaRouter)) {
            // TODO(76455906): Workaround for cast mode, need a solid way to identify cast mode.
            // In cast mode, disable switch entry.
            preference.setEnabled(false);
            preference.setSummary(mContext.getText(R.string.media_output_summary_unavailable));
            return;
        }

        if (isOngoingCallStatus()) {
            // Ongoing call status, switch entry for media will be disabled.
            preference.setEnabled(false);
            preference.setSummary(
                    mContext.getText(R.string.media_out_summary_ongoing_call_state));
            return;
        }

        // Otherwise, list all of the A2DP connected device and display the active device.
        mConnectedDevices = null;
        BluetoothDevice activeDevice = null;
        if (mAudioManager.getMode() == AudioManager.MODE_NORMAL) {
            final A2dpProfile a2dpProfile = mProfileManager.getA2dpProfile();
            if (a2dpProfile != null) {
                mConnectedDevices = a2dpProfile.getConnectedDevices();
                activeDevice = a2dpProfile.getActiveDevice();
            }
        }

        final int numDevices = ArrayUtils.size(mConnectedDevices);
        if (numDevices == 0) {
            // Disable switch entry if there is no connected devices.
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
        if (mAudioManager.getMode() == AudioManager.MODE_NORMAL) {
            mProfileManager.getA2dpProfile().setActiveDevice(device);
        }
    }

    private static boolean isCastDevice(MediaRouter mediaRouter) {
        final MediaRouter.RouteInfo selected = mediaRouter.getSelectedRoute(
                ROUTE_TYPE_REMOTE_DISPLAY);
        return selected != null && selected.getPresentationDisplay() != null
                && selected.getPresentationDisplay().isValid();
    }
}
