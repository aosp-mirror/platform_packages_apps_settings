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

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settings.core.InstrumentedFragment;
import com.android.settingslib.bluetooth.BluetoothLeBroadcastMetadataExt;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.qrcode.QrCodeGenerator;
import com.android.settingslib.utils.ThreadUtils;

import com.google.zxing.WriterException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public class AudioStreamsQrCodeFragment extends InstrumentedFragment {
    private static final String TAG = "AudioStreamsQrCodeFragment";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.AUDIO_STREAM_QR_CODE;
    }

    @Override
    public final View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bluetooth_audio_streams_qr_code, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Collapse or expand the app bar based on orientation for better display the qr code image.
        AudioStreamsHelper.configureAppBarByOrientation(getActivity());
        var unused = ThreadUtils.postOnBackgroundThread(
                () -> {
                    BluetoothLeBroadcastMetadata broadcastMetadata = getBroadcastMetadata();
                    if (broadcastMetadata == null) {
                        return;
                    }
                    Bitmap bm = getQrCodeBitmap(broadcastMetadata).orElse(null);
                    if (bm == null) {
                        return;
                    }

                    ThreadUtils.postOnMainThread(
                            () -> {
                                ((ImageView) view.requireViewById(R.id.qrcode_view))
                                        .setImageBitmap(bm);
                                if (broadcastMetadata.getBroadcastCode() != null) {
                                    String password =
                                            new String(
                                                    broadcastMetadata.getBroadcastCode(),
                                                    StandardCharsets.UTF_8);
                                    String passwordText =
                                            getString(
                                                    R.string.audio_streams_qr_code_page_password,
                                                    password);
                                    ((TextView) view.requireViewById(R.id.password))
                                            .setText(passwordText);
                                }
                                TextView summaryView = view.requireViewById(android.R.id.summary);
                                String summary =
                                        getString(
                                                R.string.audio_streams_qr_code_page_description,
                                                broadcastMetadata.getBroadcastName());
                                summaryView.setText(summary);
                            });
                });
    }

    private Optional<Bitmap> getQrCodeBitmap(@Nullable BluetoothLeBroadcastMetadata metadata) {
        if (metadata == null) {
            Log.d(TAG, "getQrCodeBitmap: broadcastMetadata is empty!");
            return Optional.empty();
        }
        String metadataStr = BluetoothLeBroadcastMetadataExt.INSTANCE.toQrCodeString(metadata);
        if (metadataStr.isEmpty()) {
            Log.d(TAG, "getQrCodeBitmap: metadataStr is empty!");
            return Optional.empty();
        }
        Log.d(TAG, "getQrCodeBitmap: metadata : " + metadata);
        try {
            int qrcodeSize =
                    getResources().getDimensionPixelSize(R.dimen.audio_streams_qrcode_size);
            Bitmap bitmap = QrCodeGenerator.encodeQrCode(metadataStr, qrcodeSize);
            return Optional.of(bitmap);
        } catch (WriterException e) {
            Log.d(
                    TAG,
                    "getQrCodeBitmap: broadcastMetadata "
                            + metadata
                            + " qrCode generation exception "
                            + e);
        }

        return Optional.empty();
    }

    @Nullable
    private BluetoothLeBroadcastMetadata getBroadcastMetadata() {
        LocalBluetoothLeBroadcast localBluetoothLeBroadcast =
                Utils.getLocalBtManager(getActivity())
                        .getProfileManager()
                        .getLeAudioBroadcastProfile();
        if (localBluetoothLeBroadcast == null) {
            Log.d(TAG, "getBroadcastMetadataQrCode: localBluetoothLeBroadcast is null!");
            return null;
        }

        List<BluetoothLeBroadcastMetadata> metadata =
                localBluetoothLeBroadcast.getAllBroadcastMetadata();
        if (metadata.isEmpty()) {
            Log.d(TAG, "getBroadcastMetadataQrCode: metadata is null!");
            return null;
        }

        return metadata.get(0);
    }
}
