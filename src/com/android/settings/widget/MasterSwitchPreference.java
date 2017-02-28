/*
 * Copyright (C) 2017 The Android Open Source Project
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
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

/**
 * A custom preference that provides inline switch toggle. It has a mandatory field for title, and
 * optional fields for icon and sub-text.
 */
public class MasterSwitchPreference extends Preference {

    private Switch mSwitch;
    private boolean mChecked;
    private boolean mMultiLine;

    public MasterSwitchPreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public MasterSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public MasterSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MasterSwitchPreference(Context context) {
        super(context);
        init();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mSwitch = (Switch) holder.itemView.findViewById(R.id.switchWidget);
        if (mSwitch != null) {
            mSwitch.setChecked(mChecked);
            mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton button, boolean isChecked) {
                    if (!callChangeListener(isChecked)) {
                        button.setChecked(!isChecked);
                    } else {
                        persistBoolean(isChecked);
                        mChecked = isChecked;
                    }
                }
            });
        }
        if (mMultiLine) {
            TextView textView = (TextView)holder.findViewById(android.R.id.title);
            if (textView != null) {
                textView.setSingleLine(false);
            }
        }
    }

    public boolean isChecked() {
        return mSwitch != null && mSwitch.isEnabled() && mChecked;
    }

    public void setChecked(boolean checked) {
        mChecked = checked;
        if (mSwitch != null) {
            mSwitch.setChecked(checked);
        }
    }

    public boolean isSwitchEnabled() {
        return mSwitch != null && mSwitch.isEnabled();
    }

    public void setSwitchEnabled(boolean enabled) {
        if (mSwitch != null) {
            mSwitch.setEnabled(enabled);
        }
    }

    public boolean isMultiLine() {
        return mMultiLine;
    }

    public void setMultiLine(boolean multiLine) {
        mMultiLine = multiLine;
    }

    /**
     * If admin is not null, disables the switch.
     * Otherwise, keep it enabled.
     */
    public void setDisabledByAdmin(EnforcedAdmin admin) {
        setSwitchEnabled(admin == null);
    }

    public Switch getSwitch() {
        return mSwitch;
    }

    private void init() {
        setLayoutResource(R.layout.preference_master_switch);
        setWidgetLayoutResource(R.layout.preference_widget_master_switch);
    }
}
