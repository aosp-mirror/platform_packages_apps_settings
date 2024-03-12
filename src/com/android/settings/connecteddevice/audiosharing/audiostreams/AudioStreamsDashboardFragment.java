/*
 * Copyright (C) 2023 The Android Open Source Project
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
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.connecteddevice.audiosharing.audiostreams.qrcode.QrCodeScanModeFragment;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.bluetooth.BluetoothLeBroadcastMetadataExt;
import com.android.settingslib.bluetooth.BluetoothUtils;

public class AudioStreamsDashboardFragment extends DashboardFragment {
    private static final String TAG = "AudioStreamsDashboardFrag";
    private static final boolean DEBUG = BluetoothUtils.D;
    private AudioStreamsScanQrCodeController mAudioStreamsScanQrCodeController;

    public AudioStreamsDashboardFragment() {
        super();
    }

    @Override
    public int getMetricsCategory() {
        // TODO: update category id.
        return 0;
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
        return R.xml.bluetooth_audio_streams;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mAudioStreamsScanQrCodeController = use(AudioStreamsScanQrCodeController.class);
        mAudioStreamsScanQrCodeController.setFragment(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
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
                        data.getStringExtra(QrCodeScanModeFragment.KEY_BROADCAST_METADATA);
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
                if (mAudioStreamsScanQrCodeController == null) {
                    Log.w(TAG, "onActivityResult() AudioStreamsScanQrCodeController is null!");
                    return;
                }
                mAudioStreamsScanQrCodeController.addSource(source);
            }
        }
    }
}
