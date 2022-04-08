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

package com.android.settings.notification.zen;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RadioButton;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settingslib.TwoTargetPreference;

/**
 * A radio button preference with a divider and a settings icon that links to another screen.
 */
public class ZenCustomRadioButtonPreference extends TwoTargetPreference
        implements View.OnClickListener {

    private RadioButton mButton;
    private boolean mChecked;

    private OnGearClickListener mOnGearClickListener;
    private OnRadioButtonClickListener mOnRadioButtonClickListener;

    public ZenCustomRadioButtonPreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.preference_two_target_radio);
    }

    public ZenCustomRadioButtonPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayoutResource(R.layout.preference_two_target_radio);
    }

    public ZenCustomRadioButtonPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_two_target_radio);
    }

    public ZenCustomRadioButtonPreference(Context context) {
        super(context);
        setLayoutResource(R.layout.preference_two_target_radio);
    }

    @Override
    protected int getSecondTargetResId() {
        return R.layout.preference_widget_gear;
    }

    public void setOnGearClickListener(OnGearClickListener l) {
        mOnGearClickListener = l;
        notifyChanged();
    }

    public void setOnRadioButtonClickListener(OnRadioButtonClickListener l) {
        mOnRadioButtonClickListener = l;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        View buttonFrame = holder.findViewById(R.id.checkbox_frame);
        if (buttonFrame != null) {
            buttonFrame.setOnClickListener(this);
        }
        mButton = (RadioButton) holder.findViewById(android.R.id.checkbox);
        if (mButton != null) {
            mButton.setChecked(mChecked);
        }

        final View gear = holder.findViewById(android.R.id.widget_frame);
        final View divider = holder.findViewById(R.id.two_target_divider);
        if (mOnGearClickListener != null) {
            divider.setVisibility(View.VISIBLE);
            gear.setVisibility(View.VISIBLE);
            gear.setOnClickListener(this);
        } else {
            divider.setVisibility(View.GONE);
            gear.setVisibility(View.GONE);
            gear.setOnClickListener(null);
        }
    }

    public boolean isChecked() {
        return mButton != null && mChecked;
    }

    public void setChecked(boolean checked) {
        mChecked = checked;
        if (mButton != null) {
            mButton.setChecked(checked);
        }
    }

    public RadioButton getRadioButton() {
        return mButton;
    }

    @Override
    public void onClick() {
        if (mOnRadioButtonClickListener != null) {
            mOnRadioButtonClickListener.onRadioButtonClick(this);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == android.R.id.widget_frame) {
            if (mOnGearClickListener != null) {
                mOnGearClickListener.onGearClick(this);
            }
        } else if (v.getId() == R.id.checkbox_frame) {
            if (mOnRadioButtonClickListener != null) {
                mOnRadioButtonClickListener.onRadioButtonClick(this);
            }
        }
    }

    public interface OnGearClickListener {
        void onGearClick(ZenCustomRadioButtonPreference p);
    }

    public interface OnRadioButtonClickListener {
        void onRadioButtonClick(ZenCustomRadioButtonPreference p);
    }
}
