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

import com.android.settings.R;
import com.android.settings.core.InstrumentedFragment;

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
    private ImageView mHeaderIcon;
    private ImageView mDevicesCheckCircleGreenHeaderIcon;
    protected TextView mTitle;
    protected TextView mSummary;
    protected View mTitleSummaryContainer;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mHeaderIcon = view.findViewById(android.R.id.icon);
        mDevicesCheckCircleGreenHeaderIcon =
                view.findViewById(R.id.devices_check_circle_green_icon);
        mTitle = view.findViewById(android.R.id.title);
        mSummary = view.findViewById(android.R.id.summary);

        // This is the LinearLayout which groups mTitle and mSummary for Talkback to announce the
        // content in a way that reflects its natural groupings.
        mTitleSummaryContainer =  view.findViewById(R.id.title_summary_container);
    }

    protected void setHeaderIconImageResource(int resId) {
        // ic_devices_check_circle_green is a LayerDrawable,
        // it has different size from other VectorDrawable icons
        if (resId == R.drawable.ic_devices_check_circle_green) {
            mHeaderIcon.setVisibility(View.GONE);
            mDevicesCheckCircleGreenHeaderIcon.setVisibility(View.VISIBLE);
        } else {
            mDevicesCheckCircleGreenHeaderIcon.setVisibility(View.GONE);
            mHeaderIcon.setImageResource(resId);
            mHeaderIcon.setVisibility(View.VISIBLE);
        }
    }
}
