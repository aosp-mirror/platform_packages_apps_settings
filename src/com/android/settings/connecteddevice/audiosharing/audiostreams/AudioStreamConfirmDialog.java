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
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settings.connecteddevice.ConnectedDeviceDashboardFragment;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.bluetooth.BluetoothLeBroadcastMetadataExt;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;

public class AudioStreamConfirmDialog extends InstrumentedDialogFragment {
    private static final String TAG = "AudioStreamConfirmDialog";

    @VisibleForTesting
    static final int DEFAULT_DEVICE_NAME = R.string.audio_streams_dialog_default_device;

    private Context mContext;
    @VisibleForTesting @Nullable Activity mActivity;
    @Nullable private BluetoothLeBroadcastMetadata mBroadcastMetadata;
    @Nullable private BluetoothDevice mConnectedDevice;
    private int mAudioStreamConfirmDialogId = SettingsEnums.PAGE_UNKNOWN;

    @Override
    public void onAttach(Context context) {
        mContext = context;
        mActivity = getActivity();
        if (mActivity == null) {
            Log.w(TAG, "onAttach() mActivity is null!");
            return;
        }
        Intent intent = mActivity.getIntent();
        mBroadcastMetadata = getMetadata(intent);
        mConnectedDevice = getConnectedDevice();
        mAudioStreamConfirmDialogId =
                getDialogId(mBroadcastMetadata != null, mConnectedDevice != null);
        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setShowsDialog(true);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return switch (mAudioStreamConfirmDialogId) {
            case SettingsEnums.DIALOG_AUDIO_STREAM_CONFIRM_FEATURE_UNSUPPORTED ->
                    getUnsupportedDialog();
            case SettingsEnums.DIALOG_AUDIO_STREAM_CONFIRM_NO_LE_DEVICE -> getNoLeDeviceDialog();
            case SettingsEnums.DIALOG_AUDIO_STREAM_CONFIRM_LISTEN -> getConfirmDialog();
            default -> getErrorDialog();
        };
    }

    @Override
    public int getMetricsCategory() {
        return mAudioStreamConfirmDialogId;
    }

    private Dialog getConfirmDialog() {
        return new AudioStreamsDialogFragment.DialogBuilder(getActivity())
                .setTitle(getString(R.string.audio_streams_dialog_listen_to_audio_stream))
                .setSubTitle1(
                        mBroadcastMetadata != null
                                ? AudioStreamsHelper.getBroadcastName(mBroadcastMetadata)
                                : "")
                .setSubTitle2(
                        getString(
                                R.string.audio_streams_dialog_control_volume,
                                getConnectedDeviceName()))
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
                            mMetricsFeatureProvider.action(
                                    getActivity(),
                                    SettingsEnums
                                            .ACTION_AUDIO_STREAM_CONFIRM_LAUNCH_MAIN_BUTTON_CLICK);
                            launchAudioStreamsActivity();
                            dismiss();
                            if (mActivity != null) {
                                mActivity.finish();
                            }
                        })
                .build();
    }

    private Dialog getUnsupportedDialog() {
        return new AudioStreamsDialogFragment.DialogBuilder(getActivity())
                .setTitle(getString(R.string.audio_streams_dialog_cannot_listen))
                .setSubTitle1(
                        mBroadcastMetadata != null
                                ? AudioStreamsHelper.getBroadcastName(mBroadcastMetadata)
                                : "")
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

    private Dialog getErrorDialog() {
        return new AudioStreamsDialogFragment.DialogBuilder(getActivity())
                .setTitle(getString(R.string.audio_streams_dialog_cannot_listen))
                .setSubTitle2(
                        getString(
                                R.string.audio_streams_dialog_cannot_play,
                                getConnectedDeviceName()))
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
                            new SubSettingLauncher(mContext)
                                    .setDestination(
                                            ConnectedDeviceDashboardFragment.class.getName())
                                    .setSourceMetricsCategory(
                                            SettingsEnums.DIALOG_AUDIO_STREAM_CONFIRM_NO_LE_DEVICE)
                                    .launch();
                            dismiss();
                            if (mActivity != null) {
                                mActivity.finish();
                            }
                        })
                .build();
    }

    private void launchAudioStreamsActivity() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_BROADCAST_METADATA, mBroadcastMetadata);
        if (mActivity != null) {
            new SubSettingLauncher(getActivity())
                    .setTitleText(getString(R.string.audio_streams_activity_title))
                    .setDestination(AudioStreamsDashboardFragment.class.getName())
                    .setArguments(bundle)
                    .setSourceMetricsCategory(getMetricsCategory())
                    .launch();
        }
    }

    private @Nullable BluetoothLeBroadcastMetadata getMetadata(Intent intent) {
        String metadata = intent.getStringExtra(KEY_BROADCAST_METADATA);
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        return BluetoothLeBroadcastMetadataExt.INSTANCE.convertToBroadcastMetadata(metadata);
    }

    private int getDialogId(boolean hasMetadata, boolean hasConnectedDevice) {
        if (!BluetoothUtils.isAudioSharingEnabled()) {
            return SettingsEnums.DIALOG_AUDIO_STREAM_CONFIRM_FEATURE_UNSUPPORTED;
        }
        if (!hasConnectedDevice) {
            return SettingsEnums.DIALOG_AUDIO_STREAM_CONFIRM_NO_LE_DEVICE;
        }
        return hasMetadata
                ? SettingsEnums.DIALOG_AUDIO_STREAM_CONFIRM_LISTEN
                : SettingsEnums.DIALOG_AUDIO_STREAM_CONFIRM_DATA_ERROR;
    }

    @Nullable
    private BluetoothDevice getConnectedDevice() {
        var localBluetoothManager = Utils.getLocalBluetoothManager(getActivity());
        if (localBluetoothManager == null) {
            return null;
        }
        LocalBluetoothLeBroadcastAssistant assistant =
                localBluetoothManager.getProfileManager().getLeAudioBroadcastAssistantProfile();
        if (assistant == null) {
            return null;
        }
        var devices = assistant.getAllConnectedDevices();
        return devices.isEmpty() ? null : devices.get(0);
    }

    private String getConnectedDeviceName() {
        if (mConnectedDevice != null) {
            String alias = mConnectedDevice.getAlias();
            return TextUtils.isEmpty(alias) ? getString(DEFAULT_DEVICE_NAME) : alias;
        }
        Log.w(TAG, "getConnectedDeviceName : no connected device!");
        return getString(DEFAULT_DEVICE_NAME);
    }
}
