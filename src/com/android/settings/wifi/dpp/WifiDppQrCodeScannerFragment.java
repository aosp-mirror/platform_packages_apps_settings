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

import static android.net.wifi.WifiInfo.sanitizeSsid;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.net.wifi.EasyConnectStatusCallback;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SimpleClock;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.ViewModelProvider;

import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.qrcode.QrCamera;
import com.android.settingslib.qrcode.QrDecorateView;
import com.android.settingslib.wifi.WifiPermissionChecker;
import com.android.wifitrackerlib.WifiEntry;
import com.android.wifitrackerlib.WifiPickerTracker;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;

public class WifiDppQrCodeScannerFragment extends WifiDppQrCodeBaseFragment implements
        SurfaceTextureListener,
        QrCamera.ScannerCallback,
        WifiManager.ActionListener {
    private static final String TAG = "WifiDppQrCodeScanner";

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
    public static final String KEY_WIFI_CONFIGURATION = "key_wifi_configuration";

    private static final int ARG_RESTART_CAMERA = 1;

    // Max age of tracked WifiEntries.
    private static final long MAX_SCAN_AGE_MILLIS = 15_000;
    // Interval between initiating WifiPickerTracker scans.
    private static final long SCAN_INTERVAL_MILLIS = 10_000;

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
    private WifiConfiguration mEnrolleeWifiConfiguration;

    private int mLatestStatusCode = WifiDppUtils.EASY_CONNECT_EVENT_FAILURE_NONE;

    private WifiPickerTracker mWifiPickerTracker;
    private HandlerThread mWorkerThread;
    private WifiPermissionChecker mWifiPermissionChecker;

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

                    if (msg.arg1 == ARG_RESTART_CAMERA) {
                        setProgressBarShown(false);
                        mDecorateView.setFocused(false);
                        restartCamera();
                    }
                    break;

                case MESSAGE_SCAN_WIFI_DPP_SUCCESS:
                    if (mScanWifiDppSuccessListener == null) {
                        // mScanWifiDppSuccessListener may be null after onDetach(), do nothing here
                        return;
                    }
                    mScanWifiDppSuccessListener.onScanWifiDppSuccess((WifiQrCode)msg.obj);

                    if (!mIsConfiguratorMode) {
                        setProgressBarShown(true);
                        startWifiDppEnrolleeInitiator((WifiQrCode)msg.obj);
                        updateEnrolleeSummary();
                        mSummary.sendAccessibilityEvent(
                                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
                    }

                    notifyUserForQrCodeRecognition();
                    break;

                case MESSAGE_SCAN_ZXING_WIFI_FORMAT_SUCCESS:
                    final Context context = getContext();
                    if (context == null) {
                        // Context may be null if the message is received after the Activity has
                        // been destroyed
                        Log.d(TAG, "Scan success but context is null");
                        return;
                    }

                    // We may get 2 WifiConfiguration if the QR code has no password in it,
                    // one for open network and one for enhanced open network.
                    final WifiManager wifiManager =
                            context.getSystemService(WifiManager.class);
                    final WifiNetworkConfig qrCodeWifiNetworkConfig =
                            (WifiNetworkConfig)msg.obj;
                    final List<WifiConfiguration> qrCodeWifiConfigurations =
                            qrCodeWifiNetworkConfig.getWifiConfigurations();

                    // Adds all Wi-Fi networks in QR code to the set of configured networks and
                    // connects to it if it's reachable.
                    boolean hasHiddenOrReachableWifiNetwork = false;
                    for (WifiConfiguration qrCodeWifiConfiguration : qrCodeWifiConfigurations) {
                        final int id = wifiManager.addNetwork(qrCodeWifiConfiguration);
                        if (id == -1) {
                            continue;
                        }

                        if (!canConnectWifi(qrCodeWifiConfiguration.SSID)) return;

                        wifiManager.enableNetwork(id, /* attemptConnect */ false);
                        // WifiTracker only contains a hidden SSID Wi-Fi network if it's saved.
                        // We can't check if a hidden SSID Wi-Fi network is reachable in advance.
                        if (qrCodeWifiConfiguration.hiddenSSID ||
                                isReachableWifiNetwork(qrCodeWifiConfiguration)) {
                            hasHiddenOrReachableWifiNetwork = true;
                            mEnrolleeWifiConfiguration = qrCodeWifiConfiguration;
                            wifiManager.connect(id,
                                    /* listener */ WifiDppQrCodeScannerFragment.this);
                        }
                    }

                    if (!hasHiddenOrReachableWifiNetwork) {
                        showErrorMessageAndRestartCamera(
                                R.string.wifi_dpp_check_connection_try_again);
                        return;
                    }

                    mMetricsFeatureProvider.action(
                            mMetricsFeatureProvider.getAttribution(getActivity()),
                            SettingsEnums.ACTION_SETTINGS_ENROLL_WIFI_QR_CODE,
                            SettingsEnums.SETTINGS_WIFI_DPP_ENROLLEE,
                            /* key */ null,
                            /* value */ Integer.MIN_VALUE);

                    notifyUserForQrCodeRecognition();
                    break;

                default:
            }
        }
    };

    @UiThread
    private void notifyUserForQrCodeRecognition() {
        if (mCamera != null) {
            mCamera.stop();
        }

        mDecorateView.setFocused(true);
        mErrorMessage.setVisibility(View.INVISIBLE);

        WifiDppUtils.triggerVibrationForQrCodeRecognition(getContext());
    }

    private boolean isReachableWifiNetwork(WifiConfiguration wifiConfiguration) {
        final List<WifiEntry> wifiEntries = mWifiPickerTracker.getWifiEntries();
        final WifiEntry connectedWifiEntry = mWifiPickerTracker.getConnectedWifiEntry();
        if (connectedWifiEntry != null) {
            // Add connected WifiEntry to prevent fail toast to users when it's connected.
            wifiEntries.add(connectedWifiEntry);
        }

        for (WifiEntry wifiEntry : wifiEntries) {
            if (!TextUtils.equals(wifiEntry.getSsid(), sanitizeSsid(wifiConfiguration.SSID))) {
                continue;
            }
            final int security =
                    WifiDppUtils.getSecurityTypeFromWifiConfiguration(wifiConfiguration);
            if (security == wifiEntry.getSecurity()) {
                return true;
            }

            // Default security type of PSK/SAE transition mode WifiEntry is SECURITY_PSK and
            // there is no way to know if a WifiEntry is of transition mode. Give it a chance.
            if (security == WifiEntry.SECURITY_SAE
                    && wifiEntry.getSecurity() == WifiEntry.SECURITY_PSK) {
                return true;
            }
        }
        return false;
    }

    @VisibleForTesting
    boolean canConnectWifi(String ssid) {
        final List<WifiEntry> wifiEntries = mWifiPickerTracker.getWifiEntries();
        for (WifiEntry wifiEntry : wifiEntries) {
            if (!TextUtils.equals(wifiEntry.getSsid(), sanitizeSsid(ssid))) continue;

            if (!wifiEntry.canConnect()) {
                Log.w(TAG, "Wi-Fi is not allowed to connect by your organization. SSID:" + ssid);
                showErrorMessageAndRestartCamera(R.string.not_allowed_by_ent);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mIsConfiguratorMode = savedInstanceState.getBoolean(KEY_IS_CONFIGURATOR_MODE);
            mLatestStatusCode = savedInstanceState.getInt(KEY_LATEST_ERROR_CODE);
            mEnrolleeWifiConfiguration = savedInstanceState.getParcelable(KEY_WIFI_CONFIGURATION);
        }

        final WifiDppInitiatorViewModel model =
                new ViewModelProvider(this).get(WifiDppInitiatorViewModel.class);

        model.getEnrolleeSuccessNetworkId().observe(this, networkId -> {
            // After configuration change, observe callback will be triggered,
            // do nothing for this case if a handshake does not end
            if (model.isWifiDppHandshaking()) {
                return;
            }

            new EasyConnectEnrolleeStatusCallback().onEnrolleeSuccess(networkId.intValue());
        });

        model.getStatusCode().observe(this, statusCode -> {
            // After configuration change, observe callback will be triggered,
            // do nothing for this case if a handshake does not end
            if (model.isWifiDppHandshaking()) {
                return;
            }

            int code = statusCode.intValue();
            Log.d(TAG, "Easy connect enrollee callback onFailure " + code);
            new EasyConnectEnrolleeStatusCallback().onFailure(code);
        });
    }

    @Override
    public void onPause() {
        if (mCamera != null) {
            mCamera.stop();
        }

        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!isWifiDppHandshaking()) {
            restartCamera();
        }
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
        void onScanWifiDppSuccess(WifiQrCode wifiQrCode);
    }
    private OnScanWifiDppSuccessListener mScanWifiDppSuccessListener;

    /**
     * Configurator container activity of the fragment should create instance with this constructor.
     */
    public WifiDppQrCodeScannerFragment() {
        super();

        mIsConfiguratorMode = true;
    }

    public WifiDppQrCodeScannerFragment(WifiPickerTracker wifiPickerTracker,
            WifiPermissionChecker wifiPermissionChecker) {
        super();

        mIsConfiguratorMode = true;
        mWifiPickerTracker = wifiPickerTracker;
        mWifiPermissionChecker = wifiPermissionChecker;
    }

    /**
     * Enrollee container activity of the fragment should create instance with this constructor and
     * specify the SSID string of the WI-Fi network to be provisioned.
     */
    WifiDppQrCodeScannerFragment(String ssid) {
        super();

        mIsConfiguratorMode = false;
        mSsid = ssid;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mWorkerThread = new HandlerThread(
                TAG + "{" + Integer.toHexString(System.identityHashCode(this)) + "}",
                Process.THREAD_PRIORITY_BACKGROUND);
        mWorkerThread.start();
        final Clock elapsedRealtimeClock = new SimpleClock(ZoneOffset.UTC) {
            @Override
            public long millis() {
                return SystemClock.elapsedRealtime();
            }
        };
        final Context context = getContext();
        mWifiPickerTracker = FeatureFactory.getFeatureFactory()
                .getWifiTrackerLibProvider()
                .createWifiPickerTracker(getSettingsLifecycle(), context,
                        new Handler(Looper.getMainLooper()),
                        mWorkerThread.getThreadHandler(),
                        elapsedRealtimeClock,
                        MAX_SCAN_AGE_MILLIS,
                        SCAN_INTERVAL_MILLIS,
                        null /* listener */);

        // setTitle for TalkBack
        if (mIsConfiguratorMode) {
            getActivity().setTitle(R.string.wifi_dpp_add_device_to_network);
        } else {
            getActivity().setTitle(R.string.wifi_dpp_scan_qr_code);
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
    public void onDestroyView() {
        mWorkerThread.quit();

        super.onDestroyView();
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
        mSummary = view.findViewById(R.id.sud_layout_subtitle);

        mTextureView = view.findViewById(R.id.preview_view);
        mTextureView.setSurfaceTextureListener(this);

        mDecorateView = view.findViewById(R.id.decorate_view);

        setProgressBarShown(isWifiDppHandshaking());

        if (mIsConfiguratorMode) {
            setHeaderTitle(R.string.wifi_dpp_add_device_to_network);

            WifiNetworkConfig wifiNetworkConfig = ((WifiNetworkConfig.Retriever) getActivity())
                .getWifiNetworkConfig();
            if (!WifiNetworkConfig.isValidConfig(wifiNetworkConfig)) {
                throw new IllegalStateException("Invalid Wi-Fi network for configuring");
            }
            mSummary.setText(getString(R.string.wifi_dpp_center_qr_code,
                    wifiNetworkConfig.getSsid()));
        } else {
            setHeaderTitle(R.string.wifi_dpp_scan_qr_code);

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
            showErrorMessage(R.string.wifi_dpp_qr_code_is_not_valid_format);
            return false;
        }

        // It's impossible to provision other device with ZXing Wi-Fi Network config format
        final String scheme = mWifiQrCode.getScheme();
        if (mIsConfiguratorMode && WifiQrCode.SCHEME_ZXING_WIFI_NETWORK_CONFIG.equals(scheme)) {
            showErrorMessage(R.string.wifi_dpp_qr_code_is_not_valid_format);
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
        Message message = mHandler.obtainMessage(MESSAGE_SCAN_WIFI_DPP_SUCCESS);
        message.obj = new WifiQrCode(mWifiQrCode.getQrCode());

        mHandler.sendMessageDelayed(message, SHOW_SUCCESS_SQUARE_INTERVAL);
    }

    private void handleZxingWifiFormat() {
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

            if (isWifiDppHandshaking()) {
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

    private void showErrorMessage(@StringRes int messageResId) {
        final Message message = mHandler.obtainMessage(MESSAGE_SHOW_ERROR_MESSAGE,
                getString(messageResId));
        message.sendToTarget();
    }

    @VisibleForTesting
    void showErrorMessageAndRestartCamera(@StringRes int messageResId) {
        final Message message = mHandler.obtainMessage(MESSAGE_SHOW_ERROR_MESSAGE,
                getString(messageResId));
        message.arg1 = ARG_RESTART_CAMERA;
        message.sendToTarget();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(KEY_IS_CONFIGURATOR_MODE, mIsConfiguratorMode);
        outState.putInt(KEY_LATEST_ERROR_CODE, mLatestStatusCode);
        outState.putParcelable(KEY_WIFI_CONFIGURATION, mEnrolleeWifiConfiguration);

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
                    mEnrolleeWifiConfiguration = wifiConfig;
                    if (!canConnectWifi(wifiConfig.SSID)) return;
                    wifiManager.connect(wifiConfig, WifiDppQrCodeScannerFragment.this);
                    return;
                }
            }

            Log.e(TAG, "Invalid networkId " + newNetworkId);
            mLatestStatusCode = EasyConnectStatusCallback.EASY_CONNECT_EVENT_FAILURE_GENERIC;
            updateEnrolleeSummary();
            showErrorMessageAndRestartCamera(R.string.wifi_dpp_check_connection_try_again);
        }

        @Override
        public void onConfiguratorSuccess(int code) {
            // Do nothing
        }

        @Override
        public void onFailure(int code) {
            Log.d(TAG, "EasyConnectEnrolleeStatusCallback.onFailure " + code);

            int errorMessageResId;
            switch (code) {
                case EasyConnectStatusCallback.EASY_CONNECT_EVENT_FAILURE_INVALID_URI:
                    errorMessageResId = R.string.wifi_dpp_qr_code_is_not_valid_format;
                    break;

                case EasyConnectStatusCallback.EASY_CONNECT_EVENT_FAILURE_AUTHENTICATION:
                    errorMessageResId = R.string.wifi_dpp_failure_authentication_or_configuration;
                    break;

                case EasyConnectStatusCallback.EASY_CONNECT_EVENT_FAILURE_NOT_COMPATIBLE:
                    errorMessageResId = R.string.wifi_dpp_failure_not_compatible;
                    break;

                case EasyConnectStatusCallback.EASY_CONNECT_EVENT_FAILURE_CONFIGURATION:
                    errorMessageResId = R.string.wifi_dpp_failure_authentication_or_configuration;
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
                    errorMessageResId = R.string.wifi_dpp_failure_timeout;
                    break;

                case EasyConnectStatusCallback.EASY_CONNECT_EVENT_FAILURE_GENERIC:
                    errorMessageResId = R.string.wifi_dpp_failure_generic;
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
            showErrorMessageAndRestartCamera(errorMessageResId);
        }

        @Override
        public void onProgress(int code) {
            // Do nothing
        }
    }

    private void startWifiDppEnrolleeInitiator(WifiQrCode wifiQrCode) {
        final WifiDppInitiatorViewModel model =
                new ViewModelProvider(this).get(WifiDppInitiatorViewModel.class);

        model.startEasyConnectAsEnrolleeInitiator(wifiQrCode.getQrCode());
    }

    @Override
    public void onSuccess() {
        final Intent resultIntent = new Intent();
        resultIntent.putExtra(KEY_WIFI_CONFIGURATION, mEnrolleeWifiConfiguration);

        final Activity hostActivity = getActivity();
        if (hostActivity == null) return;
        if (mWifiPermissionChecker == null) {
            mWifiPermissionChecker = new WifiPermissionChecker(hostActivity);
        }

        if (!mWifiPermissionChecker.canAccessWifiState()) {
            Log.w(TAG, "Calling package does not have ACCESS_WIFI_STATE permission for result.");
            EventLog.writeEvent(0x534e4554, "187176859",
                    mWifiPermissionChecker.getLaunchedPackage(), "no ACCESS_WIFI_STATE permission");
            hostActivity.finish();
            return;
        }

        if (!mWifiPermissionChecker.canAccessFineLocation()) {
            Log.w(TAG, "Calling package does not have ACCESS_FINE_LOCATION permission for result.");
            EventLog.writeEvent(0x534e4554, "187176859",
                    mWifiPermissionChecker.getLaunchedPackage(),
                    "no ACCESS_FINE_LOCATION permission");
            hostActivity.finish();
            return;
        }

        hostActivity.setResult(Activity.RESULT_OK, resultIntent);
        hostActivity.finish();
    }

    @Override
    public void onFailure(int reason) {
        Log.d(TAG, "Wi-Fi connect onFailure reason - " + reason);
        showErrorMessageAndRestartCamera(R.string.wifi_dpp_check_connection_try_again);
    }

    // Check is Easy Connect handshaking or not
    private boolean isWifiDppHandshaking() {
        final WifiDppInitiatorViewModel model =
                new ViewModelProvider(this).get(WifiDppInitiatorViewModel.class);

        return model.isWifiDppHandshaking();
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

    private void updateEnrolleeSummary() {
        if (isWifiDppHandshaking()) {
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

    @VisibleForTesting
    protected boolean isDecodeTaskAlive() {
        return mCamera != null && mCamera.isDecodeTaskAlive();
    }

    @Override
    protected boolean isFooterAvailable() {
        return false;
    }
}
