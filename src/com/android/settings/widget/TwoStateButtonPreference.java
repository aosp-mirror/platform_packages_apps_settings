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
import android.support.v4.content.res.TypedArrayUtils;
import android.util.AttributeSet;
import android.widget.Button;

import com.android.settings.R;
import com.android.settings.applications.LayoutPreference;

/**
 * Preference that presents a button with two states(On vs Off)
 */
public class TwoStateButtonPreference extends LayoutPreference {
    public TwoStateButtonPreference(Context context, AttributeSet attrs) {
        super(context, attrs, TypedArrayUtils.getAttr(
                context, R.attr.twoStateButtonPreferenceStyle, android.R.attr.preferenceStyle));

        if (attrs != null) {
            final TypedArray styledAttrs = context.obtainStyledAttributes(attrs,
                    R.styleable.TwoStateButtonPreference);
            final int textOnId = styledAttrs.getResourceId(
                    R.styleable.TwoStateButtonPreference_textOn,
                    R.string.summary_placeholder);
            final int textOffId = styledAttrs.getResourceId(
                    R.styleable.TwoStateButtonPreference_textOff,
                    R.string.summary_placeholder);
            styledAttrs.recycle();

            final Button buttonOn = getStateOnButton();
            buttonOn.setText(textOnId);
            final Button buttonOff = getStateOffButton();
            buttonOff.setText(textOffId);
        }
    }

    public Button getStateOnButton() {
        return findViewById(R.id.state_on_button);
    }


    public Button getStateOffButton() {
        return findViewById(R.id.state_off_button);
    }
}