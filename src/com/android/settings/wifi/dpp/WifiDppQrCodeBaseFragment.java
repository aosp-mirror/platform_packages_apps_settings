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
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.settings.core.InstrumentedFragment;
import com.android.settings.R;

/**
 * TODO: b/120645817 should refine code to only initiate UI component in each child fragment.
 */

/**
 * There are below 4 fragments for Wi-Fi DPP UI flow, to reduce redundant code of UI components,
 * this parent fragment instantiates all UI components and provides setting APIs for them.
 *
 * {@code WifiDppQrCodeScannerFragment}
 * {@code WifiDppQrCodeGeneratorFragment}
 * {@code WifiDppChooseSavedWifiNetworkFragment}
 * {@code WifiDppAddDeviceFragment}
 */
public abstract class WifiDppQrCodeBaseFragment extends InstrumentedFragment {
    private ImageView mHeaderIcon;
    private TextView mTitle;
    private TextView mDescription;

    private TextView mErrorMessage;         //optional, for WifiDppQrCodeScannerFragment
    private ListView mSavedWifiNetworkList; //optional, for WifiDppChooseSavedWifiNetworkFragment
    private ProgressBar mProgressBar;       //optional, for WifiDppAddDeviceFragment
    private ImageView mWifiApPictureView;   //optional, for WifiDppAddDeviceFragment
    private TextView mChooseDifferentNetwork;//optional, for WifiDppAddDeviceFragment

    private Button mButtonLeft;             //optional, for WifiDppChooseSavedWifiNetworkFragment,
                                            //              WifiDppAddDeviceFragment
    private Button mButtonRight;            //optional, for WifiDppChooseSavedWifiNetworkFragment,
                                            //              WifiDppAddDeviceFragment

    abstract protected int getLayout();

    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public final View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(getLayout(), container, false);
        initView(view);
        return view;
    }

    private void initView(View view) {
        mHeaderIcon = view.findViewById(R.id.header_icon);
        mTitle = view.findViewById(R.id.title);
        mDescription = view.findViewById(R.id.description);
        mErrorMessage = view.findViewById(R.id.error_message);

        mSavedWifiNetworkList = view.findViewById(R.id.saved_wifi_network_list);

        mProgressBar = view.findViewById(R.id.progress_bar);
        mWifiApPictureView = view.findViewById(R.id.wifi_ap_picture_view);
        mChooseDifferentNetwork = view.findViewById(R.id.choose_different_network);

        mButtonLeft = view.findViewById(R.id.button_left);
        mButtonRight = view.findViewById(R.id.button_right);
    }

    protected void setHeaderIconImageResource(int resId) {
        mHeaderIcon.setImageResource(resId);
    }

    protected void setTitle(String title) {
        mTitle.setText(title);
    }

    protected void setDescription(String description) {
        mDescription.setText(description);
    }

    /** optional, for WifiDppQrCodeScannerFragment */
    protected void showErrorMessage(boolean show) {
        if (mErrorMessage != null) {
            mErrorMessage.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        }
    }

    /** optional, for WifiDppQrCodeScannerFragment */
    protected void setErrorMessage(String errorMessage) {
        if (mErrorMessage != null) {
            mErrorMessage.setText(errorMessage);
        }
    }

    /**
     * optional, for WifiDppChooseSavedWifiNetworkFragment,
     *               WifiDppAddDeviceFragment
     */
    protected void setLeftButtonText(String text) {
        if (mButtonLeft != null) {
            mButtonLeft.setText(text);
        }
    }

    /**
     * optional, for WifiDppChooseSavedWifiNetworkFragment,
     *               WifiDppAddDeviceFragment
     */
    protected void setRightButtonText(String text) {
        if (mButtonRight != null) {
            mButtonRight.setText(text);
        }
    }

    /**
     * optional, for WifiDppChooseSavedWifiNetworkFragment,
     *               WifiDppAddDeviceFragment
     */
    protected void hideLeftButton() {
        if (mButtonLeft != null) {
            mButtonLeft.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * optional, for WifiDppChooseSavedWifiNetworkFragment,
     *               WifiDppAddDeviceFragment
     */
    protected void hideRightButton() {
        if (mButtonRight != null) {
            mButtonRight.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * optional, for WifiDppChooseSavedWifiNetworkFragment,
     *               WifiDppAddDeviceFragment
     */
    protected void setLeftButtonOnClickListener(View.OnClickListener listener) {
        if (mButtonLeft != null) {
            mButtonLeft.setOnClickListener(listener);
        }
    }

    /**
     * optional, for WifiDppChooseSavedWifiNetworkFragment,
     *               WifiDppAddDeviceFragment
     */
    protected void setRightButtonOnClickListener(View.OnClickListener listener) {
        if (mButtonRight != null) {
            mButtonRight.setOnClickListener(listener);
        }
    }
}
