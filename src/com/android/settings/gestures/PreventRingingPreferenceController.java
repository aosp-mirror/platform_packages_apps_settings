/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static android.provider.Settings.Secure.VOLUME_HUSH_GESTURE;
import static android.provider.Settings.Secure.VOLUME_HUSH_MUTE;
import static android.provider.Settings.Secure.VOLUME_HUSH_OFF;
import static android.provider.Settings.Secure.VOLUME_HUSH_VIBRATE;

import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.widget.VideoPreference;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnCreate;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.core.lifecycle.events.OnSaveInstanceState;

public class PreventRingingPreferenceController extends BasePreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener,
        LifecycleObserver, OnResume, OnPause, OnCreate, OnSaveInstanceState {

    private static final String PREF_KEY_VIDEO = "gesture_prevent_ringing_video";
    @VisibleForTesting
    static final String KEY_VIDEO_PAUSED = "key_video_paused";

    private VideoPreference mVideoPreference;
    @VisibleForTesting
    boolean mVideoPaused;

    private final String SECURE_KEY = VOLUME_HUSH_GESTURE;

    public PreventRingingPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_volumeHushGestureEnabled)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (isAvailable()) {
            mVideoPreference = (VideoPreference) screen.findPreference(getVideoPrefKey());
        }
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (preference != null) {
            if (preference instanceof ListPreference) {
                ListPreference pref = (ListPreference) preference;
                int value = Settings.Secure.getInt(
                        mContext.getContentResolver(), SECURE_KEY, VOLUME_HUSH_VIBRATE);
                switch (value) {
                    case VOLUME_HUSH_VIBRATE:
                        pref.setValue(String.valueOf(value));
                        break;
                    case VOLUME_HUSH_MUTE:
                        pref.setValue(String.valueOf(value));
                        break;
                    default:
                        pref.setValue(String.valueOf(VOLUME_HUSH_OFF));
                }
            }
        }
    }

    @Override
    public CharSequence getSummary() {
        int value = Settings.Secure.getInt(
                mContext.getContentResolver(), SECURE_KEY, VOLUME_HUSH_VIBRATE);
        int summary;
        switch (value) {
            case VOLUME_HUSH_VIBRATE:
                summary = R.string.prevent_ringing_option_vibrate_summary;
                break;
            case VOLUME_HUSH_MUTE:
                summary = R.string.prevent_ringing_option_mute_summary;
                break;
            default:
                summary = R.string.prevent_ringing_option_none_summary;
        }
        return mContext.getString(summary);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mVideoPaused = savedInstanceState.getBoolean(KEY_VIDEO_PAUSED, false);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(KEY_VIDEO_PAUSED, mVideoPaused);
    }

    @Override
    public void onPause() {
        if (mVideoPreference != null) {
            mVideoPaused = mVideoPreference.isVideoPaused();
            mVideoPreference.onViewInvisible();
        }
    }

    @Override
    public void onResume() {
        if (mVideoPreference != null) {
            mVideoPreference.onViewVisible(mVideoPaused);
        }
    }

    protected String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int value = Integer.parseInt((String) newValue);
        Settings.Secure.putInt(mContext.getContentResolver(), SECURE_KEY, value);
        preference.setSummary(getSummary());
        return true;
    }
}
