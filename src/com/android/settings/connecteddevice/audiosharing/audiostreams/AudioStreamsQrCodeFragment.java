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

import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settings.core.InstrumentedFragment;
import com.android.settingslib.bluetooth.BluetoothLeBroadcastMetadataExt;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.qrcode.QrCodeGenerator;

import com.google.zxing.WriterException;

import java.util.Optional;

public class AudioStreamsQrCodeFragment extends InstrumentedFragment {
    private static final String TAG = "AudioStreamsQrCodeFragment";

    @Override
    public int getMetricsCategory() {
        // TODO(chelseahao): update metrics id
        return 0;
    }

    @Override
    public final View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.xml.bluetooth_audio_streams_qr_code, container, false);
        getQrCodeBitmap()
                .ifPresent(
                        bm ->
                                ((ImageView) view.requireViewById(R.id.qrcode_view))
                                        .setImageBitmap(bm));
        return view;
    }

    private Optional<Bitmap> getQrCodeBitmap() {
        String broadcastMetadata = getBroadcastMetadataQrCode();
        if (broadcastMetadata.isEmpty()) {
            Log.d(TAG, "onCreateView: broadcastMetadata is empty!");
            return Optional.empty();
        }

        try {
            int qrcodeSize = getContext().getResources().getDimensionPixelSize(R.dimen.qrcode_size);
            Bitmap bitmap = QrCodeGenerator.encodeQrCode(broadcastMetadata, qrcodeSize);
            return Optional.of(bitmap);
        } catch (WriterException e) {
            Log.d(
                    TAG,
                    "onCreateView: broadcastMetadata "
                            + broadcastMetadata
                            + " qrCode generation exception "
                            + e);
        }

        return Optional.empty();
    }

    private String getBroadcastMetadataQrCode() {
        LocalBluetoothLeBroadcast localBluetoothLeBroadcast =
                Utils.getLocalBtManager(getActivity())
                        .getProfileManager()
                        .getLeAudioBroadcastProfile();
        if (localBluetoothLeBroadcast == null) {
            Log.d(TAG, "getBroadcastMetadataQrCode: localBluetoothLeBroadcast is null!");
            return "";
        }

        BluetoothLeBroadcastMetadata metadata =
                localBluetoothLeBroadcast.getLatestBluetoothLeBroadcastMetadata();
        if (metadata == null) {
            Log.d(TAG, "getBroadcastMetadataQrCode: metadata is null!");
            return "";
        }

        return BluetoothLeBroadcastMetadataExt.INSTANCE.toQrCodeString(metadata);
    }
}
