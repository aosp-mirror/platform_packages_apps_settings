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
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.bluetooth.BluetoothUtils;

import com.google.common.base.Strings;

import java.util.concurrent.TimeUnit;

public class AudioSharingLoadingStateDialogFragment extends InstrumentedDialogFragment {
    private static final String TAG = "AudioSharingLoadingDlg";

    private static final String BUNDLE_KEY_MESSAGE = "bundle_key_message";
    private static final long AUTO_DISMISS_TIME_THRESHOLD_MS = TimeUnit.SECONDS.toMillis(10);
    private static final int AUTO_DISMISS_MESSAGE_ID = R.id.message;

    private static String sMessage = "";
    @Nullable
    private Handler mHandler;

    @Override
    public int getMetricsCategory() {
        // TODO: add metrics
        return 0;
    }

    /**
     * Display the {@link AudioSharingLoadingStateDialogFragment} dialog.
     *
     * @param host    The Fragment this dialog will be hosted by.
     * @param message The content to be shown on the dialog.
     */
    public static void show(@Nullable Fragment host, @NonNull String message) {
        if (host == null || !BluetoothUtils.isAudioSharingEnabled()) return;
        final FragmentManager manager;
        try {
            manager = host.getChildFragmentManager();
        } catch (IllegalStateException e) {
            Log.d(TAG, "Fail to show dialog: " + e.getMessage());
            return;
        }
        AlertDialog dialog = AudioSharingDialogHelper.getDialogIfShowing(manager, TAG);
        if (dialog != null) {
            if (sMessage.equals(message)) {
                Log.d(TAG, "Dialog is showing with same message, return.");
                return;
            } else {
                Log.d(TAG, "Dialog is showing with different message, dismiss and reshow.");
                dialog.dismiss();
            }
        }
        sMessage = message;
        Log.d(TAG, "Show up the loading dialog.");
        Bundle args = new Bundle();
        args.putString(BUNDLE_KEY_MESSAGE, message);
        AudioSharingLoadingStateDialogFragment dialogFrag =
                new AudioSharingLoadingStateDialogFragment();
        dialogFrag.setArguments(args);
        dialogFrag.show(manager, TAG);
    }

    /** Dismiss the {@link AudioSharingLoadingStateDialogFragment} dialog. */
    public static void dismiss(@Nullable Fragment host) {
        if (host == null || !BluetoothUtils.isAudioSharingEnabled()) return;
        final FragmentManager manager;
        try {
            manager = host.getChildFragmentManager();
        } catch (IllegalStateException e) {
            Log.d(TAG, "Fail to dismiss dialog: " + e.getMessage());
            return;
        }
        AlertDialog dialog = AudioSharingDialogHelper.getDialogIfShowing(manager, TAG);
        if (dialog != null) {
            Log.d(TAG, "Dialog is showing, dismiss.");
            dialog.dismiss();
        }
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        mHandler = new Handler(Looper.getMainLooper());
        mHandler.postDelayed(() -> dismiss(), AUTO_DISMISS_MESSAGE_ID,
                AUTO_DISMISS_TIME_THRESHOLD_MS);
        Bundle args = requireArguments();
        String message = args.getString(BUNDLE_KEY_MESSAGE, "");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = LayoutInflater.from(builder.getContext());
        View customView = inflater.inflate(R.layout.dialog_audio_sharing_loading_state, /* root= */
                null);
        TextView textView = customView.findViewById(R.id.message);
        if (!Strings.isNullOrEmpty(message)) textView.setText(message);
        AlertDialog dialog = builder.setView(customView).setCancelable(false).create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mHandler != null) {
            mHandler.removeMessages(AUTO_DISMISS_MESSAGE_ID);
        }
    }
}
