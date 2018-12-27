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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;

/**
 * After getting Wi-Fi network information and(or) QR code, this fragment config a device to connect
 * to the Wi-Fi network.
 */
public class WifiDppAddDeviceFragment extends WifiDppQrCodeBaseFragment {
    private ProgressBar mProgressBar;
    private ImageView mWifiApPictureView;
    private TextView mChooseDifferentNetwork;
    private Button mButtonLeft;
    private Button mButtonRight;

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.SETTINGS_WIFI_DPP_CONFIGURATOR;
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

        mProgressBar = view.findViewById(R.id.progress_bar);
        mWifiApPictureView = view.findViewById(R.id.wifi_ap_picture_view);
        mChooseDifferentNetwork = view.findViewById(R.id.choose_different_network);
        mButtonLeft = view.findViewById(R.id.button_left);
        mButtonRight = view.findViewById(R.id.button_right);
    }
}
