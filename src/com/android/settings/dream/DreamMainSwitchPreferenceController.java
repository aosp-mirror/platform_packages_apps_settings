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

package com.android.settings.dream;

import static androidx.lifecycle.Lifecycle.Event.ON_START;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.lifecycle.OnLifecycleEvent;

import com.android.settings.widget.SettingsMainSwitchPreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.dream.DreamBackend;

/**
 * Preference controller for switching dreams on/off.
 */
public class DreamMainSwitchPreferenceController extends
        SettingsMainSwitchPreferenceController implements LifecycleObserver {
    static final String MAIN_SWITCH_PREF_KEY = "dream_main_settings_switch";
    private final DreamBackend mBackend;

    private final ContentObserver mObserver = new ContentObserver(
            new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange) {
            updateState(mSwitchPreference);
        }
    };

    public DreamMainSwitchPreferenceController(Context context, String key) {
        super(context, key);
        mBackend = DreamBackend.getInstance(context);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return mBackend.isEnabled();
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        mBackend.setEnabled(isChecked);
        return true;
    }

    @Override
    public boolean isSliceable() {
        return false;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        // not needed since it's not sliceable
        return NO_RES;
    }

    @OnLifecycleEvent(ON_START)
    void onStart() {
        mContext.getContentResolver().registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.SCREENSAVER_ENABLED),
                /* notifyForDescendants= */ false, mObserver);
    }

    @OnLifecycleEvent(ON_STOP)
    void onStop() {
        mContext.getContentResolver().unregisterContentObserver(mObserver);
    }
}
