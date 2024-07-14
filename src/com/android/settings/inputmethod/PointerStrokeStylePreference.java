/*
 * Copyright 2024 The Android Open Source Project
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
package com.android.settings.inputmethod;

import static android.view.PointerIcon.POINTER_ICON_VECTOR_STYLE_STROKE_BLACK;
import static android.view.PointerIcon.POINTER_ICON_VECTOR_STYLE_STROKE_NONE;
import static android.view.PointerIcon.POINTER_ICON_VECTOR_STYLE_STROKE_WHITE;

import android.content.Context;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.PointerIcon;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

public class PointerStrokeStylePreference extends Preference {

    public PointerStrokeStylePreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.pointer_icon_stroke_style_layout);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        LinearLayout buttonHolder = (LinearLayout) holder.findViewById(R.id.button_holder);
        // Intercept hover events so setting row does not highlight when hovering buttons.
        buttonHolder.setOnHoverListener((v, e) -> true);

        int currentStroke = getPreferenceDataStore().getInt(Settings.System.POINTER_STROKE_STYLE,
                POINTER_ICON_VECTOR_STYLE_STROKE_WHITE);
        initRadioButton(holder, R.id.stroke_style_white, POINTER_ICON_VECTOR_STYLE_STROKE_WHITE,
                currentStroke);
        initRadioButton(holder, R.id.stroke_style_black, POINTER_ICON_VECTOR_STYLE_STROKE_BLACK,
                currentStroke);
        initRadioButton(holder, R.id.stroke_style_none, POINTER_ICON_VECTOR_STYLE_STROKE_NONE,
                currentStroke);
    }

    private void initRadioButton(@NonNull PreferenceViewHolder holder, int id, int strokeStyle,
            int currentStroke) {
        RadioButton radioButton = (RadioButton) holder.findViewById(id);
        if (radioButton == null) {
            return;
        }
        radioButton.setOnCheckedChangeListener((v, isChecked) -> {
            if (isChecked) {
                getPreferenceDataStore().putInt(Settings.System.POINTER_STROKE_STYLE, strokeStyle);
            }
        });
        radioButton.setChecked(currentStroke == strokeStyle);
        radioButton.setPointerIcon(PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_ARROW));
    }
}
