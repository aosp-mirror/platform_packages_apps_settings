/*
 * Copyright (C) 2020 The Android Open Source Project
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
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.system.ResetDashboardFragment;
import com.android.settings.wifi.dpp.WifiDppUtils;

public class EraseEuiccDataDialogFragment extends InstrumentedDialogFragment implements
        DialogInterface.OnClickListener {

    public static final String TAG = "EraseEuiccDataDlg";
    private static final String PACKAGE_NAME_EUICC_DATA_MANAGEMENT_CALLBACK =
            "com.android.settings.network";

    public static void show(ResetDashboardFragment host) {
        final EraseEuiccDataDialogFragment dialog = new EraseEuiccDataDialogFragment();
        dialog.setTargetFragment(host, 0 /* requestCode */);
        final FragmentManager manager = host.getActivity().getSupportFragmentManager();
        dialog.show(manager, TAG);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.RESET_EUICC;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.reset_esim_title)
                .setMessage(R.string.reset_esim_desc)
                .setPositiveButton(R.string.erase_sim_confirm_button, this)
                .setNegativeButton(R.string.cancel, null)
                .setOnDismissListener(this)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        final Fragment fragment = getTargetFragment();
        if (!(fragment instanceof ResetDashboardFragment)) {
            Log.e(TAG, "getTargetFragment return unexpected type");
        }

        if (which == DialogInterface.BUTTON_POSITIVE) {
            Context context = getContext();
            WifiDppUtils.showLockScreen(context, () -> runAsyncWipe(context));
        }
    }

    private void runAsyncWipe(Context context) {
        Runnable runnable = (new ResetNetworkOperationBuilder(context))
                .resetEsim(PACKAGE_NAME_EUICC_DATA_MANAGEMENT_CALLBACK)
                .build();
        AsyncTask.execute(runnable);
    }
}
