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

package com.android.settings.gestures;

import android.content.Context;
import android.net.Uri;
import android.provider.Settings;

import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.SliderPreferenceController;
import com.android.settings.widget.LabeledSeekBarPreference;

/** Handles changes to the long press power button sensitivity slider. */
public class LongPressPowerSensitivityPreferenceController extends SliderPreferenceController
        implements PowerMenuSettingsUtils.SettingsStateCallback, LifecycleObserver {

    @Nullable
    private final int[] mSensitivityValues;

    private final PowerMenuSettingsUtils mUtils;

    @Nullable
    private LabeledSeekBarPreference mPreference;

    public LongPressPowerSensitivityPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mSensitivityValues = context.getResources().getIntArray(
                com.android.internal.R.array.config_longPressOnPowerDurationSettings);
        mUtils = new PowerMenuSettingsUtils(context);
    }

    /** @OnLifecycleEvent(Lifecycle.Event.ON_START) */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        mUtils.registerObserver(this);
    }

    /** @OnLifecycleEvent(Lifecycle.Event.ON_STOP) */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        mUtils.unregisterObserver();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        if (mPreference != null) {
            mPreference.setContinuousUpdates(false);
            mPreference.setHapticFeedbackMode(
                    LabeledSeekBarPreference.HAPTIC_FEEDBACK_MODE_ON_TICKS);
            mPreference.setMin(getMin());
            mPreference.setMax(getMax());
        }
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        final LabeledSeekBarPreference pref = (LabeledSeekBarPreference) preference;
        pref.setVisible(
                PowerMenuSettingsUtils.isLongPressPowerForAssistantEnabled(mContext)
                        && getAvailabilityStatus() == AVAILABLE);
        pref.setProgress(getSliderPosition());
    }

    @Override
    public int getAvailabilityStatus() {
        if (mSensitivityValues == null
                || mSensitivityValues.length < 2
                || !PowerMenuSettingsUtils.isLongPressPowerSettingAvailable(mContext)) {
            return UNSUPPORTED_ON_DEVICE;
        }
        return AVAILABLE;
    }

    @Override
    public int getSliderPosition() {
        return mSensitivityValues == null ? 0 : closestValueIndex(mSensitivityValues,
                getCurrentSensitivityValue());
    }

    @Override
    public boolean setSliderPosition(int position) {
        if (mSensitivityValues == null || position < 0 || position >= mSensitivityValues.length) {
            return false;
        }
        return Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.POWER_BUTTON_LONG_PRESS_DURATION_MS,
                mSensitivityValues[position]);
    }

    @Override
    public void onChange(Uri uri) {
        if (mPreference != null) {
            updateState(mPreference);
        }
    }

    @Override
    public int getMax() {
        if (mSensitivityValues == null || mSensitivityValues.length == 0) {
            return 0;
        }
        return mSensitivityValues.length - 1;
    }

    @Override
    public int getMin() {
        return 0;
    }

    private int getCurrentSensitivityValue() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.POWER_BUTTON_LONG_PRESS_DURATION_MS,
                mContext.getResources().getInteger(
                        com.android.internal.R.integer.config_longPressOnPowerDurationMs));
    }

    private static int closestValueIndex(int[] values, int needle) {
        int minDistance = Integer.MAX_VALUE;
        int valueIndex = 0;
        for (int i = 0; i < values.length; i++) {
            int diff = Math.abs(values[i] - needle);
            if (diff < minDistance) {
                minDistance = diff;
                valueIndex = i;
            }
        }
        return valueIndex;
    }
}
