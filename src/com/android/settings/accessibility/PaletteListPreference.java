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

package com.android.settings.accessibility;

import static android.graphics.drawable.GradientDrawable.Orientation;

import static com.android.settings.accessibility.AccessibilityUtil.getScreenHeightPixels;
import static com.android.settings.accessibility.AccessibilityUtil.getScreenWidthPixels;

import static com.google.common.primitives.Ints.max;

import android.content.Context;
import android.graphics.Paint.FontMetrics;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.IntDef;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** Preference that easier preview by matching name to color. */
public final class PaletteListPreference extends Preference {

    private final List<Integer> mGradientColors = new ArrayList<>();
    private final List<Float> mGradientOffsets = new ArrayList<>();

    @IntDef({
            Position.START,
            Position.CENTER,
            Position.END,
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface Position {
        int START = 0;
        int CENTER = 1;
        int END = 2;
    }

    /**
     * Constructs a new PaletteListPreference with the given context's theme and the supplied
     * attribute set.
     *
     * @param context The Context this is associated with, through which it can access the current
     *                theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    public PaletteListPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Constructs a new PaletteListPreference with the given context's theme, the supplied
     * attribute set, and default style attribute.
     *
     * @param context The Context this is associated with, through which it can access the
     *                current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @param defStyleAttr An attribute in the current theme that contains a reference to a style
     *                     resource that supplies default
     *                     values for the view. Can be 0 to not look for
     *                     defaults.
     */
    public PaletteListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayoutResource(R.layout.daltonizer_preview);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        final ViewGroup paletteView = holder.itemView.findViewById(R.id.palette_view);
        initPaletteAttributes(getContext());
        initPaletteView(getContext(), paletteView);
    }

    private void initPaletteAttributes(Context context) {
        final int defaultColor = context.getColor(R.color.palette_list_gradient_background);
        mGradientColors.add(Position.START, defaultColor);
        mGradientColors.add(Position.CENTER, defaultColor);
        mGradientColors.add(Position.END, defaultColor);

        mGradientOffsets.add(Position.START, /* element= */ 0.0f);
        mGradientOffsets.add(Position.CENTER, /* element= */ 0.5f);
        mGradientOffsets.add(Position.END, /* element= */ 1.0f);
    }

    private void initPaletteView(Context context, ViewGroup rootView) {
        if (rootView.getChildCount() > 0) {
            rootView.removeAllViews();
        }

        final List<Integer> paletteColors = getPaletteColors(context);
        final List<String> paletteData = getPaletteData(context);

        final float textPadding =
                context.getResources().getDimension(R.dimen.accessibility_layout_margin_start_end);
        final String maxLengthData =
                Collections.max(paletteData, Comparator.comparing(String::length));
        final int textWidth = getTextWidth(context, maxLengthData);
        final float textBound = (textWidth + textPadding) / getScreenWidthPixels(context);
        mGradientOffsets.set(Position.CENTER, textBound);

        final int screenHalfHeight = getScreenHeightPixels(context) / 2;
        final int paletteItemHeight =
                max(screenHalfHeight / paletteData.size(), getTextLineHeight(context));

        for (int i = 0; i < paletteData.size(); ++i) {
            final TextView textView = new TextView(context);
            textView.setText(paletteData.get(i));
            textView.setHeight(paletteItemHeight);
            textView.setPaddingRelative(Math.round(textPadding), 0, 0, 0);
            textView.setGravity(Gravity.CENTER_VERTICAL);
            textView.setBackground(createGradientDrawable(rootView, paletteColors.get(i)));

            rootView.addView(textView);
        }

        updateFirstAndLastItemsBackground(context, rootView, paletteData.size());
    }

    private GradientDrawable createGradientDrawable(ViewGroup rootView, @ColorInt int color) {
        mGradientColors.set(Position.END, color);

        final GradientDrawable gradientDrawable = new GradientDrawable();
        final Orientation orientation =
                rootView.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL
                        ? Orientation.RIGHT_LEFT
                        : Orientation.LEFT_RIGHT;
        gradientDrawable.setOrientation(orientation);
        gradientDrawable.setColors(Ints.toArray(mGradientColors), Floats.toArray(mGradientOffsets));

        return gradientDrawable;
    }

    private void updateFirstAndLastItemsBackground(Context context, ViewGroup rootView, int size) {
        final int radius =
                context.getResources().getDimensionPixelSize(
                        R.dimen.accessibility_illustration_view_radius);
        final int lastIndex = size - 1;
        final GradientDrawable firstItem =
                (GradientDrawable) rootView.getChildAt(0).getBackground();
        final GradientDrawable lastItem =
                (GradientDrawable) rootView.getChildAt(lastIndex).getBackground();
        firstItem.setCornerRadii(new float[]{radius, radius, radius, radius, 0, 0, 0, 0});
        lastItem.setCornerRadii(new float[]{0, 0, 0, 0, radius, radius, radius, radius});
    }

    private List<Integer> getPaletteColors(Context context) {
        final int[] paletteResources =
                context.getResources().getIntArray(R.array.setting_palette_colors);
        return Arrays.stream(paletteResources).boxed().collect(Collectors.toList());
    }

    private List<String> getPaletteData(Context context) {
        final String[] paletteResources =
                context.getResources().getStringArray(R.array.setting_palette_data);
        return Arrays.asList(paletteResources);
    }

    private int getTextWidth(Context context, String text) {
        final TextView tempView = new TextView(context);
        return Math.round(tempView.getPaint().measureText(text));
    }

    private int getTextLineHeight(Context context) {
        final TextView tempView = new TextView(context);
        final FontMetrics fontMetrics = tempView.getPaint().getFontMetrics();
        return Math.round(fontMetrics.bottom - fontMetrics.top);
    }
}
