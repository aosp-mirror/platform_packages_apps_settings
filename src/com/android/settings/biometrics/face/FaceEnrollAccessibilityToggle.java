/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.biometrics.face;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.settings.R;

import com.google.android.material.materialswitch.MaterialSwitch;

/**
 * A layout that contains a start-justified title, and an end-justified switch.
 */
public class FaceEnrollAccessibilityToggle extends LinearLayout {

    private final MaterialSwitch mSwitch;

    public FaceEnrollAccessibilityToggle(Context context) {
        this(context, null /* attrs */);
    }

    public FaceEnrollAccessibilityToggle(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FaceEnrollAccessibilityToggle(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater.from(context).inflate(R.layout.face_enroll_accessibility_toggle,
                this, true /* attachToRoot */);

        final TypedArray a =
                context.obtainStyledAttributes(attrs, R.styleable.FaceEnrollAccessibilityToggle);
        try {
            final CharSequence title =
                    a.getText(R.styleable.FaceEnrollAccessibilityToggle_messageText);
            final TextView titleTextView = findViewById(R.id.title);
            titleTextView.setText(title);
        } finally {
            a.recycle();
        }
        mSwitch = findViewById(R.id.toggle);
        mSwitch.setChecked(false);
        mSwitch.setClickable(false);
        mSwitch.setFocusable(false);
    }

    public boolean isChecked() {
        return mSwitch.isChecked();
    }

    public void setChecked(boolean checked) {
        mSwitch.setChecked(checked);
        mSwitch.jumpDrawablesToCurrentState(); // Do not trigger animation from activity
    }

    public void setListener(CompoundButton.OnCheckedChangeListener listener) {
        mSwitch.setOnCheckedChangeListener(listener);
    }

    public CompoundButton getSwitch() {
        return mSwitch;
    }
}
