/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.connecteddevice.audiosharing.audiostreams;

import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamsDashboardFragment.KEY_BROADCAST_METADATA;

import android.app.Activity;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settings.connecteddevice.audiosharing.AudioSharingUtils;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.bluetooth.BluetoothLeBroadcastMetadataExt;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;

import com.google.common.base.Strings;

public class AudioStreamConfirmDialog extends InstrumentedDialogFragment {
    private static final String TAG = "AudioStreamConfirmDialog";
    private static final int DEFAULT_DEVICE_NAME = R.string.audio_streams_dialog_default_device;
    @Nullable private LocalBluetoothManager mLocalBluetoothManager;
    @Nullable private LocalBluetoothProfileManager mProfileManager;
    @Nullable private Activity mActivity;
    @Nullable private String mBroadcastMetadataStr;
    @Nullable private BluetoothLeBroadcastMetadata mBroadcastMetadata;
    private boolean mIsRequestValid = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setShowsDialog(true);
        mActivity = getActivity();
        if (mActivity == null) {
            Log.w(TAG, "onCreate() mActivity is null!");
            return;
        }
        mLocalBluetoothManager = Utils.getLocalBluetoothManager(mActivity);
        mProfileManager =
                mLocalBluetoothManager == null ? null : mLocalBluetoothManager.getProfileManager();
        mBroadcastMetadataStr = mActivity.getIntent().getStringExtra(KEY_BROADCAST_METADATA);
        if (Strings.isNullOrEmpty(mBroadcastMetadataStr)) {
            Log.w(TAG, "onCreate() mBroadcastMetadataStr is null or empty!");
            return;
        }
        mBroadcastMetadata =
                BluetoothLeBroadcastMetadataExt.INSTANCE.convertToBroadcastMetadata(
                        mBroadcastMetadataStr);
        if (mBroadcastMetadata == null) {
            Log.w(TAG, "onCreate() mBroadcastMetadata is null!");
        } else {
            mIsRequestValid = true;
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (!AudioSharingUtils.isFeatureEnabled()) {
            return getUnsupporteDialog();
        }
        if (AudioSharingUtils.isAudioSharingProfileReady(mProfileManager)) {
            CachedBluetoothDevice connectedLeDevice =
                    AudioStreamsHelper.getCachedBluetoothDeviceInSharingOrLeConnected(
                                    mLocalBluetoothManager)
                            .orElse(null);
            if (connectedLeDevice == null) {
                return getNoLeDeviceDialog();
            }
            String deviceName = connectedLeDevice.getName();
            return mIsRequestValid ? getConfirmDialog(deviceName) : getErrorDialog(deviceName);
        }
        Log.d(TAG, "onCreateDialog() : profile not ready!");
        String defaultDeviceName =
                mActivity != null ? mActivity.getString(DEFAULT_DEVICE_NAME) : "";
        return mIsRequestValid
                ? getConfirmDialog(defaultDeviceName)
                : getErrorDialog(defaultDeviceName);
    }

    @Override
    public int getMetricsCategory() {
        // TODO(chelseahao): update metrics id
        return 0;
    }

    private Dialog getConfirmDialog(String name) {
        return new AudioStreamsDialogFragment.DialogBuilder(getActivity())
                .setTitle(getString(R.string.audio_streams_dialog_listen_to_audio_stream))
                .setSubTitle1(
                        mBroadcastMetadata != null
                                ? AudioStreamsHelper.getBroadcastName(mBroadcastMetadata)
                                : "")
                .setSubTitle2(getString(R.string.audio_streams_dialog_control_volume, name))
                .setLeftButtonText(getString(com.android.settings.R.string.cancel))
                .setLeftButtonOnClickListener(
                        unused -> {
                            dismiss();
                            if (mActivity != null) {
                                mActivity.finish();
                            }
                        })
                .setRightButtonText(getString(R.string.audio_streams_dialog_listen))
                .setRightButtonOnClickListener(
                        unused -> {
                            launchAudioStreamsActivity();
                            dismiss();
                            if (mActivity != null) {
                                mActivity.finish();
                            }
                        })
                .build();
    }

    private Dialog getUnsupporteDialog() {
        return new AudioStreamsDialogFragment.DialogBuilder(getActivity())
                .setTitle(getString(R.string.audio_streams_dialog_cannot_listen))
                .setSubTitle2(getString(R.string.audio_streams_dialog_unsupported_device_subtitle))
                .setRightButtonText(getString(R.string.audio_streams_dialog_close))
                .setRightButtonOnClickListener(
                        unused -> {
                            dismiss();
                            if (mActivity != null) {
                                mActivity.finish();
                            }
                        })
                .build();
    }

    private Dialog getErrorDialog(String name) {
        return new AudioStreamsDialogFragment.DialogBuilder(getActivity())
                .setTitle(getString(R.string.audio_streams_dialog_cannot_listen))
                .setSubTitle2(getString(R.string.audio_streams_dialog_cannot_play, name))
                .setRightButtonText(getString(R.string.audio_streams_dialog_close))
                .setRightButtonOnClickListener(
                        unused -> {
                            dismiss();
                            if (mActivity != null) {
                                mActivity.finish();
                            }
                        })
                .build();
    }

    private Dialog getNoLeDeviceDialog() {
        return new AudioStreamsDialogFragment.DialogBuilder(getActivity())
                .setTitle(getString(R.string.audio_streams_dialog_no_le_device_title))
                .setSubTitle2(getString(R.string.audio_streams_dialog_no_le_device_subtitle))
                .setLeftButtonText(getString(R.string.audio_streams_dialog_close))
                .setLeftButtonOnClickListener(
                        unused -> {
                            dismiss();
                            if (mActivity != null) {
                                mActivity.finish();
                            }
                        })
                .setRightButtonText(getString(R.string.audio_streams_dialog_no_le_device_button))
                .setRightButtonOnClickListener(
                        dialog -> {
                            if (mActivity != null) {
                                mActivity.startActivity(
                                        new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                                                .setPackage(mActivity.getPackageName()));
                            }
                            dismiss();
                            if (mActivity != null) {
                                mActivity.finish();
                            }
                        })
                .build();
    }

    private void launchAudioStreamsActivity() {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_BROADCAST_METADATA, mBroadcastMetadataStr);
        if (mActivity != null) {
            new SubSettingLauncher(getActivity())
                    .setTitleText(getString(R.string.audio_streams_activity_title))
                    .setDestination(AudioStreamsDashboardFragment.class.getName())
                    .setArguments(bundle)
                    .setSourceMetricsCategory(SettingsEnums.PAGE_UNKNOWN)
                    .launch();
        }
    }
}
