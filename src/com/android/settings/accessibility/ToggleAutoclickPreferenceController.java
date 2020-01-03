/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.accessibility;

import static android.content.Context.MODE_PRIVATE;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.provider.Settings;
import android.util.ArrayMap;
import android.view.accessibility.AccessibilityManager;

import androidx.lifecycle.LifecycleObserver;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.widget.SeekBarPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.RadioButtonPreference;

import java.util.Map;

/**
 * Controller class that controls accessibility autoclick settings.
 */
public class ToggleAutoclickPreferenceController extends BasePreferenceController implements
        LifecycleObserver, RadioButtonPreference.OnClickListener, PreferenceControllerMixin,
        Preference.OnPreferenceChangeListener {
    // Min allowed autoclick delay value.
    static final int MIN_AUTOCLICK_DELAY_MS = 200;

    // Max allowed autoclick delay value.
    static final int MAX_AUTOCLICK_DELAY_MS = 1000;

    private static final String CONTROL_AUTOCLICK_DELAY_SECURE =
            Settings.Secure.ACCESSIBILITY_AUTOCLICK_DELAY;
    private static final String KEY_AUTOCLICK_DELA = "autoclick_delay";
    private static final String KEY_CUSTOM_DELAY_VALUE = "custom_delay_value";
    private static final String KEY_DELAY_MODE = "delay_mode";

    // Allowed autoclick delay values are discrete.
    // This is the difference between two allowed values.
    private static final int AUTOCLICK_DELAY_STEP = 100;
    private static final int AUTOCLICK_OFF_MODE = 0;
    private static final int AUTOCLICK_CUSTOM_MODE = 2000;

    // Pair the preference key and autoclick mode value.
    private final Map<String, Integer> mAccessibilityAutoclickKeyToValueMap = new ArrayMap<>();

    private SharedPreferences mSharedPreferences;
    private final ContentResolver mContentResolver;
    private final Resources mResources;
    private OnChangeListener mOnChangeListener;
    private RadioButtonPreference mDelayModePref;

    /**
     * Seek bar preference for autoclick delay value. The seek bar has values between 0 and
     * number of possible discrete autoclick delay values. These will have to be converted to actual
     * delay values before saving them in settings.
     */
    private SeekBarPreference mCustomDelayPref;
    private int mCurrentUiAutoClickMode;

    public ToggleAutoclickPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);

        mSharedPreferences = context.getSharedPreferences(context.getPackageName(), MODE_PRIVATE);
        mContentResolver = context.getContentResolver();
        mResources = context.getResources();

        setAutoclickModeToKeyMap();
    }

    public ToggleAutoclickPreferenceController(Context context, Lifecycle lifecycle,
            String preferenceKey) {
        super(context, preferenceKey);

        mSharedPreferences = context.getSharedPreferences(context.getPackageName(), MODE_PRIVATE);
        mContentResolver = context.getContentResolver();
        mResources = context.getResources();

        setAutoclickModeToKeyMap();

        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    public void setOnChangeListener(OnChangeListener listener) {
        mOnChangeListener = listener;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mDelayModePref = (RadioButtonPreference)
                screen.findPreference(getPreferenceKey());
        mDelayModePref.setOnClickListener(this);

        int delay = getSharedPreferenceForDelayValue();

        // Initialize seek bar preference. Sets seek bar size to the number of possible delay
        // values.
        mCustomDelayPref = (SeekBarPreference) screen.findPreference(KEY_AUTOCLICK_DELA);
        mCustomDelayPref.setMax(delayToSeekBarProgress(MAX_AUTOCLICK_DELAY_MS));
        mCustomDelayPref.setProgress(delayToSeekBarProgress(delay));
        mCustomDelayPref.setOnPreferenceChangeListener(this);

        updateState((Preference) mDelayModePref);
    }

    @Override
    public void onRadioButtonClicked(RadioButtonPreference preference) {
        int value = mAccessibilityAutoclickKeyToValueMap.get(mPreferenceKey);
        handleRadioButtonPreferenceChange(value);
        if (mOnChangeListener != null) {
            mOnChangeListener.onCheckedChanged(mDelayModePref);
        }
    }

    private void updatePreferenceCheckedState(int mode) {
        if (mCurrentUiAutoClickMode == mode) {
            mDelayModePref.setChecked(true);
        }
    }

    private void updatePreferenceVisibleState(int mode) {
        mCustomDelayPref.setVisible(mCurrentUiAutoClickMode == mode);
    }

    private void updateSeekBarProgressState() {
        if (mCurrentUiAutoClickMode == AUTOCLICK_CUSTOM_MODE) {
            int delay = getSharedPreferenceForDelayValue();
            mCustomDelayPref.setProgress(delayToSeekBarProgress(delay));
        }
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        mCurrentUiAutoClickMode = getSharedPreferenceForAutoClickMode();

        // Reset RadioButton.
        mDelayModePref.setChecked(false);
        int mode = mAccessibilityAutoclickKeyToValueMap.get(mDelayModePref.getKey());
        updateSeekBarProgressState();
        updatePreferenceCheckedState(mode);
        updatePreferenceVisibleState(mode);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mCustomDelayPref && newValue instanceof Integer) {
            putSecureInt(CONTROL_AUTOCLICK_DELAY_SECURE, seekBarProgressToDelay((int) newValue));
            mSharedPreferences.edit().putInt(KEY_CUSTOM_DELAY_VALUE,
                    seekBarProgressToDelay((int) newValue)).apply();
            return true;
        }
        return false;
    }

    /** Listener interface handles checked event. */
    public interface OnChangeListener {
        /**
         * A hook that is called when preference checked.
         */
        void onCheckedChanged(Preference preference);
    }

    private void setAutoclickModeToKeyMap() {
        String[] autoclickKeys = mResources.getStringArray(
                R.array.accessibility_autoclick_control_selector_keys);

        int[] autoclickValues = mResources.getIntArray(
                R.array.accessibility_autoclick_selector_values);

        final int autoclickValueCount = autoclickValues.length;
        for (int i = 0; i < autoclickValueCount; i++) {
            mAccessibilityAutoclickKeyToValueMap.put(autoclickKeys[i], autoclickValues[i]);
        }
    }

    private void handleRadioButtonPreferenceChange(int preference) {
        if (preference == AUTOCLICK_OFF_MODE) {
            putSecureInt(Settings.Secure.ACCESSIBILITY_AUTOCLICK_ENABLED, /*value= */ 0);
        } else {
            putSecureInt(Settings.Secure.ACCESSIBILITY_AUTOCLICK_ENABLED, /*value= */ 1);
        }

        mSharedPreferences.edit().putInt(KEY_DELAY_MODE, preference).apply();

        if (preference == AUTOCLICK_CUSTOM_MODE) {
            putSecureInt(CONTROL_AUTOCLICK_DELAY_SECURE, getSharedPreferenceForDelayValue());
        } else {
            putSecureInt(CONTROL_AUTOCLICK_DELAY_SECURE, preference);
        }
    }

    /** Converts seek bar preference progress value to autoclick delay associated with it. */
    private int seekBarProgressToDelay(int progress) {
        return progress * AUTOCLICK_DELAY_STEP + MIN_AUTOCLICK_DELAY_MS;
    }

    /**
     * Converts autoclick delay value to seek bar preference progress values that represents said
     * delay.
     */
    private int delayToSeekBarProgress(int delay) {
        return (delay - MIN_AUTOCLICK_DELAY_MS) / AUTOCLICK_DELAY_STEP;
    }

    private void putSecureInt(String name, int value) {
        Settings.Secure.putInt(mContentResolver, name, value);
    }

    private int getSharedPreferenceForDelayValue() {
        int mode = mSharedPreferences.getInt(KEY_DELAY_MODE, AUTOCLICK_OFF_MODE);
        int delay = mSharedPreferences.getInt(KEY_CUSTOM_DELAY_VALUE,
                AccessibilityManager.AUTOCLICK_DELAY_DEFAULT);

        return mode == AUTOCLICK_CUSTOM_MODE ? delay : mode;
    }

    private int getSharedPreferenceForAutoClickMode() {
        return mSharedPreferences.getInt(KEY_DELAY_MODE, AUTOCLICK_OFF_MODE);
    }
}
