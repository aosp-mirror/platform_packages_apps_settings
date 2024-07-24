/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.media.MediaOutputConstants;

/**
 * This Dialog allowed users to do some actions for broadcast media or find the
 * nearby broadcast sources.
 */
public class BluetoothBroadcastDialog extends InstrumentedDialogFragment {

    public static final String KEY_APP_LABEL = "app_label";
    public static final String KEY_DEVICE_ADDRESS =
            BluetoothFindBroadcastsFragment.KEY_DEVICE_ADDRESS;
    public static final String KEY_MEDIA_STREAMING = "media_streaming";

    private static final String TAG = "BTBroadcastsDialog";
    private static final CharSequence UNKNOWN_APP_LABEL = "unknown";
    private Context mContext;
    private CharSequence mCurrentAppLabel = UNKNOWN_APP_LABEL;
    private String mDeviceAddress;
    private boolean mIsMediaStreaming;
    private LocalBluetoothManager mLocalBluetoothManager;
    private AlertDialog mAlertDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        mCurrentAppLabel = getActivity().getIntent().getCharSequenceExtra(KEY_APP_LABEL);
        mDeviceAddress = getActivity().getIntent().getStringExtra(KEY_DEVICE_ADDRESS);
        mIsMediaStreaming = getActivity().getIntent().getBooleanExtra(KEY_MEDIA_STREAMING, false);
        mLocalBluetoothManager = Utils.getLocalBtManager(mContext);
        setShowsDialog(true);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View layout = View.inflate(mContext,
                com.android.settingslib.R.layout.broadcast_dialog, null);

        TextView title = layout.findViewById(com.android.settingslib.R.id.dialog_title);
        TextView subTitle = layout.findViewById(com.android.settingslib.R.id.dialog_subtitle);

        Button broadcastBtn = layout.findViewById(com.android.settingslib.R.id.positive_btn);
        if (isBroadcastSupported() && mIsMediaStreaming) {
            title.setText(mContext.getString(R.string.bluetooth_broadcast_dialog_title));
            subTitle.setText(
                    mContext.getString(R.string.bluetooth_broadcast_dialog_broadcast_message));
            broadcastBtn.setVisibility(View.VISIBLE);
            if (TextUtils.isEmpty(mCurrentAppLabel)) {
                broadcastBtn.setText(mContext.getString(R.string.bluetooth_broadcast_dialog_title));
            } else {
                broadcastBtn.setText(mContext.getString(
                    R.string.bluetooth_broadcast_dialog_broadcast_app,
                    String.valueOf(mCurrentAppLabel)));
            }
            broadcastBtn.setOnClickListener((view) -> {
                launchMediaOutputBroadcastDialog();
            });
        } else {
            title.setText(mContext.getString(R.string.bluetooth_find_broadcast));
            subTitle.setText(
                    mContext.getString(R.string.bluetooth_broadcast_dialog_find_message));
            broadcastBtn.setVisibility(View.GONE);
        }

        Button findBroadcastBtn = layout.findViewById(com.android.settingslib.R.id.negative_btn);
        findBroadcastBtn.setText(mContext.getString(R.string.bluetooth_find_broadcast));
        findBroadcastBtn.setOnClickListener((view) -> {
            launchFindBroadcastsActivity();
        });

        Button cancelBtn = layout.findViewById(com.android.settingslib.R.id.neutral_btn);
        cancelBtn.setOnClickListener((view) -> {
            dismiss();
            getActivity().finish();
        });

        mAlertDialog = new AlertDialog.Builder(mContext,
                com.android.settingslib.widget.theme.R.style.Theme_AlertDialog_SettingsLib)
            .setView(layout)
            .create();

        return mAlertDialog;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_LE_AUDIO_BROADCAST;
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        dismiss();
        getActivity().finish();
    }

    private void launchFindBroadcastsActivity() {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_DEVICE_ADDRESS, mDeviceAddress);

        new SubSettingLauncher(mContext)
                .setTitleRes(R.string.bluetooth_find_broadcast_title)
                .setDestination(BluetoothFindBroadcastsFragment.class.getName())
                .setArguments(bundle)
                .setSourceMetricsCategory(SettingsEnums.PAGE_UNKNOWN)
                .launch();
        dismissVolumePanel();
    }

    private void launchMediaOutputBroadcastDialog() {
        if (startBroadcast()) {
            mContext.sendBroadcast(new Intent()
                    .setPackage(MediaOutputConstants.SYSTEMUI_PACKAGE_NAME)
                    .setAction(MediaOutputConstants.ACTION_LAUNCH_MEDIA_OUTPUT_BROADCAST_DIALOG)
                    .putExtra(MediaOutputConstants.EXTRA_PACKAGE_NAME,
                            getActivity().getPackageName()));
            dismissVolumePanel();
        }
    }

    private LocalBluetoothLeBroadcast getLEAudioBroadcastProfile() {
        if (mLocalBluetoothManager != null && mLocalBluetoothManager.getProfileManager() != null) {
            LocalBluetoothLeBroadcast bluetoothLeBroadcast =
                    mLocalBluetoothManager.getProfileManager().getLeAudioBroadcastProfile();
            if (bluetoothLeBroadcast != null) {
                return bluetoothLeBroadcast;
            }
        }
        Log.d(TAG, "Can not get LE Audio Broadcast Profile");
        return null;
    }

    private boolean startBroadcast() {
        LocalBluetoothLeBroadcast btLeBroadcast = getLEAudioBroadcastProfile();
        if (btLeBroadcast != null) {
            btLeBroadcast.startBroadcast(String.valueOf(mCurrentAppLabel), null);
            return true;
        }
        Log.d(TAG, "Can not broadcast successfully");
        return false;
    }

    private void dismissVolumePanel() {
        // Dismiss volume panel
        mContext.sendBroadcast(new Intent()
                .setPackage(MediaOutputConstants.SETTINGS_PACKAGE_NAME)
                .setAction(MediaOutputConstants.ACTION_CLOSE_PANEL));
    }

    boolean isBroadcastSupported() {
        LocalBluetoothLeBroadcast broadcast =
                mLocalBluetoothManager.getProfileManager().getLeAudioBroadcastProfile();
        return broadcast != null;
    }
}
