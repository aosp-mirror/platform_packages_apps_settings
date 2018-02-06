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
import android.support.v7.preference.PreferenceScreen;
import android.view.View;
import android.widget.Button;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

/**
 * Controller to update the button with two states(On vs Off).
 */
public abstract class TwoStateButtonPreferenceController extends BasePreferenceController
        implements View.OnClickListener {
    private Button mButtonOn;
    private Button mButtonOff;

    public TwoStateButtonPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final TwoStateButtonPreference preference =
                (TwoStateButtonPreference) screen.findPreference(getPreferenceKey());
        mButtonOn = preference.getStateOnButton();
        mButtonOn.setOnClickListener(this);
        mButtonOff = preference.getStateOffButton();
        mButtonOff.setOnClickListener(this);
    }

    protected void setButtonVisibility(boolean stateOn) {
        if (stateOn) {
            mButtonOff.setVisibility(View.GONE);
            mButtonOn.setVisibility(View.VISIBLE);
        } else {
            mButtonOff.setVisibility(View.VISIBLE);
            mButtonOn.setVisibility(View.GONE);
        }
    }

    protected void setButtonEnabled(boolean enabled) {
        mButtonOn.setEnabled(enabled);
        mButtonOff.setEnabled(enabled);
    }

    @Override
    public void onClick(View v) {
        final boolean stateOn = v.getId() == R.id.state_on_button;
        onButtonClicked(stateOn);
    }

    /**
     * Callback when button is clicked
     *
     * @param stateOn {@code true} if stateOn button is clicked, otherwise it means stateOff
     *                button is clicked
     */
    public abstract void onButtonClicked(boolean stateOn);
}