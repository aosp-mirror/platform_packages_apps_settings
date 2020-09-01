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
 * limitations under the License.
 */

package com.android.settings.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.preference.CheckBoxPreference;
import androidx.preference.PreferenceViewHolder;

/**
 * A CheckboxPreference that can disable its checkbox separate from its text.
 * Differs from CheckboxPreference.setDisabled() in that the text is not dimmed.
 */
public class DisabledCheckBoxPreference extends CheckBoxPreference {
    private PreferenceViewHolder mViewHolder;
    private View mCheckBox;
    private boolean mEnabledCheckBox;

    public DisabledCheckBoxPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setupDisabledCheckBoxPreference(context, attrs, defStyleAttr, defStyleRes);
    }

    public DisabledCheckBoxPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setupDisabledCheckBoxPreference(context, attrs, defStyleAttr, 0);
    }

    public DisabledCheckBoxPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupDisabledCheckBoxPreference(context, attrs, 0, 0);
    }

    public DisabledCheckBoxPreference(Context context) {
        super(context);
        setupDisabledCheckBoxPreference(context, null, 0, 0);
    }

    private void setupDisabledCheckBoxPreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        final TypedArray a = context.obtainStyledAttributes(
                attrs, com.android.internal.R.styleable.Preference, defStyleAttr, defStyleRes);
        for (int i = a.getIndexCount() - 1; i >= 0; i--) {
            int attr = a.getIndex(i);
            switch (attr) {
                case com.android.internal.R.styleable.Preference_enabled:
                    mEnabledCheckBox = a.getBoolean(attr, true);
                    break;
            }
        }
        a.recycle();

        // Always tell super class this preference is enabled.
        // We manually enable/disable checkbox using enableCheckBox.
        super.setEnabled(true);
        enableCheckbox(mEnabledCheckBox);
    }

    public void enableCheckbox(boolean enabled) {
        mEnabledCheckBox = enabled;
        if (mViewHolder != null && mCheckBox != null) {
            mCheckBox.setEnabled(mEnabledCheckBox);
            mViewHolder.itemView.setEnabled(mEnabledCheckBox);
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mViewHolder = holder;
        mCheckBox = holder.findViewById(android.R.id.checkbox);

        enableCheckbox(mEnabledCheckBox);

        TextView title = (TextView) holder.findViewById(android.R.id.title);
        if (title != null) {
            title.setSingleLine(false);
            title.setMaxLines(2);
            title.setEllipsize(TextUtils.TruncateAt.END);
        }
    }

    @Override
    protected void performClick(View view) {
        // only perform clicks if the checkbox is enabled
        if (mEnabledCheckBox) {
            super.performClick(view);
        }
    }

}
