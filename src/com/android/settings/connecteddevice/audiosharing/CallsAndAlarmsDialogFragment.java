/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.connecteddevice.audiosharing;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.flags.Flags;

/** Provides a dialog to choose the active device for calls and alarms. */
public class CallsAndAlarmsDialogFragment extends InstrumentedDialogFragment {
    private static final String TAG = "CallsAndAlarmsDialog";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_AUDIO_SHARING_SWITCH_ACTIVE;
    }

    /**
     * Display the {@link CallsAndAlarmsDialogFragment} dialog.
     *
     * @param host The Fragment this dialog will be hosted.
     */
    public static void show(Fragment host) {
        if (!Flags.enableLeAudioSharing()) return;
        final FragmentManager manager = host.getChildFragmentManager();
        if (manager.findFragmentByTag(TAG) == null) {
            final CallsAndAlarmsDialogFragment dialog = new CallsAndAlarmsDialogFragment();
            dialog.show(manager, TAG);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // TODO: use real device names
        String[] choices = {"Buds 1", "Buds 2"};
        AlertDialog.Builder builder =
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.calls_and_alarms_device_title)
                        .setSingleChoiceItems(
                                choices,
                                0, // TODO: set to current active device.
                                (dialog, which) -> {
                                    // TODO: set device to active device for calls and alarms.
                                });
        return builder.create();
    }
}
