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
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;

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
    private static @Nullable CachedBluetoothDevice sNewDevice;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_STOP_AUDIO_SHARING;
    }

    /**
     * Display the {@link AudioSharingStopDialogFragment} dialog.
     *
     * <p>If the dialog is showing, update the dialog message and event listener.
     *
     * @param host The Fragment this dialog will be hosted.
     * @param newDevice The latest connected device triggered this dialog.
     * @param listener The callback to handle the user action on this dialog.
     */
    public static void show(
            Fragment host, CachedBluetoothDevice newDevice, DialogEventListener listener) {
        if (!AudioSharingUtils.isFeatureEnabled()) return;
        final FragmentManager manager = host.getChildFragmentManager();
        sListener = listener;
        sNewDevice = newDevice;
        Fragment dialog = manager.findFragmentByTag(TAG);
        if (dialog != null
                && ((DialogFragment) dialog).getDialog() != null
                && ((DialogFragment) dialog).getDialog().isShowing()) {
            Log.d(TAG, "Dialog is showing, update the content.");
            updateDialog(newDevice.getName(), (AlertDialog) ((DialogFragment) dialog).getDialog());
        } else {
            Log.d(TAG, "Show up the dialog.");
            final Bundle bundle = new Bundle();
            bundle.putString(BUNDLE_KEY_NEW_DEVICE_NAME, newDevice.getName());
            AudioSharingStopDialogFragment dialogFrag = new AudioSharingStopDialogFragment();
            dialogFrag.setArguments(bundle);
            dialogFrag.show(manager, TAG);
        }
    }

    /** Return the tag of {@link AudioSharingStopDialogFragment} dialog. */
    public static @NonNull String tag() {
        return TAG;
    }

    /** Get the latest connected device which triggers the dialog. */
    public @Nullable CachedBluetoothDevice getDevice() {
        return sNewDevice;
    }

    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle arguments = requireArguments();
        String newDeviceName = arguments.getString(BUNDLE_KEY_NEW_DEVICE_NAME);
        final AlertDialog.Builder builder =
                new AlertDialog.Builder(getActivity()).setCancelable(false);
        LayoutInflater inflater = LayoutInflater.from(builder.getContext());
        // Set custom title for the dialog.
        View customTitle =
                inflater.inflate(R.layout.dialog_custom_title_audio_sharing, /* parent= */ null);
        ImageView icon = customTitle.findViewById(R.id.title_icon);
        icon.setImageResource(R.drawable.ic_warning_24dp);
        TextView title = customTitle.findViewById(R.id.title_text);
        title.setText("Stop sharing audio?");
        builder.setPositiveButton(
                "Stop sharing", (dialog, which) -> sListener.onStopSharingClick());
        builder.setNegativeButton("Cancel", (dialog, which) -> dismiss());
        AlertDialog dialog = builder.setCustomTitle(customTitle).create();
        dialog.setCanceledOnTouchOutside(false);
        updateDialog(newDeviceName, dialog);
        dialog.show();
        TextView messageView = (TextView) dialog.findViewById(android.R.id.message);
        if (messageView != null) {
            Typeface typeface = Typeface.create(Typeface.DEFAULT_FAMILY, Typeface.NORMAL);
            messageView.setTypeface(typeface);
            messageView.setTextDirection(View.TEXT_DIRECTION_LOCALE);
            messageView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        } else {
            Log.w(TAG, "sssFail to update dialog: message view is null");
        }
        return dialog;
    }

    private static void updateDialog(String newDeviceName, @NonNull AlertDialog dialog) {
        dialog.setMessage(
                newDeviceName + " wants to connect, headphones in audio sharing will disconnect.");
    }
}
