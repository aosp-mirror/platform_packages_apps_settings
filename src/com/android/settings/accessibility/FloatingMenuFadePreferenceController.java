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
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

/** Preference controller that controls the fade switch button in accessibility button page. */
public class FloatingMenuFadePreferenceController extends BasePreferenceController implements
        Preference.OnPreferenceChangeListener, LifecycleObserver, OnResume, OnPause {

    private static final int OFF = 0;
    private static final int ON = 1;

    private final ContentResolver mContentResolver;
    @VisibleForTesting
    final ContentObserver mContentObserver;

    @VisibleForTesting
    TwoStatePreference mPreference;

    public FloatingMenuFadePreferenceController(Context context, String preferenceKey) {
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
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (boolean) newValue;
        putFloatingMenuFadeValue(isEnabled);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        final TwoStatePreference switchPreference = (TwoStatePreference) preference;

        switchPreference.setChecked(getFloatingMenuFadeValue() == ON);
    }

    @Override
    public void onResume() {
        mContentResolver.registerContentObserver(
                Settings.Secure.getUriFor(
                        Settings.Secure.ACCESSIBILITY_BUTTON_MODE),
                        /* notifyForDescendants= */ false, mContentObserver);
    }

    @Override
    public void onPause() {
        mContentResolver.unregisterContentObserver(mContentObserver);
    }

    private void updateAvailabilityStatus() {
        mPreference.setEnabled(AccessibilityUtil.isFloatingMenuEnabled(mContext));
    }

    private int getFloatingMenuFadeValue() {
        return Settings.Secure.getInt(mContentResolver,
                Settings.Secure.ACCESSIBILITY_FLOATING_MENU_FADE_ENABLED, ON);
    }

    private void putFloatingMenuFadeValue(boolean isEnabled) {
        Settings.Secure.putInt(mContentResolver,
                Settings.Secure.ACCESSIBILITY_FLOATING_MENU_FADE_ENABLED,
                isEnabled ? ON : OFF);
    }
}
