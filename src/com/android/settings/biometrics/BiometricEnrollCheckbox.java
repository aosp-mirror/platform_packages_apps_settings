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

package com.android.settings.biometrics;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.android.settings.R;

/**
 * Widget contain space for an icon, title, description, and checkbox. On devices with multiple
 * biometric sensors, allows users to choose sensors during {@link BiometricEnrollActivity}.
 */
public class BiometricEnrollCheckbox extends LinearLayout {

    @NonNull private final CheckBox mCheckBox;
    @NonNull private final TextView mDescriptionView;
    @Nullable private View.OnClickListener mListener;

    public BiometricEnrollCheckbox(Context context) {
        this(context, null /* attrs */);
    }

    public BiometricEnrollCheckbox(Context context,
            @Nullable AttributeSet attrs) {
        this(context, attrs, 0 /* defStyleAttr */);
    }

    public BiometricEnrollCheckbox(Context context, @Nullable AttributeSet attrs,
            int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0 /* defStyleRes */);
    }

    public BiometricEnrollCheckbox(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        LayoutInflater.from(context).inflate(R.layout.biometric_enroll_checkbox,
                this, true /* attachToRoot */);

        mCheckBox = findViewById(R.id.checkbox);
        final ImageView iconView = findViewById(R.id.icon);
        final TextView titleView = findViewById(R.id.title);
        mDescriptionView = findViewById(R.id.description);

        setOnClickListener(view -> {
            if (isEnabled()) {
                mCheckBox.toggle();
            }
            if (mListener != null) {
                mListener.onClick(view);
            }
        });

        final TypedArray a = context
                .obtainStyledAttributes(attrs, R.styleable.BiometricEnrollCheckbox);
        try {
            final Drawable icon =
                    a.getDrawable(R.styleable.BiometricEnrollCheckbox_icon);
            final CharSequence title =
                    a.getText(R.styleable.BiometricEnrollCheckbox_title);
            final CharSequence description =
                    a.getText(R.styleable.BiometricEnrollCheckbox_description);
            iconView.setImageDrawable(icon);
            titleView.setText(title);
            mDescriptionView.setText(description);
        } finally {
            a.recycle();
        }
    }

    public void setListener(View.OnClickListener listener) {
        mListener = listener;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mCheckBox.setEnabled(enabled);
    }

    public boolean isChecked() {
        return mCheckBox.isChecked();
    }

    public void setDescription(@StringRes int resId) {
        mDescriptionView.setText(resId);
    }
}
