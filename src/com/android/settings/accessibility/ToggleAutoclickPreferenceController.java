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

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;
import static com.android.settings.accessibility.AutoclickUtils.KEY_DELAY_MODE;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.provider.Settings;
import android.util.ArrayMap;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.widget.LayoutPreference;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import java.util.Map;

/** Controller class that controls accessibility autoclick settings. */
public class ToggleAutoclickPreferenceController extends BasePreferenceController implements
        LifecycleObserver, OnStart, OnStop, SelectorWithWidgetPreference.OnClickListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String KEY_AUTOCLICK_CUSTOM_SEEKBAR = "autoclick_custom_seekbar";
    private static final int AUTOCLICK_OFF_MODE = 0;
    private static final int AUTOCLICK_CUSTOM_MODE = 2000;

    /**
     * Seek bar preference for autoclick delay value. The seek bar has values between 0 and
     * number of possible discrete autoclick delay values. These will have to be converted to actual
     * delay values before saving them in settings.
     */
    private LayoutPreference mSeekBerPreference;
    private SelectorWithWidgetPreference mDelayModePref;
    private Map<String, Integer> mAccessibilityAutoclickKeyToValueMap = new ArrayMap<>();
    private final SharedPreferences mSharedPreferences;
    private final ContentResolver mContentResolver;
    private final Resources mResources;

    public ToggleAutoclickPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mSharedPreferences = context.getSharedPreferences(context.getPackageName(), MODE_PRIVATE);
        mContentResolver = context.getContentResolver();
        mResources = context.getResources();
    }

    @Override
    public void onStart() {
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onStop() {
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            @Nullable String key) {
        updateState(mDelayModePref);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mDelayModePref = screen.findPreference(getPreferenceKey());
        mDelayModePref.setOnClickListener(this);
        mSeekBerPreference = screen.findPreference(KEY_AUTOCLICK_CUSTOM_SEEKBAR);
        updateState(mDelayModePref);
    }

    @Override
    public void onRadioButtonClicked(SelectorWithWidgetPreference preference) {
        final int mode = getAutoclickModeToKeyMap().get(mPreferenceKey);
        Settings.Secure.putInt(mContentResolver, Settings.Secure.ACCESSIBILITY_AUTOCLICK_ENABLED,
                (mode != AUTOCLICK_OFF_MODE) ? ON : OFF);
        mSharedPreferences.edit().putInt(KEY_DELAY_MODE, mode).apply();
        if (mode != AUTOCLICK_CUSTOM_MODE) {
            Settings.Secure.putInt(mContentResolver, Settings.Secure.ACCESSIBILITY_AUTOCLICK_DELAY,
                    mode);
        }
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        final boolean enabled = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_AUTOCLICK_ENABLED, OFF) == ON;
        final int currentUiAutoClickMode = enabled
                ? mSharedPreferences.getInt(KEY_DELAY_MODE, AUTOCLICK_CUSTOM_MODE)
                : AUTOCLICK_OFF_MODE;
        final int mode = getAutoclickModeToKeyMap().get(mDelayModePref.getKey());
        mDelayModePref.setChecked(currentUiAutoClickMode == mode);
        if (mode == AUTOCLICK_CUSTOM_MODE) {
            mSeekBerPreference.setVisible(mDelayModePref.isChecked());
        }
    }

    /** Returns the paring preference key and autoclick mode value listing. */
    private Map<String, Integer> getAutoclickModeToKeyMap() {
        if (mAccessibilityAutoclickKeyToValueMap.size() == 0) {
            final String[] autoclickKeys = mResources.getStringArray(
                    R.array.accessibility_autoclick_control_selector_keys);
            final int[] autoclickValues = mResources.getIntArray(
                    R.array.accessibility_autoclick_selector_values);
            final int autoclickValueCount = autoclickValues.length;
            for (int i = 0; i < autoclickValueCount; i++) {
                mAccessibilityAutoclickKeyToValueMap.put(autoclickKeys[i], autoclickValues[i]);
            }
        }
        return mAccessibilityAutoclickKeyToValueMap;
    }
}
