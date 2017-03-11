/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.SeekBarPreference;
import com.android.settings.core.PreferenceController;
import com.android.settings.core.lifecycle.Lifecycle;
import com.android.settings.core.lifecycle.LifecycleObserver;
import com.android.settings.core.lifecycle.events.OnPause;
import com.android.settings.core.lifecycle.events.OnResume;
import com.android.settings.overlay.FeatureFactory;

public class AssistGestureSensitivityPreferenceController extends PreferenceController
        implements Preference.OnPreferenceChangeListener, LifecycleObserver, OnPause, OnResume {

    private static final String PREF_KEY_ASSIST_GESTURE_SENSITIVITY = "gesture_assist_sensitivity";

    private final AssistGestureFeatureProvider mFeatureProvider;
    private final SettingObserver mSettingObserver;

    private PreferenceScreen mScreen;
    private SeekBarPreference mPreference;

    public AssistGestureSensitivityPreferenceController(Context context, Lifecycle lifecycle) {
        super(context);
        mFeatureProvider = FeatureFactory.getFactory(context).getAssistGestureFeatureProvider();
        mSettingObserver = new SettingObserver();

        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public void onResume() {
        mSettingObserver.register(mContext.getContentResolver(), true /* register */);
        updatePreference();
    }

    @Override
    public void onPause() {
        mSettingObserver.register(mContext.getContentResolver(), false /* register */);
    }

    @Override
    public boolean isAvailable() {
        // The sensitivity control is contingent on the assist gesture being supported and the
        // gesture being enabled.
        final int gestureEnabled = Settings.Secure.getInt(
                mContext.getContentResolver(),
                Settings.Secure.ASSIST_GESTURE_ENABLED,
                1);
        return (gestureEnabled == 1) && mFeatureProvider.isSupported(mContext);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mScreen = screen;
        mPreference = (SeekBarPreference) screen.findPreference(getPreferenceKey());
        // Call super last or AbstractPreferenceController might remove the preference from the
        // screen (if !isAvailable()) before we can save a reference to it.
        super.displayPreference(screen);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        updatePreference();
    }

    private void updatePreference() {
        if (mPreference == null) {
            return;
        }

        if (isAvailable()) {
            if (mScreen.findPreference(getPreferenceKey()) == null) {
                mScreen.addPreference(mPreference);
            }
        } else {
            mScreen.removePreference(mPreference);
        }

        final int sensitivity = Settings.Secure.getInt(
                mContext.getContentResolver(),
                Settings.Secure.ASSIST_GESTURE_SENSITIVITY,
                mPreference.getProgress());
        mPreference.setProgress(sensitivity);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final int sensitivity = (int) newValue;
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ASSIST_GESTURE_SENSITIVITY, sensitivity);
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY_ASSIST_GESTURE_SENSITIVITY;
    }

    class SettingObserver extends ContentObserver {

        private final Uri ASSIST_GESTURE_ENABLED_URI =
                Settings.Secure.getUriFor(Settings.Secure.ASSIST_GESTURE_ENABLED);
        private final Uri ASSIST_GESTURE_SENSITIVITY_URI =
                Settings.Secure.getUriFor(Settings.Secure.ASSIST_GESTURE_SENSITIVITY);

        public SettingObserver() {
            super(null /* handler */);
        }

        public void register(ContentResolver cr, boolean register) {
            if (register) {
                cr.registerContentObserver(ASSIST_GESTURE_ENABLED_URI, false, this);
                cr.registerContentObserver(ASSIST_GESTURE_SENSITIVITY_URI, false, this);
            } else {
                cr.unregisterContentObserver(this);
            }
        }

        @Override
        public void onChange(boolean selfChange) {
            updatePreference();
        }
    }
}
