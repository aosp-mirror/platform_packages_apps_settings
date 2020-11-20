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

package com.android.settings.gestures;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.emergencynumber.EmergencyNumberUtils;

/**
 * Preference controller for emergency gesture number override.
 */
public class EmergencyGestureNumberOverridePreferenceController extends BasePreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    @VisibleForTesting
    EmergencyNumberUtils mEmergencyNumberUtils;
    private final Handler mHandler;
    private final ContentObserver mSettingsObserver;
    private Preference mPreference;

    public EmergencyGestureNumberOverridePreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey);
        mEmergencyNumberUtils = new EmergencyNumberUtils(context);
        mHandler = new Handler(Looper.getMainLooper());
        mSettingsObserver = new EmergencyGestureNumberOverrideSettingsObserver(mHandler);
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getResources()
                .getBoolean(R.bool.config_show_emergency_gesture_settings) ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public CharSequence getSummary() {
        return mContext.getString(R.string.emergency_gesture_call_for_help_summary,
                mEmergencyNumberUtils.getPoliceNumber());
    }

    @Override
    public void onStart() {
        mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(
                Settings.Secure.EMERGENCY_GESTURE_CALL_NUMBER), false, mSettingsObserver);
    }

    @Override
    public void onStop() {
        mContext.getContentResolver().unregisterContentObserver(mSettingsObserver);
    }

    private class EmergencyGestureNumberOverrideSettingsObserver extends ContentObserver {
        EmergencyGestureNumberOverrideSettingsObserver(Handler h) {
            super(h);
        }

        @Override
        public void onChange(boolean selfChange) {
            if (mPreference != null) {
                updateState(mPreference);
            }
        }
    }
}
