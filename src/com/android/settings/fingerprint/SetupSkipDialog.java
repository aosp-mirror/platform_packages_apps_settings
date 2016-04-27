/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.fingerprint;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.android.settings.R;

public class SetupSkipDialog extends DialogFragment implements DialogInterface.OnClickListener {

    public static final String EXTRA_FRP_SUPPORTED = ":settings:frp_supported";

    private static final String ARG_FRP_SUPPORTED = "frp_supported";
    private static final String TAG_SKIP_DIALOG = "skip_dialog";
    private static final int RESULT_SKIP = Activity.RESULT_FIRST_USER + 10;

    public static SetupSkipDialog newInstance(boolean isFrpSupported) {
        SetupSkipDialog dialog = new SetupSkipDialog();
        Bundle args = new Bundle();
        args.putBoolean(ARG_FRP_SUPPORTED, isFrpSupported);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return onCreateDialogBuilder().create();
    }

    @NonNull
    public AlertDialog.Builder onCreateDialogBuilder() {
        Bundle args = getArguments();
        return new AlertDialog.Builder(getContext())
                .setPositiveButton(R.string.skip_anyway_button_label, this)
                .setNegativeButton(R.string.go_back_button_label, this)
                .setMessage(args.getBoolean(ARG_FRP_SUPPORTED) ?
                        R.string.lock_screen_intro_skip_dialog_text_frp :
                        R.string.lock_screen_intro_skip_dialog_text);
    }

    @Override
    public void onClick(DialogInterface dialog, int button) {
        switch (button) {
            case DialogInterface.BUTTON_POSITIVE:
                Activity activity = getActivity();
                activity.setResult(RESULT_SKIP);
                activity.finish();
                break;
        }
    }

    public void show(FragmentManager manager) {
        show(manager, TAG_SKIP_DIALOG);
    }
}