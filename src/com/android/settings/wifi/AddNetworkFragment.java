/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.wifi;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.wifi.dpp.WifiDppUtils;

public class AddNetworkFragment extends InstrumentedFragment implements WifiConfigUiBase,
        View.OnClickListener {

    final static String WIFI_CONFIG_KEY = "wifi_config_key";
    @VisibleForTesting
    final static int SUBMIT_BUTTON_ID = android.R.id.button1;
    @VisibleForTesting
    final static int CANCEL_BUTTON_ID = android.R.id.button2;
    final static int SSID_SCANNER_BUTTON_ID = R.id.ssid_scanner_button;

    private static final int REQUEST_CODE_WIFI_DPP_ENROLLEE_QR_CODE_SCANNER = 0;

    private WifiConfigController mUIController;
    private Button mSubmitBtn;
    private Button mCancelBtn;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_WIFI_ADD_NETWORK;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.wifi_add_network_view, container, false);

        final Button neutral = rootView.findViewById(android.R.id.button3);
        if (neutral != null) {
            neutral.setVisibility(View.GONE);
        }

        mSubmitBtn = rootView.findViewById(SUBMIT_BUTTON_ID);
        mCancelBtn = rootView.findViewById(CANCEL_BUTTON_ID);
        final ImageButton ssidScannerButton = rootView.findViewById(SSID_SCANNER_BUTTON_ID);
        mSubmitBtn.setOnClickListener(this);
        mCancelBtn.setOnClickListener(this);
        ssidScannerButton.setOnClickListener(this);
        mUIController = new WifiConfigController(this, rootView, null, getMode());

        return rootView;
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        mUIController.updatePassword();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == SUBMIT_BUTTON_ID) {
            handleSubmitAction();
        } else if (view.getId() == CANCEL_BUTTON_ID) {
            handleCancelAction();
        } else if (view.getId() == SSID_SCANNER_BUTTON_ID) {
            final TextView ssidEditText = getView().findViewById(R.id.ssid);
            final String ssid = ssidEditText.getText().toString();

            // Launch QR code scanner to join a network.
            startActivityForResult(WifiDppUtils.getEnrolleeQrCodeScannerIntent(ssid),
                    REQUEST_CODE_WIFI_DPP_ENROLLEE_QR_CODE_SCANNER);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_WIFI_DPP_ENROLLEE_QR_CODE_SCANNER) {
            if (resultCode != Activity.RESULT_OK) {
                return;
            }

            final WifiConfiguration config = data.getParcelableExtra(
                    WifiDialogActivity.KEY_WIFI_CONFIGURATION);
            successfullyFinish(config);
        }
    }

    @Override
    public int getMode() {
        return WifiConfigUiBase.MODE_CONNECT;
    }

    @Override
    public WifiConfigController getController() {
        return mUIController;
    }

    @Override
    public void dispatchSubmit() {
        handleSubmitAction();
    }

    @Override
    public void setTitle(int id) {
        getActivity().setTitle(id);
    }

    @Override
    public void setTitle(CharSequence title) {
        getActivity().setTitle(title);
    }

    @Override
    public void setSubmitButton(CharSequence text) {
        mSubmitBtn.setText(text);
    }

    @Override
    public void setCancelButton(CharSequence text) {
        mCancelBtn.setText(text);
    }

    @Override
    public void setForgetButton(CharSequence text) {
        // AddNetwork doesn't need forget button.
    }

    @Override
    public Button getSubmitButton() {
        return mSubmitBtn;
    }

    @Override
    public Button getCancelButton() {
        return mCancelBtn;
    }

    @Override
    public Button getForgetButton() {
        // AddNetwork doesn't need forget button.
        return null;
    }

    @VisibleForTesting
    void handleSubmitAction() {
        successfullyFinish(mUIController.getConfig());
    }

    private void successfullyFinish(WifiConfiguration config) {
        final Intent intent = new Intent();
        final Activity activity = getActivity();
        intent.putExtra(WIFI_CONFIG_KEY, config);
        activity.setResult(Activity.RESULT_OK, intent);
        activity.finish();
    }

    @VisibleForTesting
    void handleCancelAction() {
        final Activity activity = getActivity();
        activity.setResult(Activity.RESULT_CANCELED);
        activity.finish();
    }
}
