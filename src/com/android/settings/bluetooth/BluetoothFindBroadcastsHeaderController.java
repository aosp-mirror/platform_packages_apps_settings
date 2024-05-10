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

import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.bluetooth.BluetoothBroadcastUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.LayoutPreference;

/**
 * This class adds a header to display the action button for joining the broadcast session
 * by scanning QR code and leaving the broadcast session
 */
public class BluetoothFindBroadcastsHeaderController extends BluetoothDetailsController {
    private static final String TAG = "BtFindBroadcastCtrl";

    private static final String KEY_BROADCAST_HEADER = "bluetooth_find_broadcast_header";
    private static final String KEY_BROADCAST_SOURCE_LIST = "broadcast_source_list";

    LayoutPreference mLayoutPreference;
    PreferenceCategory mBroadcastSourceList;
    TextView mTitle;
    TextView mSummary;
    Button mBtnFindBroadcast;
    LinearLayout mBtnBroadcastLayout;
    Button mBtnLeaveBroadcast;
    Button mBtnScanQrCode;
    BluetoothFindBroadcastsFragment mBluetoothFindBroadcastsFragment;
    public BluetoothFindBroadcastsHeaderController(Context context,
            BluetoothFindBroadcastsFragment fragment, CachedBluetoothDevice device,
            Lifecycle lifecycle, LocalBluetoothManager bluetoothManager) {
        super(context, fragment, device, lifecycle);
        mBluetoothFindBroadcastsFragment = fragment;
    }

    @Override
    protected void init(PreferenceScreen screen) {
        mLayoutPreference = screen.findPreference(KEY_BROADCAST_HEADER);
        mBroadcastSourceList = screen.findPreference(KEY_BROADCAST_SOURCE_LIST);

        refresh();
    }

    @Override
    protected void refresh() {
        if (mLayoutPreference == null || mCachedDevice == null) {
            return;
        }

        mTitle = mLayoutPreference.findViewById(R.id.entity_header_title);
        mTitle.setText(mCachedDevice.getName());
        mSummary = mLayoutPreference.findViewById(R.id.entity_header_summary);
        mSummary.setText("");

        mBtnFindBroadcast = mLayoutPreference.findViewById(R.id.button_find_broadcast);
        mBtnFindBroadcast.setOnClickListener(v -> scanBroadcastSource());
        mBtnBroadcastLayout = mLayoutPreference.findViewById(R.id.button_broadcast_layout);
        mBtnLeaveBroadcast = mLayoutPreference.findViewById(R.id.button_leave_broadcast);
        mBtnLeaveBroadcast.setOnClickListener(v -> leaveBroadcastSession());
        mBtnScanQrCode = mLayoutPreference.findViewById(R.id.button_scan_qr_code);
        mBtnScanQrCode.setOnClickListener(v -> launchQrCodeScanner());

        updateHeaderLayout();
    }

    private boolean isBroadcastSourceExist() {
        return mBroadcastSourceList.getPreferenceCount() > 0;
    }

    private void updateHeaderLayout() {
        if (isBroadcastSourceExist()) {
            mBtnFindBroadcast.setVisibility(View.GONE);
            mBtnBroadcastLayout.setVisibility(View.VISIBLE);
        } else {
            mBtnFindBroadcast.setVisibility(View.VISIBLE);
            mBtnBroadcastLayout.setVisibility(View.GONE);
        }

        mBtnLeaveBroadcast.setEnabled(false);
        if (mBluetoothFindBroadcastsFragment != null && mCachedDevice != null) {
            LocalBluetoothLeBroadcastAssistant broadcastAssistant =
                    mBluetoothFindBroadcastsFragment.getLeBroadcastAssistant();
            if (broadcastAssistant != null
                    && broadcastAssistant.getConnectionStatus(mCachedDevice.getDevice())
                    == BluetoothProfile.STATE_CONNECTED) {
                mBtnLeaveBroadcast.setEnabled(true);
            }
        }
    }

    private void scanBroadcastSource() {
        // TODO(b/231543455) : Using the BluetoothDeviceUpdater to refactor it.
        if (mBluetoothFindBroadcastsFragment == null) {
            return;
        }
        mBluetoothFindBroadcastsFragment.scanBroadcastSource();
    }

    private void leaveBroadcastSession() {
        if (mBluetoothFindBroadcastsFragment == null) {
            return;
        }
        mBluetoothFindBroadcastsFragment.leaveBroadcastSession();
    }

    private void launchQrCodeScanner() {
        final Intent intent = new Intent(mContext, QrCodeScanModeActivity.class);
        intent.setAction(BluetoothBroadcastUtils.ACTION_BLUETOOTH_LE_AUDIO_QR_CODE_SCANNER)
                .putExtra(BluetoothBroadcastUtils.EXTRA_BLUETOOTH_SINK_IS_GROUP, true)
                .putExtra(BluetoothBroadcastUtils.EXTRA_BLUETOOTH_DEVICE_SINK,
                        mCachedDevice.getDevice());
        mBluetoothFindBroadcastsFragment.startActivityForResult(intent,
                BluetoothFindBroadcastsFragment.REQUEST_SCAN_BT_BROADCAST_QR_CODE);
    }

    @Override
    public void onDeviceAttributesChanged() {
        if (mCachedDevice != null) {
            refresh();
        }
    }

    @Override
    public String getPreferenceKey() {
        return KEY_BROADCAST_HEADER;
    }

    /**
     * Updates the UI
     */
    public void refreshUi() {
        updateHeaderLayout();
    }
}
