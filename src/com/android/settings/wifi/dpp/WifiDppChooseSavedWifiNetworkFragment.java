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
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.settings.R;

/**
 * After a camera APP scanned a Wi-Fi DPP QR code, it can trigger
 * {@code WifiDppConfiguratorActivity} to start with this fragment to choose a saved Wi-Fi network.
 */
public class WifiDppChooseSavedWifiNetworkFragment extends WifiDppQrCodeBaseFragment {
    private static final String TAG_FRAGMENT_WIFI_NETWORK_LIST = "wifi_network_list_fragment";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_WIFI_DPP_CONFIGURATOR;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Embedded WifiNetworkListFragment as child fragment within
        // WifiDppChooseSavedWifiNetworkFragment.
        final FragmentManager fragmentManager = getChildFragmentManager();
        final WifiNetworkListFragment fragment = new WifiNetworkListFragment();
        final Bundle args = getArguments();
        if (args != null) {
            fragment.setArguments(args);
        }
        final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.wifi_network_list_container, fragment,
                TAG_FRAGMENT_WIFI_NETWORK_LIST);
        fragmentTransaction.commit();
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

        setHeaderTitle(R.string.wifi_dpp_choose_network);
        mSummary.setText(R.string.wifi_dpp_choose_network_to_connect_device);

        mLeftButton.setText(getContext(), R.string.cancel);
        mLeftButton.setOnClickListener(v -> {
            String action = null;
            final Intent intent = getActivity().getIntent();
            if (intent != null) {
                action = intent.getAction();
            }
            if (WifiDppConfiguratorActivity.ACTION_CONFIGURATOR_QR_CODE_SCANNER.equals(action) ||
                    WifiDppConfiguratorActivity
                    .ACTION_CONFIGURATOR_QR_CODE_GENERATOR.equals(action)) {
                getFragmentManager().popBackStack();
            } else {
                getActivity().finish();
            }
        });

        mRightButton.setVisibility(View.GONE);
    }

    @Override
    protected boolean isFooterAvailable() {
        return true;
    }
}
