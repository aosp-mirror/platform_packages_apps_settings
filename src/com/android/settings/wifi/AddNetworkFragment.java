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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

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
    final static int SCANNER_BUTTON_ID = R.id.ssid_scanner_button;

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

        mSubmitBtn = rootView.findViewById(SUBMIT_BUTTON_ID);
        mCancelBtn = rootView.findViewById(CANCEL_BUTTON_ID);
        final ImageButton scannerButton = rootView.findViewById(SCANNER_BUTTON_ID);
        mSubmitBtn.setOnClickListener(this);
        mCancelBtn.setOnClickListener(this);
        scannerButton.setOnClickListener(this);
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
        switch (view.getId()) {
            case SUBMIT_BUTTON_ID:
                handleSubmitAction();
                break;
            case CANCEL_BUTTON_ID:
                handleCancelAction();
                break;
            case SCANNER_BUTTON_ID:
                // Launch QR code scanner to join a network.
                getContext().startActivity(
                        WifiDppUtils.getEnrolleeQrCodeScannerIntent(/* ssid */ null));
                break;
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
        final Intent intent = new Intent();
        final Activity activity = getActivity();
        intent.putExtra(WIFI_CONFIG_KEY, mUIController.getConfig());
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
