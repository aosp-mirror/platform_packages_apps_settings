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

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.PreferenceScreen;

import com.android.server.accessibility.Flags;
import com.android.settings.core.SliderPreferenceController;

/** Controller class that controls accessibility autoclick cursor area size settings. */
public class ToggleAutoclickCursorAreaSizeController extends SliderPreferenceController
        implements LifecycleObserver, SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = ToggleAutoclickCursorAreaSizeController.class.getSimpleName();

    private static final int MIN_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final int DEFAULT_SIZE = 60;

    private final SharedPreferences mSharedPreferences;

    public ToggleAutoclickCursorAreaSizeController(@NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
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
    }

    @Override
    public int getAvailabilityStatus() {
        return Flags.enableAutoclickIndicator() ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void onSharedPreferenceChanged(
            @NonNull SharedPreferences sharedPreferences, @Nullable String key) {
        // TODO(b/383901288): Update slider.
    }

    @Override
    public boolean setSliderPosition(int position) {
        // TODO(b/383901288): Update settings.
        return true;
    }

    @Override
    public int getSliderPosition() {
        // TODO(b/383901288): retrieve from settings and fallback to default.
        return DEFAULT_SIZE;
    }

    @Override
    public int getMax() {
        return MAX_SIZE;
    }

    @Override
    public int getMin() {
        return MIN_SIZE;
    }
}
