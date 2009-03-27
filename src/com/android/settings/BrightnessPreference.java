/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.settings;

import android.content.Context;
import android.os.RemoteException;
import android.os.IHardwareService;
import android.os.ServiceManager;
import android.preference.SeekBarPreference;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;

import java.util.Map;

public class BrightnessPreference extends SeekBarPreference implements
        SeekBar.OnSeekBarChangeListener {

    private SeekBar mSeekBar;
    
    private int mOldBrightness;
    
    // Backlight range is from 0 - 255. Need to make sure that user
    // doesn't set the backlight to 0 and get stuck
    private static final int MINIMUM_BACKLIGHT = android.os.Power.BRIGHTNESS_DIM + 10;
    private static final int MAXIMUM_BACKLIGHT = android.os.Power.BRIGHTNESS_ON;

    public BrightnessPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mSeekBar = getSeekBar(view);
        mSeekBar.setOnSeekBarChangeListener(this);
        mSeekBar.setMax(MAXIMUM_BACKLIGHT - MINIMUM_BACKLIGHT);
        try {
            mOldBrightness = Settings.System.getInt(getContext().getContentResolver(), 
                Settings.System.SCREEN_BRIGHTNESS);
        } catch (SettingNotFoundException snfe) {
            mOldBrightness = MAXIMUM_BACKLIGHT;
        }
        mSeekBar.setProgress(mOldBrightness - MINIMUM_BACKLIGHT);
    }

    public void onProgressChanged(SeekBar seekBar, int progress,
            boolean fromTouch) {
        setBrightness(progress + MINIMUM_BACKLIGHT);
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
        // NA
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
        // NA
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        
        if (positiveResult) {
            Settings.System.putInt(getContext().getContentResolver(), 
                    Settings.System.SCREEN_BRIGHTNESS,
                    mSeekBar.getProgress() + MINIMUM_BACKLIGHT);
        } else {
            setBrightness(mOldBrightness);
        }
    }
    
    private void setBrightness(int brightness) {
        try {
            IHardwareService hardware = IHardwareService.Stub.asInterface(
                    ServiceManager.getService("hardware"));
            if (hardware != null) {
                hardware.setBacklights(brightness);
            }
        } catch (RemoteException doe) {
            
        }        
    }
}

