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

import android.app.Activity;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.os.Bundle;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settings.connecteddevice.audiosharing.audiostreams.qrcode.QrCodeScanModeFragment;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.bluetooth.BluetoothLeBroadcastMetadataExt;

import com.google.common.base.Strings;

public class AudioStreamConfirmDialog extends InstrumentedDialogFragment {
    public static final String KEY_BROADCAST_METADATA = "key_broadcast_metadata";
    private static final String TAG = "AudioStreamConfirmDialog";
    private Activity mActivity;
    private String mBroadcastMetadataStr;
    private BluetoothLeBroadcastMetadata mBroadcastMetadata;
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
        mBroadcastMetadataStr =
                mActivity.getIntent().getStringExtra(QrCodeScanModeFragment.KEY_BROADCAST_METADATA);
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
            // Warm up LE_AUDIO_BROADCAST_ASSISTANT service
            Utils.getLocalBluetoothManager(mActivity);
            mIsRequestValid = true;
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return mIsRequestValid ? getConfirmDialog() : getErrorDialog();
    }

    @Override
    public int getMetricsCategory() {
        // TODO(chelseahao): update metrics id
        return 0;
    }

    private Dialog getConfirmDialog() {
        return new AudioStreamsDialogFragment.DialogBuilder(mActivity)
                .setTitle("Listen to audio stream")
                .setSubTitle1(mBroadcastMetadata.getBroadcastName())
                .setSubTitle2(
                        "The audio stream will play on the active LE audio device. Use this device"
                                + " to control the volume.")
                .setLeftButtonText("Cancel")
                .setLeftButtonOnClickListener(
                        unused -> {
                            dismiss();
                            mActivity.finish();
                        })
                .setRightButtonText("Listen")
                .setRightButtonOnClickListener(
                        unused -> {
                            launchAudioStreamsActivity();
                            dismiss();
                            mActivity.finish();
                        })
                .build();
    }

    private Dialog getErrorDialog() {
        return new AudioStreamsDialogFragment.DialogBuilder(mActivity)
                .setTitle("Can't listen to audio stream")
                .setSubTitle1("Can't play this audio stream. Learn more")
                .setRightButtonText("Close")
                .setRightButtonOnClickListener(
                        unused -> {
                            dismiss();
                            mActivity.finish();
                        })
                .build();
    }

    private void launchAudioStreamsActivity() {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_BROADCAST_METADATA, mBroadcastMetadataStr);

        new SubSettingLauncher(mActivity)
                .setTitleRes(R.string.bluetooth_find_broadcast_title)
                .setDestination(AudioStreamsDashboardFragment.class.getName())
                .setArguments(bundle)
                .setSourceMetricsCategory(SettingsEnums.PAGE_UNKNOWN)
                .launch();
    }
}
