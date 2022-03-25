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

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.PrimarySwitchPreference;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/** A controller can control the behavior of night display setting. */
public class NightDisplayPreferenceController extends TogglePreferenceController
        implements NightDisplayListener.Callback, LifecycleObserver, OnStart, OnStop {

    private final ColorDisplayManager mColorDisplayManager;
    private final NightDisplayListener mNightDisplayListener;
    private final NightDisplayTimeFormatter mTimeFormatter;
    private PrimarySwitchPreference mPreference;

    public NightDisplayPreferenceController(Context context, String key) {
        super(context, key);

        mColorDisplayManager = context.getSystemService(ColorDisplayManager.class);
        mNightDisplayListener = new NightDisplayListener(context);
        mTimeFormatter = new NightDisplayTimeFormatter(context);
    }

    public static boolean isSuggestionComplete(Context context) {
        final boolean isEnabled = context.getResources().getBoolean(
                R.bool.config_night_light_suggestion_enabled);
        // The suggestion is always complete if not enabled.
        if (!isEnabled) {
            return true;
        }
        final ColorDisplayManager manager = context.getSystemService(ColorDisplayManager.class);
        return manager.getNightDisplayAutoMode() != ColorDisplayManager.AUTO_MODE_DISABLED;
    }

    @Override
    public void onStart() {
        // Listen for changes only while attached.
        mNightDisplayListener.setCallback(this);
    }

    @Override
    public void onStop() {
        // Stop listening for state changes.
        mNightDisplayListener.setCallback(null);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public int getAvailabilityStatus() {
        return ColorDisplayManager.isNightDisplayAvailable(mContext)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isChecked() {
        return mColorDisplayManager.isNightDisplayActivated();
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return mColorDisplayManager.setNightDisplayActivated(isChecked);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setSummary(mTimeFormatter.getAutoModeSummary(mContext, mColorDisplayManager));
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_display;
    }

    @Override
    public void onActivated(boolean activated) {
        updateState(mPreference);
    }
}