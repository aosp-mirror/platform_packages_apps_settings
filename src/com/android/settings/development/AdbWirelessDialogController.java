/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.development;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.widget.TextView;

import com.android.settings.R;

/**
 * The class for allowing UIs like {@link AdbWirelessDialog} and {@link AdbWirelessDialogUiBase} to
 * share the logic for controlling buttons, text fields, etc.
 */
public class AdbWirelessDialogController {
    private static final String TAG = "AdbWirelessDialogCtrl";

    private final AdbWirelessDialogUiBase mUi;
    private final View mView;

    private int mMode;

    // The dialog for showing the six-digit code
    private TextView mPairingCodeTitle;
    private TextView mSixDigitCode;
    private TextView mIpAddr;

    // The dialog for showing pairing failed message
    private TextView mFailedMsg;

    private Context mContext;

    public AdbWirelessDialogController(AdbWirelessDialogUiBase parent, View view,
            int mode) {
        mUi = parent;
        mView = view;
        mMode = mode;

        mContext = mUi.getContext();
        final Resources res = mContext.getResources();

        mSixDigitCode = mView.findViewById(R.id.pairing_code);
        mIpAddr = mView.findViewById(R.id.ip_addr);

        switch (mMode) {
            case AdbWirelessDialogUiBase.MODE_PAIRING:
                String title = res.getString(
                        com.android.settingslib.R.string.adb_pairing_device_dialog_title);
                mUi.setTitle(title);
                mView.findViewById(R.id.l_pairing_six_digit).setVisibility(View.VISIBLE);
                mUi.setCancelButton(res.getString(R.string.cancel));
                mUi.setCanceledOnTouchOutside(false);
                break;
            case AdbWirelessDialogUiBase.MODE_PAIRING_FAILED:
                String msg = res.getString(
                        com.android.settingslib.R.string.adb_pairing_device_dialog_failed_msg);
                mUi.setTitle(
                        com.android.settingslib.R.string.adb_pairing_device_dialog_failed_title);
                mView.findViewById(R.id.l_pairing_failed).setVisibility(View.VISIBLE);
                mFailedMsg = (TextView) mView.findViewById(R.id.pairing_failed_label);
                mFailedMsg.setText(msg);
                mUi.setSubmitButton(res.getString(R.string.okay));
                break;
            case AdbWirelessDialogUiBase.MODE_QRCODE_FAILED:
                mUi.setTitle(
                        com.android.settingslib.R.string.adb_pairing_device_dialog_failed_title);
                mView.findViewById(R.id.l_qrcode_pairing_failed).setVisibility(View.VISIBLE);
                mUi.setSubmitButton(res.getString(R.string.okay));
                break;
        }

        // After done view show and hide, request focus from parent view
        mView.findViewById(R.id.l_adbwirelessdialog).requestFocus();
    }

    /**
     * Set the pairing code UI text field to code.
     *
     * @param code the pairing code string
     */
    public void setPairingCode(String code) {
        mSixDigitCode.setText(code);
    }

    /**
     * Set the Ip address UI text field to ipAddr.
     *
     * @param ipAddr the ip address string
     */
    public void setIpAddr(String ipAddr) {
        mIpAddr.setText(ipAddr);
    }
}
