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

import static com.android.settings.accessibility.ToggleAutoclickPreferenceController.KEY_DELAY_MODE;
import static com.android.settings.accessibility.ToggleAutoclickPreferenceFragment.Quantity;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.widget.LayoutPreference;

/**
 * Controller class that controls accessibility autoclick seekbar settings.
 */
public class ToggleAutoclickCustomSeekbarController extends BasePreferenceController
        implements LifecycleObserver, OnResume, OnPause,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String CONTROL_AUTOCLICK_DELAY_SECURE =
            Settings.Secure.ACCESSIBILITY_AUTOCLICK_DELAY;
    private static final String KEY_CUSTOM_DELAY_VALUE = "custom_delay_value";

    // Min allowed autoclick delay value.
    static final int MIN_AUTOCLICK_DELAY_MS = 200;

    // Max allowed autoclick delay value.
    static final int MAX_AUTOCLICK_DELAY_MS = 1000;

    // Allowed autoclick delay values are discrete.
    // This is the difference between two allowed values.
    private static final int AUTOCLICK_DELAY_STEP = 100;

    private final SharedPreferences mSharedPreferences;
    private final ContentResolver mContentResolver;
    private ImageView mShorter;
    private ImageView mLonger;
    private SeekBar mSeekBar;
    private TextView mDelayLabel;

    private final SeekBar.OnSeekBarChangeListener mSeekBarChangeListener =
            new SeekBar.OnSeekBarChangeListener() {

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    updateCustomDelayValue(seekBarProgressToDelay(progress));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    // Nothing to do.
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    // Nothing to do.
                }
            };

    public ToggleAutoclickCustomSeekbarController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mSharedPreferences = context.getSharedPreferences(context.getPackageName(), MODE_PRIVATE);
        mContentResolver = context.getContentResolver();
    }

    public ToggleAutoclickCustomSeekbarController(Context context, Lifecycle lifecycle,
            String preferenceKey) {
        this(context, preferenceKey);

        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void onResume() {
        if (mSharedPreferences != null) {
            mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onPause() {
        if (mSharedPreferences != null) {
            mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final LayoutPreference preference = screen.findPreference(getPreferenceKey());

        if (isAvailable()) {
            int delayMillis = getSharedPreferenceForDelayValue();
            // Initialize seek bar preference. Sets seek bar size to the number of possible delay
            // values.
            mSeekBar = preference.findViewById(R.id.autoclick_delay);
            mSeekBar.setMax(delayToSeekBarProgress(MAX_AUTOCLICK_DELAY_MS));
            mSeekBar.setProgress(delayToSeekBarProgress(delayMillis));
            mSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);

            mDelayLabel = preference.findViewById(R.id.current_label);
            mDelayLabel.setText(delayTimeToString(delayMillis));

            mShorter = preference.findViewById(R.id.shorter);
            mShorter.setOnClickListener(v -> {
                minusDelayByImageView();
            });

            mLonger = preference.findViewById(R.id.longer);
            mLonger.setOnClickListener(v -> {
                plusDelayByImageView();
            });
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (KEY_DELAY_MODE.equals(key)) {
            final int delayMillis = getSharedPreferenceForDelayValue();
            updateCustomDelayValue(delayMillis);
        }
    }

    /** Converts seek bar preference progress value to autoclick delay associated with it. */
    private int seekBarProgressToDelay(int progress) {
        return progress * AUTOCLICK_DELAY_STEP + MIN_AUTOCLICK_DELAY_MS;
    }

    /**
     * Converts autoclick delay value to seek bar preference progress values that represents said
     * delay.
     */
    private int delayToSeekBarProgress(int delayMillis) {
        return (delayMillis - MIN_AUTOCLICK_DELAY_MS) / AUTOCLICK_DELAY_STEP;
    }

    private int getSharedPreferenceForDelayValue() {
        final int delayMillis = Settings.Secure.getInt(mContentResolver,
                Settings.Secure.ACCESSIBILITY_AUTOCLICK_DELAY,
                AccessibilityManager.AUTOCLICK_DELAY_DEFAULT);

        return mSharedPreferences.getInt(KEY_CUSTOM_DELAY_VALUE, delayMillis);
    }

    private void putSecureInt(String name, int value) {
        Settings.Secure.putInt(mContentResolver, name, value);
    }

    private void updateCustomDelayValue(int delayMillis) {
        putSecureInt(CONTROL_AUTOCLICK_DELAY_SECURE, delayMillis);
        mSharedPreferences.edit().putInt(KEY_CUSTOM_DELAY_VALUE, delayMillis).apply();
        mSeekBar.setProgress(delayToSeekBarProgress(delayMillis));
        mDelayLabel.setText(delayTimeToString(delayMillis));
    }

    private void minusDelayByImageView() {
        final int delayMillis = getSharedPreferenceForDelayValue();
        if (delayMillis > MIN_AUTOCLICK_DELAY_MS) {
            updateCustomDelayValue(delayMillis - AUTOCLICK_DELAY_STEP);
        }
    }

    private void plusDelayByImageView() {
        final int delayMillis = getSharedPreferenceForDelayValue();
        if (delayMillis < MAX_AUTOCLICK_DELAY_MS) {
            updateCustomDelayValue(delayMillis + AUTOCLICK_DELAY_STEP);
        }
    }

    private CharSequence delayTimeToString(int delayMillis) {
        final int quantity = (delayMillis == 1000) ? Quantity.ONE : Quantity.FEW;
        final float delaySecond = (float) delayMillis / 1000;
        // Only show integer when delay time is 1.
        final String decimalFormat = (delaySecond == 1) ? "%.0f" : "%.1f";

        return mContext.getResources().getQuantityString(
                R.plurals.accessibilty_autoclick_delay_unit_second,
                quantity, String.format(decimalFormat, delaySecond));
    }
}
