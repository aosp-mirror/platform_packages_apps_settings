/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkScoreManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.os.SimpleClock;
import android.os.SystemClock;

import com.android.settings.R;
import com.android.settings.SetupWizardUtils;
import com.android.settings.wifi.dpp.WifiDppUtils;
import com.android.settingslib.core.lifecycle.ObservableActivity;
import com.android.wifitrackerlib.NetworkDetailsTracker;
import com.android.wifitrackerlib.WifiEntry;

import com.google.android.setupcompat.util.WizardManagerHelper;

import java.time.Clock;
import java.time.ZoneOffset;

/**
 * The activity shows a CONNECT_MODE Wi-fi editor dialog.
 */
public class WifiDialogActivity extends ObservableActivity implements
        WifiDialog2.WifiDialog2Listener, DialogInterface.OnDismissListener {

    private static final String TAG = "WifiDialogActivity";

    public static final String KEY_CHOSEN_WIFIENTRY_KEY = "key_chosen_wifientry_key";

    private static final int RESULT_CONNECTED = RESULT_FIRST_USER;
    private static final int RESULT_FORGET = RESULT_FIRST_USER + 1;

    private static final int REQUEST_CODE_WIFI_DPP_ENROLLEE_QR_CODE_SCANNER = 0;

    // Max age of tracked WifiEntries.
    private static final long MAX_SCAN_AGE_MILLIS = 15_000;
    // Interval between initiating NetworkDetailsTracker scans.
    private static final long SCAN_INTERVAL_MILLIS = 10_000;

    private WifiDialog2 mDialog;
    private Intent mIntent;
    private NetworkDetailsTracker mNetworkDetailsTracker;
    private HandlerThread mWorkerThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mIntent = getIntent();
        if (WizardManagerHelper.isSetupWizardIntent(mIntent)) {
            setTheme(SetupWizardUtils.getTransparentTheme(mIntent));
        }

        super.onCreate(savedInstanceState);

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
        mNetworkDetailsTracker = NetworkDetailsTracker.createNetworkDetailsTracker(
                getLifecycle(),
                this,
                getSystemService(WifiManager.class),
                getSystemService(ConnectivityManager.class),
                getSystemService(NetworkScoreManager.class),
                new Handler(Looper.getMainLooper()),
                mWorkerThread.getThreadHandler(),
                elapsedRealtimeClock,
                MAX_SCAN_AGE_MILLIS,
                SCAN_INTERVAL_MILLIS,
                mIntent.getStringExtra(KEY_CHOSEN_WIFIENTRY_KEY));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mDialog != null) {
            return;
        }

        if (WizardManagerHelper.isAnySetupWizard(getIntent())) {
            mDialog = WifiDialog2.createModal(this, this, mNetworkDetailsTracker.getWifiEntry(),
                    WifiConfigUiBase2.MODE_CONNECT, R.style.SuwAlertDialogThemeCompat_Light);
        } else {
            mDialog = WifiDialog2.createModal(this, this, mNetworkDetailsTracker.getWifiEntry(),
                    WifiConfigUiBase2.MODE_CONNECT);
        }
        mDialog.show();
        mDialog.setOnDismissListener(this);
    }

    @Override
    public void finish() {
        overridePendingTransition(0, 0);

        super.finish();
    }

    @Override
    public void onDestroy() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
            mDialog = null;
        }
        mWorkerThread.quit();

        super.onDestroy();
    }

    @Override
    public void onForget(WifiDialog2 dialog) {
        final WifiEntry wifiEntry = dialog.getController().getWifiEntry();
        if (wifiEntry != null && wifiEntry.canForget()) {
            wifiEntry.forget(null /* callback */);
        }

        setResult(RESULT_FORGET);
        finish();
    }

    @Override
    public void onSubmit(WifiDialog2 dialog) {
        final WifiEntry wifiEntry = dialog.getController().getWifiEntry();
        final WifiConfiguration config = dialog.getController().getConfig();
        if (config == null && wifiEntry != null && wifiEntry.canConnect()) {
            wifiEntry.connect(null /* callback */);
        } else {
            getSystemService(WifiManager.class).connect(config, null /* listener */);
        }

        setResult(RESULT_CONNECTED);
        finish();
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        mDialog = null;
        finish();
    }

    @Override
    public void onScan(WifiDialog2 dialog, String ssid) {
        Intent intent = WifiDppUtils.getEnrolleeQrCodeScannerIntent(ssid);
        WizardManagerHelper.copyWizardManagerExtras(mIntent, intent);

        // Launch QR code scanner to join a network.
        startActivityForResult(intent, REQUEST_CODE_WIFI_DPP_ENROLLEE_QR_CODE_SCANNER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_WIFI_DPP_ENROLLEE_QR_CODE_SCANNER) {
            if (resultCode != RESULT_OK) {
                return;
            }

            setResult(RESULT_CONNECTED, data);
            finish();
        }
    }
}
