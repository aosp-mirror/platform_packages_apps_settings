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

import com.android.settings.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

/**
 * This activity requests users permission to allow scanning even when Wi-Fi is turned off
 */
public class WifiScanModeActivity extends Activity {
    private DialogFragment mDialog;
    private String mApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (savedInstanceState == null) {
            if (intent != null && intent.getAction()
                    .equals(WifiManager.ACTION_REQUEST_SCAN_ALWAYS_AVAILABLE)) {
                mApp = getCallingPackage();
            } else {
                finish();
                return;
            }
        } else {
            mApp = savedInstanceState.getString("app");
        }
        createDialog();
    }

    private void createDialog() {
        if (mDialog == null) {
            mDialog = AlertDialogFragment.newInstance(mApp);
            mDialog.show(getFragmentManager(), "dialog");
        }
    }

    private void dismissDialog() {
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

    private void doPositiveClick() {
        Settings.Global.putInt(getContentResolver(),
                Settings.Global.WIFI_SCAN_ALWAYS_AVAILABLE, 1);
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

    public static class AlertDialogFragment extends DialogFragment {
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
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setMessage(getString(R.string.wifi_scan_always_turnon_message, mApp))
                    .setPositiveButton(R.string.wifi_scan_always_confirm_allow,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    ((WifiScanModeActivity) getActivity()).doPositiveClick();
                                }
                            }
                    )
                    .setNegativeButton(R.string.wifi_scan_always_confirm_deny,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    ((WifiScanModeActivity) getActivity()).doNegativeClick();
                                }
                            }
                    )
                    .create();
        }
        @Override
        public void onCancel(DialogInterface dialog) {
            ((WifiScanModeActivity) getActivity()).doNegativeClick();
        }
    }
}
