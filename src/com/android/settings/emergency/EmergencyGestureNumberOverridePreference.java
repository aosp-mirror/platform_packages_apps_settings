/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.emergency;

import static android.content.DialogInterface.BUTTON_POSITIVE;

import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settingslib.CustomDialogPreferenceCompat;
import com.android.settingslib.emergencynumber.EmergencyNumberUtils;

/**
 * A dialog preference allowing user to provide a phone number to call during emergency gesture.
 */
public class EmergencyGestureNumberOverridePreference extends
        CustomDialogPreferenceCompat {
    private static final String TAG = "EmergencyGestureNumberO";
    @VisibleForTesting
    EditText mEditText;

    private EmergencyNumberUtils mEmergencyNumberUtils;

    public EmergencyGestureNumberOverridePreference(Context context,
            AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    public EmergencyGestureNumberOverridePreference(Context context, AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public EmergencyGestureNumberOverridePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public EmergencyGestureNumberOverridePreference(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        mEmergencyNumberUtils = new EmergencyNumberUtils(context);
    }

    @Override
    public void setNegativeButtonText(int negativeButtonTextResId) {
        super.setNegativeButtonText(negativeButtonTextResId);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        mEditText = view.findViewById(R.id.emergency_gesture_number_override);
        final String defaultNumber = mEmergencyNumberUtils.getDefaultPoliceNumber();
        mEditText.setHint(defaultNumber);
        final String number = mEmergencyNumberUtils.getPoliceNumber();
        if (!TextUtils.equals(number, defaultNumber)) {
            mEditText.setText(number);
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == BUTTON_POSITIVE) {
            final String input = mEditText.getText().toString();
            if (!TextUtils.isEmpty(input)) {
                mEmergencyNumberUtils.setEmergencyNumberOverride(input);
            } else {
                mEmergencyNumberUtils.setEmergencyNumberOverride(
                        mEmergencyNumberUtils.getDefaultPoliceNumber());
            }
        }
    }
}
