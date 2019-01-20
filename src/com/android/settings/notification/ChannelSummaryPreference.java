/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.notification;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.CheckBox;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settingslib.TwoTargetPreference;

/**
 * A custom preference that provides inline checkbox and tappable target.
 */
public class ChannelSummaryPreference extends TwoTargetPreference {

    private Context mContext;
    private Intent mIntent;
    private CheckBox mCheckBox;
    private boolean mChecked;
    private boolean mEnableCheckBox = true;

    public ChannelSummaryPreference(Context context) {
        super(context);
        setLayoutResource(R.layout.preference_checkable_two_target);
        mContext = context;
        setWidgetLayoutResource(R.layout.zen_rule_widget);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        View settingsWidget = view.findViewById(android.R.id.widget_frame);
        View divider = view.findViewById(R.id.two_target_divider);
        if (mIntent != null) {
            divider.setVisibility(View.VISIBLE);
            settingsWidget.setVisibility(View.VISIBLE);
            settingsWidget.setOnClickListener(v -> mContext.startActivity(mIntent));
        } else {
            divider.setVisibility(View.GONE);
            settingsWidget.setVisibility(View.GONE);
            settingsWidget.setOnClickListener(null);
        }

        View checkboxContainer = view.findViewById(R.id.checkbox_container);
        if (checkboxContainer != null) {
            checkboxContainer.setOnClickListener(mOnCheckBoxClickListener);
        }
        mCheckBox = (CheckBox) view.findViewById(com.android.internal.R.id.checkbox);
        if (mCheckBox != null) {
            mCheckBox.setChecked(mChecked);
            mCheckBox.setEnabled(mEnableCheckBox);
        }
    }

    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void setIntent(Intent intent) {
        mIntent = intent;
    }

    @Override
    public void onClick() {
        mOnCheckBoxClickListener.onClick(null);
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

    private View.OnClickListener mOnCheckBoxClickListener = new View.OnClickListener() {
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
    };
}
