/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.android.settings.applications.appinfo;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.IntDef;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Fragment to show the dialog for uninstall or forcestop. This fragment uses function in
 * target fragment to handle the dialog button click.
 */
public class ButtonActionDialogFragment extends InstrumentedDialogFragment implements
        DialogInterface.OnClickListener {

    /**
     * Interface to handle the dialog click
     */
    public interface AppButtonsDialogListener {
        void handleDialogClick(int type);
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            DialogType.DISABLE,
            DialogType.SPECIAL_DISABLE,
            DialogType.FORCE_STOP
    })
    public @interface DialogType {
        int DISABLE = 0;
        int SPECIAL_DISABLE = 1;
        int FORCE_STOP = 2;
    }

    private static final String ARG_ID = "id";
    @VisibleForTesting
    int mId;

    public static ButtonActionDialogFragment newInstance(@DialogType int id) {
        ButtonActionDialogFragment dialogFragment = new ButtonActionDialogFragment();
        Bundle args = new Bundle(1);
        args.putInt(ARG_ID, id);
        dialogFragment.setArguments(args);

        return dialogFragment;
    }

    @Override
    public int getMetricsCategory() {
        //TODO(35810915): update the metrics label because for now this fragment will be shown
        // in two screens
        return SettingsEnums.DIALOG_APP_INFO_ACTION;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle bundle = getArguments();
        mId = bundle.getInt(ARG_ID);
        Dialog dialog = createDialog(mId);
        if (dialog == null) {
            throw new IllegalArgumentException("unknown id " + mId);
        }
        return dialog;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        final AppButtonsDialogListener lsn =
                (AppButtonsDialogListener) getTargetFragment();
        lsn.handleDialogClick(mId);
    }

    private AlertDialog createDialog(int id) {
        final Context context = getContext();
        switch (id) {
            case DialogType.DISABLE:
            case DialogType.SPECIAL_DISABLE:
                return new AlertDialog.Builder(context)
                        .setMessage(R.string.app_disable_dlg_text)
                        .setPositiveButton(R.string.app_disable_dlg_positive, this)
                        .setNegativeButton(R.string.dlg_cancel, null)
                        .create();
            case DialogType.FORCE_STOP:
                return new AlertDialog.Builder(context)
                        .setTitle(R.string.force_stop_dlg_title)
                        .setMessage(R.string.force_stop_dlg_text)
                        .setPositiveButton(R.string.dlg_ok, this)
                        .setNegativeButton(R.string.dlg_cancel, null)
                        .create();
        }
        return null;
    }
}

