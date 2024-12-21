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

package com.android.settings.connecteddevice.virtual;

import android.app.Activity;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.companion.CompanionDeviceManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/**
 * Implements an AlertDialog for confirming that a user wishes to unpair or "forget" a paired
 * device.
 */
public class ForgetDeviceDialogFragment extends InstrumentedDialogFragment {

    public static final String TAG = ForgetDeviceDialogFragment.class.getSimpleName();

    private static final String DEVICE_ARG = "virtual_device_arg";

    @VisibleForTesting
    CompanionDeviceManager mCompanionDeviceManager;
    @VisibleForTesting
    VirtualDeviceWrapper mDevice;

    static ForgetDeviceDialogFragment newInstance(VirtualDeviceWrapper device) {
        Bundle args = new Bundle(1);
        args.putParcelable(DEVICE_ARG, device);
        ForgetDeviceDialogFragment dialog = new ForgetDeviceDialogFragment();
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_VIRTUAL_DEVICE_FORGET;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mCompanionDeviceManager = context.getSystemService(CompanionDeviceManager.class);
        mDevice = getArguments().getParcelable(DEVICE_ARG, VirtualDeviceWrapper.class);
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(@Nullable Bundle inState) {
        Context context = getContext();
        CharSequence deviceName = mDevice.getDeviceName(context);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setPositiveButton(R.string.virtual_device_forget_dialog_confirm_button,
                        this::onForgetButtonClick)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.setTitle(
                context.getString(R.string.virtual_device_forget_dialog_title, deviceName));
        dialog.setMessage(
                context.getString(R.string.virtual_device_forget_dialog_body, deviceName));
        return dialog;
    }

    private void onForgetButtonClick(DialogInterface dialog, int which) {
        mCompanionDeviceManager.disassociate(mDevice.getAssociationInfo().getId());
        Activity activity = getActivity();
        if (activity != null) {
            activity.finish();
        }
    }
}
