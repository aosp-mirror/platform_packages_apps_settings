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

import static android.view.PointerIcon.POINTER_ICON_VECTOR_STYLE_FILL_BLACK;
import static android.view.PointerIcon.POINTER_ICON_VECTOR_STYLE_FILL_BLUE;
import static android.view.PointerIcon.POINTER_ICON_VECTOR_STYLE_FILL_GREEN;
import static android.view.PointerIcon.POINTER_ICON_VECTOR_STYLE_FILL_PINK;
import static android.view.PointerIcon.POINTER_ICON_VECTOR_STYLE_FILL_PURPLE;
import static android.view.PointerIcon.POINTER_ICON_VECTOR_STYLE_FILL_RED;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.PointerIcon;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;


public class PointerFillStylePreference extends Preference {
    private static final int[] STATE_HOVERED_SELECTED =
            new int[]{android.R.attr.state_hovered, android.R.attr.state_selected};
    private static final int[] STATE_SELECTED = new int[]{android.R.attr.state_selected};
    private static final int[] STATE_HOVERED = new int[]{android.R.attr.state_hovered};
    private static final int[] STATE_DEFAULT = new int[]{};

    @Nullable private LinearLayout mButtonHolder;

    public PointerFillStylePreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.pointer_icon_fill_style_layout);
        setSelectable(false);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        mButtonHolder = (LinearLayout) holder.findViewById(R.id.button_holder);
        // Intercept hover events so container does not highlight when hovering buttons.
        if (mButtonHolder != null) {
            mButtonHolder.setOnHoverListener((v, e) -> true);
        }

        int currentStyle = getPreferenceDataStore().getInt(Settings.System.POINTER_FILL_STYLE,
                POINTER_ICON_VECTOR_STYLE_FILL_BLACK);
        initStyleButton(holder, R.id.button_black, POINTER_ICON_VECTOR_STYLE_FILL_BLACK,
                currentStyle);
        initStyleButton(holder, R.id.button_green, POINTER_ICON_VECTOR_STYLE_FILL_GREEN,
                currentStyle);
        initStyleButton(holder, R.id.button_red, POINTER_ICON_VECTOR_STYLE_FILL_RED,
                currentStyle);
        initStyleButton(holder, R.id.button_pink, POINTER_ICON_VECTOR_STYLE_FILL_PINK,
                currentStyle);
        initStyleButton(holder, R.id.button_blue, POINTER_ICON_VECTOR_STYLE_FILL_BLUE,
                currentStyle);
        initStyleButton(holder, R.id.button_purple, POINTER_ICON_VECTOR_STYLE_FILL_PURPLE,
                currentStyle);
    }

    private void initStyleButton(@NonNull PreferenceViewHolder holder, int id, int style,
            int currentStyle) {
        ImageView button = (ImageView) holder.findViewById(id);
        if (button == null) {
            return;
        }
        tintButtonByStyle(button, style);
        button.setOnClickListener(
                (v) -> {
                    getPreferenceDataStore().putInt(Settings.System.POINTER_FILL_STYLE, style);
                    setButtonChecked(id);
                });
        button.setSelected(style == currentStyle);
        button.setPointerIcon(PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_ARROW));
    }

    private void tintButtonByStyle(ImageView button, int style) {
        int[] attrs = {com.android.internal.R.attr.pointerIconVectorFill};
        try (TypedArray ta = getContext().obtainStyledAttributes(
                PointerIcon.vectorFillStyleToResource(style), attrs)) {
            // Index 0, as there is only one attribute returned here.
            int color = ta.getColor(/* index= */ 0, Color.BLACK);
            StateListDrawable stateListDrawable = (StateListDrawable) button.getDrawable();
            tintDrawableByLayerId(stateListDrawable, STATE_HOVERED_SELECTED,
                    R.id.tintableCircleHoveredSelected, color);
            tintDrawableByLayerId(stateListDrawable, STATE_SELECTED, R.id.tintableCircleSelected,
                    color);
            tintDrawableByLayerId(stateListDrawable, STATE_HOVERED, R.id.tintableCircleHovered,
                    color);
            tintDrawableByLayerId(stateListDrawable, STATE_DEFAULT, R.id.tintableCircleDefault,
                    color);
        }
    }

    private void tintDrawableByLayerId(StateListDrawable stateListDrawable, int[] stateSet,
            int layerId, int color) {
        int index = stateListDrawable.findStateDrawableIndex(stateSet);
        LayerDrawable layerDrawable = (LayerDrawable) stateListDrawable.getStateDrawable(index);
        Drawable drawable = layerDrawable.findDrawableByLayerId(layerId);
        drawable.setTint(color);
    }

    private void setButtonChecked(int id) {
        if (mButtonHolder == null) {
            return;
        }
        for (int i = 0; i < mButtonHolder.getChildCount(); i++) {
            View child = mButtonHolder.getChildAt(i);
            if (child instanceof ImageView) {
                child.setSelected(child.getId() == id);
            }
        }
    }
}
