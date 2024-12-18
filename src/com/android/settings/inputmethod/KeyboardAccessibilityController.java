/*
 * Copyright 2024 The Android Open Source Project
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

package com.android.settings.inputmethod;

import static androidx.lifecycle.Lifecycle.Event.ON_PAUSE;
import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;

import com.android.settings.core.TogglePreferenceController;
import com.android.settings.keyboard.Flags;

/**
 * Abstract class for toggle controllers of Keyboard accessibility related function.
 */
public abstract class KeyboardAccessibilityController extends TogglePreferenceController implements
        LifecycleObserver {
    private final ContentResolver mContentResolver;
    private final ContentObserver mContentObserver = new ContentObserver(new Handler(true)) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (getSettingUri().equals(uri)) {
                updateKeyboardAccessibilitySettings();
            }
        }
    };

    protected abstract void updateKeyboardAccessibilitySettings();

    protected abstract Uri getSettingUri();

    public KeyboardAccessibilityController(@NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
        mContentResolver = context.getContentResolver();
    }

    @Override
    public void updateState(@NonNull Preference preference) {
        super.updateState(preference);
        refreshSummary(preference);
    }

    @Override
    public int getAvailabilityStatus() {
        return Flags.keyboardAndTouchpadA11yNewPageEnabled() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return 0;
    }

    /** Invoked when the panel is resumed. */
    @OnLifecycleEvent(ON_RESUME)
    public void onResume() {
        registerSettingsObserver();
    }

    /** Invoked when the panel is paused. */
    @OnLifecycleEvent(ON_PAUSE)
    public void onPause() {
        unregisterSettingsObserver();
    }

    private void registerSettingsObserver() {
        unregisterSettingsObserver();
        mContentResolver.registerContentObserver(
                getSettingUri(),
                false,
                mContentObserver,
                UserHandle.myUserId());
        updateKeyboardAccessibilitySettings();
    }

    private void unregisterSettingsObserver() {
        mContentResolver.unregisterContentObserver(mContentObserver);
    }
}
