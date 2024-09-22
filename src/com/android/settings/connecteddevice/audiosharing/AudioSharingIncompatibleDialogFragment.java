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

package com.android.settings.connecteddevice.audiosharing;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.bluetooth.BluetoothUtils;

public class AudioSharingIncompatibleDialogFragment extends InstrumentedDialogFragment {
    private static final String TAG = "AudioSharingIncompatDlg";

    private static final String BUNDLE_KEY_DEVICE_NAME = "bundle_key_device_name";

    // The host creates an instance of this dialog fragment must implement this interface to receive
    // event callbacks.
    public interface DialogEventListener {
        /**
         * Called when the dialog is dismissed.
         */
        void onDialogDismissed();
    }

    @Nullable
    private static DialogEventListener sListener;

    @Override
    public int getMetricsCategory() {
        // TODO: add metrics
        return 0;
    }

    /**
     * Display the {@link AudioSharingIncompatibleDialogFragment} dialog.
     *
     * @param host The Fragment this dialog will be hosted.
     */
    public static void show(@Nullable Fragment host, @NonNull String deviceName,
            @NonNull DialogEventListener listener) {
        if (host == null || !BluetoothUtils.isAudioSharingEnabled()) return;
        final FragmentManager manager;
        try {
            manager = host.getChildFragmentManager();
        } catch (IllegalStateException e) {
            Log.d(TAG, "Fail to show dialog: " + e.getMessage());
            return;
        }
        sListener = listener;
        AlertDialog dialog = AudioSharingDialogHelper.getDialogIfShowing(manager, TAG);
        if (dialog != null) {
            Log.d(TAG, "Dialog is showing, return.");
            return;
        }
        Log.d(TAG, "Show up the incompatible device dialog.");
        final Bundle bundle = new Bundle();
        bundle.putString(BUNDLE_KEY_DEVICE_NAME, deviceName);
        AudioSharingIncompatibleDialogFragment dialogFrag =
                new AudioSharingIncompatibleDialogFragment();
        dialogFrag.setArguments(bundle);
        dialogFrag.show(manager, TAG);
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle arguments = requireArguments();
        String deviceName = arguments.getString(BUNDLE_KEY_DEVICE_NAME);
        // TODO: move strings to res once they are finalized
        AlertDialog dialog =
                AudioSharingDialogFactory.newBuilder(getActivity())
                        .setTitle("Can't share audio with " + deviceName)
                        .setTitleIcon(com.android.settings.R.drawable.ic_warning_24dp)
                        .setIsCustomBodyEnabled(true)
                        .setCustomMessage(
                                "Audio sharing only works with headphones that support LE Audio.")
                        .setPositiveButton(com.android.settings.R.string.okay, (d, w) -> {})
                        .build();
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (sListener != null) {
            sListener.onDialogDismissed();
        }
    }
}
