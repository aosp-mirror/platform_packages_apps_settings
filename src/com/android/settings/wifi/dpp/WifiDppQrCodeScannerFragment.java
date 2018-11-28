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
import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.android.settings.R;
import com.android.settings.wifi.qrcode.QrCamera;
import com.android.settings.wifi.qrcode.QrDecorateView;

public class WifiDppQrCodeScannerFragment extends WifiDppQrCodeBaseFragment implements
        SurfaceHolder.Callback,
        QrCamera.ScannerCallback {
    private QrCamera mCamera;
    private SurfaceView mSurfaceView;
    private QrDecorateView mDecorateView;

    @Override
    protected int getLayout() {
        return R.layout.wifi_dpp_qrcode_scanner_fragment;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setTitle(getString(R.string.wifi_dpp_add_device_to_network));

        String ssid = null;
        final Intent intent = getActivity().getIntent();
        if (intent != null) {
            ssid = intent.getStringExtra(WifiDppConfiguratorActivity.EXTRA_SSID);
        }
        if (TextUtils.isEmpty(ssid)) {
            throw new IllegalArgumentException("Invalid SSID");
        }
        setDescription(getString(R.string.wifi_dpp_center_qr_code, ssid));

        hideRightButton();

        setLeftButtonText(getString(android.R.string.cancel));

        setLeftButtonOnClickListener((view) -> {
            getActivity().setResult(Activity.RESULT_CANCELED);
            getActivity().finish();
        });
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mSurfaceView = (SurfaceView) view.findViewById(R.id.preview_view);
        final SurfaceHolder surfaceHolder = mSurfaceView.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder.addCallback(this);

        mDecorateView = (QrDecorateView) view.findViewById(R.id.decorate_view);
    }

    @Override
    public void surfaceCreated(final SurfaceHolder holder) {
        initCamera(holder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        destroyCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Do nothing
    }

    @Override
    public Size getViewSize() {
        return new Size(mSurfaceView.getWidth(), mSurfaceView.getHeight());
    }

    @Override
    public Rect getFramePosition(Size previewSize, int cameraOrientation) {
        return new Rect(0, 0, previewSize.getHeight(), previewSize.getHeight());
    }

    @Override
    public void handleSuccessfulResult(String qrCode) {
        destroyCamera();
        mDecorateView.setFocused(true);
        // TODO(b/120243131): Add a network by Wi-Fi Network config shared via QR code.
    }

    @Override
    public void handleCameraFailure() {
        destroyCamera();
    }

    private void initCamera(SurfaceHolder holder) {
        // Check if the camera has already created.
        if (mCamera == null) {
            mCamera = new QrCamera(getContext(), this);
            mCamera.start(holder);
        }
    }

    private void destroyCamera() {
        if (mCamera != null) {
            mCamera.stop();
            mCamera = null;
        }
    }
}
