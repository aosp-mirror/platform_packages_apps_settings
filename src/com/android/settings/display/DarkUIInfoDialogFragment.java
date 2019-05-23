/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.display;

import android.app.Dialog;
import android.app.UiModeManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

public class DarkUIInfoDialogFragment extends InstrumentedDialogFragment
        implements DialogInterface.OnClickListener{

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_DARK_UI_INFO;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Context context = getContext();
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(dialog.getContext());
        View titleView = inflater.inflate(R.layout.settings_dialog_title, null);
        ((ImageView) titleView.findViewById(R.id.settings_icon))
                .setImageDrawable(context.getDrawable(R.drawable.dark_theme));
        ((TextView) titleView.findViewById(R.id.settings_title)).setText(R.string.dark_ui_mode);

        dialog.setCustomTitle(titleView)
                .setMessage(R.string.dark_ui_settings_dark_summary)
                .setPositiveButton(
                        R.string.dark_ui_settings_dialog_acknowledge,
                        this);
        return dialog.create();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        enableDarkTheme();
        super.onDismiss(dialog);
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        // We have to manually dismiss the dialog because changing night mode causes it to
        // recreate itself.
        dialogInterface.dismiss();
        enableDarkTheme();
    }

    private void enableDarkTheme() {
        final Context context = getContext();
        if (context != null) {
            Settings.Secure.putInt(context.getContentResolver(),
                    Settings.Secure.DARK_MODE_DIALOG_SEEN,
                    DarkUIPreferenceController.DIALOG_SEEN);
            context.getSystemService(UiModeManager.class)
                    .setNightMode(UiModeManager.MODE_NIGHT_YES);
        }
    }
}
