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

import android.annotation.NonNull;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;

import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Custom ListView {@link ListView} which displays palette to deploy the color code preview.
 *
 * <p>The preview shows gradient from color white to specific color code on each list view item, in
 * addition, text view adjusts the attribute of width for adapting the text length.
 *
 * <p>The text cannot fills the whole view for ensuring the gradient color preview can purely
 * display also the view background shows the color beside the text variable end point.
 */
public class PaletteListView extends ListView {
    private final Context mContext;
    private final DisplayAdapter mDisplayAdapter;
    private final LayoutInflater mLayoutInflater;
    private final String mDefaultGradientColorCodeString;
    private final int mDefaultGradientColor;
    private float mTextBound;
    private static final float LANDSCAPE_MAX_WIDTH_PERCENTAGE = 100f;

    public PaletteListView(Context context) {
        this(context, null);
    }

    public PaletteListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PaletteListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        mDisplayAdapter = new DisplayAdapter();
        mLayoutInflater = LayoutInflater.from(context);
        mDefaultGradientColorCodeString =
                getResources().getString(R.color.palette_list_gradient_background);
        mDefaultGradientColor =
                getResources().getColor(R.color.palette_list_gradient_background, null);
        mTextBound = 0.0f;
        init();
    }

    private static int getScreenWidth(WindowManager windowManager) {
        final Display display = windowManager.getDefaultDisplay();
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getMetrics(displayMetrics);
        return displayMetrics.widthPixels;
    }

    private void init() {
        final TypedArray colorNameArray = getResources().obtainTypedArray(
                R.array.setting_palette_colors);
        final TypedArray colorCodeArray = getResources().obtainTypedArray(
                R.array.setting_palette_data);
        final int colorNameArrayLength = colorNameArray.length();
        final List<ColorAttributes> colorList = new ArrayList<>();
        computeTextWidthBounds(colorNameArray);

        for (int index = 0; index < colorNameArrayLength; index++) {
            colorList.add(
                    new ColorAttributes(
                            /* colorName= */ colorNameArray.getString(index),
                            /* colorCode= */ colorCodeArray.getColor(index, mDefaultGradientColor),
                            /* textBound= */ mTextBound,
                            /* gradientDrawable= */
                            new GradientDrawable(Orientation.LEFT_RIGHT, null)));
        }

        mDisplayAdapter.setColorList(colorList);
        setAdapter(mDisplayAdapter);
        setDividerHeight(/* height= */ 0);
    }

    /**
     * Sets string array that required the color name and color code for deploy the new color
     * preview.
     *
     * <p>The parameters not allow null define but two array length inconsistent are acceptable, in
     * addition, to prevent IndexOutOfBoundsException the algorithm will check array data, and base
     * on the array size to display data, or fills color code array if length less than other.
     *
     * @param colorNames a string array of color name
     * @param colorCodes a string array of color code
     * @return true if new array data apply successful
     */
    @VisibleForTesting
    boolean setPaletteListColors(@NonNull String[] colorNames, @NonNull String[] colorCodes) {
        if (colorNames == null || colorCodes == null) {
            return false;
        }

        final int colorNameArrayLength = colorNames.length;
        final int colorCodeArrayLength = colorCodes.length;
        final List<ColorAttributes> colorList = new ArrayList<>();
        final String[] colorCodeArray = fillColorCodeArray(colorCodes, colorNameArrayLength,
                colorCodeArrayLength);
        computeTextWidthBounds(colorNames);

        for (int index = 0; index < colorNameArrayLength; index++) {
            colorList.add(
                    new ColorAttributes(
                            /* colorName= */ colorNames[index],
                            /* colorCode= */ Color.parseColor(colorCodeArray[index]),
                            /* textBound= */ mTextBound,
                            /* gradientDrawable= */
                            new GradientDrawable(Orientation.LEFT_RIGHT, null)));
        }

        mDisplayAdapter.setColorList(colorList);
        mDisplayAdapter.notifyDataSetChanged();
        return true;
    }

    private String[] fillColorCodeArray(String[] colorCodes, int colorNameArrayLength,
            int colorCodeArrayLength) {
        if (colorNameArrayLength == colorCodeArrayLength
                || colorNameArrayLength < colorCodeArrayLength) {
            return colorCodes;
        }

        final String[] colorCodeArray = new String[colorNameArrayLength];
        for (int index = 0; index < colorNameArrayLength; index++) {
            if (index < colorCodeArrayLength) {
                colorCodeArray[index] = colorCodes[index];
            } else {
                colorCodeArray[index] = mDefaultGradientColorCodeString;
            }
        }
        return colorCodeArray;
    }

    private void computeTextWidthBounds(TypedArray colorNameTypedArray) {
        final int colorNameArrayLength = colorNameTypedArray.length();
        final String[] colorNames = new String[colorNameArrayLength];
        for (int index = 0; index < colorNameArrayLength; index++) {
            colorNames[index] = colorNameTypedArray.getString(index);
        }

        measureBound(colorNames);
    }

    private void computeTextWidthBounds(String[] colorNameArray) {
        final int colorNameArrayLength = colorNameArray.length;
        final String[] colorNames = new String[colorNameArrayLength];
        for (int index = 0; index < colorNameArrayLength; index++) {
            colorNames[index] = colorNameArray[index];
        }

        measureBound(colorNames);
    }

    private void measureBound(String[] dataArray) {
        final WindowManager windowManager = (WindowManager) mContext.getSystemService(
                Context.WINDOW_SERVICE);
        final View view = mLayoutInflater.inflate(R.layout.palette_listview_item, null);
        final TextView textView = view.findViewById(R.id.item_textview);
        final List<String> colorNameList = new ArrayList<>(Arrays.asList(dataArray));
        Collections.sort(colorNameList, Comparator.comparing(String::length));
        // Gets the last index of list which sort by text length.
        textView.setText(Iterables.getLast(colorNameList));

        final float textWidth = textView.getPaint().measureText(textView.getText().toString());
        // Computes rate of text width compare to screen width, and measures the round the double
        // to two decimal places manually.
        final float textBound = Math.round(
                textWidth / getScreenWidth(windowManager) * LANDSCAPE_MAX_WIDTH_PERCENTAGE)
                / LANDSCAPE_MAX_WIDTH_PERCENTAGE;

        // Left padding and right padding with color preview.
        final float paddingPixel = getResources().getDimension(
                R.dimen.accessibility_layout_margin_start_end);
        final float paddingWidth =
                Math.round(paddingPixel / getScreenWidth(windowManager)
                        * LANDSCAPE_MAX_WIDTH_PERCENTAGE) / LANDSCAPE_MAX_WIDTH_PERCENTAGE;
        mTextBound = textBound + paddingWidth + paddingWidth;
    }

    private static class ViewHolder {
        public TextView textView;
    }

    /** An adapter that converts color text title and color code to text views. */
    private final class DisplayAdapter extends BaseAdapter {

        private List<ColorAttributes> mColorList;

        @Override
        public int getCount() {
            return mColorList.size();
        }

        @Override
        public Object getItem(int position) {
            return mColorList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final ViewHolder viewHolder;
            final ColorAttributes paletteAttribute = mColorList.get(position);
            final String colorName = paletteAttribute.getColorName();
            final GradientDrawable gradientDrawable = paletteAttribute.getGradientDrawable();

            if (convertView == null) {
                convertView = mLayoutInflater.inflate(R.layout.palette_listview_item, null);
                viewHolder = new ViewHolder();
                viewHolder.textView = convertView.findViewById(R.id.item_textview);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            viewHolder.textView.setText(colorName);
            viewHolder.textView.setBackground(gradientDrawable);
            return convertView;
        }

        protected void setColorList(List<ColorAttributes> colorList) {
            mColorList = colorList;
        }
    }

    private final class ColorAttributes {
        private final int mColorIndex = 2; // index for inject color.
        private final int mColorOffsetIndex = 1; // index for offset effect.
        private final String mColorName;
        private final GradientDrawable mGradientDrawable;
        private final int[] mGradientColors =
                {/* startColor=*/ mDefaultGradientColor, /* centerColor=*/ mDefaultGradientColor,
                        /* endCode= */ 0};
        private final float[] mGradientOffsets =
                {/* starOffset= */ 0.0f, /* centerOffset= */ 0.5f, /* endOffset= */ 1.0f};

        ColorAttributes(
                String colorName, int colorCode, float textBound,
                GradientDrawable gradientDrawable) {
            mGradientColors[mColorIndex] = colorCode;
            mGradientOffsets[mColorOffsetIndex] = textBound;
            gradientDrawable.setColors(mGradientColors, mGradientOffsets);
            mColorName = colorName;
            mGradientDrawable = gradientDrawable;
        }

        public String getColorName() {
            return mColorName;
        }

        public GradientDrawable getGradientDrawable() {
            return mGradientDrawable;
        }
    }
}
