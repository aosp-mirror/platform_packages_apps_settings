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

package com.android.settings.wifi.dpp;

import android.app.ActionBar;
import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.android.settings.R;

/**
 * After getting Wi-Fi network information and(or) QR code, this fragment config a device to connect
 * to the Wi-Fi network.
 */
public class WifiDppAddDeviceFragment extends WifiDppQrCodeBaseFragment {
    private static final String TAG = "WifiDppAddDeviceFragment";

    private ImageView mWifiApPictureView;
    private Button mChooseDifferentNetwork;
    private Button mButtonLeft;
    private Button mButtonRight;

    private class EasyConnectStatusCallback extends android.net.wifi.EasyConnectStatusCallback {
        @Override
        public void onEnrolleeSuccess(int newNetworkId) {
            // Do nothing
        }

        @Override
        public void onConfiguratorSuccess(int code) {
            // Update success UI.
            mTitle.setText(R.string.wifi_dpp_wifi_shared_with_device);
            mSummary.setVisibility(View.INVISIBLE);
            mChooseDifferentNetwork.setVisibility(View.INVISIBLE);
            mButtonLeft.setText(R.string.wifi_dpp_add_another_device);
            mButtonLeft.setOnClickListener(v -> getFragmentManager().popBackStack());
            mButtonRight.setText(R.string.done);
            mButtonRight.setOnClickListener(v -> getActivity().finish());
        }

        @Override
        public void onFailure(int code) {
            Log.d(TAG, "EasyConnectStatusCallback.onFailure " + code);

            // Update fail UI.
            mTitle.setText(R.string.wifi_dpp_could_not_add_device);
            mSummary.setVisibility(View.INVISIBLE);
            mChooseDifferentNetwork.setVisibility(View.INVISIBLE);
            mButtonRight.setText(R.string.retry);
        }

        @Override
        public void onProgress(int code) {
            // Do nothing
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_WIFI_DPP_CONFIGURATOR;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final ActionBar actionBar = getActivity().getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
    }

    @Override
    public final View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.wifi_dpp_add_device_fragment, container,
                /* attachToRoot */ false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final WifiQrCode wifiQrCode = ((WifiDppConfiguratorActivity) getActivity())
                .getWifiDppQrCode();
        final String information = wifiQrCode.getInformation();
        if (TextUtils.isEmpty(information)) {
            mTitle.setText(R.string.wifi_dpp_device_found);
        } else {
            mTitle.setText(information);
        }

        final WifiNetworkConfig wifiNetworkConfig = ((WifiDppConfiguratorActivity) getActivity())
                .getWifiNetworkConfig();
        if (!WifiNetworkConfig.isValidConfig(wifiNetworkConfig)) {
            throw new IllegalStateException("Invalid Wi-Fi network for configuring");
        }
        mSummary.setText(getString(R.string.wifi_dpp_add_device_to_wifi,
                wifiNetworkConfig.getSsid()));

        mWifiApPictureView = view.findViewById(R.id.wifi_ap_picture_view);

        mChooseDifferentNetwork = view.findViewById(R.id.choose_different_network);
        mChooseDifferentNetwork.setOnClickListener(v ->
                mClickChooseDifferentNetworkListener.onClickChooseDifferentNetwork());

        mButtonLeft = view.findViewById(R.id.button_left);
        mButtonLeft.setText(R.string.cancel);
        mButtonLeft.setOnClickListener(v -> {
            getActivity().setResult(Activity.RESULT_CANCELED);
            getActivity().finish();
        });

        mButtonRight = view.findViewById(R.id.button_right);
        mButtonRight.setText(R.string.wifi_dpp_share_wifi);
        mButtonRight.setOnClickListener(v -> startWifiDppInitiator());
    }

    private void startWifiDppInitiator() {
        final WifiQrCode wifiQrCode = ((WifiDppConfiguratorActivity) getActivity())
                .getWifiDppQrCode();
        final String qrCode = wifiQrCode.getQrCode();
        final int networkId =
                ((WifiDppConfiguratorActivity) getActivity()).getWifiNetworkConfig().getNetworkId();
        final WifiManager wifiManager = getContext().getSystemService(WifiManager.class);

        wifiManager.startEasyConnectAsConfiguratorInitiator(qrCode, networkId,
                WifiManager.EASY_CONNECT_NETWORK_ROLE_STA, /* handler */ null,
                new EasyConnectStatusCallback());
    }

    // Container Activity must implement this interface
    public interface OnClickChooseDifferentNetworkListener {
        public void onClickChooseDifferentNetwork();
    }
    OnClickChooseDifferentNetworkListener mClickChooseDifferentNetworkListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mClickChooseDifferentNetworkListener = (OnClickChooseDifferentNetworkListener) context;
    }

    @Override
    public void onDetach() {
        mClickChooseDifferentNetworkListener = null;

        super.onDetach();
    }
}
