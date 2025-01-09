/*
 * Copyright 2025 The Android Open Source Project
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
import static android.view.accessibility.AccessibilityManager.AUTOCLICK_CURSOR_AREA_INCREMENT_SIZE;
import static android.view.accessibility.AccessibilityManager.AUTOCLICK_CURSOR_AREA_SIZE_MAX;
import static android.view.accessibility.AccessibilityManager.AUTOCLICK_CURSOR_AREA_SIZE_MIN;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.PreferenceScreen;

import com.android.server.accessibility.Flags;
import com.android.settings.core.SliderPreferenceController;
import com.android.settingslib.widget.SliderPreference;

/** Controller class that controls accessibility autoclick cursor area size settings. */
public class ToggleAutoclickCursorAreaSizeController extends SliderPreferenceController
        implements LifecycleObserver, SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = ToggleAutoclickCursorAreaSizeController.class.getSimpleName();

    private final ContentResolver mContentResolver;
    private final SharedPreferences mSharedPreferences;
    private SliderPreference mPreference;

    public ToggleAutoclickCursorAreaSizeController(@NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);

        mContentResolver = context.getContentResolver();
        mSharedPreferences = context.getSharedPreferences(context.getPackageName(), MODE_PRIVATE);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        if (mSharedPreferences != null) {
            mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        if (mSharedPreferences != null) {
            mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        if (mPreference != null) {
            mPreference.setMin(getMin());
            mPreference.setMax(getMax());
            mPreference.setSliderIncrement(AUTOCLICK_CURSOR_AREA_INCREMENT_SIZE);
            mPreference.setValue(getSliderPosition());
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return Flags.enableAutoclickIndicator() ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void onSharedPreferenceChanged(
            @NonNull SharedPreferences sharedPreferences, @Nullable String key) {
        // TODO(b/383901288): Update slider if interested preference has changed.
    }

    @Override
    public boolean setSliderPosition(int position) {
        int size = validateSize(position);
        Settings.Secure.putInt(
                mContentResolver, Settings.Secure.ACCESSIBILITY_AUTOCLICK_CURSOR_AREA_SIZE, size);
        return true;
    }

    @Override
    public int getSliderPosition() {
        int size = Settings.Secure.getInt(mContentResolver,
                Settings.Secure.ACCESSIBILITY_AUTOCLICK_CURSOR_AREA_SIZE,
                AccessibilityManager.AUTOCLICK_CURSOR_AREA_SIZE_DEFAULT);
        return validateSize(size);
    }

    @Override
    public int getMax() {
        return AUTOCLICK_CURSOR_AREA_SIZE_MAX;
    }

    @Override
    public int getMin() {
        return AUTOCLICK_CURSOR_AREA_SIZE_MIN;
    }

    private int validateSize(int size) {
        size = Math.min(size, AUTOCLICK_CURSOR_AREA_SIZE_MAX);
        size = Math.max(size, AUTOCLICK_CURSOR_AREA_SIZE_MIN);
        return size;
    }
}
