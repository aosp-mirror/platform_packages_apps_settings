/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.accessibility.MagnificationCapabilities.MagnificationMode;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

/**
 * Controller that accesses and switches the preference status of the magnification always on
 * feature, where the magnifier will not deactivate on Activity transitions; it will only zoom out
 * to 100%.
 */
public class MagnificationAlwaysOnPreferenceController extends TogglePreferenceController
        implements LifecycleObserver, OnResume, OnPause {

    private static final String TAG =
            MagnificationAlwaysOnPreferenceController.class.getSimpleName();
    static final String PREF_KEY = Settings.Secure.ACCESSIBILITY_MAGNIFICATION_ALWAYS_ON_ENABLED;

    private Preference mPreference;

    @VisibleForTesting
    final ContentObserver mContentObserver = new ContentObserver(
            new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange, @Nullable Uri uri) {
            updateState(mPreference);
        }
    };

    public MagnificationAlwaysOnPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public void onResume() {
        if (Flags.hideMagnificationAlwaysOnToggleWhenWindowModeOnly()) {
            MagnificationCapabilities.registerObserver(mContext, mContentObserver);
        }
    }

    @Override
    public void onPause() {
        if (Flags.hideMagnificationAlwaysOnToggleWhenWindowModeOnly()) {
            MagnificationCapabilities.unregisterObserver(mContext, mContentObserver);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        updateState(mPreference);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_ALWAYS_ON_ENABLED, ON) == ON;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_ALWAYS_ON_ENABLED,
                (isChecked ? ON : OFF));
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_accessibility;
    }

    @Override
    public CharSequence getSummary() {
        if (!Flags.hideMagnificationAlwaysOnToggleWhenWindowModeOnly()) {
            return super.getSummary();
        }

        @StringRes int resId = mPreference.isEnabled()
                ? R.string.accessibility_screen_magnification_always_on_summary
                : R.string.accessibility_screen_magnification_always_on_unavailable_summary;
        return mContext.getString(resId);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (!Flags.hideMagnificationAlwaysOnToggleWhenWindowModeOnly()) {
            return;
        }

        if (preference == null) {
            return;
        }
        @MagnificationMode int mode =
                MagnificationCapabilities.getCapabilities(mContext);
        preference.setEnabled(
                mode == MagnificationMode.FULLSCREEN || mode == MagnificationMode.ALL);
        refreshSummary(preference);
    }
}
