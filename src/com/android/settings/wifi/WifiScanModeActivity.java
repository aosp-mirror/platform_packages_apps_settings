/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.wifi.WifiPermissionChecker;

/** This activity requests users permission to allow scanning even when Wi-Fi is turned off */
public class WifiScanModeActivity extends FragmentActivity {
    private static final String TAG = "WifiScanModeActivity";
    private DialogFragment mDialog;
    @VisibleForTesting String mApp;
    @VisibleForTesting WifiPermissionChecker mWifiPermissionChecker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow()
                .addSystemFlags(
                        WindowManager.LayoutParams.SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS);
        Intent intent = getIntent();
        if (savedInstanceState == null) {
            if (intent != null
                    && WifiManager.ACTION_REQUEST_SCAN_ALWAYS_AVAILABLE.equals(
                            intent.getAction())) {
                refreshAppLabel();
            } else {
                finish();
                return;
            }
        } else {
            mApp = savedInstanceState.getString("app");
        }
        createDialog();
    }

    @VisibleForTesting
    void refreshAppLabel() {
        if (mWifiPermissionChecker == null) {
            mWifiPermissionChecker = new WifiPermissionChecker(this);
        }
        String packageName = mWifiPermissionChecker.getLaunchedPackage();
        if (TextUtils.isEmpty(packageName)) {
            mApp = null;
            return;
        }
        mApp = Utils.getApplicationLabel(getApplicationContext(), packageName).toString();
    }

    @VisibleForTesting
    void createDialog() {
        if (isGuestUser(getApplicationContext())) {
            Log.e(TAG, "Guest user is not allowed to configure Wi-Fi Scan Mode!");
            EventLog.writeEvent(0x534e4554, "235601169", -1 /* UID */, "User is a guest");
            finish();
            return;
        }

        if (!isWifiScanModeConfigAllowed(getApplicationContext())) {
            Log.e(TAG, "This user is not allowed to configure Wi-Fi Scan Mode!");
            finish();
            return;
        }

        if (mDialog == null) {
            mDialog = AlertDialogFragment.newInstance(mApp);
            mDialog.show(getSupportFragmentManager(), "dialog");
        }
    }

    private void dismissDialog() {
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

    private void doPositiveClick() {
        getApplicationContext().getSystemService(WifiManager.class).setScanAlwaysAvailable(true);
        setResult(RESULT_OK);
        finish();
    }

    private void doNegativeClick() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("app", mApp);
    }

    @Override
    public void onPause() {
        super.onPause();
        dismissDialog();
    }

    public void onResume() {
        super.onResume();
        createDialog();
    }

    public static class AlertDialogFragment extends InstrumentedDialogFragment {
        static AlertDialogFragment newInstance(String app) {
            AlertDialogFragment frag = new AlertDialogFragment(app);
            return frag;
        }

        private final String mApp;

        public AlertDialogFragment(String app) {
            super();
            mApp = app;
        }

        public AlertDialogFragment() {
            super();
            mApp = null;
        }

        @Override
        public int getMetricsCategory() {
            return SettingsEnums.DIALOG_WIFI_SCAN_MODE;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setMessage(
                            TextUtils.isEmpty(mApp)
                                    ? getString(R.string.wifi_scan_always_turn_on_message_unknown)
                                    : getString(R.string.wifi_scan_always_turnon_message, mApp))
                    .setPositiveButton(
                            R.string.wifi_scan_always_confirm_allow,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    ((WifiScanModeActivity) getActivity()).doPositiveClick();
                                }
                            })
                    .setNegativeButton(
                            R.string.wifi_scan_always_confirm_deny,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    ((WifiScanModeActivity) getActivity()).doNegativeClick();
                                }
                            })
                    .create();
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            ((WifiScanModeActivity) getActivity()).doNegativeClick();
        }
    }

    private static boolean isGuestUser(Context context) {
        final UserManager userManager = context.getSystemService(UserManager.class);
        if (userManager == null) return false;
        return userManager.isGuestUser();
    }

    private static boolean isWifiScanModeConfigAllowed(Context context) {
        final UserManager userManager = context.getSystemService(UserManager.class);
        if (userManager == null) return true;
        return !userManager.hasUserRestriction(UserManager.DISALLOW_CONFIG_LOCATION);
    }
}
