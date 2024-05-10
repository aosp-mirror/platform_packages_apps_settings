/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.development;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.debug.AdbManager;
import android.debug.IAdbManager;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;

import androidx.annotation.StringRes;

import com.android.settings.R;
import com.android.settings.SetupWizardUtils;
import com.android.settings.wifi.dpp.AdbQrCode;
import com.android.settings.wifi.dpp.WifiDppQrCodeBaseFragment;
import com.android.settings.wifi.dpp.WifiNetworkConfig;
import com.android.settingslib.qrcode.QrCamera;
import com.android.settingslib.qrcode.QrDecorateView;

import com.google.android.setupdesign.util.ThemeHelper;

/**
 * Fragment shown when clicking on the "Pair by QR code" preference in
 * the Wireless Debugging fragment.
 */
public class AdbQrcodeScannerFragment extends WifiDppQrCodeBaseFragment implements
        SurfaceTextureListener,
        QrCamera.ScannerCallback {
    private static final String TAG = "AdbQrcodeScannerFrag";

    /** Message sent to hide error message */
    private static final int MESSAGE_HIDE_ERROR_MESSAGE = 1;

    /** Message sent to show error message */
    private static final int MESSAGE_SHOW_ERROR_MESSAGE = 2;

    private static final long SHOW_ERROR_MESSAGE_INTERVAL = 10000;
    private static final long SHOW_SUCCESS_SQUARE_INTERVAL = 1000;

    private QrCamera mCamera;
    private TextureView mTextureView;
    private QrDecorateView mDecorateView;
    private View mQrCameraView;
    private View mVerifyingView;
    private TextView mVerifyingTextView;
    private TextView mErrorMessage;

    /** QR code data scanned by camera */
    private AdbQrCode mAdbQrCode;
    private WifiNetworkConfig mAdbConfig;

    private IAdbManager mAdbManager;

    private IntentFilter mIntentFilter;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (AdbManager.WIRELESS_DEBUG_PAIRING_RESULT_ACTION.equals(action)) {
                Integer res = intent.getIntExtra(
                        AdbManager.WIRELESS_STATUS_EXTRA,
                        AdbManager.WIRELESS_STATUS_FAIL);
                if (res.equals(AdbManager.WIRELESS_STATUS_SUCCESS)) {
                    Intent i = new Intent();
                    i.putExtra(
                            WirelessDebuggingFragment.PAIRING_DEVICE_REQUEST_TYPE,
                            WirelessDebuggingFragment.SUCCESS_ACTION);
                    getActivity().setResult(Activity.RESULT_OK, i);
                    getActivity().finish();
                } else if (res.equals(AdbManager.WIRELESS_STATUS_FAIL)) {
                    Intent i = new Intent();
                    i.putExtra(
                            WirelessDebuggingFragment.PAIRING_DEVICE_REQUEST_TYPE,
                            WirelessDebuggingFragment.FAIL_ACTION);
                    getActivity().setResult(Activity.RESULT_OK, i);
                    getActivity().finish();
                } else if (res.equals(AdbManager.WIRELESS_STATUS_CONNECTED)) {
                    int port = intent.getIntExtra(AdbManager.WIRELESS_DEBUG_PORT_EXTRA, 0);
                    Log.i(TAG, "Got Qr pairing code port=" + port);
                }
            }
        }
    };

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_HIDE_ERROR_MESSAGE:
                    mErrorMessage.setVisibility(View.INVISIBLE);
                    break;

                case MESSAGE_SHOW_ERROR_MESSAGE:
                    final String errorMessage = (String) msg.obj;

                    mErrorMessage.setVisibility(View.VISIBLE);
                    mErrorMessage.setText(errorMessage);
                    mErrorMessage.sendAccessibilityEvent(
                            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);

                    // Cancel any pending messages to hide error view and requeue the message so
                    // user has time to see error
                    removeMessages(MESSAGE_HIDE_ERROR_MESSAGE);
                    sendEmptyMessageDelayed(MESSAGE_HIDE_ERROR_MESSAGE,
                            SHOW_ERROR_MESSAGE_INTERVAL);
                    break;

                default:
                    return;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Context context = getContext();
        context.setTheme(SetupWizardUtils.getTheme(context, getActivity().getIntent()));
        ThemeHelper.trySetDynamicColor(getContext());
        super.onCreate(savedInstanceState);

        mIntentFilter = new IntentFilter(AdbManager.WIRELESS_DEBUG_PAIRING_RESULT_ACTION);
    }

    @Override
    public final View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.adb_qrcode_scanner_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mSummary = view.findViewById(R.id.sud_layout_subtitle);

        mTextureView = (TextureView) view.findViewById(R.id.preview_view);
        mTextureView.setSurfaceTextureListener(this);

        mDecorateView = view.findViewById(R.id.decorate_view);
        setProgressBarShown(false);

        mQrCameraView = view.findViewById(R.id.camera_layout);
        mVerifyingView = view.findViewById(R.id.verifying_layout);
        mVerifyingTextView = view.findViewById(R.id.verifying_textview);

        setHeaderTitle(R.string.wifi_dpp_scan_qr_code);
        mSummary.setText(com.android.settingslib.R.string.adb_wireless_qrcode_pairing_description);

        mErrorMessage = view.findViewById(R.id.error_message);
    }

    @Override
    public void onResume() {
        super.onResume();

        restartCamera();

        mAdbManager = IAdbManager.Stub.asInterface(ServiceManager.getService(Context.ADB_SERVICE));
        getActivity().registerReceiver(mReceiver, mIntentFilter,
                Context.RECEIVER_EXPORTED_UNAUDITED);
    }

    @Override
    public void onPause() {
        if (mCamera != null) {
            mCamera.stop();
        }

        super.onPause();

        getActivity().unregisterReceiver(mReceiver);
        try {
            mAdbManager.disablePairing();
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to cancel pairing");
        }
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Do nothing
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // setTitle for TalkBack
        getActivity().setTitle(R.string.wifi_dpp_scan_qr_code);
    }

    @Override
    public int getMetricsCategory() {
        return 0;
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
    public Size getViewSize() {
        return new Size(mTextureView.getWidth(), mTextureView.getHeight());
    }

    @Override
    public void setTransform(Matrix transform) {
        mTextureView.setTransform(transform);
    }

    @Override
    public Rect getFramePosition(Size previewSize, int cameraOrientation) {
        return new Rect(0, 0, previewSize.getHeight(), previewSize.getHeight());
    }

    @Override
    public boolean isValid(String qrCode) {
        try {
            // WIFI:T:ADB;S:myname;P:mypass;;
            mAdbQrCode = new AdbQrCode(qrCode);
        } catch (IllegalArgumentException e) {
            showErrorMessage(R.string.wifi_dpp_qr_code_is_not_valid_format);
            return false;
        }

        mAdbConfig = mAdbQrCode.getAdbNetworkConfig();

        return true;
    }

    @Override
    public void handleSuccessfulResult(String qrCode) {
        destroyCamera();
        mDecorateView.setFocused(true);
        mQrCameraView.setVisibility(View.GONE);
        mVerifyingView.setVisibility(View.VISIBLE);
        AdbQrCode.triggerVibrationForQrCodeRecognition(getContext());
        mVerifyingTextView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
        try {
            mAdbManager.enablePairingByQrCode(mAdbConfig.getSsid(),
                    mAdbConfig.getPreSharedKey());
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to enable QR code pairing");
            getActivity().setResult(Activity.RESULT_CANCELED);
            getActivity().finish();
        }
    }

    @Override
    public void handleCameraFailure() {
        destroyCamera();
    }

    private void initCamera(SurfaceTexture surface) {
        // Check if the camera has alread been created.
        if (mCamera == null) {
            mCamera = new QrCamera(getContext(), this);
            mCamera.start(surface);
        }
    }

    /**
     * To resume camera decoding task after handshake fail or Wi-Fi connection fail.
     */
    private void restartCamera() {
        if (mCamera == null) {
            Log.d(TAG, "mCamera is not available for restarting camera");
            return;
        }

        if (mCamera.isDecodeTaskAlive()) {
            mCamera.stop();
        }

        final SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
        if (surfaceTexture == null) {
            throw new IllegalStateException("SurfaceTexture is not ready for restarting camera");
        }

        mCamera.start(surfaceTexture);
    }

    private void destroyCamera() {
        if (mCamera != null) {
            mCamera.stop();
            mCamera = null;
        }
    }

    private void showErrorMessage(@StringRes int messageResId) {
        final Message message = mHandler.obtainMessage(MESSAGE_SHOW_ERROR_MESSAGE,
                getString(messageResId));
        message.sendToTarget();
    }

    @Override
    protected boolean isFooterAvailable() {
        return false;
    }
}
