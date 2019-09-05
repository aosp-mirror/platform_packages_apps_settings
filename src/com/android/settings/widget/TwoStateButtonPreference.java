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
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

import androidx.annotation.VisibleForTesting;
import androidx.core.content.res.TypedArrayUtils;

import com.android.settings.R;
import com.android.settingslib.widget.LayoutPreference;

/**
 * Preference that presents a button with two states(On vs Off)
 */
public class TwoStateButtonPreference extends LayoutPreference implements
        View.OnClickListener {

    private boolean mIsChecked;
    private final Button mButtonOn;
    private final Button mButtonOff;

    public TwoStateButtonPreference(Context context, AttributeSet attrs) {
        super(context, attrs, TypedArrayUtils.getAttr(
                context, R.attr.twoStateButtonPreferenceStyle, android.R.attr.preferenceStyle));

        if (attrs == null) {
            mButtonOn = null;
            mButtonOff = null;
        } else {
            final TypedArray styledAttrs = context.obtainStyledAttributes(attrs,
                    R.styleable.TwoStateButtonPreference);
            final int textOnId = styledAttrs.getResourceId(
                    R.styleable.TwoStateButtonPreference_textOn,
                    R.string.summary_placeholder);
            final int textOffId = styledAttrs.getResourceId(
                    R.styleable.TwoStateButtonPreference_textOff,
                    R.string.summary_placeholder);
            styledAttrs.recycle();

            mButtonOn = findViewById(R.id.state_on_button);
            mButtonOn.setText(textOnId);
            mButtonOn.setOnClickListener(this);
            mButtonOff = findViewById(R.id.state_off_button);
            mButtonOff.setText(textOffId);
            mButtonOff.setOnClickListener(this);
            setChecked(isChecked());
        }
    }

    @Override
    public void onClick(View v) {
        final boolean stateOn = v.getId() == R.id.state_on_button;
        setChecked(stateOn);
        callChangeListener(stateOn);
    }

    public void setChecked(boolean checked) {
        // Update state
        mIsChecked = checked;
        // And update UI
        if (checked) {
            mButtonOn.setVisibility(View.GONE);
            mButtonOff.setVisibility(View.VISIBLE);
        } else {
            mButtonOn.setVisibility(View.VISIBLE);
            mButtonOff.setVisibility(View.GONE);
        }
    }

    public boolean isChecked() {
        return mIsChecked;
    }

    public void setButtonEnabled(boolean enabled) {
        mButtonOn.setEnabled(enabled);
        mButtonOff.setEnabled(enabled);
    }

    @VisibleForTesting
    public Button getStateOnButton() {
        return mButtonOn;
    }

    @VisibleForTesting
    public Button getStateOffButton() {
        return mButtonOff;
    }
}