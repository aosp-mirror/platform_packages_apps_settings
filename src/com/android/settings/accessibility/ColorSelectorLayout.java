/*
 * Copyright (C) 2023 The Android Open Source Project
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

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

import com.android.settings.R;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Layout for Screen flash notification color picker.
 */
public class ColorSelectorLayout extends LinearLayout {
    // Holds the checked id. The selection is empty by default
    private int mCheckedId = -1;
    // Tracks children radio buttons checked state
    private CompoundButton.OnCheckedChangeListener mChildOnCheckedChangeListener;

    private ColorSelectorLayout.OnCheckedChangeListener mOnCheckedChangeListener;

    private final List<Integer> mRadioButtonResourceIds = Arrays.asList(
            R.id.color_radio_button_00,
            R.id.color_radio_button_01,
            R.id.color_radio_button_02,
            R.id.color_radio_button_03,
            R.id.color_radio_button_04,
            R.id.color_radio_button_05,
            R.id.color_radio_button_06,
            R.id.color_radio_button_07,
            R.id.color_radio_button_08,
            R.id.color_radio_button_09,
            R.id.color_radio_button_10,
            R.id.color_radio_button_11
    );

    private List<Integer> mColorList;

    public ColorSelectorLayout(Context context) {
        super(context);
        mChildOnCheckedChangeListener = new CheckedStateTracker();
        inflate(mContext, R.layout.layout_color_selector, this);
        init();
        mColorList = Arrays.stream(mContext.getResources()
                        .getIntArray(R.array.screen_flash_notification_preset_opacity_colors))
                .boxed()
                .collect(Collectors.toList());
    }

    public ColorSelectorLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mChildOnCheckedChangeListener = new CheckedStateTracker();
        inflate(mContext, R.layout.layout_color_selector, this);
        init();
        mColorList = Arrays.stream(mContext.getResources()
                        .getIntArray(R.array.screen_flash_notification_preset_opacity_colors))
                .boxed()
                .collect(Collectors.toList());
    }

    private void init() {
        for (int resId : mRadioButtonResourceIds) {
            RadioButton radioButton = findViewById(resId);
            if (radioButton != null) {
                radioButton.setOnCheckedChangeListener(mChildOnCheckedChangeListener);
            }
        }
    }

    void setOnCheckedChangeListener(ColorSelectorLayout.OnCheckedChangeListener listener) {
        mOnCheckedChangeListener = listener;
    }

    void setCheckedColor(@ColorInt int color) {
        int resId = getResId(mColorList.indexOf(color));
        if (resId != NO_ID && resId == mCheckedId) return;

        if (mCheckedId != NO_ID) {
            setCheckedStateForView(mCheckedId, false);
        }

        if (resId != NO_ID) {
            setCheckedStateForView(resId, true);
        }

        setCheckedId(resId);
    }

    int getCheckedColor(int defaultColor) {
        int checkedItemIndex = mRadioButtonResourceIds.indexOf(mCheckedId);
        if (checkedItemIndex < 0 || checkedItemIndex >= mColorList.size()) {
            return defaultColor;
        } else {
            return mColorList.get(checkedItemIndex);
        }
    }

    private int getResId(int index) {
        if (index < 0 || index >= mRadioButtonResourceIds.size()) {
            return NO_ID;
        } else {
            return mRadioButtonResourceIds.get(index);
        }
    }

    private void setCheckedId(int resId) {
        mCheckedId = resId;
        if (mOnCheckedChangeListener != null) {
            mOnCheckedChangeListener.onCheckedChanged(this);
        }
    }

    private void setCheckedStateForView(int viewId, boolean checked) {
        final View checkedView = findViewById(viewId);
        if (checkedView instanceof RadioButton) {
            ((RadioButton) checkedView).setChecked(checked);
        }
    }

    interface OnCheckedChangeListener {
        void onCheckedChanged(ColorSelectorLayout group);
    }

    private class CheckedStateTracker implements CompoundButton.OnCheckedChangeListener {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (!isChecked) {
                return;
            }

            if (mCheckedId != NO_ID) {
                setCheckedStateForView(mCheckedId, false);
            }

            int id = buttonView.getId();
            setCheckedId(id);
        }
    }
}
