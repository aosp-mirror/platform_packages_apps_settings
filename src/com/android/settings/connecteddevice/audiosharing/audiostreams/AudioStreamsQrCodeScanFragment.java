/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.connecteddevice.audiosharing.audiostreams;

import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamsDashboardFragment.KEY_BROADCAST_METADATA;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settings.core.InstrumentedFragment;
import com.android.settingslib.bluetooth.BluetoothBroadcastUtils;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.qrcode.QrCamera;

import java.time.Duration;

public class AudioStreamsQrCodeScanFragment extends InstrumentedFragment
        implements TextureView.SurfaceTextureListener, QrCamera.ScannerCallback {
    private static final boolean DEBUG = BluetoothUtils.D;
    private static final String TAG = "AudioStreamsQrCodeScanFragment";
    private static final int MESSAGE_HIDE_ERROR_MESSAGE = 1;
    private static final int MESSAGE_SHOW_ERROR_MESSAGE = 2;
    private static final int MESSAGE_SCAN_BROADCAST_SUCCESS = 3;
    @VisibleForTesting static final long SHOW_ERROR_MESSAGE_INTERVAL = 10000;
    @VisibleForTesting static final long SHOW_SUCCESS_SQUARE_INTERVAL = 1000;
    private static final Duration VIBRATE_DURATION_QR_CODE_RECOGNITION = Duration.ofMillis(3);
    private final Handler mHandler =
            new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case MESSAGE_HIDE_ERROR_MESSAGE:
                            mErrorMessage.setVisibility(View.INVISIBLE);
                            break;
                        case MESSAGE_SHOW_ERROR_MESSAGE:
                            String errorMessage = (String) msg.obj;
                            mErrorMessage.setVisibility(View.VISIBLE);
                            mErrorMessage.setText(errorMessage);
                            mErrorMessage.sendAccessibilityEvent(
                                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
                            // Cancel any pending messages to hide error view and requeue the
                            // message so user has time to see error
                            removeMessages(MESSAGE_HIDE_ERROR_MESSAGE);
                            sendEmptyMessageDelayed(
                                    MESSAGE_HIDE_ERROR_MESSAGE, SHOW_ERROR_MESSAGE_INTERVAL);
                            break;
                        case MESSAGE_SCAN_BROADCAST_SUCCESS:
                            Log.d(TAG, "scan success");
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra(KEY_BROADCAST_METADATA, mBroadcastMetadata);
                            if (getActivity() != null) {
                                getActivity().setResult(Activity.RESULT_OK, resultIntent);
                                notifyUserForQrCodeRecognition();
                            }
                            break;
                    }
                }
            };
    private LocalBluetoothManager mLocalBluetoothManager;
    private int mCornerRadius;
    @Nullable private String mBroadcastMetadata;
    private Context mContext;
    @Nullable private QrCamera mCamera;
    private TextureView mTextureView;
    private TextView mErrorMessage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getContext();
        mLocalBluetoothManager = Utils.getLocalBluetoothManager(mContext);
    }

    @Override
    public final View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Collapse or expand the app bar based on orientation for better display the qr camera.
        AudioStreamsHelper.configureAppBarByOrientation(getActivity());
        return inflater.inflate(
                R.layout.qrcode_scanner_fragment, container, /* attachToRoot */ false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mTextureView = view.findViewById(R.id.preview_view);
        mCornerRadius =
                mContext.getResources()
                        .getDimensionPixelSize(R.dimen.audio_streams_qrcode_preview_radius);
        mTextureView.setSurfaceTextureListener(this);
        mTextureView.setOutlineProvider(
                new ViewOutlineProvider() {
                    @Override
                    public void getOutline(View view, Outline outline) {
                        outline.setRoundRect(
                                0, 0, view.getWidth(), view.getHeight(), mCornerRadius);
                    }
                });
        mTextureView.setClipToOutline(true);
        mErrorMessage = view.findViewById(R.id.error_message);

        var device =
                AudioStreamsHelper.getCachedBluetoothDeviceInSharingOrLeConnected(
                        mLocalBluetoothManager);
        TextView summary = view.findViewById(android.R.id.summary);
        if (summary != null && device.isPresent()) {
            summary.setText(
                    getString(
                            R.string.audio_streams_main_page_qr_code_scanner_summary,
                            device.get().getName()));
        }
    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
        if (mCamera == null) {
            mCamera = new QrCamera(mContext, this);
            mCamera.start(surface);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(
            @NonNull SurfaceTexture surface, int width, int height) {}

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
        destroyCamera();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {}

    @Override
    public void handleSuccessfulResult(String qrCode) {
        if (DEBUG) {
            Log.d(TAG, "handleSuccessfulResult(), get the qr code string.");
        }
        mBroadcastMetadata = qrCode;
        Message message = mHandler.obtainMessage(MESSAGE_SCAN_BROADCAST_SUCCESS);
        mHandler.sendMessageDelayed(message, SHOW_SUCCESS_SQUARE_INTERVAL);
    }

    @Override
    public void handleCameraFailure() {
        destroyCamera();
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
        if (qrCode.startsWith(BluetoothBroadcastUtils.SCHEME_BT_BROADCAST_METADATA)) {
            return true;
        }
        Message message =
                mHandler.obtainMessage(
                        MESSAGE_SHOW_ERROR_MESSAGE,
                        getString(R.string.audio_streams_qr_code_is_not_valid_format));
        message.sendToTarget();
        return false;
    }

    private void destroyCamera() {
        if (mCamera != null) {
            mCamera.stop();
            mCamera = null;
        }
    }

    private void notifyUserForQrCodeRecognition() {
        if (mCamera != null) {
            mCamera.stop();
        }

        mErrorMessage.setVisibility(View.INVISIBLE);
        mTextureView.setVisibility(View.INVISIBLE);

        triggerVibrationForQrCodeRecognition(mContext);

        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    private static void triggerVibrationForQrCodeRecognition(Context context) {
        Vibrator vibrator = context.getSystemService(Vibrator.class);
        if (vibrator == null) {
            return;
        }
        vibrator.vibrate(
                VibrationEffect.createOneShot(
                        VIBRATE_DURATION_QR_CODE_RECOGNITION.toMillis(),
                        VibrationEffect.DEFAULT_AMPLITUDE));
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.AUDIO_STREAM_QR_CODE_SCAN;
    }
}
