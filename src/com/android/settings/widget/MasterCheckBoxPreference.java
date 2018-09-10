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
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settingslib.TwoTargetPreference;

/**
 * A custom preference that provides inline checkbox. It has a mandatory field for title, and
 * optional fields for icon and sub-text.
 */
public class MasterCheckBoxPreference extends TwoTargetPreference {

    private CheckBox mCheckBox;
    private boolean mChecked;
    private boolean mEnableCheckBox = true;

    public MasterCheckBoxPreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public MasterCheckBoxPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MasterCheckBoxPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MasterCheckBoxPreference(Context context) {
        super(context);
    }

    @Override
    protected int getSecondTargetResId() {
        return R.layout.preference_widget_master_checkbox;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        final View widgetView = holder.findViewById(android.R.id.widget_frame);
        if (widgetView != null) {
            widgetView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mCheckBox != null && !mCheckBox.isEnabled()) {
                        return;
                    }
                    setChecked(!mChecked);
                    if (!callChangeListener(mChecked)) {
                        setChecked(!mChecked);
                    } else {
                        persistBoolean(mChecked);
                    }
                }
            });
        }

        mCheckBox = (CheckBox) holder.findViewById(R.id.checkboxWidget);
        if (mCheckBox != null) {
            mCheckBox.setContentDescription(getTitle());
            mCheckBox.setChecked(mChecked);
            mCheckBox.setEnabled(mEnableCheckBox);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setCheckBoxEnabled(enabled);
    }

    public boolean isChecked() {
        return mCheckBox != null && mChecked;
    }

    public void setChecked(boolean checked) {
        mChecked = checked;
        if (mCheckBox != null) {
            mCheckBox.setChecked(checked);
        }
    }

    public void setCheckBoxEnabled(boolean enabled) {
        mEnableCheckBox = enabled;
        if (mCheckBox != null) {
            mCheckBox.setEnabled(enabled);
        }
    }

    public CheckBox getCheckBox() {
        return mCheckBox;
    }
}
