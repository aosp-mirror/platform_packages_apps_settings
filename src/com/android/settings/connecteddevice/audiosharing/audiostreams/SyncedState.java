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

import android.app.AlertDialog;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.utils.ThreadUtils;

import java.nio.charset.StandardCharsets;

class SyncedState extends AudioStreamStateHandler {
    private static final String TAG = "SyncedState";
    private static final boolean DEBUG = BluetoothUtils.D;
    @Nullable private static SyncedState sInstance = null;

    SyncedState() {}

    static SyncedState getInstance() {
        if (sInstance == null) {
            sInstance = new SyncedState();
        }
        return sInstance;
    }

    @Override
    Preference.OnPreferenceClickListener getOnClickListener(
            AudioStreamsProgressCategoryController controller) {
        return p -> addSourceOrShowDialog(p, controller);
    }

    @Override
    AudioStreamsProgressCategoryController.AudioStreamState getStateEnum() {
        return AudioStreamsProgressCategoryController.AudioStreamState.SYNCED;
    }

    private boolean addSourceOrShowDialog(
            Preference preference, AudioStreamsProgressCategoryController controller) {
        var p = (AudioStreamPreference) preference;
        if (DEBUG) {
            Log.d(
                    TAG,
                    "preferenceClicked(): attempt to join broadcast id : "
                            + p.getAudioStreamBroadcastId());
        }
        var source = p.getAudioStreamMetadata();
        if (source != null) {
            if (source.isEncrypted()) {
                ThreadUtils.postOnMainThread(() -> launchPasswordDialog(source, p, controller));
            } else {
                controller.handleSourceAddRequest(p, source);
            }
        }
        return true;
    }

    private void launchPasswordDialog(
            BluetoothLeBroadcastMetadata source,
            AudioStreamPreference preference,
            AudioStreamsProgressCategoryController controller) {
        View layout =
                LayoutInflater.from(preference.getContext())
                        .inflate(R.layout.bluetooth_find_broadcast_password_dialog, null);
        ((TextView) layout.requireViewById(R.id.broadcast_name_text))
                .setText(preference.getTitle());
        AlertDialog alertDialog =
                new AlertDialog.Builder(preference.getContext())
                        .setTitle(R.string.find_broadcast_password_dialog_title)
                        .setView(layout)
                        .setNeutralButton(android.R.string.cancel, null)
                        .setPositiveButton(
                                R.string.bluetooth_connect_access_dialog_positive,
                                (dialog, which) -> {
                                    var code =
                                            ((EditText)
                                                            layout.requireViewById(
                                                                    R.id.broadcast_edit_text))
                                                    .getText()
                                                    .toString();
                                    var metadata =
                                            new BluetoothLeBroadcastMetadata.Builder(source)
                                                    .setBroadcastCode(
                                                            code.getBytes(StandardCharsets.UTF_8))
                                                    .build();
                                    controller.handleSourceAddRequest(preference, metadata);
                                })
                        .create();
        alertDialog.show();
    }
}
