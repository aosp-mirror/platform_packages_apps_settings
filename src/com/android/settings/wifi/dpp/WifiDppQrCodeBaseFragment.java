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
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.core.InstrumentedFragment;
import com.android.settings.R;

/**
 * There are below 4 fragments for Wi-Fi DPP UI flow, to reduce redundant code of UI components,
 * this parent fragment instantiates common UI components
 *
 * {@code WifiDppQrCodeScannerFragment}
 * {@code WifiDppQrCodeGeneratorFragment}
 * {@code WifiDppChooseSavedWifiNetworkFragment}
 * {@code WifiDppAddDeviceFragment}
 */
public abstract class WifiDppQrCodeBaseFragment extends InstrumentedFragment {
    protected ImageView mHeaderIcon;
    protected TextView mTitle;
    protected TextView mSummary;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mHeaderIcon = view.findViewById(android.R.id.icon);
        mTitle = view.findViewById(android.R.id.title);
        mSummary = view.findViewById(android.R.id.summary);
    }
}
