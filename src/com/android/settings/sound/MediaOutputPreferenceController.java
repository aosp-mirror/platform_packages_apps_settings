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

import static com.android.settingslib.media.flags.Flags.enableOutputSwitcherForSystemRouting;

import android.annotation.Nullable;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.media.MediaOutputUtils;
import com.android.settingslib.Utils;
import com.android.settingslib.bluetooth.A2dpProfile;
import com.android.settingslib.bluetooth.HearingAidProfile;
import com.android.settingslib.media.MediaOutputConstants;

import java.util.List;

/**
 * This class allows launching MediaOutputDialog to switch output device.
 * Preference would hide only when
 * - Bluetooth = OFF
 * - Bluetooth = ON and Connected Devices = 0 and Previously Connected = 0
 * - Media stream captured by remote device
 * - During a call.
 */
public class MediaOutputPreferenceController extends AudioSwitchPreferenceController {

    private static final String TAG = "MediaOutputPreferenceController";
    @Nullable private MediaController mMediaController;
    private MediaSessionManager mMediaSessionManager;

    public MediaOutputPreferenceController(Context context, String key) {
        super(context, key);
        mMediaSessionManager = context.getSystemService(MediaSessionManager.class);
        mMediaController = MediaOutputUtils.getActiveLocalMediaController(mMediaSessionManager);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreference.setVisible(!Utils.isAudioModeOngoingCall(mContext)
                && (enableOutputSwitcherForSystemRouting() ? true : mMediaController != null));
    }

    @Override
    public void updateState(Preference preference) {
        if (preference == null) {
            // In case UI is not ready.
            return;
        }

        if (enableOutputSwitcherForSystemRouting()) {
            mMediaController = MediaOutputUtils.getActiveLocalMediaController(mMediaSessionManager);
        } else {
            if (mMediaController == null) {
                // No active local playback
                return;
            }
        }


        if (Utils.isAudioModeOngoingCall(mContext)) {
            // Ongoing call status, switch entry for media will be disabled.
            mPreference.setVisible(false);
            preference.setSummary(
                    mContext.getText(R.string.media_out_summary_ongoing_call_state));
            return;
        }

        BluetoothDevice activeDevice = null;
        // Show preference if there is connected or previously connected device
        // Find active device and set its name as the preference's summary
        List<BluetoothDevice> connectedA2dpDevices = getConnectedA2dpDevices();
        List<BluetoothDevice> connectedHADevices = getConnectedHearingAidDevices();
        List<BluetoothDevice> connectedLeAudioDevices = getConnectedLeAudioDevices();
        if (mAudioManager.getMode() == AudioManager.MODE_NORMAL
                && ((connectedA2dpDevices != null && !connectedA2dpDevices.isEmpty())
                || (connectedHADevices != null && !connectedHADevices.isEmpty())
                || (connectedLeAudioDevices != null && !connectedLeAudioDevices.isEmpty()))) {
            activeDevice = findActiveDevice();
        }

        if (mMediaController == null) {
            mPreference.setTitle(mContext.getString(R.string.media_output_title_without_playing));
        } else {
            mPreference.setTitle(mContext.getString(R.string.media_output_label_title,
                    com.android.settings.Utils.getApplicationLabel(mContext,
                    mMediaController.getPackageName())));
        }
        mPreference.setSummary((activeDevice == null) ?
                mContext.getText(R.string.media_output_default_summary) :
                activeDevice.getAlias());
    }

    @Override
    public BluetoothDevice findActiveDevice() {
        BluetoothDevice haActiveDevice = findActiveHearingAidDevice();
        BluetoothDevice leAudioActiveDevice = findActiveLeAudioDevice();
        final A2dpProfile a2dpProfile = mProfileManager.getA2dpProfile();

        if (haActiveDevice != null) {
            return haActiveDevice;
        }

        if (leAudioActiveDevice != null) {
            return leAudioActiveDevice;
        }

        if (a2dpProfile != null && a2dpProfile.getActiveDevice() != null) {
            return a2dpProfile.getActiveDevice();
        }

        return null;
    }

    /**
     * Find active hearing aid device
     */
    @Override
    protected BluetoothDevice findActiveHearingAidDevice() {
        final HearingAidProfile hearingAidProfile = mProfileManager.getHearingAidProfile();

        if (hearingAidProfile != null) {
            List<BluetoothDevice> activeDevices = hearingAidProfile.getActiveDevices();
            for (BluetoothDevice btDevice : activeDevices) {
                if (btDevice != null) {
                    return btDevice;
                }
            }
        }
        return null;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            if (enableOutputSwitcherForSystemRouting() && mMediaController == null) {
                mContext.sendBroadcast(new Intent()
                        .setAction(MediaOutputConstants.ACTION_LAUNCH_SYSTEM_MEDIA_OUTPUT_DIALOG)
                        .setPackage(MediaOutputConstants.SYSTEMUI_PACKAGE_NAME));
            } else if (mMediaController != null) {
                mContext.sendBroadcast(new Intent()
                        .setAction(MediaOutputConstants.ACTION_LAUNCH_MEDIA_OUTPUT_DIALOG)
                        .setPackage(MediaOutputConstants.SYSTEMUI_PACKAGE_NAME)
                        .putExtra(MediaOutputConstants.EXTRA_PACKAGE_NAME,
                                mMediaController.getPackageName())
                        .putExtra(MediaOutputConstants.KEY_MEDIA_SESSION_TOKEN,
                                mMediaController.getSessionToken()));
            }
            return true;
        }
        return false;
    }
}
