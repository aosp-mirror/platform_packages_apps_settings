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

import android.app.settings.SettingsEnums;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.wifi.qrcode.QrCodeGenerator;

import com.google.zxing.WriterException;

/**
 * After sharing a saved Wi-Fi network, {@code WifiDppConfiguratorActivity} start with this fragment
 * to generate a Wi-Fi DPP QR code for other device to initiate as an enrollee.
 */
public class WifiDppQrCodeGeneratorFragment extends WifiDppQrCodeBaseFragment {
    private static final String TAG = "WifiDppQrCodeGeneratorFragment";

    private ImageView mQrCodeView;
    private String mQrCode;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_WIFI_DPP_CONFIGURATOR;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // setTitle for TalkBack
        final WifiNetworkConfig wifiNetworkConfig = getWifiNetworkConfigFromHostActivity();
        if (wifiNetworkConfig.isHotspot()) {
            getActivity().setTitle(R.string.wifi_dpp_share_hotspot);
        } else {
            getActivity().setTitle(R.string.wifi_dpp_share_wifi);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        final MenuItem menuItem = menu.findItem(Menu.FIRST);
        if (menuItem != null) {
            menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public final View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.wifi_dpp_qrcode_generator_fragment, container,
                /* attachToRoot */ false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mQrCodeView = view.findViewById(R.id.qrcode_view);

        final WifiNetworkConfig wifiNetworkConfig = getWifiNetworkConfigFromHostActivity();
        if (wifiNetworkConfig.isHotspot()) {
            setHeaderTitle(R.string.wifi_dpp_share_hotspot);
        } else {
            setHeaderTitle(R.string.wifi_dpp_share_wifi);
        }

        final String password = wifiNetworkConfig.getPreSharedKey();
        TextView passwordView = view.findViewById(R.id.password);
        if (TextUtils.isEmpty(password)) {
            mSummary.setText(getString(
                    R.string.wifi_dpp_scan_open_network_qr_code_with_another_device,
                    wifiNetworkConfig.getSsid()));

            passwordView.setVisibility(View.GONE);
        } else {
            mSummary.setText(getString(R.string.wifi_dpp_scan_qr_code_with_another_device,
                    wifiNetworkConfig.getSsid()));

            if (wifiNetworkConfig.isHotspot()) {
                passwordView.setText(getString(R.string.wifi_dpp_hotspot_password, password));
            } else {
                passwordView.setText(getString(R.string.wifi_dpp_wifi_password, password));
            }
        }

        mQrCode = wifiNetworkConfig.getQrCode();
        setQrCode();
    }

    private void setQrCode() {
        try {
            final int qrcodeSize = getContext().getResources().getDimensionPixelSize(
                    R.dimen.qrcode_size);
            final Bitmap bmp = QrCodeGenerator.encodeQrCode(mQrCode, qrcodeSize);
            mQrCodeView.setImageBitmap(bmp);
        } catch (WriterException e) {
            Log.e(TAG, "Error generating QR code bitmap " + e);
        }
    }

    private WifiNetworkConfig getWifiNetworkConfigFromHostActivity() {
        final WifiNetworkConfig wifiNetworkConfig = ((WifiNetworkConfig.Retriever) getActivity())
                .getWifiNetworkConfig();
        if (!WifiNetworkConfig.isValidConfig(wifiNetworkConfig)) {
            throw new IllegalStateException("Invalid Wi-Fi network for configuring");
        }

        return wifiNetworkConfig;
    }

    @Override
    protected boolean isFooterAvailable() {
        return false;
    }
}
