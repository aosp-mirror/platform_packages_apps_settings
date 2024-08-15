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

import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamsScanQrCodeController.REQUEST_SCAN_BT_BROADCAST_QR_CODE;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.bluetooth.BluetoothLeBroadcastMetadataExt;
import com.android.settingslib.bluetooth.BluetoothUtils;

public class AudioStreamsDashboardFragment extends DashboardFragment {
    public static final String KEY_BROADCAST_METADATA = "key_broadcast_metadata";
    private static final String TAG = "AudioStreamsDashboardFrag";
    private static final boolean DEBUG = BluetoothUtils.D;
    private AudioStreamsProgressCategoryController mAudioStreamsProgressCategoryController;

    public AudioStreamsDashboardFragment() {
        super();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.AUDIO_STREAM_MAIN;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_audio_sharing;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.bluetooth_le_audio_streams;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        use(AudioStreamsScanQrCodeController.class).setFragment(this);
        mAudioStreamsProgressCategoryController = use(AudioStreamsProgressCategoryController.class);
        mAudioStreamsProgressCategoryController.setFragment(this);

        if (getArguments() != null) {
            var broadcastMetadata =
                    getArguments()
                            .getParcelable(
                                    KEY_BROADCAST_METADATA, BluetoothLeBroadcastMetadata.class);
            if (broadcastMetadata != null) {
                mAudioStreamsProgressCategoryController.setSourceFromQrCode(
                        broadcastMetadata, SourceOriginForLogging.QR_CODE_SCAN_OTHER);
                mMetricsFeatureProvider.action(
                        getContext(),
                        SettingsEnums.ACTION_AUDIO_STREAM_QR_CODE_SCAN_SUCCEED,
                        SourceOriginForLogging.QR_CODE_SCAN_OTHER.ordinal());
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (DEBUG) {
            Log.d(
                    TAG,
                    "onActivityResult() requestCode : "
                            + requestCode
                            + " resultCode : "
                            + resultCode);
        }
        if (requestCode == REQUEST_SCAN_BT_BROADCAST_QR_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                String broadcastMetadata =
                        data != null ? data.getStringExtra(KEY_BROADCAST_METADATA) : "";
                BluetoothLeBroadcastMetadata source =
                        BluetoothLeBroadcastMetadataExt.INSTANCE.convertToBroadcastMetadata(
                                broadcastMetadata);
                if (source == null) {
                    Log.w(TAG, "onActivityResult() source is null!");
                    return;
                }
                if (DEBUG) {
                    Log.d(TAG, "onActivityResult() broadcastId : " + source.getBroadcastId());
                }
                if (mAudioStreamsProgressCategoryController == null) {
                    Log.w(
                            TAG,
                            "onActivityResult() AudioStreamsProgressCategoryController is null!");
                    return;
                }
                mAudioStreamsProgressCategoryController.setSourceFromQrCode(
                        source, SourceOriginForLogging.QR_CODE_SCAN_SETTINGS);
                mMetricsFeatureProvider.action(
                        getContext(),
                        SettingsEnums.ACTION_AUDIO_STREAM_QR_CODE_SCAN_SUCCEED,
                        SourceOriginForLogging.QR_CODE_SCAN_SETTINGS.ordinal());
            }
        }
    }
}
