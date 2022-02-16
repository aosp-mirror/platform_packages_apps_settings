/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.display;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.hardware.display.ColorDisplayManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import com.android.settings.core.TogglePreferenceController;

public class DisplayWhiteBalancePreferenceController extends TogglePreferenceController
    implements LifecycleObserver, OnStart, OnStop {

    private ColorDisplayManager mColorDisplayManager;
    @VisibleForTesting
    ContentObserver mContentObserver;
    private Preference mPreference;

    public DisplayWhiteBalancePreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return getColorDisplayManager().isDisplayWhiteBalanceAvailable(mContext) ?
            AVAILABLE : DISABLED_FOR_USER;
    }

    @Override
    public boolean isChecked() {
        return getColorDisplayManager().isDisplayWhiteBalanceEnabled();
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return getColorDisplayManager().setDisplayWhiteBalanceEnabled(isChecked);
    }

    @Override
    public void onStart() {
        if (!isAvailable()) {
            return;
        }

        final ContentResolver cr = mContext.getContentResolver();
        mContentObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                super.onChange(selfChange, uri);
                updateVisibility();
            }
        };
        cr.registerContentObserver(
                Secure.getUriFor(Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED),
                false /* notifyForDescendants */, mContentObserver,
                ActivityManager.getCurrentUser());
        cr.registerContentObserver(
                Secure.getUriFor(Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED),
                false /* notifyForDescendants */, mContentObserver,
                ActivityManager.getCurrentUser());
        cr.registerContentObserver(
                System.getUriFor(System.DISPLAY_COLOR_MODE),
                false /* notifyForDescendants */, mContentObserver,
                ActivityManager.getCurrentUser());

        updateVisibility();
    }

    @Override
    public void onStop() {
        if (mContentObserver != null) {
            mContext.getContentResolver().unregisterContentObserver(mContentObserver);
            mContentObserver = null;
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @VisibleForTesting
    ColorDisplayManager getColorDisplayManager() {
        if (mColorDisplayManager == null) {
            mColorDisplayManager = mContext.getSystemService(ColorDisplayManager.class);
        }
        return mColorDisplayManager;
    }

    @VisibleForTesting
    void updateVisibility() {
        if (mPreference != null) {
            ColorDisplayManager cdm = getColorDisplayManager();

            // Display white balance is only valid in linear light space. COLOR_MODE_SATURATED
            // implies unmanaged color mode, and hence unknown color processing conditions.
            // We also disallow display white balance when color accessibility features are enabled.
            mPreference.setVisible(cdm.getColorMode() != ColorDisplayManager.COLOR_MODE_SATURATED &&
                    !cdm.areAccessibilityTransformsEnabled(mContext));
        }
    }
}
