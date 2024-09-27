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

package com.android.settings.bluetooth;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

import com.google.common.base.Strings;

public class ProgressDialogFragment extends InstrumentedDialogFragment {
    private static final String TAG = "BTProgressDialog";

    private static final String BUNDLE_KEY_MESSAGE = "bundle_key_message";

    @Nullable private static FragmentManager sManager;
    @Nullable private static Lifecycle sLifecycle;
    private String mMessage = "";
    @Nullable private AlertDialog mAlertDialog;

    @Override
    public int getMetricsCategory() {
        // TODO: add metrics
        return 0;
    }

    /**
     * Returns a new instance of {@link ProgressDialogFragment} dialog.
     *
     * @param host The Fragment this dialog will be hosted.
     */
    @Nullable
    public static ProgressDialogFragment newInstance(@Nullable Fragment host) {
        if (host == null) return null;
        try {
            sManager = host.getChildFragmentManager();
            sLifecycle = host.getLifecycle();
        } catch (IllegalStateException e) {
            Log.d(TAG, "Fail to create new instance: " + e.getMessage());
            return null;
        }
        return new ProgressDialogFragment();
    }

    /**
     * Display {@link ProgressDialogFragment} dialog.
     *
     * @param message The message to be shown on the dialog
     */
    public void show(@NonNull String message) {
        if (sManager == null) return;
        Lifecycle.State currentState = sLifecycle == null ? null : sLifecycle.getCurrentState();
        if (currentState == null || !currentState.isAtLeast(Lifecycle.State.STARTED)) {
            Log.d(TAG, "Fail to show dialog with state: " + currentState);
            return;
        }
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            if (!mMessage.equals(message)) {
                Log.d(TAG, "Update dialog message.");
                TextView messageView = mAlertDialog.findViewById(R.id.message);
                if (messageView != null) {
                    messageView.setText(message);
                }
                mMessage = message;
            }
            Log.d(TAG, "Dialog is showing, return.");
            return;
        }
        mMessage = message;
        Log.d(TAG, "Show up the progress dialog.");
        Bundle args = new Bundle();
        args.putString(BUNDLE_KEY_MESSAGE, message);
        setArguments(args);
        show(sManager, TAG);
    }

    /** Returns the current message on the dialog. */
    @VisibleForTesting
    @NonNull
    public String getMessage() {
        return mMessage;
    }

    private ProgressDialogFragment() {
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle args = requireArguments();
        String message = args.getString(BUNDLE_KEY_MESSAGE, "");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = LayoutInflater.from(builder.getContext());
        View customView = inflater.inflate(
                R.layout.dialog_audio_sharing_progress, /* root= */ null);
        TextView textView = customView.findViewById(R.id.message);
        if (textView != null && !Strings.isNullOrEmpty(message)) {
            textView.setText(message);
        }
        AlertDialog dialog = builder.setView(customView).setCancelable(false).create();
        dialog.setCanceledOnTouchOutside(false);
        mAlertDialog = dialog;
        return dialog;
    }
}
