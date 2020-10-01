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

import androidx.lifecycle.LifecycleObserver;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.LayoutPreference;
import com.android.settingslib.widget.RadioButtonPreference;

import java.util.Map;

/**
 * Controller class that controls accessibility autoclick settings.
 */
public class ToggleAutoclickPreferenceController extends BasePreferenceController implements
        LifecycleObserver, RadioButtonPreference.OnClickListener, PreferenceControllerMixin {

    private static final String CONTROL_AUTOCLICK_DELAY_SECURE =
            Settings.Secure.ACCESSIBILITY_AUTOCLICK_DELAY;
    private static final String KEY_AUTOCLICK_CUSTOM_SEEKBAR = "autoclick_custom_seekbar";
    static final String KEY_DELAY_MODE = "delay_mode";

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
    private LayoutPreference mSeekBerPreference;
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
        mSeekBerPreference = (LayoutPreference) screen.findPreference(KEY_AUTOCLICK_CUSTOM_SEEKBAR);
        updateState((Preference) mDelayModePref);
    }

    @Override
    public void onRadioButtonClicked(RadioButtonPreference preference) {
        final int value = mAccessibilityAutoclickKeyToValueMap.get(mPreferenceKey);
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
        mSeekBerPreference.setVisible(mCurrentUiAutoClickMode == mode);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        final boolean enabled = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_AUTOCLICK_ENABLED, 0) == 1;

        mCurrentUiAutoClickMode =
                enabled ? getSharedPreferenceForAutoClickMode() : AUTOCLICK_OFF_MODE;

        // Reset RadioButton.
        mDelayModePref.setChecked(false);
        final int mode = mAccessibilityAutoclickKeyToValueMap.get(mDelayModePref.getKey());
        updatePreferenceCheckedState(mode);
        updatePreferenceVisibleState(mode);
    }

    /** Listener interface handles checked event. */
    public interface OnChangeListener {
        /**
         * A hook that is called when preference checked.
         */
        void onCheckedChanged(Preference preference);
    }

    private void setAutoclickModeToKeyMap() {
        final String[] autoclickKeys = mResources.getStringArray(
                R.array.accessibility_autoclick_control_selector_keys);

        final int[] autoclickValues = mResources.getIntArray(
                R.array.accessibility_autoclick_selector_values);

        final int autoclickValueCount = autoclickValues.length;
        for (int i = 0; i < autoclickValueCount; i++) {
            mAccessibilityAutoclickKeyToValueMap.put(autoclickKeys[i], autoclickValues[i]);
        }
    }

    private void handleRadioButtonPreferenceChange(int preference) {
        putSecureInt(Settings.Secure.ACCESSIBILITY_AUTOCLICK_ENABLED,
                (preference != AUTOCLICK_OFF_MODE) ? /* enabled */ 1 : /* disabled */ 0);

        mSharedPreferences.edit().putInt(KEY_DELAY_MODE, preference).apply();

        if (preference != AUTOCLICK_CUSTOM_MODE) {
            putSecureInt(CONTROL_AUTOCLICK_DELAY_SECURE, preference);
        }
    }

    private void putSecureInt(String name, int value) {
        Settings.Secure.putInt(mContentResolver, name, value);
    }

    private int getSharedPreferenceForAutoClickMode() {
        return mSharedPreferences.getInt(KEY_DELAY_MODE, AUTOCLICK_CUSTOM_MODE);
    }
}
