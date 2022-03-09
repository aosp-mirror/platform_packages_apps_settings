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
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.os.SimpleClock;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.SetupWizardUtils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.wifi.dpp.WifiDppUtils;
import com.android.settingslib.core.lifecycle.ObservableActivity;
import com.android.settingslib.wifi.AccessPoint;
import com.android.wifitrackerlib.NetworkDetailsTracker;
import com.android.wifitrackerlib.WifiEntry;

import com.google.android.setupcompat.util.WizardManagerHelper;
import com.google.android.setupdesign.util.ThemeHelper;

import java.time.Clock;
import java.time.ZoneOffset;

/**
 * The activity shows a Wi-fi editor dialog.
 *
 * TODO(b/152571756): This activity supports both WifiTrackerLib and SettingsLib because this is an
 *                    exported UI component, some other APPs (e.g., SetupWizard) still use
 *                    SettingsLib. Remove the SettingsLib compatible part after these APPs use
 *                    WifiTrackerLib.
 */
public class WifiDialogActivity extends ObservableActivity implements WifiDialog.WifiDialogListener,
        WifiDialog2.WifiDialog2Listener, DialogInterface.OnDismissListener {

    private static final String TAG = "WifiDialogActivity";

    // For the callers which support WifiTrackerLib.
    public static final String KEY_CHOSEN_WIFIENTRY_KEY = "key_chosen_wifientry_key";

    // For the callers which support SettingsLib.
    public static final String KEY_ACCESS_POINT_STATE = "access_point_state";

    /**
     * Boolean extra indicating whether this activity should connect to an access point on the
     * caller's behalf. If this is set to false, the caller should check
     * {@link #KEY_WIFI_CONFIGURATION} in the result data and save that using
     * {@link WifiManager#connect(WifiConfiguration, ActionListener)}. Default is true.
     */
    @VisibleForTesting
    static final String KEY_CONNECT_FOR_CALLER = "connect_for_caller";

    public static final String KEY_WIFI_CONFIGURATION = "wifi_configuration";

    private static final int RESULT_CONNECTED = RESULT_FIRST_USER;
    private static final int RESULT_FORGET = RESULT_FIRST_USER + 1;

    private static final int REQUEST_CODE_WIFI_DPP_ENROLLEE_QR_CODE_SCANNER = 0;

    // Max age of tracked WifiEntries.
    private static final long MAX_SCAN_AGE_MILLIS = 15_000;
    // Interval between initiating NetworkDetailsTracker scans.
    private static final long SCAN_INTERVAL_MILLIS = 10_000;

    private WifiDialog mDialog;
    private AccessPoint mAccessPoint;

    private WifiDialog2 mDialog2;

    // The received intent supports a key of WifiTrackerLib or SettingsLib.
    private boolean mIsWifiTrackerLib;

    private Intent mIntent;
    private NetworkDetailsTracker mNetworkDetailsTracker;
    private HandlerThread mWorkerThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mIntent = getIntent();
        if (WizardManagerHelper.isSetupWizardIntent(mIntent)) {
            setTheme(SetupWizardUtils.getTransparentTheme(this, mIntent));
        }

        super.onCreate(savedInstanceState);

        mIsWifiTrackerLib = !TextUtils.isEmpty(mIntent.getStringExtra(KEY_CHOSEN_WIFIENTRY_KEY));

        if (mIsWifiTrackerLib) {
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
            mNetworkDetailsTracker = FeatureFactory.getFactory(this)
                    .getWifiTrackerLibProvider()
                    .createNetworkDetailsTracker(
                            getLifecycle(),
                            this,
                            new Handler(Looper.getMainLooper()),
                            mWorkerThread.getThreadHandler(),
                            elapsedRealtimeClock,
                            MAX_SCAN_AGE_MILLIS,
                            SCAN_INTERVAL_MILLIS,
                            mIntent.getStringExtra(KEY_CHOSEN_WIFIENTRY_KEY));
        } else {
            final Bundle accessPointState = mIntent.getBundleExtra(KEY_ACCESS_POINT_STATE);
            if (accessPointState != null) {
                mAccessPoint = new AccessPoint(this, accessPointState);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mDialog2 != null || mDialog != null) {
            return;
        }

        if (WizardManagerHelper.isAnySetupWizard(getIntent())) {
            final int targetStyle = ThemeHelper.isSetupWizardDayNightEnabled(this)
                    ? R.style.SuwAlertDialogThemeCompat_DayNight :
                    R.style.SuwAlertDialogThemeCompat_Light;
            if (mIsWifiTrackerLib) {
                mDialog2 = WifiDialog2.createModal(this, this,
                        mNetworkDetailsTracker.getWifiEntry(),
                        WifiConfigUiBase2.MODE_CONNECT, targetStyle);
            } else {
                mDialog = WifiDialog.createModal(this, this, mAccessPoint,
                        WifiConfigUiBase.MODE_CONNECT, targetStyle);
            }
        } else {
            if (mIsWifiTrackerLib) {
                mDialog2 = WifiDialog2.createModal(this, this,
                        mNetworkDetailsTracker.getWifiEntry(), WifiConfigUiBase2.MODE_CONNECT);
            } else {
                mDialog = WifiDialog.createModal(
                        this, this, mAccessPoint, WifiConfigUiBase.MODE_CONNECT);
            }
        }

        if (mIsWifiTrackerLib) {
            mDialog2.show();
            mDialog2.setOnDismissListener(this);
        } else {
            mDialog.show();
            mDialog.setOnDismissListener(this);
        }
    }

    @Override
    public void finish() {
        overridePendingTransition(0, 0);

        super.finish();
    }

    @Override
    public void onDestroy() {
        if (mIsWifiTrackerLib) {
            if (mDialog2 != null && mDialog2.isShowing()) {
                mDialog2 = null;
            }
            mWorkerThread.quit();
        } else {
            if (mDialog != null && mDialog.isShowing()) {
                mDialog = null;
            }
        }

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
    public void onForget(WifiDialog dialog) {
        final WifiManager wifiManager = getSystemService(WifiManager.class);
        final AccessPoint accessPoint = dialog.getController().getAccessPoint();
        if (accessPoint != null) {
            if (!accessPoint.isSaved()) {
                if (accessPoint.getNetworkInfo() != null &&
                        accessPoint.getNetworkInfo().getState() != NetworkInfo.State.DISCONNECTED) {
                    // Network is active but has no network ID - must be ephemeral.
                    wifiManager.disableEphemeralNetwork(
                            AccessPoint.convertToQuotedString(accessPoint.getSsidStr()));
                } else {
                    // Should not happen, but a monkey seems to trigger it
                    Log.e(TAG, "Failed to forget invalid network " + accessPoint.getConfig());
                }
            } else {
                wifiManager.forget(accessPoint.getConfig().networkId, null /* listener */);
            }
        }

        Intent resultData = new Intent();
        if (accessPoint != null) {
            Bundle accessPointState = new Bundle();
            accessPoint.saveWifiState(accessPointState);
            resultData.putExtra(KEY_ACCESS_POINT_STATE, accessPointState);
        }
        setResult(RESULT_FORGET);
        finish();
    }

    @Override
    public void onSubmit(WifiDialog2 dialog) {
        final WifiEntry wifiEntry = dialog.getController().getWifiEntry();
        final WifiConfiguration config = dialog.getController().getConfig();

        if (getIntent().getBooleanExtra(KEY_CONNECT_FOR_CALLER, true)) {
            if (config == null && wifiEntry != null && wifiEntry.canConnect()) {
                wifiEntry.connect(null /* callback */);
            } else {
                getSystemService(WifiManager.class).connect(config, null /* listener */);
            }
        }

        final Intent resultData = new Intent();
        if (config != null) {
            resultData.putExtra(KEY_WIFI_CONFIGURATION, config);
        }
        setResult(RESULT_CONNECTED, resultData);
        finish();
    }

    @Override
    public void onSubmit(WifiDialog dialog) {
        final WifiConfiguration config = dialog.getController().getConfig();
        final AccessPoint accessPoint = dialog.getController().getAccessPoint();
        final WifiManager wifiManager = getSystemService(WifiManager.class);

        if (getIntent().getBooleanExtra(KEY_CONNECT_FOR_CALLER, true)) {
            if (config == null) {
                if (accessPoint != null && accessPoint.isSaved()) {
                    wifiManager.connect(accessPoint.getConfig(), null /* listener */);
                }
            } else {
                wifiManager.save(config, null /* listener */);
                if (accessPoint != null) {
                    // accessPoint is null for "Add network"
                    NetworkInfo networkInfo = accessPoint.getNetworkInfo();
                    if (networkInfo == null || !networkInfo.isConnected()) {
                        wifiManager.connect(config, null /* listener */);
                    }
                }
            }
        }

        Intent resultData = new Intent();
        if (accessPoint != null) {
            Bundle accessPointState = new Bundle();
            accessPoint.saveWifiState(accessPointState);
            resultData.putExtra(KEY_ACCESS_POINT_STATE, accessPointState);
        }
        if (config != null) {
            resultData.putExtra(KEY_WIFI_CONFIGURATION, config);
        }
        setResult(RESULT_CONNECTED, resultData);
        finish();
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        mDialog2 = null;
        mDialog = null;
        finish();
    }

    @Override
    public void onScan(WifiDialog2 dialog, String ssid) {
        Intent intent = WifiDppUtils.getEnrolleeQrCodeScannerIntent(dialog.getContext(), ssid);
        WizardManagerHelper.copyWizardManagerExtras(mIntent, intent);

        // Launch QR code scanner to join a network.
        startActivityForResult(intent, REQUEST_CODE_WIFI_DPP_ENROLLEE_QR_CODE_SCANNER);
    }

    @Override
    public void onScan(WifiDialog dialog, String ssid) {
        Intent intent = WifiDppUtils.getEnrolleeQrCodeScannerIntent(dialog.getContext(), ssid);
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
