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

import android.app.Activity;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.HelpUtils;

public class WifiScanningRequiredFragment extends InstrumentedDialogFragment implements
        DialogInterface.OnClickListener {

    private static final String TAG = "WifiScanReqFrag";

    public static WifiScanningRequiredFragment newInstance() {
        WifiScanningRequiredFragment fragment = new WifiScanningRequiredFragment();
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setTitle(R.string.wifi_settings_scanning_required_title)
                .setView(R.layout.wifi_settings_scanning_required_view)
                .setPositiveButton(R.string.wifi_settings_scanning_required_turn_on, this)
                .setNegativeButton(R.string.cancel, null);
        addButtonIfNeeded(builder);

        return builder.create();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.WIFI_SCANNING_NEEDED_DIALOG;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        Context context = getContext();
        ContentResolver contentResolver = context.getContentResolver();
        switch(which) {
            case DialogInterface.BUTTON_POSITIVE:
                context.getSystemService(WifiManager.class).setScanAlwaysAvailable(true);
                Toast.makeText(
                        context,
                        context.getString(R.string.wifi_settings_scanning_required_enabled),
                        Toast.LENGTH_SHORT).show();
                getTargetFragment().onActivityResult(
                        getTargetRequestCode(),
                        Activity.RESULT_OK,
                        null);
                break;
            case DialogInterface.BUTTON_NEUTRAL:
                openHelpPage();
                break;
            case DialogInterface.BUTTON_NEGATIVE:
            default:
                // do nothing
        }
    }

    void addButtonIfNeeded(AlertDialog.Builder builder) {
        // Only show "learn more" if there is a help page to show
        if (!TextUtils.isEmpty(getContext().getString(R.string.help_uri_wifi_scanning_required))) {
            builder.setNeutralButton(R.string.learn_more, this);
        }
    }

    private void openHelpPage() {
        Intent intent = getHelpIntent(getContext());
        if (intent != null) {
            try {
                getActivity().startActivityForResult(intent, 0);
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "Activity was not found for intent, " + intent.toString());
            }
        }
    }

    @VisibleForTesting
    Intent getHelpIntent(Context context) {
        return HelpUtils.getHelpIntent(
                    context,
                    context.getString(R.string.help_uri_wifi_scanning_required),
                    context.getClass().getName());
    }
}
