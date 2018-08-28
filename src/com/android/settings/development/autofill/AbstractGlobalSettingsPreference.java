/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.development.autofill;

import android.content.Context;
import android.provider.Settings;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.android.settings.Utils;
import com.android.settingslib.CustomEditTextPreferenceCompat;

/**
 * Base class for Autofill integer properties that are backed by
 * {@link android.provider.Settings.Global}.
 */
abstract class AbstractGlobalSettingsPreference extends CustomEditTextPreferenceCompat {

    private static final String TAG = "AbstractGlobalSettingsPreference";

    private final String mKey;
    private final int mDefaultValue;

    private final AutofillDeveloperSettingsObserver mObserver;

    protected AbstractGlobalSettingsPreference(Context context, AttributeSet attrs,
            String key, int defaultValue) {
        super(context, attrs);

        mKey = key;
        mDefaultValue = defaultValue;
        mObserver = new AutofillDeveloperSettingsObserver(context, () -> updateSummary());
    }

    @Override
    public void onAttached() {
        super.onAttached();

        mObserver.register();
        updateSummary();
    }

    @Override
    public void onDetached() {
        mObserver.unregister();

        super.onDetached();
    }

    private String getCurrentValue() {
        final int value = Settings.Global.getInt(getContext().getContentResolver(),
                mKey, mDefaultValue);

        return Integer.toString(value);
    }

    private void updateSummary() {
        setSummary(getCurrentValue());

    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        EditText editText = view.findViewById(android.R.id.edit);
        if (editText != null) {
            editText.setInputType(InputType.TYPE_CLASS_NUMBER);
            editText.setText(getCurrentValue());
            Utils.setEditTextCursorPosition(editText);
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            final String stringValue = getText();
            int newValue = mDefaultValue;
            try {
                newValue = Integer.parseInt(stringValue);
            } catch (Exception e) {
                Log.e(TAG, "Error converting '" + stringValue + "' to integer. Using "
                        + mDefaultValue + " instead");
            }
            Settings.Global.putInt(getContext().getContentResolver(), mKey, newValue);
        }
    }
}
