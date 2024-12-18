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

import static android.os.UserManager.DISALLOW_ADD_WIFI_CONFIG;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.UserManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.wifi.dpp.WifiDppQrCodeScannerFragment;
import com.android.settings.wifi.dpp.WifiDppUtils;

/**
 * A full screen UI component for users to edit and add a Wi-Fi network.
 */
public class AddNetworkFragment extends InstrumentedFragment implements WifiConfigUiBase2,
        View.OnClickListener {
    private static final String TAG = "AddNetworkFragment";

    public static final String WIFI_CONFIG_KEY = "wifi_config_key";
    @VisibleForTesting
    final static int SUBMIT_BUTTON_ID = android.R.id.button1;
    @VisibleForTesting
    final static int CANCEL_BUTTON_ID = android.R.id.button2;
    final static int SSID_SCANNER_BUTTON_ID = R.id.ssid_scanner_button;

    private static final int REQUEST_CODE_WIFI_DPP_ENROLLEE_QR_CODE_SCANNER = 0;

    private static final String EXTRA_SAVE_WHEN_SUBMIT = ":settings:save_when_submit";

    private WifiConfigController2 mUIController;
    private Button mSubmitBtn;
    private Button mCancelBtn;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isAddWifiConfigAllowed(getContext())) {
            getActivity().finish();
            return;
        }
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
        mUIController = new WifiConfigController2(this, rootView, null, getMode());

        // Resize the layout when keyboard opens.
        getActivity().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mUIController.showSecurityFields(
            /* refreshEapMethods */ false, /* refreshCertificates */ true);
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
            startActivityForResult(
                    WifiDppUtils.getEnrolleeQrCodeScannerIntent(view.getContext(), ssid),
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
                    WifiDppQrCodeScannerFragment.KEY_WIFI_CONFIGURATION);
            successfullyFinish(config);
        }
    }

    @Override
    public int getMode() {
        return WifiConfigUiBase2.MODE_CONNECT;
    }

    @Override
    public WifiConfigController2 getController() {
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
        Activity activity = getActivity();
        boolean autoSave = activity.getIntent().getBooleanExtra(EXTRA_SAVE_WHEN_SUBMIT, false);
        if (autoSave && config != null) {
            WifiManager.ActionListener saveListener = new WifiManager.ActionListener() {
                @Override
                public void onSuccess() {
                    if (activity != null && !activity.isFinishing()) {
                        activity.setResult(Activity.RESULT_OK);
                        activity.finish();
                    }
                }

                @Override
                public void onFailure(int reason) {
                    if (activity != null && !activity.isFinishing()) {
                        Toast.makeText(activity, R.string.wifi_failed_save_message,
                                Toast.LENGTH_SHORT).show();
                        activity.finish();
                    }
                }
            };

            activity.getSystemService(WifiManager.class).save(config, saveListener);
        } else {
            Intent intent = new Intent();
            intent.putExtra(WIFI_CONFIG_KEY, config);
            activity.setResult(Activity.RESULT_OK, intent);
            activity.finish();
        }
    }

    @VisibleForTesting
    void handleCancelAction() {
        final Activity activity = getActivity();
        activity.setResult(Activity.RESULT_CANCELED);
        activity.finish();
    }

    @VisibleForTesting
    static boolean isAddWifiConfigAllowed(Context context) {
        UserManager userManager = context.getSystemService(UserManager.class);
        if (userManager != null && userManager.hasUserRestriction(DISALLOW_ADD_WIFI_CONFIG)) {
            Log.e(TAG, "The user is not allowed to add Wi-Fi configuration.");
            return false;
        }
        return true;
    }
}
