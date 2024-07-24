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

import androidx.annotation.IntDef;
import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

import com.google.common.primitives.Ints;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Preference controller that controls the preferred size in accessibility button page. */
public class FloatingMenuSizePreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener, LifecycleObserver, OnResume, OnPause {

    private final ContentResolver mContentResolver;
    @VisibleForTesting
    final ContentObserver mContentObserver;

    @VisibleForTesting
    ListPreference mPreference;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            Size.UNKNOWN,
            Size.SMALL,
            Size.LARGE,
    })
    public @interface Size {
        int UNKNOWN = -1;
        int SMALL = 0;
        int LARGE = 1;
    }

    public FloatingMenuSizePreferenceController(Context context, String preferenceKey) {
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
        final Integer value = Ints.tryParse((String) newValue);
        if (value != null) {
            putAccessibilityFloatingMenuSize(value);
        }
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        final ListPreference listPreference = (ListPreference) preference;

        listPreference.setValue(String.valueOf(getAccessibilityFloatingMenuSize()));
    }

    @Override
    public void onResume() {
        mContentResolver.registerContentObserver(
                Settings.Secure.getUriFor(
                        Settings.Secure.ACCESSIBILITY_BUTTON_MODE), /* notifyForDescendants= */
                false, mContentObserver);

    }

    @Override
    public void onPause() {
        mContentResolver.unregisterContentObserver(mContentObserver);
    }

    private void updateAvailabilityStatus() {
        mPreference.setEnabled(AccessibilityUtil.isFloatingMenuEnabled(mContext));
    }

    @Size
    private int getAccessibilityFloatingMenuSize() {
        return Settings.Secure.getInt(mContentResolver,
                Settings.Secure.ACCESSIBILITY_FLOATING_MENU_SIZE, Size.SMALL);
    }

    private void putAccessibilityFloatingMenuSize(@Size int value) {
        Settings.Secure.putInt(mContentResolver,
                Settings.Secure.ACCESSIBILITY_FLOATING_MENU_SIZE, value);
    }
}
