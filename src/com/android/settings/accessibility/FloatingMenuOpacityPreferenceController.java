/*
 * Copyright (C) 2021 The Android Open Source Project
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

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.SliderPreferenceController;
import com.android.settings.widget.SeekBarPreference;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

/** Preference controller that controls the opacity seekbar in accessibility button page. */
public class FloatingMenuOpacityPreferenceController extends SliderPreferenceController
        implements LifecycleObserver, OnResume, OnPause {

    @VisibleForTesting
    static final float DEFAULT_OPACITY = 0.55f;
    private static final int FADE_ENABLED = 1;
    private static final float MIN_PROGRESS = 10f;
    private static final float MAX_PROGRESS = 100f;
    @VisibleForTesting
    static final float PRECISION = 100f;

    private final ContentResolver mContentResolver;
    @VisibleForTesting
    final ContentObserver mContentObserver;

    @VisibleForTesting
    SeekBarPreference mPreference;

    public FloatingMenuOpacityPreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey);
        mContentResolver = context.getContentResolver();
        mContentObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                updateAvailabilityStatus();
            }
        };
    }

    @Override
    public int getAvailabilityStatus() {
        return AccessibilityUtil.isFloatingMenuEnabled(mContext)
                ? AVAILABLE : DISABLED_DEPENDENT_SETTING;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreference = screen.findPreference(getPreferenceKey());
        mPreference.setContinuousUpdates(true);
        mPreference.setMax(getMax());
        mPreference.setMin(getMin());
        mPreference.setHapticFeedbackMode(SeekBarPreference.HAPTIC_FEEDBACK_MODE_ON_ENDS);

        updateState(mPreference);
    }

    @Override
    public void onResume() {
        mContentResolver.registerContentObserver(
                Settings.Secure.getUriFor(
                        Settings.Secure.ACCESSIBILITY_BUTTON_MODE), /* notifyForDescendants= */
                false, mContentObserver);
        mContentResolver.registerContentObserver(
                Settings.Secure.getUriFor(
                        Settings.Secure.ACCESSIBILITY_FLOATING_MENU_FADE_ENABLED),
                        /* notifyForDescendants= */ false, mContentObserver);
    }

    @Override
    public void onPause() {
        mContentResolver.unregisterContentObserver(mContentObserver);
    }

    @Override
    public int getSliderPosition() {
        return convertOpacityFloatToInt(getOpacity());
    }

    @Override
    public boolean setSliderPosition(int position) {
        final float value = convertOpacityIntToFloat(position);

        return Settings.Secure.putFloat(mContentResolver,
                Settings.Secure.ACCESSIBILITY_FLOATING_MENU_OPACITY, value);
    }

    @Override
    public int getMax() {
        return (int) MAX_PROGRESS;
    }

    @Override
    public int getMin() {
        return (int) MIN_PROGRESS;
    }

    private void updateAvailabilityStatus() {
        final boolean fadeEnabled = Settings.Secure.getInt(mContentResolver,
                Settings.Secure.ACCESSIBILITY_FLOATING_MENU_FADE_ENABLED, FADE_ENABLED)
                == FADE_ENABLED;

        mPreference.setEnabled(AccessibilityUtil.isFloatingMenuEnabled(mContext) && fadeEnabled);
    }

    private int convertOpacityFloatToInt(float value) {
        return Math.round(value * PRECISION);
    }

    private float convertOpacityIntToFloat(int value) {
        return (float) value / PRECISION;
    }

    private float getOpacity() {
        float value = Settings.Secure.getFloat(mContentResolver,
                Settings.Secure.ACCESSIBILITY_FLOATING_MENU_OPACITY, DEFAULT_OPACITY);
        final float minValue = MIN_PROGRESS / PRECISION;
        final float maxValue = MAX_PROGRESS / PRECISION;

        return (value < minValue || value > maxValue) ? DEFAULT_OPACITY : value;
    }
}

