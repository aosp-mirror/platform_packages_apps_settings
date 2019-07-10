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

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.net.wifi.EasyConnectStatusCallback;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.ImageView;

import androidx.lifecycle.ViewModelProviders;

import com.android.settings.R;

import java.util.concurrent.Executor;

/**
 * After getting Wi-Fi network information and(or) QR code, this fragment config a device to connect
 * to the Wi-Fi network.
 */
public class WifiDppAddDeviceFragment extends WifiDppQrCodeBaseFragment {
    private static final String TAG = "WifiDppAddDeviceFragment";

    private ImageView mWifiApPictureView;
    private Button mChooseDifferentNetwork;

    private int mLatestStatusCode = WifiDppUtils.EASY_CONNECT_EVENT_FAILURE_NONE;

    // Key for Bundle usage
    private static final String KEY_LATEST_ERROR_CODE = "key_latest_error_code";

    private class EasyConnectConfiguratorStatusCallback extends EasyConnectStatusCallback {
        @Override
        public void onEnrolleeSuccess(int newNetworkId) {
            // Do nothing
        }

        @Override
        public void onConfiguratorSuccess(int code) {
            showSuccessUi(/* isConfigurationChange */ false);
        }

        @Override
        public void onFailure(int code) {
            Log.d(TAG, "EasyConnectConfiguratorStatusCallback.onFailure " + code);

            showErrorUi(code, /* isConfigurationChange */ false);
        }

        @Override
        public void onProgress(int code) {
            // Do nothing
        }
    }

    private void showSuccessUi(boolean isConfigurationChange) {
        setHeaderIconImageResource(R.drawable.ic_devices_check_circle_green_32dp);
        setHeaderTitle(R.string.wifi_dpp_wifi_shared_with_device);
        setProgressBarShown(isGoingInitiator());
        mSummary.setVisibility(View.INVISIBLE);
        mWifiApPictureView.setImageResource(R.drawable.wifi_dpp_success);
        mChooseDifferentNetwork.setVisibility(View.INVISIBLE);
        mLeftButton.setText(getContext(), R.string.wifi_dpp_add_another_device);
        mLeftButton.setOnClickListener(v -> getFragmentManager().popBackStack());
        mRightButton.setText(getContext(), R.string.done);
        mRightButton.setOnClickListener(v -> {
            final Activity activity = getActivity();
            activity.setResult(Activity.RESULT_OK);
            activity.finish();
        });
        mRightButton.setVisibility(View.VISIBLE);

        if (!isConfigurationChange) {
            mLatestStatusCode = WifiDppUtils.EASY_CONNECT_EVENT_SUCCESS;
        }
    }

    private void showErrorUi(int code, boolean isConfigurationChange) {
        CharSequence summaryCharSequence;
        switch (code) {
            case EasyConnectStatusCallback.EASY_CONNECT_EVENT_FAILURE_INVALID_URI:
                summaryCharSequence = getText(R.string.wifi_dpp_qr_code_is_not_valid_format);
                break;

            case EasyConnectStatusCallback.EASY_CONNECT_EVENT_FAILURE_AUTHENTICATION:
                summaryCharSequence = getText(
                        R.string.wifi_dpp_failure_authentication_or_configuration);
                break;

            case EasyConnectStatusCallback.EASY_CONNECT_EVENT_FAILURE_NOT_COMPATIBLE:
                summaryCharSequence = getText(R.string.wifi_dpp_failure_not_compatible);
                break;

            case EasyConnectStatusCallback.EASY_CONNECT_EVENT_FAILURE_CONFIGURATION:
                summaryCharSequence = getText(
                        R.string.wifi_dpp_failure_authentication_or_configuration);
                break;

            case EasyConnectStatusCallback.EASY_CONNECT_EVENT_FAILURE_BUSY:
                if (isConfigurationChange) {
                    return;
                }

                if (code == mLatestStatusCode) {
                    throw(new IllegalStateException("Tried restarting EasyConnectSession but still"
                            + "receiving EASY_CONNECT_EVENT_FAILURE_BUSY"));
                }

                mLatestStatusCode = code;
                final WifiManager wifiManager =
                        getContext().getSystemService(WifiManager.class);
                wifiManager.stopEasyConnectSession();
                startWifiDppConfiguratorInitiator();
                return;

            case EasyConnectStatusCallback.EASY_CONNECT_EVENT_FAILURE_TIMEOUT:
                summaryCharSequence = getText(R.string.wifi_dpp_failure_timeout);
                break;

            case EasyConnectStatusCallback.EASY_CONNECT_EVENT_FAILURE_GENERIC:
                summaryCharSequence = getText(R.string.wifi_dpp_failure_generic);
                break;

            case EasyConnectStatusCallback.EASY_CONNECT_EVENT_FAILURE_NOT_SUPPORTED:
                summaryCharSequence = getString(
                        R.string.wifi_dpp_failure_not_supported, getSsid());
                break;

            case EasyConnectStatusCallback.EASY_CONNECT_EVENT_FAILURE_INVALID_NETWORK:
                throw(new IllegalStateException("Wi-Fi DPP configurator used a non-PSK/non-SAE"
                        + "network to handshake"));

            default:
                throw(new IllegalStateException("Unexpected Wi-Fi DPP error"));
        }

        setHeaderTitle(R.string.wifi_dpp_could_not_add_device);
        mSummary.setText(summaryCharSequence);
        mWifiApPictureView.setImageResource(R.drawable.wifi_dpp_error);
        mChooseDifferentNetwork.setVisibility(View.INVISIBLE);
        if (hasRetryButton(code)) {
            mRightButton.setText(getContext(), R.string.retry);
        } else {
            mRightButton.setText(getContext(), R.string.done);
            mRightButton.setOnClickListener(v -> getActivity().finish());
            mLeftButton.setVisibility(View.INVISIBLE);
        }

        if (isGoingInitiator()) {
            mSummary.setText(R.string.wifi_dpp_sharing_wifi_with_this_device);
        }

        setProgressBarShown(isGoingInitiator());
        mRightButton.setVisibility(isGoingInitiator() ? View.INVISIBLE : View.VISIBLE);

        if (!isConfigurationChange) {
            mLatestStatusCode = code;
        }
    }

    private boolean hasRetryButton(int code) {
        switch (code) {
            case EasyConnectStatusCallback.EASY_CONNECT_EVENT_FAILURE_INVALID_URI:
            case EasyConnectStatusCallback.EASY_CONNECT_EVENT_FAILURE_NOT_COMPATIBLE:
                return false;

            default:
                break;
        }

        return true;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_WIFI_DPP_CONFIGURATOR;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mLatestStatusCode = savedInstanceState.getInt(KEY_LATEST_ERROR_CODE);
        }

        final WifiDppInitiatorViewModel model =
                ViewModelProviders.of(this).get(WifiDppInitiatorViewModel.class);

        model.getStatusCode().observe(this, statusCode -> {
            // After configuration change, observe callback will be triggered,
            // do nothing for this case if a handshake does not end
            if (model.isGoingInitiator()) {
                return;
            }

            int code = statusCode.intValue();
            if (code == WifiDppUtils.EASY_CONNECT_EVENT_SUCCESS) {
                new EasyConnectConfiguratorStatusCallback().onConfiguratorSuccess(code);
            } else {
                new EasyConnectConfiguratorStatusCallback().onFailure(code);
            }
        });
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

        setHeaderIconImageResource(R.drawable.ic_devices_other_32dp);

        final WifiQrCode wifiQrCode = ((WifiDppConfiguratorActivity) getActivity())
                .getWifiDppQrCode();
        final String information = wifiQrCode.getInformation();
        if (TextUtils.isEmpty(information)) {
            setHeaderTitle(R.string.wifi_dpp_device_found);
        } else {
            setHeaderTitle(information);
        }

        updateSummary();
        mWifiApPictureView = view.findViewById(R.id.wifi_ap_picture_view);

        mChooseDifferentNetwork = view.findViewById(R.id.choose_different_network);
        mChooseDifferentNetwork.setOnClickListener(v ->
            mClickChooseDifferentNetworkListener.onClickChooseDifferentNetwork()
        );

        mLeftButton.setText(getContext(), R.string.cancel);
        mLeftButton.setOnClickListener(v -> getActivity().finish());

        mRightButton.setText(getContext(), R.string.wifi_dpp_share_wifi);
        mRightButton.setOnClickListener(v -> {
            setProgressBarShown(true);
            mRightButton.setVisibility(View.INVISIBLE);
            startWifiDppConfiguratorInitiator();
            updateSummary();
        });

        if (savedInstanceState != null) {
            if (mLatestStatusCode == WifiDppUtils.EASY_CONNECT_EVENT_SUCCESS) {
                showSuccessUi(/* isConfigurationChange */ true);
            } else if (mLatestStatusCode == WifiDppUtils.EASY_CONNECT_EVENT_FAILURE_NONE) {
                setProgressBarShown(isGoingInitiator());
                mRightButton.setVisibility(isGoingInitiator() ? View.INVISIBLE : View.VISIBLE);
            } else {
                showErrorUi(mLatestStatusCode, /* isConfigurationChange */ true);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_LATEST_ERROR_CODE, mLatestStatusCode);

        super.onSaveInstanceState(outState);
    }

    private String getSsid() {
        final WifiNetworkConfig wifiNetworkConfig = ((WifiDppConfiguratorActivity) getActivity())
                .getWifiNetworkConfig();
        if (!WifiNetworkConfig.isValidConfig(wifiNetworkConfig)) {
            throw new IllegalStateException("Invalid Wi-Fi network for configuring");
        }
        return wifiNetworkConfig.getSsid();
    }

    private void startWifiDppConfiguratorInitiator() {
        final WifiQrCode wifiQrCode = ((WifiDppConfiguratorActivity) getActivity())
                .getWifiDppQrCode();
        final String qrCode = wifiQrCode.getQrCode();
        final int networkId =
                ((WifiDppConfiguratorActivity) getActivity()).getWifiNetworkConfig().getNetworkId();
        final WifiDppInitiatorViewModel model =
                ViewModelProviders.of(this).get(WifiDppInitiatorViewModel.class);

        model.startEasyConnectAsConfiguratorInitiator(qrCode, networkId);
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

    // Check is Easy Connect handshaking or not
    private boolean isGoingInitiator() {
        final WifiDppInitiatorViewModel model =
                ViewModelProviders.of(this).get(WifiDppInitiatorViewModel.class);

        return model.isGoingInitiator();
    }

    private void updateSummary() {
        if (isGoingInitiator()) {
            mSummary.setText(R.string.wifi_dpp_sharing_wifi_with_this_device);
        } else {
            mSummary.setText(getString(R.string.wifi_dpp_add_device_to_wifi, getSsid()));
        }
    }

    /**
     * This fragment will change UI display and text messages for events. To improve Talkback user
     * experienience, using this method to focus on a right component and announce a changed text
     * after an UI changing event.
     *
     * @param focusView The UI component which will be focused
     * @param announceView The UI component's text will be talked
     */
    private void changeFocusAndAnnounceChange(View focusView, View announceView) {
        focusView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
        announceView.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    @Override
    protected boolean isFooterAvailable() {
        return true;
    }
}
