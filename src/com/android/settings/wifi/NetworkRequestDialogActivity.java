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

package com.android.settings.wifi;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.NetworkRequestMatchCallback;
import android.net.wifi.WifiManager.NetworkRequestUserSelectionCallback;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.Message;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;
import com.android.settings.wifi.NetworkRequestErrorDialogFragment.ERROR_DIALOG_TYPE;

import java.util.List;

/**
 * When other applications request to have a wifi connection, framework will bring up this activity
 * to let user select which wifi ap wanna to connect. This activity contains
 * {@code NetworkRequestDialogFragment}, {@code NetworkRequestSingleSsidDialogFragment} to show UI
 * and handles framework callback.
 */
public class NetworkRequestDialogActivity extends FragmentActivity implements
        NetworkRequestMatchCallback {
    private static String TAG = "NetworkRequestDialogActivity";

    /** Message sent to stop scanning wifi and pop up timeout dialog. */
    private static final int MESSAGE_STOP_SCAN_WIFI_LIST = 0;

    /** Delayed time to stop scanning wifi. */
    private static final int DELAY_TIME_STOP_SCAN_MS = 30 * 1000;

    final static String EXTRA_IS_SPECIFIED_SSID =
        "com.android.settings.wifi.extra.REQUEST_IS_FOR_SINGLE_NETWORK";

    private NetworkRequestDialogBaseFragment mDialogFragment;
    private NetworkRequestUserSelectionCallback mUserSelectionCallback;
    private boolean mIsSpecifiedSsid;
    private boolean mShowingErrorDialog;
    private WifiConfiguration mMatchedConfig;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        if (intent != null) {
            mIsSpecifiedSsid = intent.getBooleanExtra(EXTRA_IS_SPECIFIED_SSID, false);
        }

        if (mIsSpecifiedSsid) {
            showProgressDialog(getString(R.string.network_connection_searching_message));
        } else {
            mDialogFragment = NetworkRequestDialogFragment.newInstance();
            mDialogFragment.show(getSupportFragmentManager(), TAG);
        }
    }

    private void showProgressDialog(String message) {
        dismissDialogs();

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setMessage(message);
        mProgressDialog.show();
    }

    private void showSingleSsidRequestDialog(String ssid, boolean isTryAgain) {
        dismissDialogs();

        mDialogFragment = new NetworkRequestSingleSsidDialogFragment();
        final Bundle bundle = new Bundle();
        bundle.putString(NetworkRequestSingleSsidDialogFragment.EXTRA_SSID, ssid);
        bundle.putBoolean(NetworkRequestSingleSsidDialogFragment.EXTRA_TRYAGAIN, isTryAgain);
        mDialogFragment.setArguments(bundle);
        mDialogFragment.show(getSupportFragmentManager(), TAG);
    }

    private void dismissDialogs() {
        if (mDialogFragment != null) {
            mDialogFragment.dismiss();
            mDialogFragment = null;
        }
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        final WifiManager wifiManager = getSystemService(WifiManager.class);
        if (wifiManager != null) {
            wifiManager.registerNetworkRequestMatchCallback(new HandlerExecutor(mHandler), this);
        }
        // Sets time-out to stop scanning.
        mHandler.sendEmptyMessageDelayed(MESSAGE_STOP_SCAN_WIFI_LIST, DELAY_TIME_STOP_SCAN_MS);
    }

    @Override
    protected void onPause() {
        mHandler.removeMessages(MESSAGE_STOP_SCAN_WIFI_LIST);
        final WifiManager wifiManager = getSystemService(WifiManager.class);
        if (wifiManager != null) {
            wifiManager.unregisterNetworkRequestMatchCallback(this);
        }

        super.onPause();
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STOP_SCAN_WIFI_LIST:
                    removeMessages(MESSAGE_STOP_SCAN_WIFI_LIST);
                    stopScanningAndPopErrorDialog(ERROR_DIALOG_TYPE.TIME_OUT);
                    break;
                default:
                    // Do nothing.
                    break;
            }
        }
    };

    protected void stopScanningAndPopErrorDialog(ERROR_DIALOG_TYPE type) {
        dismissDialogs();

        // Throws error dialog.
        final NetworkRequestErrorDialogFragment dialogFragment =
                NetworkRequestErrorDialogFragment.newInstance();
        dialogFragment.setRejectCallback(mUserSelectionCallback);
        final Bundle bundle = new Bundle();
        bundle.putSerializable(NetworkRequestErrorDialogFragment.DIALOG_TYPE, type);
        dialogFragment.setArguments(bundle);
        dialogFragment.show(getSupportFragmentManager(), TAG);
        mShowingErrorDialog = true;
    }

    @Override
    public void onUserSelectionCallbackRegistration(
        NetworkRequestUserSelectionCallback userSelectionCallback) {
        if (mIsSpecifiedSsid) {
            mUserSelectionCallback = userSelectionCallback;
            return;
        }

        mDialogFragment.onUserSelectionCallbackRegistration(userSelectionCallback);
    }

    @Override
    public void onAbort() {
        stopScanningAndPopErrorDialog(ERROR_DIALOG_TYPE.ABORT);
    }

    @Override
    public void onMatch(List<ScanResult> scanResults) {
        if (mShowingErrorDialog) {
            // Don't do anything since error dialog shows.
            return;
        }

        mHandler.removeMessages(MESSAGE_STOP_SCAN_WIFI_LIST);

        if (mIsSpecifiedSsid) {
            // Prevent from throwing same dialog, because onMatch() will be called many times.
            if (mMatchedConfig == null) {
                mMatchedConfig = WifiUtils.getWifiConfig(
                    null /* accesspoint */, scanResults.get(0), null /* password */);
                showSingleSsidRequestDialog(
                        WifiInfo.sanitizeSsid(mMatchedConfig.SSID), false /* isTryAgain */);
            }
            return;
        }

        mDialogFragment.onMatch(scanResults);
    }

    @Override
    public void onUserSelectionConnectSuccess(WifiConfiguration wificonfiguration) {
        if (!isFinishing()) {
            Toast.makeText(this, R.string.network_connection_connect_successful, Toast.LENGTH_SHORT)
                    .show();
            setResult(RESULT_OK);
            finish();
        }
    }

    @Override
    public void onUserSelectionConnectFailure(WifiConfiguration wificonfiguration) {
        if (!isFinishing()) {
            Toast.makeText(this, R.string.network_connection_connect_failure, Toast.LENGTH_SHORT)
                    .show();
            setResult(RESULT_OK);
            finish();
        }
    }

    // Called when user click "Connect" button. Called by
    // {@code NetworkRequestSingleSsidDialogFragment}.
    public void onClickConnectButton() {
        if (mUserSelectionCallback != null) {
            mUserSelectionCallback.select(mMatchedConfig);
            showProgressDialog(getString(R.string.network_connection_connecting_message));
        }
    }

    // Called when user click retry button. Called by {@link NetworkRequestErrorDialogFragment}.
    public void onClickRescanButton() {
        // Sets time-out to stop scanning.
        mHandler.sendEmptyMessageDelayed(MESSAGE_STOP_SCAN_WIFI_LIST, DELAY_TIME_STOP_SCAN_MS);

        mShowingErrorDialog = false;

        if (mIsSpecifiedSsid) {
            mMatchedConfig = null;
            showProgressDialog(getString(R.string.network_connection_searching_message));
        } else {
            mDialogFragment = NetworkRequestDialogFragment.newInstance();
            mDialogFragment.show(getSupportFragmentManager(), TAG);
        }
    }

    // Called when user click cancel button.
    public void onCancel() {
        dismissDialogs();

        if (mUserSelectionCallback != null) {
            mUserSelectionCallback.reject();
        }
        finish();
    }
}
