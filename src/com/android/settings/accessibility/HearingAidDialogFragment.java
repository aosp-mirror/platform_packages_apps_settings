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

package com.android.settings.accessibility;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.bluetooth.BluetoothPairingDetail;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

public class HearingAidDialogFragment extends InstrumentedDialogFragment {
    public static HearingAidDialogFragment newInstance() {
        HearingAidDialogFragment frag = new HearingAidDialogFragment();
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setMessage(R.string.accessibility_hearingaid_pair_instructions_message)
                .setPositiveButton(R.string.accessibility_hearingaid_instruction_continue_button,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                launchBluetoothAddDeviceSetting();
                            }
                        })
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) { }
                        })
                .create();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_ACCESSIBILITY_HEARINGAID;
    }

    private void launchBluetoothAddDeviceSetting() {
        new SubSettingLauncher(getActivity())
                .setDestination(BluetoothPairingDetail.class.getName())
                .setSourceMetricsCategory(SettingsEnums.ACCESSIBILITY)
                .launch();
    }
}
