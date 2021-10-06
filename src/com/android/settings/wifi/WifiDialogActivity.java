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

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.ActionListener;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.SetupWizardUtils;
import com.android.settings.wifi.dpp.WifiDppUtils;
import com.android.settingslib.wifi.AccessPoint;

import com.google.android.setupcompat.util.WizardManagerHelper;

public class WifiDialogActivity extends Activity implements WifiDialog.WifiDialogListener,
        DialogInterface.OnDismissListener {

    private static final String TAG = "WifiDialogActivity";

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

    private WifiDialog mDialog;

    private Intent mIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mIntent = getIntent();
        if (WizardManagerHelper.isSetupWizardIntent(mIntent)) {
            setTheme(SetupWizardUtils.getTransparentTheme(mIntent));
        }

        super.onCreate(savedInstanceState);

        final Bundle accessPointState = mIntent.getBundleExtra(KEY_ACCESS_POINT_STATE);
        AccessPoint accessPoint = null;
        if (accessPointState != null) {
            accessPoint = new AccessPoint(this, accessPointState);
        }

        if (WizardManagerHelper.isAnySetupWizard(getIntent())) {
            mDialog = WifiDialog.createModal(this, this, accessPoint,
                    WifiConfigUiBase.MODE_CONNECT, R.style.SuwAlertDialogThemeCompat_Light);
        } else {
            mDialog = WifiDialog.createModal(
                    this, this, accessPoint, WifiConfigUiBase.MODE_CONNECT);
        }
        mDialog.show();
        mDialog.setOnDismissListener(this);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
            mDialog = null;
        }
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
        mDialog = null;
        finish();
    }

    @Override
    public void onScan(WifiDialog dialog, String ssid) {
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
