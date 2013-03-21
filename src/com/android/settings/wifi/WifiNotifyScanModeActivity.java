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
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

/**
 * This activity notifies the user that wifi scans are still available when Wi-Fi is being
 * turned off
 */
public class WifiNotifyScanModeActivity extends Activity {
    private DialogFragment mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent != null && intent.getAction()
                .equals(WifiManager.ACTION_NOTIFY_SCAN_ALWAYS_AVAILABLE)) {
            createDialog();
        } else {
            finish();
            return;
        }
    }

    private void createDialog() {
        if (mDialog == null) {
            mDialog = AlertDialogFragment.newInstance();
            mDialog.show(getFragmentManager(), "dialog");
        }
    }

    private void dismissDialog() {
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
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

    void doPositiveButton(boolean checked) {
        Settings.Global.putInt(getContentResolver(),
                Settings.Global.WIFI_NOTIFY_SCAN_ALWAYS_AVAILABLE, checked ? 0 : 1);
        finish();
    }

    public static class AlertDialogFragment extends DialogFragment {
        static AlertDialogFragment newInstance() {
            AlertDialogFragment frag = new AlertDialogFragment();
            return frag;
        }

        public AlertDialogFragment() {
            super();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            View checkBoxView = View.inflate(getActivity(), R.layout.wifi_notify_scan_mode, null);
            final CheckBox checkBox = (CheckBox) checkBoxView.findViewById(R.id.checkbox);
            final String msg;
            if (Settings.Secure.isLocationProviderEnabled(getActivity().getContentResolver(),
                    LocationManager.NETWORK_PROVIDER)) {
                msg = getString(R.string.wifi_scan_notify_text_location_on);
            } else {
                msg = getString(R.string.wifi_scan_notify_text_location_off);
            }
            return new AlertDialog.Builder(getActivity())
                    .setMessage(msg)
                    .setView(checkBoxView)
                    .setPositiveButton(R.string.dlg_ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    ((WifiNotifyScanModeActivity) getActivity()).doPositiveButton(
                                            checkBox.isChecked());
                                }
                            }
                    )
                    .setNegativeButton(R.string.dlg_cancel,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    ((WifiNotifyScanModeActivity) getActivity()).finish();
                                }
                            }
                    )
                    .create();
        }
        @Override
        public void onCancel(DialogInterface dialog) {
            ((WifiNotifyScanModeActivity) getActivity()).finish();
        }
    }
}
