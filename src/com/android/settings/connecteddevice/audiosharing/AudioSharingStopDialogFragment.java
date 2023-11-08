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

import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.flags.Flags;

public class AudioSharingStopDialogFragment extends InstrumentedDialogFragment {
    private static final String TAG = "AudioSharingStopDialog";

    private static final String BUNDLE_KEY_NEW_DEVICE_NAME = "bundle_key_new_device_name";

    // The host creates an instance of this dialog fragment must implement this interface to receive
    // event callbacks.
    public interface DialogEventListener {
        /** Called when users click the stop sharing button in the dialog. */
        void onStopSharingClick();
    }

    private static DialogEventListener sListener;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_STOP_AUDIO_SHARING;
    }

    /**
     * Display the {@link AudioSharingStopDialogFragment} dialog.
     *
     * @param host The Fragment this dialog will be hosted.
     */
    public static void show(Fragment host, String newDeviceName, DialogEventListener listener) {
        if (!Flags.enableLeAudioSharing()) return;
        final FragmentManager manager = host.getChildFragmentManager();
        sListener = listener;
        if (manager.findFragmentByTag(TAG) == null) {
            final Bundle bundle = new Bundle();
            bundle.putString(BUNDLE_KEY_NEW_DEVICE_NAME, newDeviceName);
            AudioSharingStopDialogFragment dialog = new AudioSharingStopDialogFragment();
            dialog.setArguments(bundle);
            dialog.show(manager, TAG);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle arguments = requireArguments();
        String newDeviceName = arguments.getString(BUNDLE_KEY_NEW_DEVICE_NAME);
        final AlertDialog.Builder builder =
                new AlertDialog.Builder(getActivity())
                        .setTitle("Stop sharing audio?")
                        .setCancelable(false);
        builder.setMessage(
                newDeviceName + " is connected, devices in audio sharing will disconnect.");
        builder.setPositiveButton(
                "Stop sharing",
                (dialog, which) -> {
                    sListener.onStopSharingClick();
                });
        builder.setNegativeButton(
                "Cancel",
                (dialog, which) -> {
                    dismiss();
                });
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }
}
