/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.security;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.PowerManager;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

public class MemtagRebootDialog extends InstrumentedDialogFragment
        implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {

    public static final String TAG = "MemtagRebootDialog";
    private int mMessage;

    public MemtagRebootDialog(Context context, int msg) {
        mMessage = msg;
    }

    public static void show(Context context, Fragment host, int msg) {
        final FragmentManager manager = host.getActivity().getSupportFragmentManager();
        if (manager.findFragmentByTag(TAG) == null) {
            final MemtagRebootDialog dialog = new MemtagRebootDialog(context, msg);
            dialog.show(manager, TAG);
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.REBOOT_WITH_MTE;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.memtag_reboot_title)
                .setMessage(mMessage)
                .setPositiveButton(R.string.memtag_reboot_yes, this /* onClickListener */)
                .setNegativeButton(R.string.memtag_reboot_no, null /* onClickListener */)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        PowerManager pm = getContext().getSystemService(PowerManager.class);
        pm.reboot(/* reason */ null);
    }
}
