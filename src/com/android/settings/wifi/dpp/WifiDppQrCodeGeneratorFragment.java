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
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.settings.R;

/**
 * After sharing a saved Wi-Fi network, {@code WifiDppConfiguratorActivity} start with this fragment
 * to generate a Wi-Fi DPP QR code for other device to initiate as an enrollee.
 */
public class WifiDppQrCodeGeneratorFragment extends WifiDppQrCodeBaseFragment {
    @Override
    protected int getLayout() {
        return R.layout.wifi_dpp_qrcode_generator_fragment;
    }

    // Container Activity must implement this interface
    public interface OnQrCodeGeneratorFragmentAddButtonClickedListener {
        public void onQrCodeGeneratorFragmentAddButtonClicked();
    }
    OnQrCodeGeneratorFragmentAddButtonClickedListener mListener;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        WifiNetworkConfig wifiNetworkConfig = ((WifiNetworkConfig.Retriever) getActivity())
                .getWifiNetworkConfig();
        if (!WifiNetworkConfig.isValidConfig(wifiNetworkConfig)) {
            throw new IllegalArgumentException("Invalid Wi-Fi network for configuring");
        }
        setTitle(getString(R.string.wifi_dpp_share_wifi));
        setDescription(getString(R.string.wifi_dpp_scan_qr_code_with_another_device,
                wifiNetworkConfig.getSsid()));

        setHasOptionsMenu(true);
        ActionBar actionBar = getActivity().getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.show();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mListener = (OnQrCodeGeneratorFragmentAddButtonClickedListener) context;
    }

    @Override
    public void onDetach() {
        mListener = null;

        super.onDetach();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem item = menu.add(0, Menu.FIRST, 0, R.string.next_label);
        item.setIcon(R.drawable.ic_menu_add);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case Menu.FIRST:
                mListener.onQrCodeGeneratorFragmentAddButtonClicked();
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }
}
