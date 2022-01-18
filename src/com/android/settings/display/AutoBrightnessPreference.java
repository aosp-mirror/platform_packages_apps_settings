/*
 * Copyright (C) 2022 Yet Another AOSP Project
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

import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;

import android.content.Context;
import android.provider.Settings;
import android.util.AttributeSet;

import com.android.settings.display.AutoBrightnessObserver;

import com.android.settingslib.PrimarySwitchPreference;

/**
 * component for the display auto brightness
 */
public class AutoBrightnessPreference extends PrimarySwitchPreference {

    private final AutoBrightnessObserver mAutoBrightnessObserver;

    private final Runnable mCallback = () -> {
        final int value = Settings.System.getInt(
                getContext().getContentResolver(),
                SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
        setChecked(value == SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
    };

    public AutoBrightnessPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mAutoBrightnessObserver = new AutoBrightnessObserver(context);
    }

    @Override
    public void onAttached() {
        super.onAttached();
        mAutoBrightnessObserver.subscribe(mCallback);
    }

    @Override
    public void onDetached() {
        super.onDetached();
        mAutoBrightnessObserver.unsubscribe();
    }
}
