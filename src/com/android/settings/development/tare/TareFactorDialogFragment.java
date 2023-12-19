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

import static android.app.tare.EconomyManager.CAKE_IN_ARC;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;

import com.android.settings.R;
import com.android.settings.Utils;

/**
 * Dialog Fragment for changing tare factor values
 */
public class TareFactorDialogFragment extends DialogFragment {
    private static final String TAG = "TareDialogFragment";

    // This follows the order in strings.xml:tare_units array.
    private static final int UNIT_IDX_ARC = 0;
    private static final int UNIT_IDX_CAKE = 1;

    private final String mFactorKey;
    private final String mFactorTitle;
    private final long mFactorValue;
    private final int mFactorPolicy;
    private final TareFactorController mTareFactorController;

    private EditText mFactorValueView;
    private Spinner mUnitSpinner;

    /**
     * @param title        the title that will show at the top of the Dialog for the Factor
     * @param key          the key of the Factor being initialized.
     * @param currentValue the current value set for the Factor
     */
    public TareFactorDialogFragment(@NonNull String title, @NonNull String key, long currentValue,
            int factorPolicy, TareFactorController tareFactorController) {
        mFactorTitle = title;
        mFactorKey = key;
        mFactorValue = currentValue;
        mFactorPolicy = factorPolicy;
        mTareFactorController = tareFactorController;
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
                    long newVal = mFactorValue;
                    try {
                        newVal = Long.parseLong(stringValue);
                        if (mUnitSpinner.getSelectedItemPosition() == UNIT_IDX_ARC) {
                            // Convert ARC to cake
                            newVal *= CAKE_IN_ARC;
                        }
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Error parsing '" + stringValue + "'. Using "
                                + mFactorValue + " instead", e);
                    }
                    mTareFactorController.updateValue(mFactorKey, newVal, mFactorPolicy);
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
        View layout = layoutInflater.inflate(R.layout.dialog_edittext_dropdown, null);
        mFactorValueView = layout.findViewById(R.id.edittext);
        mFactorValueView.setInputType(InputType.TYPE_CLASS_NUMBER);

        mUnitSpinner = layout.findViewById(R.id.spinner);
        final String[] units = getResources().getStringArray(R.array.tare_units);
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(
                getActivity(), android.R.layout.simple_spinner_item, units);
        mUnitSpinner.setAdapter(spinnerArrayAdapter);

        final int unitIdx;
        if (mFactorValue % CAKE_IN_ARC == 0) {
            mFactorValueView.setText(String.format("%d", mFactorValue / CAKE_IN_ARC));
            unitIdx = UNIT_IDX_ARC;
        } else {
            mFactorValueView.setText(String.format("%d", mFactorValue));
            unitIdx = UNIT_IDX_CAKE;
        }
        mUnitSpinner.setSelection(unitIdx);
        mUnitSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private int mSelectedPosition = unitIdx;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mSelectedPosition == position) {
                    return;
                }
                mSelectedPosition = position;
                final String stringValue = mFactorValueView.getText().toString();

                try {
                    long newVal = Long.parseLong(stringValue);
                    if (mUnitSpinner.getSelectedItemPosition() == UNIT_IDX_ARC) {
                        // Convert cake to ARC
                        newVal /= CAKE_IN_ARC;
                    } else {
                        // Convert ARC to cake
                        newVal *= CAKE_IN_ARC;
                    }
                    mFactorValueView.setText(String.format("%d", newVal));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error parsing '" + stringValue + "'", e);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        Utils.setEditTextCursorPosition(mFactorValueView);
        return layout;
    }
}
