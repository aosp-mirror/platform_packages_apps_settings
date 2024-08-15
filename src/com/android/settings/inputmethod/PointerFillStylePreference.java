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
import static android.view.PointerIcon.POINTER_ICON_VECTOR_STYLE_FILL_YELLOW;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.StateSet;
import android.view.Gravity;
import android.view.PointerIcon;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settingslib.Utils;


public class PointerFillStylePreference extends Preference {

    @Nullable private LinearLayout mButtonHolder;

    public PointerFillStylePreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.pointer_icon_fill_style_layout);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        mButtonHolder = (LinearLayout) holder.findViewById(R.id.button_holder);
        // Intercept hover events so setting row does not highlight when hovering buttons.
        if (mButtonHolder != null) {
            mButtonHolder.setOnHoverListener((v, e) -> true);
        }

        int currentStyle = getPreferenceDataStore().getInt(Settings.System.POINTER_FILL_STYLE,
                POINTER_ICON_VECTOR_STYLE_FILL_BLACK);
        initStyleButton(holder, R.id.button_black, POINTER_ICON_VECTOR_STYLE_FILL_BLACK,
                currentStyle);
        initStyleButton(holder, R.id.button_green, POINTER_ICON_VECTOR_STYLE_FILL_GREEN,
                currentStyle);
        initStyleButton(holder, R.id.button_yellow, POINTER_ICON_VECTOR_STYLE_FILL_YELLOW,
                currentStyle);
        initStyleButton(holder, R.id.button_pink, POINTER_ICON_VECTOR_STYLE_FILL_PINK,
                currentStyle);
        initStyleButton(holder, R.id.button_blue, POINTER_ICON_VECTOR_STYLE_FILL_BLUE,
                currentStyle);
    }

    private void initStyleButton(@NonNull PreferenceViewHolder holder, int id, int style,
            int currentStyle) {
        ImageView button = (ImageView) holder.findViewById(id);
        if (button == null) {
            return;
        }
        int[] attrs = {com.android.internal.R.attr.pointerIconVectorFill};
        try (TypedArray ta = getContext().obtainStyledAttributes(
                PointerIcon.vectorFillStyleToResource(style), attrs)) {
            button.setBackground(getBackgroundSelector(ta.getColor(0, Color.BLACK)));
        }
        button.setForeground(getForegroundDrawable(style, currentStyle));
        button.setForegroundGravity(Gravity.CENTER);
        button.setOnClickListener(
                (v) -> {
                    getPreferenceDataStore().putInt(Settings.System.POINTER_FILL_STYLE, style);
                    setButtonChecked(id);
                });
        button.setPointerIcon(PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_ARROW));
    }

    // Generate background instead of defining in XML so we can use res color from the platform.
    private StateListDrawable getBackgroundSelector(int color) {
        StateListDrawable background = new StateListDrawable();
        Resources res = getContext().getResources();
        int ovalSize = res.getDimensionPixelSize(R.dimen.pointer_fill_style_circle_diameter);
        background.setBounds(0, 0, ovalSize, ovalSize);

        // Add hovered state first! The order states are added matters for a StateListDrawable.
        GradientDrawable hoveredOval = new GradientDrawable();
        hoveredOval.setColor(color);
        int textColor = Utils.getColorAttr(getContext(),
                com.android.internal.R.attr.materialColorOutline).getDefaultColor();
        hoveredOval.setStroke(
                res.getDimensionPixelSize(R.dimen.pointer_fill_style_shape_hovered_stroke),
                textColor);
        hoveredOval.setSize(ovalSize, ovalSize);
        hoveredOval.setBounds(0, 0, ovalSize, ovalSize);
        hoveredOval.setCornerRadius(ovalSize / 2f);
        background.addState(new int[]{android.R.attr.state_hovered}, hoveredOval);

        GradientDrawable defaultOval = new GradientDrawable();
        defaultOval.setColor(color);
        defaultOval.setStroke(
                res.getDimensionPixelSize(R.dimen.pointer_fill_style_shape_default_stroke),
                textColor);
        defaultOval.setSize(ovalSize, ovalSize);
        defaultOval.setBounds(0, 0, ovalSize, ovalSize);
        defaultOval.setCornerRadius(ovalSize / 2f);
        background.addState(StateSet.WILD_CARD, defaultOval);

        return background;
    }

    private Drawable getForegroundDrawable(int style, int currentStyle) {
        Resources res = getContext().getResources();
        int ovalSize = res.getDimensionPixelSize(R.dimen.pointer_fill_style_circle_diameter);
        Drawable checkMark = getContext().getDrawable(R.drawable.ic_check_24dp);
        int padding = res.getDimensionPixelSize(R.dimen.pointer_fill_style_circle_padding) / 2;
        checkMark.setBounds(padding, padding, ovalSize - padding, ovalSize - padding);
        checkMark.setColorFilter(new BlendModeColorFilter(Color.WHITE, BlendMode.SRC_IN));
        checkMark.setAlpha(style == currentStyle ? 255 : 0);
        return checkMark;
    }

    private void setButtonChecked(int id) {
        if (mButtonHolder == null) {
            return;
        }
        for (int i = 0; i < mButtonHolder.getChildCount(); i++) {
            View child = mButtonHolder.getChildAt(i);
            if (child != null) {
                child.getForeground().setAlpha(child.getId() == id ? 255 : 0);
            }
        }
    }
}
