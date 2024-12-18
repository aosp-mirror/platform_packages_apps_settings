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

package com.android.settings.inputmethod;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.hardware.input.InputSettings;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.SliderPreferenceController;
import com.android.settings.widget.SeekBarPreference;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;


public class MouseScrollingSpeedPreferenceController extends SliderPreferenceController implements
        Preference.OnPreferenceChangeListener, LifecycleObserver, OnStop, OnStart  {

    private final ContentResolver mContentResolver;
    private final ContentObserver mContentObserver;

    @Nullable
    private SeekBarPreference mPreference;

    public MouseScrollingSpeedPreferenceController(@NonNull Context context, @NonNull String key) {
        super(context, key);

        mContentResolver = context.getContentResolver();
        mContentObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                updateAvailabilityStatus();
            }
        };
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        mPreference.setMax(getMax());
        mPreference.setMin(getMin());
        mPreference.setProgress(getSliderPosition());
        updateState(mPreference);
    }

    @Override
    public int getAvailabilityStatus() {
        if (!InputSettings.isMouseScrollingAccelerationFeatureFlagEnabled()) {
            return UNSUPPORTED_ON_DEVICE;
        }
        return shouldEnableSlideBar() ? AVAILABLE : DISABLED_DEPENDENT_SETTING;
    }

    @Override
    public boolean setSliderPosition(int position) {
        if (position < getMin() || position > getMax()) {
            return false;
        }
        InputSettings.setMouseScrollingSpeed(mContext, position);

        return true;
    }

    @Override
    public int getSliderPosition() {
        return InputSettings.getMouseScrollingSpeed(mContext);
    }

    @Override
    public int getMin() {
        return InputSettings.MIN_MOUSE_SCROLLING_SPEED;
    }

    @Override
    public int getMax() {
        return InputSettings.MAX_MOUSE_SCROLLING_SPEED;
    }

    /**
     * Returns whether the mouse scrolling speed slide bar should allow users to customize or not.
     */
    public boolean shouldEnableSlideBar() {
        return !InputSettings.isMouseScrollingAccelerationEnabled(mContext);
    }

    @Override
    public void onStart() {
        mContentResolver.registerContentObserver(
                Settings.System.getUriFor(
                        Settings.System.MOUSE_SCROLLING_ACCELERATION),
                /* notifyForDescendants= */ false, mContentObserver);
    }

    @Override
    public void onStop() {
        mContentResolver.unregisterContentObserver(mContentObserver);
    }

    private void updateAvailabilityStatus() {
        if (mPreference != null) {
            mPreference.setEnabled(shouldEnableSlideBar());
        }
    }
}
