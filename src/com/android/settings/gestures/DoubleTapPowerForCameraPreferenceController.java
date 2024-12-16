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

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

public class DoubleTapPowerForCameraPreferenceController extends BasePreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    @Nullable private Preference mPreference;
    private final ContentObserver mSettingsObserver =
            new ContentObserver(new Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange, @Nullable Uri uri) {
                    if (mPreference == null || uri == null) {
                        return;
                    }
                    if (uri.equals(
                            DoubleTapPowerSettingsUtils
                                    .DOUBLE_TAP_POWER_BUTTON_GESTURE_ENABLED_URI)) {
                        mPreference.setEnabled(
                                DoubleTapPowerSettingsUtils.isDoubleTapPowerButtonGestureEnabled(
                                        mContext));
                    } else if (uri.equals(
                            DoubleTapPowerSettingsUtils
                                    .DOUBLE_TAP_POWER_BUTTON_GESTURE_TARGET_ACTION_URI)) {
                        updateState(mPreference);
                    }
                }
            };

    public DoubleTapPowerForCameraPreferenceController(
            @NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        if (!DoubleTapPowerSettingsUtils
                .isMultiTargetDoubleTapPowerButtonGestureAvailable(mContext)) {
            return UNSUPPORTED_ON_DEVICE;
        }
        return DoubleTapPowerSettingsUtils.isDoubleTapPowerButtonGestureEnabled(mContext)
                ? AVAILABLE
                : DISABLED_DEPENDENT_SETTING;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void updateState(@NonNull Preference preference) {
        super.updateState(preference);
        if (preference instanceof SelectorWithWidgetPreference) {
            ((SelectorWithWidgetPreference) preference)
                    .setChecked(
                            DoubleTapPowerSettingsUtils
                                    .isDoubleTapPowerButtonGestureForCameraLaunchEnabled(mContext));
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(@NonNull Preference preference) {
        if (!getPreferenceKey().equals(preference.getKey())) {
            return false;
        }
        DoubleTapPowerSettingsUtils.setDoubleTapPowerButtonForCameraLaunch(mContext);
        if (preference instanceof SelectorWithWidgetPreference) {
            ((SelectorWithWidgetPreference) preference).setChecked(true);
        }
        return true;
    }

    @Override
    public void onStart() {
        DoubleTapPowerSettingsUtils.registerObserver(mContext, mSettingsObserver);
    }

    @Override
    public void onStop() {
        DoubleTapPowerSettingsUtils.unregisterObserver(mContext, mSettingsObserver);
    }
}
