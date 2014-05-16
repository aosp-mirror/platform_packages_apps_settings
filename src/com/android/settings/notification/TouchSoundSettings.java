/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.notification;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.preference.TwoStatePreference;
import android.provider.Settings.System;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;

import java.util.ArrayList;
import java.util.List;

public class TouchSoundSettings extends SettingsPreferenceFragment implements Indexable {
    private static final String TAG = "TouchSoundSettings";

    private static final String KEY_DIAL_PAD_TONES = "dial_pad_tones";
    private static final String KEY_SCREEN_LOCKING_SOUNDS = "screen_locking_sounds";
    private static final String KEY_OTHER_TOUCH_SOUNDS = "other_touch_sounds";
    private static final String KEY_VIBRATE_ON_TOUCH = "vibrate_on_touch";

    private final Handler mHandler = new Handler();
    private final SettingsObserver mSettingsObserver = new SettingsObserver();

    private AudioManager mAudioManager;
    private SystemSettingPref mDialPadTones;
    private SystemSettingPref mScreenLockingSounds;
    private SystemSettingPref mOtherTouchSounds;
    private SystemSettingPref mVibrateOnTouch;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.touch_sound_settings);

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        final boolean hasVoice = Utils.isVoiceCapable(getActivity());
        final Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        final boolean hasHaptic = vibrator != null && vibrator.hasVibrator();

        mDialPadTones = new SystemSettingPref(hasVoice,
                KEY_DIAL_PAD_TONES, System.DTMF_TONE_WHEN_DIALING);

        mScreenLockingSounds = new SystemSettingPref(true,
                KEY_SCREEN_LOCKING_SOUNDS, System.LOCKSCREEN_SOUNDS_ENABLED);

        mOtherTouchSounds = new SystemSettingPref(true,
                KEY_OTHER_TOUCH_SOUNDS,System.SOUND_EFFECTS_ENABLED) {
            @Override
            protected void setSetting(boolean value) {
                if (value) {
                    mAudioManager.loadSoundEffects();
                } else {
                    mAudioManager.unloadSoundEffects();
                }
                super.setSetting(value);
            }
        };

        mVibrateOnTouch = new SystemSettingPref(hasHaptic,
                KEY_VIBRATE_ON_TOUCH, System.HAPTIC_FEEDBACK_ENABLED);
    }

    @Override
    public void onResume() {
        super.onResume();
        mSettingsObserver.register(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        mSettingsObserver.register(false);
    }

    // === Common system setting preference helper ===

    private class SystemSettingPref {
        private final String mSetting;
        private final Uri mUri;

        private TwoStatePreference mPref;

        private SystemSettingPref(boolean applicable, String key, String setting) {
            mSetting = setting;
            mUri = System.getUriFor(mSetting);
            if (!applicable) removePreference(key);
            mPref = (TwoStatePreference) getPreferenceScreen().findPreference(key);
            if (mPref == null) return;
            update();
            mPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    setSetting((Boolean)newValue);
                    return true;
                }
            });
        }

        protected void setSetting(boolean value) {
            System.putInt(getContentResolver(), mSetting, value ? 1 : 0);
        }

        private Uri getUri() {
            return mUri;
        }

        private void update() {
            if (mPref == null) return;
            mPref.setChecked(System.getInt(getContentResolver(), mSetting, 1) != 0);
        }
    }

    // === Indexing ===

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
                final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();
                add(result, context, R.string.touch_sound_settings);
                add(result, context, R.string.dial_pad_tones_title);
                add(result, context, R.string.screen_locking_sounds_title);
                add(result, context, R.string.other_touch_sounds_title);
                add(result, context, R.string.vibrate_on_touch_title);
                return result;
            }

            private void add(List<SearchIndexableRaw> result, Context context, int title) {
                final Resources res = context.getResources();
                final SearchIndexableRaw data = new SearchIndexableRaw(context);
                data.title = res.getString(title);
                data.screenTitle = res.getString(R.string.touch_sound_settings);
                result.add(data);
            }
        };

    // === Callbacks ===

    private final class SettingsObserver extends ContentObserver {
        public SettingsObserver() {
            super(mHandler);
        }

        public void register(boolean register) {
            final ContentResolver cr = getContentResolver();
            if (register) {
                cr.registerContentObserver(mDialPadTones.getUri(), false, this);
                cr.registerContentObserver(mScreenLockingSounds.getUri(), false, this);
                cr.registerContentObserver(mOtherTouchSounds.getUri(), false, this);
                cr.registerContentObserver(mVibrateOnTouch.getUri(), false, this);
            } else {
                cr.unregisterContentObserver(this);
            }
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (mDialPadTones.getUri().equals(uri)) {
                mDialPadTones.update();
            }
            if (mScreenLockingSounds.getUri().equals(uri)) {
                mScreenLockingSounds.update();
            }
            if (mOtherTouchSounds.getUri().equals(uri)) {
                mOtherTouchSounds.update();
            }
            if (mVibrateOnTouch.getUri().equals(uri)) {
                mVibrateOnTouch.update();
            }
        }
    }
}
