/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.biometrics.fingerprint;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;

import com.android.settings.R;

/**
 * A layout that contains a start-justified title, and an end-justified switch.
 */
public class FingerprintRequireScreenOnToAuthToggle extends LinearLayout {
    private Switch mSwitch;

    public FingerprintRequireScreenOnToAuthToggle(Context context) {
        this(context, null /* attrs */);
    }

    public FingerprintRequireScreenOnToAuthToggle(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FingerprintRequireScreenOnToAuthToggle(
            Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater.from(context).inflate(R.layout.sfps_require_screen_on_to_auth_toggle,
                this, true /* attachToRoot */);

        mSwitch = findViewById(R.id.toggle);
        mSwitch.setClickable(true);
        mSwitch.setFocusable(false);
    }

    public boolean isChecked() {
        return mSwitch.isChecked();
    }

    /**
     *
     * @param checked
     */
    public void setChecked(boolean checked) {
        mSwitch.setChecked(checked);
    }

    /**
     *
     * @param listener
     */
    public void setListener(CompoundButton.OnCheckedChangeListener listener) {
        mSwitch.setOnCheckedChangeListener(listener);
    }

    public Switch getSwitch() {
        return mSwitch;
    }
}
