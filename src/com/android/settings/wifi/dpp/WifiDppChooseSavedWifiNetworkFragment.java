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
import android.widget.ListView;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;

/**
 * After a camera APP scanned a Wi-Fi DPP QR code, it can trigger
 * {@code WifiDppConfiguratorActivity} to start with this fragment to choose a saved Wi-Fi network.
 */
public class WifiDppChooseSavedWifiNetworkFragment extends WifiDppQrCodeBaseFragment {
    private ListView mSavedWifiNetworkList;
    private Button mButtonLeft;
    private Button mButtonRight;

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.SETTINGS_WIFI_DPP_CONFIGURATOR;
    }

    @Override
    public final View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.wifi_dpp_choose_saved_wifi_network_fragment, container,
                /* attachToRoot */ false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mSavedWifiNetworkList = view.findViewById(R.id.saved_wifi_network_list);
        mButtonLeft = view.findViewById(R.id.button_left);
        mButtonRight = view.findViewById(R.id.button_right);
    }
}
