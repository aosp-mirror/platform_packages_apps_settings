/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.settings;

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.CheckBoxPreference;
import android.provider.Settings;
import android.util.Log;

public class SoundAndDisplaySettings extends PreferenceActivity implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "SoundAndDisplaysSettings";

    /** If there is no setting in the provider, use this. */
    private static final int FALLBACK_SCREEN_TIMEOUT_VALUE = 30000;
    
    private static final String KEY_SILENT = "silent";
    private static final String KEY_VIBRATE = "vibrate";
    private static final String KEY_SCREEN_TIMEOUT = "screen_timeout";
    private static final String KEY_DTMF_TONE = "dtmf_tone";
    private static final String KEY_SOUND_EFFECTS = "sound_effects";
    
    private CheckBoxPreference mSilent;
    private CheckBoxPreference mVibrate;
    private CheckBoxPreference mDtmfTone;
    private CheckBoxPreference mSoundEffects;
    
    private AudioManager mAudioManager;
    
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            
            int ringerMode = intent
                    .getIntExtra(AudioManager.EXTRA_RINGER_MODE, AudioManager.RINGER_MODE_NORMAL);
            boolean isSilentMode = ringerMode != AudioManager.RINGER_MODE_NORMAL;
            
            if (mSilent.isChecked() != isSilentMode) {
                mSilent.setChecked(isSilentMode);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContentResolver resolver = getContentResolver();
        
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        
        addPreferencesFromResource(R.xml.sound_and_display_settings);
        
        mSilent = (CheckBoxPreference) findPreference(KEY_SILENT);
        mVibrate = (CheckBoxPreference) findPreference(KEY_VIBRATE);
        mDtmfTone = (CheckBoxPreference) findPreference(KEY_DTMF_TONE);
        mDtmfTone.setPersistent(false);
        mDtmfTone.setChecked(Settings.System.getInt(resolver,
                Settings.System.DTMF_TONE_WHEN_DIALING, 1) != 0);
        mSoundEffects = (CheckBoxPreference) findPreference(KEY_SOUND_EFFECTS);
        mSoundEffects.setPersistent(false);
        mSoundEffects.setChecked(Settings.System.getInt(resolver,
                Settings.System.SOUND_EFFECTS_ENABLED, 0) != 0);
        
        ListPreference screenTimeoutPreference =
            (ListPreference) findPreference(KEY_SCREEN_TIMEOUT);
        screenTimeoutPreference.setValue(String.valueOf(Settings.System.getInt(
                getContentResolver(), SCREEN_OFF_TIMEOUT, FALLBACK_SCREEN_TIMEOUT_VALUE)));
        screenTimeoutPreference.setOnPreferenceChangeListener(this);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        updateState(true);
        
        IntentFilter filter = new IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION);
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(mReceiver);
    }

    private void updateState(boolean force) {
        final boolean silent = mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL;
        final boolean phoneVibrate = mAudioManager
                .getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER)
                        == AudioManager.VIBRATE_SETTING_ON;
        
        if (silent != mSilent.isChecked() || force) {
            mSilent.setChecked(silent);
        }
        
        if (phoneVibrate != mVibrate.isChecked() || force) {
            mVibrate.setChecked(phoneVibrate);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        if (preference == mSilent) {
            final boolean silent = mSilent.isChecked();
            mAudioManager.setRingerMode(silent ? AudioManager.RINGER_MODE_SILENT
                    : AudioManager.RINGER_MODE_NORMAL);
            
        } else if (preference == mVibrate) {
            final boolean vibrate = mVibrate.isChecked();
            mAudioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER,
                    vibrate ? AudioManager.VIBRATE_SETTING_ON
                            : AudioManager.VIBRATE_SETTING_OFF);
        } else if (preference == mDtmfTone) {
            Settings.System.putInt(getContentResolver(), Settings.System.DTMF_TONE_WHEN_DIALING,
                    mDtmfTone.isChecked() ? 1 : 0);
            
        } else if (preference == mSoundEffects) {
            if (mSoundEffects.isChecked()) {
                mAudioManager.loadSoundEffects();
            } else {
                mAudioManager.unloadSoundEffects();
            }
            Settings.System.putInt(getContentResolver(), Settings.System.SOUND_EFFECTS_ENABLED,
                    mSoundEffects.isChecked() ? 1 : 0);
        }
        return true;
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (KEY_SCREEN_TIMEOUT.equals(preference.getKey())) {
            int value = Integer.parseInt((String) objValue);
            try {
                Settings.System.putInt(getContentResolver(), 
                        SCREEN_OFF_TIMEOUT, value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist screen timeout setting", e);
            }
        }
        
        return true;
    }

}
