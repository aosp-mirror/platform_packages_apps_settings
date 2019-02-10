/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.display;

import android.content.Context;
import android.hardware.display.ColorDisplayManager;
import android.hardware.display.NightDisplayListener;
import android.util.AttributeSet;

import androidx.preference.SwitchPreference;

import java.time.LocalTime;

public class NightDisplayPreference extends SwitchPreference
        implements NightDisplayListener.Callback {

    private ColorDisplayManager mColorDisplayManager;
    private NightDisplayListener mNightDisplayListener;
    private NightDisplayTimeFormatter mTimeFormatter;

    public NightDisplayPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mColorDisplayManager = context.getSystemService(ColorDisplayManager.class);
        mNightDisplayListener = new NightDisplayListener(context);
        mTimeFormatter = new NightDisplayTimeFormatter(context);
    }

    @Override
    public void onAttached() {
        super.onAttached();

        // Listen for changes only while attached.
        mNightDisplayListener.setCallback(this);

        // Update the summary since the state may have changed while not attached.
        updateSummary();
    }

    @Override
    public void onDetached() {
        super.onDetached();

        // Stop listening for state changes.
        mNightDisplayListener.setCallback(null);
    }

    @Override
    public void onActivated(boolean activated) {
        updateSummary();
    }

    @Override
    public void onAutoModeChanged(int autoMode) {
        updateSummary();
    }

    @Override
    public void onCustomStartTimeChanged(LocalTime startTime) {
        updateSummary();
    }

    @Override
    public void onCustomEndTimeChanged(LocalTime endTime) {
        updateSummary();
    }

    private void updateSummary() {
        setSummary(mTimeFormatter.getAutoModeTimeSummary(getContext(), mColorDisplayManager));
    }
}
