/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.gestures;

import static com.android.settings.gestures.DoubleTapPowerSettingsUtils.DOUBLE_TAP_POWER_BUTTON_GESTURE_ENABLED_URI;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.widget.SettingsMainSwitchPreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/** The controller to handle double tap power button main switch enable or disable state. */
public class DoubleTapPowerMainSwitchPreferenceController
        extends SettingsMainSwitchPreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    private final ContentObserver mSettingsObserver =
            new ContentObserver(new Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange, @Nullable Uri uri) {
                    if (mSwitchPreference == null || uri == null) {
                        return;
                    }
                    updateState(mSwitchPreference);
                }
            };

    public DoubleTapPowerMainSwitchPreferenceController(
            @NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return DoubleTapPowerSettingsUtils.isDoubleTapPowerButtonGestureAvailable(mContext)
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isChecked() {
        return DoubleTapPowerSettingsUtils.isDoubleTapPowerButtonGestureEnabled(mContext);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return DoubleTapPowerSettingsUtils.setDoubleTapPowerButtonGestureEnabled(
                mContext, isChecked);
    }

    @Override
    public void onStart() {
        final ContentResolver resolver = mContext.getContentResolver();
        resolver.registerContentObserver(
                DOUBLE_TAP_POWER_BUTTON_GESTURE_ENABLED_URI, true, mSettingsObserver);
    }

    @Override
    public void onStop() {
        DoubleTapPowerSettingsUtils.unregisterObserver(mContext, mSettingsObserver);
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_system;
    }
}
