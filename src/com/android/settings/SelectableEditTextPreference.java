/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.settings;

import android.content.Context;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

public class SelectableEditTextPreference extends EditTextPreference {

    private int mSelectionMode;

    public static final int SELECTION_CURSOR_END   = 0;
    public static final int SELECTION_CURSOR_START = 1;
    public static final int SELECTION_SELECT_ALL   = 2;

    public SelectableEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Sets the selection mode for the text when it shows up in the dialog
     * @hide
     * @param selectionMode can be SELECTION_CURSOR_START, SELECTION_CURSOR_END or
     * SELECTION_SELECT_ALL. Default is SELECTION_CURSOR_END
     */
    public void setInitialSelectionMode(int selectionMode) {
        mSelectionMode = selectionMode;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        EditText editText = getEditText();
        // Set the selection based on the mSelectionMode
        int length = editText.getText() != null ? editText.getText().length() : 0;
        if (!TextUtils.isEmpty(editText.getText())) {
            switch (mSelectionMode) {
            case SELECTION_CURSOR_END:
                editText.setSelection(length);
                break;
            case SELECTION_CURSOR_START:
                editText.setSelection(0);
                break;
            case SELECTION_SELECT_ALL:
                editText.setSelection(0, length);
                break;
            }
        }
    }
}

