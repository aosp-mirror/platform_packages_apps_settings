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
package com.android.settings.accessibility;

import static com.android.settings.accessibility.DaltonizerPreferenceUtil.isSecureAccessibilityDaltonizerEnabled;
import static com.android.settings.accessibility.DaltonizerPreferenceUtil.getSecureAccessibilityDaltonizerValue;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.server.accessibility.Flags;
import com.android.settings.R;
import com.android.settings.core.SliderPreferenceController;
import com.android.settings.widget.SeekBarPreference;

/**
 * The controller of the seekbar preference for the saturation level of color correction.
 */
public class DaltonizerSaturationSeekbarPreferenceController
        extends SliderPreferenceController
        implements DefaultLifecycleObserver {

    private static final int DEFAULT_SATURATION_LEVEL = 7;
    private static final int SATURATION_MAX = 10;
    private static final int SATURATION_MIN = 1;

    private int mSliderPosition;
    private final ContentResolver mContentResolver;

    @Nullable
    private SeekBarPreference mPreference;

    public final ContentObserver mContentObserver = new ContentObserver(
            new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange) {
            if (mPreference != null) {
                updateState(mPreference);
            }
        }
    };

    public DaltonizerSaturationSeekbarPreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey);
        mContentResolver = context.getContentResolver();
        mSliderPosition = Settings.Secure.getInt(
                mContentResolver,
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_SATURATION_LEVEL,
                DEFAULT_SATURATION_LEVEL);
        setSliderPosition(mSliderPosition);
        // TODO: Observer color correction on/off and enable/disable based on secure settings.
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (!isAvailable()) return;
        mContentResolver.registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER),
                true,
                mContentObserver
        );
        mContentResolver.registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED),
                true,
                mContentObserver
        );
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (!isAvailable()) return;
        mContentResolver.unregisterContentObserver(mContentObserver);
        mPreference = null;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        SeekBarPreference preference = screen.findPreference(getPreferenceKey());
        mPreference = preference;
        preference.setMax(getMax());
        preference.setMin(getMin());
        preference.setProgress(mSliderPosition);
        preference.setContinuousUpdates(true);
    }

    @Override
    public int getAvailabilityStatus() {
        if (Flags.enableColorCorrectionSaturation()) {
            return shouldSeekBarEnabled() ? AVAILABLE : DISABLED_DEPENDENT_SETTING;
        }
        return CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public int getSliderPosition() {
        return mSliderPosition;
    }

    @Override
    public boolean setSliderPosition(int position) {
        if (position < getMin() || position > getMax()) {
            return false;
        }
        mSliderPosition = position;
        Settings.Secure.putInt(
                mContentResolver,
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_SATURATION_LEVEL,
                mSliderPosition);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        if (preference == null) {
            return;
        }

        var shouldSeekbarEnabled = shouldSeekBarEnabled();
        // setSummary not working yet on SeekBarPreference.
        String summary = shouldSeekbarEnabled
                ? ""
                : mContext.getString(R.string.daltonizer_saturation_unavailable_summary);
        preference.setSummary(summary);
        preference.setEnabled(shouldSeekbarEnabled);
    }

    @Override
    public int getMax() {
        return SATURATION_MAX;
    }

    @Override
    public int getMin() {
        return SATURATION_MIN;
    }

    private boolean shouldSeekBarEnabled() {
        boolean enabled = isSecureAccessibilityDaltonizerEnabled(mContentResolver);
        int mode = getSecureAccessibilityDaltonizerValue(mContentResolver);

        // mode == 0 is gray scale where saturation level isn't applicable.
        // mode == -1 is disabled and also default.
        return enabled && mode != -1 && mode != 0;
    }
}
