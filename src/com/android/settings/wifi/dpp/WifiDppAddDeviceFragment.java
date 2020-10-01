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

import static android.provider.Settings.EXTRA_EASY_CONNECT_ATTEMPTED_SSID;
import static android.provider.Settings.EXTRA_EASY_CONNECT_BAND_LIST;
import static android.provider.Settings.EXTRA_EASY_CONNECT_CHANNEL_LIST;
import static android.provider.Settings.EXTRA_EASY_CONNECT_ERROR_CODE;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.EasyConnectStatusCallback;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import androidx.lifecycle.ViewModelProviders;

import com.android.settings.R;

import com.google.android.setupcompat.template.FooterButton;

import org.json.JSONArray;
import org.json.JSONObject;

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
    private static final String KEY_LATEST_STATUS_CODE = "key_latest_status_code";

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
        public void onFailure(int code, String ssid, SparseArray<int[]> channelListArray,
                int[] operatingClassArray) {
            Log.d(TAG, "EasyConnectConfiguratorStatusCallback.onFailure: " + code);
            if (!TextUtils.isEmpty(ssid)) {
                Log.d(TAG, "Tried SSID: " + ssid);
            }
            if (channelListArray.size() != 0) {
                Log.d(TAG, "Tried channels: " + channelListArray);
            }
            if (operatingClassArray != null && operatingClassArray.length > 0) {
                StringBuilder sb = new StringBuilder("Supported bands: ");
                for (int i = 0; i < operatingClassArray.length; i++) {
                    sb.append(operatingClassArray[i] + " ");
                }
                Log.d(TAG, sb.toString());
            }

            showErrorUi(code, getResultIntent(code, ssid, channelListArray,
                    operatingClassArray), /* isConfigurationChange */ false);
        }

        @Override
        public void onProgress(int code) {
            // Do nothing
        }
    }

    private void showSuccessUi(boolean isConfigurationChange) {
        setHeaderIconImageResource(R.drawable.ic_devices_check_circle_green_32dp);
        setHeaderTitle(R.string.wifi_dpp_wifi_shared_with_device);
        setProgressBarShown(isEasyConnectHandshaking());
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

    private Intent getResultIntent(int code, String ssid, SparseArray<int[]> channelListArray,
            int[] operatingClassArray) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_EASY_CONNECT_ERROR_CODE, code);

        if (!TextUtils.isEmpty(ssid)) {
            intent.putExtra(EXTRA_EASY_CONNECT_ATTEMPTED_SSID, ssid);
        }
        if (channelListArray != null && channelListArray.size() != 0) {
            int key;
            int index = 0;
            JSONObject formattedChannelList = new JSONObject();

            // Build a JSON array of operating classes, with an array of channels for each
            // operating class.
            do {
                try {
                    key = channelListArray.keyAt(index);
                } catch (java.lang.ArrayIndexOutOfBoundsException e) {
                    break;
                }
                JSONArray channelsInClassArray = new JSONArray();

                int[] output = channelListArray.get(key);
                for (int i = 0; i < output.length; i++) {
                    channelsInClassArray.put(output[i]);
                }
                try {
                    formattedChannelList.put(Integer.toString(key), channelsInClassArray);
                } catch (org.json.JSONException e) {
                    formattedChannelList = new JSONObject();
                    break;
                }
                index++;
            } while (true);

            intent.putExtra(EXTRA_EASY_CONNECT_CHANNEL_LIST,
                    formattedChannelList.toString());
        }
        if (operatingClassArray != null && operatingClassArray.length != 0) {
            intent.putExtra(EXTRA_EASY_CONNECT_BAND_LIST, operatingClassArray);
        }

        return intent;
    }

    private void showErrorUi(int code, Intent resultIntent, boolean isConfigurationChange) {
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

            case EasyConnectStatusCallback.EASY_CONNECT_EVENT_FAILURE_CANNOT_FIND_NETWORK:
                summaryCharSequence = getText(R.string.wifi_dpp_failure_cannot_find_network);
                break;

            case EasyConnectStatusCallback.EASY_CONNECT_EVENT_FAILURE_ENROLLEE_AUTHENTICATION:
                summaryCharSequence = getText(R.string.wifi_dpp_failure_enrollee_authentication);
                break;

            case EasyConnectStatusCallback
                    .EASY_CONNECT_EVENT_FAILURE_ENROLLEE_REJECTED_CONFIGURATION:
                summaryCharSequence =
                        getText(R.string.wifi_dpp_failure_enrollee_rejected_configuration);
                break;

            default:
                throw(new IllegalStateException("Unexpected Wi-Fi DPP error"));
        }

        setHeaderTitle(R.string.wifi_dpp_could_not_add_device);
        mSummary.setText(summaryCharSequence);
        mWifiApPictureView.setImageResource(R.drawable.wifi_dpp_error);
        mChooseDifferentNetwork.setVisibility(View.INVISIBLE);
        FooterButton finishingButton = mLeftButton;
        if (hasRetryButton(code)) {
            mRightButton.setText(getContext(), R.string.retry);
        } else {
            mRightButton.setText(getContext(), R.string.done);
            finishingButton = mRightButton;
            mLeftButton.setVisibility(View.INVISIBLE);
        }
        finishingButton.setOnClickListener(v -> {
            getActivity().setResult(Activity.RESULT_CANCELED, resultIntent);
            getActivity().finish();
        });

        if (isEasyConnectHandshaking()) {
            mSummary.setText(R.string.wifi_dpp_sharing_wifi_with_this_device);
        }

        setProgressBarShown(isEasyConnectHandshaking());
        mRightButton.setVisibility(isEasyConnectHandshaking() ? View.INVISIBLE : View.VISIBLE);

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
            mLatestStatusCode = savedInstanceState.getInt(KEY_LATEST_STATUS_CODE);
        }

        final WifiDppInitiatorViewModel model =
                ViewModelProviders.of(this).get(WifiDppInitiatorViewModel.class);

        model.getStatusCode().observe(this, statusCode -> {
            // After configuration change, observe callback will be triggered,
            // do nothing for this case if a handshake does not end
            if (model.isWifiDppHandshaking()) {
                return;
            }

            int code = statusCode.intValue();
            if (code == WifiDppUtils.EASY_CONNECT_EVENT_SUCCESS) {
                new EasyConnectConfiguratorStatusCallback().onConfiguratorSuccess(code);
            } else {
                new EasyConnectConfiguratorStatusCallback().onFailure(code, model.getTriedSsid(),
                        model.getTriedChannels(), model.getBandArray());
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
                setProgressBarShown(isEasyConnectHandshaking());
                mRightButton.setVisibility(isEasyConnectHandshaking() ?
                        View.INVISIBLE : View.VISIBLE);
            } else {
                showErrorUi(mLatestStatusCode, /* reslutIntent */ null, /* isConfigurationChange */
                        true);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_LATEST_STATUS_CODE, mLatestStatusCode);

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
        void onClickChooseDifferentNetwork();
    }
    private OnClickChooseDifferentNetworkListener mClickChooseDifferentNetworkListener;

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
    private boolean isEasyConnectHandshaking() {
        final WifiDppInitiatorViewModel model =
                ViewModelProviders.of(this).get(WifiDppInitiatorViewModel.class);

        return model.isWifiDppHandshaking();
    }

    private void updateSummary() {
        if (isEasyConnectHandshaking()) {
            mSummary.setText(R.string.wifi_dpp_sharing_wifi_with_this_device);
        } else {
            mSummary.setText(getString(R.string.wifi_dpp_add_device_to_wifi, getSsid()));
        }
    }

    @Override
    protected boolean isFooterAvailable() {
        return true;
    }
}
