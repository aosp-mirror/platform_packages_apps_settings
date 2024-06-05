/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.network;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.system.ResetDashboardFragment;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.ObservableDialogFragment;

public class EuiccRacConnectivityDialogFragment extends ObservableDialogFragment
        implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener {
    public static final String TAG = "EuiccRacConnectivityDlg";
    private static final int METRICS_TAG =
            SettingsEnums.ACTION_RESET_ESIMS_RAC_CONNECTIVITY_WARNING;
    private static final int METRICS_CANCEL_VALUE = 0;
    private static final int METRICS_CONTINUE_VALUE = 1;

    private MetricsFeatureProvider mMetricsFeatureProvider;

    static void show(ResetDashboardFragment host) {
        if (host.getActivity() == null) {
            return;
        }
        final EuiccRacConnectivityDialogFragment dialog = new EuiccRacConnectivityDialogFragment();
        dialog.setTargetFragment(host, /* requestCode= */ 0);
        final FragmentManager manager = host.getActivity().getSupportFragmentManager();
        dialog.show(manager, TAG);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        String title = getString(R.string.wifi_warning_dialog_title);
        String message = getString(R.string.wifi_warning_dialog_text);

        AlertDialog.Builder builder =
                new AlertDialog.Builder(getContext())
                        .setOnDismissListener(this)
                        // Return is on the right side
                        .setPositiveButton(R.string.wifi_warning_return_button, this)
                        // Continue is on the left side
                        .setNegativeButton(R.string.wifi_warning_continue_button, this);

        View content =
                LayoutInflater.from(getContext())
                        .inflate(R.layout.sim_warning_dialog_wifi_connectivity, null);

        // Found the layout resource
        if (content != null) {
            TextView dialogTitle = content.findViewById(R.id.title);
            if (!TextUtils.isEmpty(title) && dialogTitle != null) {
                dialogTitle.setText(title);
                dialogTitle.setVisibility(View.VISIBLE);
            }
            TextView dialogMessage = content.findViewById(R.id.msg);
            if (!TextUtils.isEmpty(message) && dialogMessage != null) {
                dialogMessage.setText(message);
                dialogMessage.setVisibility(View.VISIBLE);
            }

            builder.setView(content);
        } else { // Not found the layout resource, use standard layout
            if (!TextUtils.isEmpty(title)) {
                builder.setTitle(title);
            }
            if (!TextUtils.isEmpty(message)) {
                builder.setMessage(message);
            }
        }

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    @Override
    public void onClick(@NonNull DialogInterface dialog, int which) {
        final Fragment fragment = getTargetFragment();
        if (!(fragment instanceof ResetDashboardFragment)) {
            Log.e(TAG, "getTargetFragment return unexpected type");
            return;
        }

        // Positions of the buttons have been switch:
        // negative button = left button = the button to continue
        if (which == DialogInterface.BUTTON_NEGATIVE) {
            logMetrics(METRICS_CONTINUE_VALUE);
            EraseEuiccDataDialogFragment.show(((ResetDashboardFragment) fragment));
        } else {
            logMetrics(METRICS_CANCEL_VALUE);
        }
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        final Fragment fragment = getTargetFragment();
        if (!(fragment instanceof ResetDashboardFragment)) {
            Log.e(TAG, "getTargetFragment return unexpected type");
            return;
        }
        logMetrics(METRICS_CANCEL_VALUE);
    }

    private void logMetrics(int value) {
        mMetricsFeatureProvider.action(getActivity(), METRICS_TAG, value);
    }
}
