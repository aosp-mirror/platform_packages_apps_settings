/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.app.ActionBar;
import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.core.InstrumentedFragment;
import com.android.settingslib.wifi.AccessPoint;

/**
 * Detail page for configuring Wi-Fi network.
 *
 * The AccessPoint should be saved to the argument when launching this class in order to properly
 * render this page.
 *
 * Migrating from Wi-Fi SettingsLib to to WifiTrackerLib, this object will be removed in the near
 * future, please develop in {@link ConfigureWifiEntryFragment}.
 */
public class ConfigureAccessPointFragment extends InstrumentedFragment implements WifiConfigUiBase {

    public static final String NETWORK_CONFIG_KEY = "network_config_key";

    private static final int SUBMIT_BUTTON_ID = android.R.id.button1;
    private static final int CANCEL_BUTTON_ID = android.R.id.button2;

    private WifiConfigController mUiController;
    private Button mSubmitBtn;
    private Button mCancelBtn;
    private AccessPoint mAccessPoint;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mAccessPoint = new AccessPoint(context, getArguments());
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_WIFI_CONFIGURE_NETWORK;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.wifi_add_network_view,
                container, false /* attachToRoot */);

        final Button neutral = rootView.findViewById(android.R.id.button3);
        if (neutral != null) {
            neutral.setVisibility(View.GONE);
        }

        mSubmitBtn = rootView.findViewById(SUBMIT_BUTTON_ID);
        mCancelBtn = rootView.findViewById(CANCEL_BUTTON_ID);
        mSubmitBtn.setOnClickListener(view -> handleSubmitAction());
        mCancelBtn.setOnClickListener(view -> handleCancelAction());

        mUiController = new WifiConfigController(this, rootView, mAccessPoint,
                getMode(), false /* requestFocus */);

        /**
         * For this add AccessPoint UI, need to remove the Home button, so set related feature as
         * false.
         */
        final ActionBar actionBar = getActivity().getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setHomeButtonEnabled(false);
            actionBar.setDisplayShowHomeEnabled(false);
        }

        return rootView;
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        mUiController.updatePassword();
    }

    @Override
    public int getMode() {
        return WifiConfigUiBase.MODE_CONNECT;
    }

    @Override
    public WifiConfigController getController() {
        return mUiController;
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
        intent.putExtra(NETWORK_CONFIG_KEY, mUiController.getConfig());
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
