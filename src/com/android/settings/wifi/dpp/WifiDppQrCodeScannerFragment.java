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

import android.annotation.Nullable;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Size;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.wifi.qrcode.QrCamera;
import com.android.settings.wifi.qrcode.QrDecorateView;

public class WifiDppQrCodeScannerFragment extends WifiDppQrCodeBaseFragment implements
        SurfaceTextureListener,
        QrCamera.ScannerCallback {
    private static final String TAG = "WifiDppQrCodeScannerFragment";

    /** Message sent to hide error message */
    private static final int MESSAGE_HIDE_ERROR_MESSAGE = 1;

    /** Message sent to show error message */
    private static final int MESSAGE_SHOW_ERROR_MESSAGE = 2;

    /** Message sent to manipulate Wi-Fi DPP QR code */
    private static final int MESSAGE_SCAN_WIFI_DPP_SUCCESS = 3;

    /** Message sent to manipulate ZXing Wi-Fi QR code */
    private static final int MESSAGE_SCAN_ZXING_WIFI_FORMAT_SUCCESS = 4;

    private static final long SHOW_ERROR_MESSAGE_INTERVAL = 2000;
    private static final long SHOW_SUCCESS_SQUARE_INTERVAL = 1000;

    // Keys for Bundle usage
    private static final String KEY_PUBLIC_KEY = "key_public_key";
    private static final String KEY_INFORMATION = "key_information";

    private QrCamera mCamera;
    private TextureView mTextureView;
    private QrDecorateView mDecorateView;

    /** true if the fragment working for configurator, false enrollee*/
    private final boolean mIsConfiguratorMode;

    /** The SSID of the Wi-Fi network which the user specify to enroll */
    private String mSsid;

    /** QR code data scanned by camera */
    private WifiQrCode mWifiQrCode;

    @Override
    protected int getLayout() {
        return R.layout.wifi_dpp_qrcode_scanner_fragment;
    }

    @Override
    public int getMetricsCategory() {
        if (mIsConfiguratorMode) {
            return MetricsProto.MetricsEvent.SETTINGS_WIFI_DPP_CONFIGURATOR;
        } else {
            return MetricsProto.MetricsEvent.SETTINGS_WIFI_DPP_ENROLLEE;
        }
    }

    // Container Activity must implement this interface
    public interface OnScanWifiDppSuccessListener {
        public void onScanWifiDppSuccess(String publicKey, String information);
    }
    OnScanWifiDppSuccessListener mScanWifiDppSuccessListener;

    // Container Activity must implement this interface
    public interface OnScanZxingWifiFormatSuccessListener {
        public void onScanZxingWifiFormatSuccess(WifiNetworkConfig wifiNetworkConfig);
    }
    OnScanZxingWifiFormatSuccessListener mScanScanZxingWifiFormatSuccessListener;

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

        setHeaderIconImageResource(R.drawable.ic_scan_24dp);

        if (mIsConfiguratorMode) {
            setTitle(getString(R.string.wifi_dpp_add_device_to_network));

            WifiNetworkConfig wifiNetworkConfig = ((WifiNetworkConfig.Retriever) getActivity())
                .getWifiNetworkConfig();
            if (!WifiNetworkConfig.isValidConfig(wifiNetworkConfig)) {
                throw new IllegalArgumentException("Invalid Wi-Fi network for configuring");
            }
            setDescription(getString(R.string.wifi_dpp_center_qr_code, wifiNetworkConfig.getSsid()));
        } else {
            setTitle(getString(R.string.wifi_dpp_scan_qr_code));

            String description;
            if (TextUtils.isEmpty(mSsid)) {
                description = getString(R.string.wifi_dpp_scan_qr_code_join_unknown_network, mSsid);
            } else {
                description = getString(R.string.wifi_dpp_scan_qr_code_join_network, mSsid);
            }
            setDescription(description);
        }

        final ActionBar actionBar = getActivity().getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.show();
        }

        setErrorMessage(getString(R.string.wifi_dpp_could_not_detect_valid_qr_code));
        showErrorMessage(false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mScanWifiDppSuccessListener = (OnScanWifiDppSuccessListener) context;
        mScanScanZxingWifiFormatSuccessListener = (OnScanZxingWifiFormatSuccessListener) context;
    }

    @Override
    public void onDetach() {
        mScanWifiDppSuccessListener = null;
        mScanScanZxingWifiFormatSuccessListener = null;

        super.onDetach();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mTextureView = (TextureView) view.findViewById(R.id.preview_view);
        mTextureView.setSurfaceTextureListener(this);

        mDecorateView = (QrDecorateView) view.findViewById(R.id.decorate_view);
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
                handleWifiDpp(mWifiQrCode.getPublicKey(), mWifiQrCode.getInformation());
                break;

            case WifiQrCode.SCHEME_ZXING_WIFI_NETWORK_CONFIG:
                handleZxingWifiFormat(mWifiQrCode.getWifiNetworkConfig());
                break;

            default:
                // continue below
        }
    }

    private void handleWifiDpp(String publicKey, String information) {
        destroyCamera();
        mDecorateView.setFocused(true);

        final Bundle bundle = new Bundle();
        bundle.putString(KEY_PUBLIC_KEY, publicKey);
        bundle.putString(KEY_INFORMATION, information);

        Message message = mHandler.obtainMessage(MESSAGE_SCAN_WIFI_DPP_SUCCESS);
        message.setData(bundle);

        mHandler.sendMessageDelayed(message, SHOW_SUCCESS_SQUARE_INTERVAL);
    }

    private void handleZxingWifiFormat(WifiNetworkConfig wifiNetworkConfig) {
        destroyCamera();
        mDecorateView.setFocused(true);

        Message message = mHandler.obtainMessage(MESSAGE_SCAN_ZXING_WIFI_FORMAT_SUCCESS);
        message.obj = wifiNetworkConfig;

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
            mCamera.start(surface);
        }
    }

    private void destroyCamera() {
        if (mCamera != null) {
            mCamera.stop();
            mCamera = null;
        }
    }

    @Override
    public void showErrorMessage(boolean show) {
        super.showErrorMessage(show);

        if (show) {
            mHandler.removeMessages(MESSAGE_HIDE_ERROR_MESSAGE);
            mHandler.sendEmptyMessageDelayed(MESSAGE_HIDE_ERROR_MESSAGE,
                    SHOW_ERROR_MESSAGE_INTERVAL);
        }
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_HIDE_ERROR_MESSAGE:
                    showErrorMessage(false);
                    break;

                case MESSAGE_SHOW_ERROR_MESSAGE:
                    showErrorMessage(true);
                    break;

                case MESSAGE_SCAN_WIFI_DPP_SUCCESS:
                    if (mScanWifiDppSuccessListener == null) {
                        return;
                    }
                    final Bundle bundle = msg.getData();
                    final String publicKey = bundle.getString(KEY_PUBLIC_KEY);
                    final String information = bundle.getString(KEY_INFORMATION);

                    mScanWifiDppSuccessListener.onScanWifiDppSuccess(publicKey, information);
                    break;

                case MESSAGE_SCAN_ZXING_WIFI_FORMAT_SUCCESS:
                    if (mScanScanZxingWifiFormatSuccessListener == null) {
                        return;
                    }
                    mScanScanZxingWifiFormatSuccessListener.onScanZxingWifiFormatSuccess(
                            (WifiNetworkConfig)msg.obj);
                    break;

                default:
                    return;
            }
        }
    };
}
