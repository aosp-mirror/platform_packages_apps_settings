/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.development.tare;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.android.settings.R;
import com.android.settings.Utils;

/**
 * Dialog Fragment for changing tare factor values
 */
public class TareFactorDialogFragment extends DialogFragment {

    private static final String TAG = "TareDialogFragment";

    private final String mFactorKey;
    private final String mFactorTitle;
    private final int mFactorValue;
    private int mFactorEditedValue;

    private EditText mFactorValueView;
    private TareFactorController mTareFactorController;

    /**
     * @param title        the title that will show at the top of the Dialog for the Factor
     * @param key          the key of the Factor being initialized.
     * @param defaultValue the initial value set for the Factor before any changes
     */
    public TareFactorDialogFragment(@NonNull String title, @NonNull String key, int defaultValue,
            TareFactorController tareFactorController) {
        mFactorTitle = title;
        mFactorKey = key;
        mFactorValue = defaultValue;
        mTareFactorController = tareFactorController;
    }

    /**
     * Gets the current value of the Factor
     */
    private String getFactorValue() {
        return Integer.toString(mFactorValue);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                getActivity())
                .setTitle(mFactorTitle)
                .setView(createDialogView())
                .setPositiveButton(R.string.tare_dialog_confirm_button_title, (dialog, which) -> {

                    final String stringValue = mFactorValueView.getText().toString();
                    mFactorEditedValue = mFactorValue;
                    try {
                        mFactorEditedValue = Integer.parseInt(stringValue);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Error converting '" + stringValue + "' to integer. Using "
                                + mFactorValue + " instead", e);
                    }
                    // TODO: Update csv with new factor value
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                    // When the negative button is clicked do nothing
                });

        return builder.create();
    }

    /**
     * Creates a view for the factor Dialog that currently
     * is linked to the basic dialog_edittext.xml layout.
     */
    private View createDialogView() {
        final LayoutInflater layoutInflater = (LayoutInflater) getActivity()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = layoutInflater.inflate(R.layout.dialog_edittext, null);
        mFactorValueView = layout.findViewById(R.id.edittext);
        mFactorValueView.setInputType(InputType.TYPE_CLASS_NUMBER);
        mFactorValueView.setText(getFactorValue());
        Utils.setEditTextCursorPosition(mFactorValueView);

        return layout;
    }
}
