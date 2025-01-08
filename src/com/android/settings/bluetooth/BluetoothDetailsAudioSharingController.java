/*
 * Copyright (C) 2025 The Android Open Source Project
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

import static com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast.EXTRA_START_LE_AUDIO_SHARING;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.connecteddevice.audiosharing.AudioSharingDashboardFragment;
import com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamsDashboardFragment;
import com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamsHelper;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.lifecycle.Lifecycle;

/** Controller for audio sharing control preferences. */
public class BluetoothDetailsAudioSharingController extends BluetoothDetailsController {
    private static final String KEY_AUDIO_SHARING_CONTROL = "audio_sharing_control";
    private static final String KEY_AUDIO_SHARING = "audio_sharing";
    private static final String KEY_FIND_AUDIO_STREAM = "find_audio_stream";

    @Nullable PreferenceCategory mProfilesContainer;
    LocalBluetoothManager mLocalBluetoothManager;

    public BluetoothDetailsAudioSharingController(
            @NonNull Context context,
            @NonNull PreferenceFragmentCompat fragment,
            @NonNull LocalBluetoothManager localBtManager,
            @NonNull CachedBluetoothDevice device,
            @NonNull Lifecycle lifecycle) {
        super(context, fragment, device, lifecycle);
        mLocalBluetoothManager = localBtManager;
    }

    @Override
    public boolean isAvailable() {
        return BluetoothUtils.isAudioSharingUIAvailable(mContext)
                && mCachedDevice.isConnectedLeAudioDevice();
    }

    @Override
    protected void init(PreferenceScreen screen) {
        mProfilesContainer = screen.findPreference(KEY_AUDIO_SHARING_CONTROL);
    }

    @Override
    protected void refresh() {
        if (mProfilesContainer == null) {
            return;
        }
        if (!isAvailable()) {
            mProfilesContainer.setVisible(false);
            return;
        }
        mProfilesContainer.setVisible(true);
        mProfilesContainer.removeAll();
        mProfilesContainer.addPreference(createAudioSharingPreference());
        if ((BluetoothUtils.isActiveLeAudioDevice(mCachedDevice)
                        || AudioStreamsHelper.hasConnectedBroadcastSource(
                                mCachedDevice, mLocalBluetoothManager))
                && !BluetoothUtils.isBroadcasting(mLocalBluetoothManager)) {
            mProfilesContainer.addPreference(createFindAudioStreamPreference());
        }
    }

    private Preference createAudioSharingPreference() {
        Preference audioSharingPref = new Preference(mContext);
        audioSharingPref.setKey(KEY_AUDIO_SHARING);
        audioSharingPref.setTitle(R.string.audio_sharing_title);
        audioSharingPref.setIcon(com.android.settingslib.R.drawable.ic_bt_le_audio_sharing);
        audioSharingPref.setOnPreferenceClickListener(
                preference -> {
                    Bundle args = new Bundle();
                    args.putBoolean(EXTRA_START_LE_AUDIO_SHARING, true);
                    new SubSettingLauncher(mContext)
                            .setDestination(AudioSharingDashboardFragment.class.getName())
                            .setSourceMetricsCategory(SettingsEnums.BLUETOOTH_DEVICE_DETAILS)
                            .setArguments(args)
                            .launch();
                    return true;
                });
        return audioSharingPref;
    }

    private Preference createFindAudioStreamPreference() {
        Preference findAudioStreamPref = new Preference(mContext);
        findAudioStreamPref.setKey(KEY_FIND_AUDIO_STREAM);
        findAudioStreamPref.setTitle(R.string.audio_streams_main_page_title);
        findAudioStreamPref.setIcon(com.android.settingslib.R.drawable.ic_bt_le_audio_sharing);
        findAudioStreamPref.setOnPreferenceClickListener(
                preference -> {
                    new SubSettingLauncher(mContext)
                            .setDestination(AudioStreamsDashboardFragment.class.getName())
                            .setSourceMetricsCategory(SettingsEnums.BLUETOOTH_DEVICE_DETAILS)
                            .launch();
                    return true;
                });
        return findAudioStreamPref;
    }

    @Override
    @NonNull
    public String getPreferenceKey() {
        return KEY_AUDIO_SHARING_CONTROL;
    }
}
