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

package com.android.settings.display;

import static android.provider.Settings.Secure.SHOW_QR_CODE_SCANNER_SETTING;

import static androidx.lifecycle.Lifecycle.Event.ON_START;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.provider.Settings;

import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

/**
 * Preference controller for enabling/disabling QR code scanner button on lock screen.
 */
public class QRCodeScannerPreferenceController extends TogglePreferenceController {
    private static final String SETTING_KEY = Settings.Secure.LOCK_SCREEN_SHOW_QR_CODE_SCANNER;
    private final ContentObserver mSettingsObserver;
    private final ContentResolver mContentResolver;
    private Preference mPreference;

    public QRCodeScannerPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mContentResolver = context.getContentResolver();
        mSettingsObserver = new ContentObserver(null) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                updateState(mPreference);
            }
        };
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    /** Called when activity starts being displayed to user. */
    @OnLifecycleEvent(ON_START)
    public void onStart() {
        mContentResolver.registerContentObserver(
                Settings.Global.getUriFor(SHOW_QR_CODE_SCANNER_SETTING), false,
                mSettingsObserver);
    }

    /** Called when activity stops being displayed to user. */
    @OnLifecycleEvent(ON_STOP)
    public void onStop() {
        mContentResolver.unregisterContentObserver(mSettingsObserver);
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getInt(mContext.getContentResolver(), SETTING_KEY, 0) != 0;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putInt(mContext.getContentResolver(), SETTING_KEY,
                isChecked ? 1 : 0);
    }

    @Override
    public int getAvailabilityStatus() {
        if (CustomizableLockScreenUtils.isFeatureEnabled(mContext)) {
            return UNSUPPORTED_ON_DEVICE;
        }

        return isScannerActivityAvailable() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        refreshSummary(preference);
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_display;
    }

    private boolean isScannerActivityAvailable() {
        return Settings.Secure.getString(mContext.getContentResolver(),
                SHOW_QR_CODE_SCANNER_SETTING) != null;
    }
}
