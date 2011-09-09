/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings.wifi.p2p;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.settings.R;

/**
 * Dialog to setup a p2p connection
 */
public class WifiP2pDialog extends AlertDialog implements AdapterView.OnItemSelectedListener {

    static final int BUTTON_SUBMIT = DialogInterface.BUTTON_POSITIVE;

    private final DialogInterface.OnClickListener mListener;

    private View mView;
    private TextView mDeviceName;
    private TextView mDeviceAddress;

    /* These values come from "wifi_p2p_wps_setup" resource array */
    private static final int WPS_PBC = 0;
    private static final int WPS_KEYPAD = 1;
    private static final int WPS_DISPLAY = 2;

    private int mWpsSetupIndex = WPS_PBC; //default is pbc

    WifiP2pDevice mDevice;

    public WifiP2pDialog(Context context, DialogInterface.OnClickListener listener,
            WifiP2pDevice device) {
        super(context);
        mListener = listener;
        mDevice = device;
    }

    public WifiP2pConfig getConfig() {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = mDeviceAddress.getText().toString();
        config.wps = new WpsInfo();
        switch (mWpsSetupIndex) {
            case WPS_PBC:
                config.wps.setup = WpsInfo.PBC;
                break;
            case WPS_KEYPAD:
                config.wps.setup = WpsInfo.KEYPAD;
                config.wps.pin = ((TextView) mView.findViewById(R.id.wps_pin)).
                        getText().toString();
                break;
            case WPS_DISPLAY:
                config.wps.setup = WpsInfo.DISPLAY;
                break;
            default:
                config.wps.setup = WpsInfo.PBC;
                break;
        }
        return config;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        mView = getLayoutInflater().inflate(R.layout.wifi_p2p_dialog, null);
        Spinner mWpsSetup = ((Spinner) mView.findViewById(R.id.wps_setup));

        setView(mView);
        setInverseBackgroundForced(true);

        Context context = getContext();

        setTitle(R.string.wifi_p2p_settings_title);
        mDeviceName = (TextView) mView.findViewById(R.id.device_name);
        mDeviceAddress = (TextView) mView.findViewById(R.id.device_address);

        setButton(BUTTON_SUBMIT, context.getString(R.string.wifi_connect), mListener);
        setButton(DialogInterface.BUTTON_NEGATIVE,
                    context.getString(R.string.wifi_cancel), mListener);

        if (mDevice != null) {
            mDeviceName.setText(mDevice.deviceName);
            mDeviceAddress.setText(mDevice.deviceAddress);
            mWpsSetup.setSelection(mWpsSetupIndex); //keep pbc as default
       }

        mWpsSetup.setOnItemSelectedListener(this);

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mWpsSetupIndex = position;

        if (mWpsSetupIndex == WPS_KEYPAD) {
            mView.findViewById(R.id.wps_pin_entry).setVisibility(View.VISIBLE);
        } else {
            mView.findViewById(R.id.wps_pin_entry).setVisibility(View.GONE);
        }
        return;
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

}
