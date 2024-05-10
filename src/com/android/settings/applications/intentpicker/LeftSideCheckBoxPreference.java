/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.applications.intentpicker;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckBox;

import androidx.preference.PreferenceViewHolder;

import com.android.settingslib.widget.TwoTargetPreference;

/** This preference has a check box in the left side. */
public class LeftSideCheckBoxPreference extends TwoTargetPreference {
    private boolean mChecked;
    private CheckBox mCheckBox;

    public LeftSideCheckBoxPreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(com.android.settingslib.R.layout.preference_checkable_two_target);
    }

    public LeftSideCheckBoxPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public LeftSideCheckBoxPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LeftSideCheckBoxPreference(Context context) {
        this(context, /* attrs= */ null);
    }

    public LeftSideCheckBoxPreference(Context context, boolean isChecked) {
        super(context);
        mChecked = isChecked;
        setLayoutResource(com.android.settingslib.R.layout.preference_checkable_two_target);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        mCheckBox = (CheckBox) view.findViewById(com.android.internal.R.id.checkbox);
        if (mCheckBox != null) {
            mCheckBox.setChecked(mChecked);
        }
    }

    @Override
    protected void onClick() {
        if (mCheckBox != null) {
            mChecked = !mChecked;
            mCheckBox.setChecked(mChecked);
            callChangeListener(mChecked);
        }
    }
}
