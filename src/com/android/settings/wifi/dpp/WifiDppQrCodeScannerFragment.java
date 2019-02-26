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
import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.net.wifi.EasyConnectStatusCallback;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProviders;

import com.android.settings.R;
import com.android.settings.wifi.WifiDialogActivity;
import com.android.settings.wifi.qrcode.QrCamera;
import com.android.settings.wifi.qrcode.QrDecorateView;

import java.util.List;

public class WifiDppQrCodeScannerFragment extends WifiDppQrCodeBaseFragment implements
        SurfaceTextureListener,
        QrCamera.ScannerCallback,
        WifiManager.ActionListener {
    private static final String TAG = "WifiDppQrCodeScannerFragment";

    /** Message sent to hide error message */
    private static final int MESSAGE_HIDE_ERROR_MESSAGE = 1;

    /** Message sent to show error message */
    private static final int MESSAGE_SHOW_ERROR_MESSAGE = 2;

    /** Message sent to manipulate Wi-Fi DPP QR code */
    private static final int MESSAGE_SCAN_WIFI_DPP_SUCCESS = 3;

    /** Message sent to manipulate ZXing Wi-Fi QR code */
    private static final int MESSAGE_SCAN_ZXING_WIFI_FORMAT_SUCCESS = 4;

    private static final long SHOW_ERROR_MESSAGE_INTERVAL = 10000;
    private static final long SHOW_SUCCESS_SQUARE_INTERVAL = 1000;

    // Key for Bundle usage
    private static final String KEY_IS_CONFIGURATOR_MODE = "key_is_configurator_mode";
    private static final String KEY_LATEST_ERROR_CODE = "key_latest_error_code";
    private static final String KEY_WIFI_CONFIGURATION = "key_wifi_configuration";

    private ProgressBar mProgressBar;
    private QrCamera mCamera;
    private TextureView mTextureView;
    private QrDecorateView mDecorateView;
    private TextView mErrorMessage;

    /** true if the fragment working for configurator, false enrollee*/
    private boolean mIsConfiguratorMode;

    /** The SSID of the Wi-Fi network which the user specify to enroll */
    private String mSsid;

    /** QR code data scanned by camera */
    private WifiQrCode mWifiQrCode;

    /** The WifiConfiguration connecting for enrollee usage */
    private WifiConfiguration mWifiConfiguration;

    private int mLatestStatusCode = WifiDppUtils.EASY_CONNECT_EVENT_FAILURE_NONE;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mIsConfiguratorMode = savedInstanceState.getBoolean(KEY_IS_CONFIGURATOR_MODE);
            mLatestStatusCode = savedInstanceState.getInt(KEY_LATEST_ERROR_CODE);
            mWifiConfiguration = savedInstanceState.getParcelable(KEY_WIFI_CONFIGURATION);
        }

        final WifiDppInitiatorViewModel model =
                ViewModelProviders.of(this).get(WifiDppInitiatorViewModel.class);

        model.getEnrolleeSuccessNetworkId().observe(this, networkId -> {
            // After configuration change, observe callback will be triggered,
            // do nothing for this case if a handshake does not end
            if (model.isGoingInitiator()) {
                return;
            }

            new EasyConnectEnrolleeStatusCallback().onEnrolleeSuccess(networkId.intValue());
        });

        model.getStatusCode().observe(this, statusCode -> {
            // After configuration change, observe callback will be triggered,
            // do nothing for this case if a handshake does not end
            if (model.isGoingInitiator()) {
                return;
            }

            int code = statusCode.intValue();
            Log.d(TAG, "Easy connect enrollee callback onFailure " + code);
            new EasyConnectEnrolleeStatusCallback().onFailure(code);
        });
    }

    @Override
    public int getMetricsCategory() {
        if (mIsConfiguratorMode) {
            return SettingsEnums.SETTINGS_WIFI_DPP_CONFIGURATOR;
        } else {
            return SettingsEnums.SETTINGS_WIFI_DPP_ENROLLEE;
        }
    }

    // Container Activity must implement this interface
    public interface OnScanWifiDppSuccessListener {
        public void onScanWifiDppSuccess(WifiQrCode wifiQrCode);
    }
    OnScanWifiDppSuccessListener mScanWifiDppSuccessListener;

    /**
     * Configurator container activity of the fragment should create instance with this constructor.
     */
    public WifiDppQrCodeScannerFragment() {
        super();

        mIsConfiguratorMode = true;
    }

    /**
     * Enrollee container activity of the fragment should create instance with this constructor and
     * specify the SSID string of the WI-Fi network to be provisioned.
     */
    public WifiDppQrCodeScannerFragment(String ssid) {
        super();

        mIsConfiguratorMode = false;
        mSsid = ssid;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final ActionBar actionBar = getActivity().getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.show();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mScanWifiDppSuccessListener = (OnScanWifiDppSuccessListener) context;
    }

    @Override
    public void onDetach() {
        mScanWifiDppSuccessListener = null;

        super.onDetach();
    }

    @Override
    public final View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.wifi_dpp_qrcode_scanner_fragment, container,
                /* attachToRoot */ false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mTextureView = (TextureView) view.findViewById(R.id.preview_view);
        mTextureView.setSurfaceTextureListener(this);

        mDecorateView = (QrDecorateView) view.findViewById(R.id.decorate_view);

        setHeaderIconImageResource(R.drawable.ic_scan_24dp);

        mProgressBar = view.findViewById(R.id.indeterminate_bar);
        mProgressBar.setVisibility(isGoingInitiator() ? View.VISIBLE : View.INVISIBLE);

        if (mIsConfiguratorMode) {
            mTitle.setText(R.string.wifi_dpp_add_device_to_network);

            WifiNetworkConfig wifiNetworkConfig = ((WifiNetworkConfig.Retriever) getActivity())
                .getWifiNetworkConfig();
            if (!WifiNetworkConfig.isValidConfig(wifiNetworkConfig)) {
                throw new IllegalStateException("Invalid Wi-Fi network for configuring");
            }
            mSummary.setText(getString(R.string.wifi_dpp_center_qr_code,
                    wifiNetworkConfig.getSsid()));
        } else {
            mTitle.setText(R.string.wifi_dpp_scan_qr_code);

            updateEnrolleeSummary();
        }

        mErrorMessage = view.findViewById(R.id.error_message);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.removeItem(Menu.FIRST);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        initCamera(surface);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // Do nothing
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        destroyCamera();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Do nothing
    }

    @Override
    public Size getViewSize() {
        return new Size(mTextureView.getWidth(), mTextureView.getHeight());
    }

    @Override
    public Rect getFramePosition(Size previewSize, int cameraOrientation) {
        return new Rect(0, 0, previewSize.getHeight(), previewSize.getHeight());
    }

    @Override
    public void setTransform(Matrix transform) {
        mTextureView.setTransform(transform);
    }

    @Override
    public boolean isValid(String qrCode) {
        try {
            mWifiQrCode = new WifiQrCode(qrCode);
        } catch (IllegalArgumentException e) {
            mHandler.sendEmptyMessage(MESSAGE_SHOW_ERROR_MESSAGE);
            return false;
        }

        final String scheme = mWifiQrCode.getScheme();

        // When SSID is specified for enrollee, avoid to connect to the Wi-Fi of different SSID
        if (!mIsConfiguratorMode && WifiQrCode.SCHEME_ZXING_WIFI_NETWORK_CONFIG.equals(scheme)) {
            final String ssidQrCode = mWifiQrCode.getWifiNetworkConfig().getSsid();
            if (!TextUtils.isEmpty(mSsid) && !mSsid.equals(ssidQrCode)) {
                mHandler.sendEmptyMessage(MESSAGE_SHOW_ERROR_MESSAGE);
                return false;
            }
        }

        // It's impossible to provision other device with ZXing Wi-Fi Network config format
        if (mIsConfiguratorMode && WifiQrCode.SCHEME_ZXING_WIFI_NETWORK_CONFIG.equals(scheme)) {
            mHandler.sendEmptyMessage(MESSAGE_SHOW_ERROR_MESSAGE);
            return false;
        }

        return true;
    }

    /**
     * This method is only called when QrCamera.ScannerCallback.isValid returns true;
     */
    @Override
    public void handleSuccessfulResult(String qrCode) {
        switch (mWifiQrCode.getScheme()) {
            case WifiQrCode.SCHEME_DPP:
                handleWifiDpp();
                break;

            case WifiQrCode.SCHEME_ZXING_WIFI_NETWORK_CONFIG:
                handleZxingWifiFormat();
                break;

            default:
                // continue below
        }
    }

    private void handleWifiDpp() {
        if (mCamera != null) {
            mCamera.stop();
        }
        mDecorateView.setFocused(true);

        Message message = mHandler.obtainMessage(MESSAGE_SCAN_WIFI_DPP_SUCCESS);
        message.obj = new WifiQrCode(mWifiQrCode.getQrCode());

        mHandler.sendMessageDelayed(message, SHOW_SUCCESS_SQUARE_INTERVAL);
    }

    private void handleZxingWifiFormat() {
        if (mCamera != null) {
            mCamera.stop();
        }
        mDecorateView.setFocused(true);

        Message message = mHandler.obtainMessage(MESSAGE_SCAN_ZXING_WIFI_FORMAT_SUCCESS);
        message.obj = new WifiQrCode(mWifiQrCode.getQrCode()).getWifiNetworkConfig();

        mHandler.sendMessageDelayed(message, SHOW_SUCCESS_SQUARE_INTERVAL);
    }

    @Override
    public void handleCameraFailure() {
        destroyCamera();
    }

    private void initCamera(SurfaceTexture surface) {
        // Check if the camera has already created.
        if (mCamera == null) {
            mCamera = new QrCamera(getContext(), this);

            if (isGoingInitiator()) {
                if (mDecorateView != null) {
                    mDecorateView.setFocused(true);
                }
            } else {
                mCamera.start(surface);
            }
        }
    }

    private void destroyCamera() {
        if (mCamera != null) {
            mCamera.stop();
            mCamera = null;
        }
    }

    public void showErrorMessage(String message) {
        mErrorMessage.setVisibility(View.VISIBLE);
        mErrorMessage.setText(message);

        mHandler.removeMessages(MESSAGE_HIDE_ERROR_MESSAGE);
        mHandler.sendEmptyMessageDelayed(MESSAGE_HIDE_ERROR_MESSAGE,
                SHOW_ERROR_MESSAGE_INTERVAL);
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_HIDE_ERROR_MESSAGE:
                    mErrorMessage.setVisibility(View.INVISIBLE);
                    break;

                case MESSAGE_SHOW_ERROR_MESSAGE:
                    showErrorMessage(getString(R.string.wifi_dpp_could_not_detect_valid_qr_code));
                    break;

                case MESSAGE_SCAN_WIFI_DPP_SUCCESS:
                    mErrorMessage.setVisibility(View.INVISIBLE);

                    if (mScanWifiDppSuccessListener == null) {
                        return;
                    }
                    mScanWifiDppSuccessListener.onScanWifiDppSuccess((WifiQrCode)msg.obj);

                    if (!mIsConfiguratorMode) {
                        mProgressBar.setVisibility(View.VISIBLE);
                        startWifiDppEnrolleeInitiator((WifiQrCode)msg.obj);
                        updateEnrolleeSummary();
                    }
                    break;

                case MESSAGE_SCAN_ZXING_WIFI_FORMAT_SUCCESS:
                    mErrorMessage.setVisibility(View.INVISIBLE);

                    final WifiNetworkConfig wifiNetworkConfig = (WifiNetworkConfig)msg.obj;
                    mWifiConfiguration = wifiNetworkConfig.getWifiConfigurationOrNull();
                    wifiNetworkConfig.connect(getContext(),
                            /* listener */ WifiDppQrCodeScannerFragment.this);
                    break;

                default:
                    return;
            }
        }
    };

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(KEY_IS_CONFIGURATOR_MODE, mIsConfiguratorMode);
        outState.putInt(KEY_LATEST_ERROR_CODE, mLatestStatusCode);
        outState.putParcelable(KEY_WIFI_CONFIGURATION, mWifiConfiguration);

        super.onSaveInstanceState(outState);
    }

    private class EasyConnectEnrolleeStatusCallback extends EasyConnectStatusCallback {
        @Override
        public void onEnrolleeSuccess(int newNetworkId) {

            // Connect to the new network.
            final WifiManager wifiManager = getContext().getSystemService(WifiManager.class);
            final List<WifiConfiguration> wifiConfigs =
                    wifiManager.getPrivilegedConfiguredNetworks();
            for (WifiConfiguration wifiConfig : wifiConfigs) {
                if (wifiConfig.networkId == newNetworkId) {
                    mLatestStatusCode = WifiDppUtils.EASY_CONNECT_EVENT_SUCCESS;
                    mWifiConfiguration = wifiConfig;
                    wifiManager.connect(wifiConfig, WifiDppQrCodeScannerFragment.this);
                    return;
                }
            }

            Log.e(TAG, "Invalid networkId " + newNetworkId);
            mLatestStatusCode = EasyConnectStatusCallback.EASY_CONNECT_EVENT_FAILURE_GENERIC;
            updateEnrolleeSummary();
            mProgressBar.setVisibility(View.INVISIBLE);
            showErrorMessage(getString(R.string.wifi_dpp_check_connection_try_again));
            restartCamera();
        }

        @Override
        public void onConfiguratorSuccess(int code) {
            // Do nothing
        }

        @Override
        public void onFailure(int code) {
            Log.d(TAG, "EasyConnectEnrolleeStatusCallback.onFailure " + code);

            switch (code) {
                case EasyConnectStatusCallback.EASY_CONNECT_EVENT_FAILURE_INVALID_URI:
                    showErrorMessage(getString(R.string.wifi_dpp_could_not_detect_valid_qr_code));
                    break;

                case EasyConnectStatusCallback.EASY_CONNECT_EVENT_FAILURE_AUTHENTICATION:
                    showErrorMessage(
                            getString(R.string.wifi_dpp_failure_authentication_or_configuration));
                    break;

                case EasyConnectStatusCallback.EASY_CONNECT_EVENT_FAILURE_NOT_COMPATIBLE:
                    showErrorMessage(getString(R.string.wifi_dpp_failure_not_compatible));
                    break;

                case EasyConnectStatusCallback.EASY_CONNECT_EVENT_FAILURE_CONFIGURATION:
                    showErrorMessage(
                            getString(R.string.wifi_dpp_failure_authentication_or_configuration));
                    break;

                case EasyConnectStatusCallback.EASY_CONNECT_EVENT_FAILURE_BUSY:
                    if (code == mLatestStatusCode) {
                        throw(new IllegalStateException("stopEasyConnectSession and try again for"
                                + "EASY_CONNECT_EVENT_FAILURE_BUSY but still failed"));
                    }

                    mLatestStatusCode = code;
                    final WifiManager wifiManager =
                        getContext().getSystemService(WifiManager.class);
                    wifiManager.stopEasyConnectSession();
                    startWifiDppEnrolleeInitiator(mWifiQrCode);
                    return;

                case EasyConnectStatusCallback.EASY_CONNECT_EVENT_FAILURE_TIMEOUT:
                    showErrorMessage(getString(R.string.wifi_dpp_failure_timeout));
                    break;

                case EasyConnectStatusCallback.EASY_CONNECT_EVENT_FAILURE_GENERIC:
                    showErrorMessage(getString(R.string.wifi_dpp_failure_generic));
                    break;

                case EasyConnectStatusCallback.EASY_CONNECT_EVENT_FAILURE_NOT_SUPPORTED:
                    throw(new IllegalStateException("EASY_CONNECT_EVENT_FAILURE_NOT_SUPPORTED" +
                            " should be a configurator only error"));

                case EasyConnectStatusCallback.EASY_CONNECT_EVENT_FAILURE_INVALID_NETWORK:
                    throw(new IllegalStateException("EASY_CONNECT_EVENT_FAILURE_INVALID_NETWORK" +
                            " should be a configurator only error"));

                default:
                    throw(new IllegalStateException("Unexpected Wi-Fi DPP error"));
            }

            mLatestStatusCode = code;
            updateEnrolleeSummary();
            mProgressBar.setVisibility(View.INVISIBLE);
            restartCamera();
        }

        @Override
        public void onProgress(int code) {
            // Do nothing
        }
    }

    private void startWifiDppEnrolleeInitiator(WifiQrCode wifiQrCode) {
        final WifiDppInitiatorViewModel model =
                ViewModelProviders.of(this).get(WifiDppInitiatorViewModel.class);

        model.startEasyConnectAsEnrolleeInitiator(wifiQrCode.getQrCode());
    }

    @Override
    public void onSuccess() {
        final Intent resultIntent = new Intent();
        resultIntent.putExtra(WifiDialogActivity.KEY_WIFI_CONFIGURATION, mWifiConfiguration);

        final Activity hostActivity = getActivity();
        hostActivity.setResult(Activity.RESULT_OK, resultIntent);
        hostActivity.finish();
    }

    @Override
    public void onFailure(int reason) {
        Log.d(TAG, "Wi-Fi connect onFailure reason - " + reason);

        mProgressBar.setVisibility(View.INVISIBLE);
        showErrorMessage(getString(R.string.wifi_dpp_check_connection_try_again));
        restartCamera();
    }

    // Check is Easy Connect handshaking or not
    private boolean isGoingInitiator() {
        final WifiDppInitiatorViewModel model =
                ViewModelProviders.of(this).get(WifiDppInitiatorViewModel.class);

        return model.isGoingInitiator();
    }

    /**
     * To resume camera decoding task after handshake fail or Wi-Fi connection fail.
     */
    private void restartCamera() {
        if (mCamera == null) {
            Log.d(TAG, "mCamera is not available for restarting camera");
            return;
        }

        final SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
        if (surfaceTexture == null) {
            throw new IllegalStateException("SurfaceTexture is not ready for restarting camera");
        }

        mCamera.start(surfaceTexture);
    }

    private void updateEnrolleeSummary() {
        if (isGoingInitiator()) {
            mSummary.setText(R.string.wifi_dpp_connecting);
        } else {
            String description;
            if (TextUtils.isEmpty(mSsid)) {
                description = getString(R.string.wifi_dpp_scan_qr_code_join_unknown_network, mSsid);
            } else {
                description = getString(R.string.wifi_dpp_scan_qr_code_join_network, mSsid);
            }
            mSummary.setText(description);
        }
    }
}
