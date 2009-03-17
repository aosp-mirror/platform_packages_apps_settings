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
import android.os.IMountService;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.CheckBoxPreference;
import android.provider.Settings;
import android.util.Log;
import android.view.IWindowManager;

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
    private static final String KEY_ANIMATIONS = "animations";
    private static final String KEY_ACCELEROMETER = "accelerometer";
    private static final String KEY_PLAY_MEDIA_NOTIFICATION_SOUNDS = "play_media_notification_sounds";
    
    private CheckBoxPreference mSilent;

    private CheckBoxPreference mPlayMediaNotificationSounds;

    private IMountService mMountService = null;

    /*
     * If we are currently in one of the silent modes (the ringer mode is set to either
     * "silent mode" or "vibrate mode"), then toggling the "Phone vibrate"
     * preference will switch between "silent mode" and "vibrate mode".
     * Otherwise, it will adjust the normal ringer mode's ring or ring+vibrate
     * setting.
     */
    private CheckBoxPreference mVibrate;
    private CheckBoxPreference mDtmfTone;
    private CheckBoxPreference mSoundEffects;
    private CheckBoxPreference mAnimations;
    private CheckBoxPreference mAccelerometer;
    private float[] mAnimationScales;
    
    private AudioManager mAudioManager;
    
    private IWindowManager mWindowManager;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateState(false);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContentResolver resolver = getContentResolver();
        
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));

        mMountService = IMountService.Stub.asInterface(ServiceManager.getService("mount"));
        
        addPreferencesFromResource(R.xml.sound_and_display_settings);
        
        mSilent = (CheckBoxPreference) findPreference(KEY_SILENT);
        mPlayMediaNotificationSounds = (CheckBoxPreference) findPreference(KEY_PLAY_MEDIA_NOTIFICATION_SOUNDS);

        mVibrate = (CheckBoxPreference) findPreference(KEY_VIBRATE);
        mDtmfTone = (CheckBoxPreference) findPreference(KEY_DTMF_TONE);
        mDtmfTone.setPersistent(false);
        mDtmfTone.setChecked(Settings.System.getInt(resolver,
                Settings.System.DTMF_TONE_WHEN_DIALING, 1) != 0);
        mSoundEffects = (CheckBoxPreference) findPreference(KEY_SOUND_EFFECTS);
        mSoundEffects.setPersistent(false);
        mSoundEffects.setChecked(Settings.System.getInt(resolver,
                Settings.System.SOUND_EFFECTS_ENABLED, 0) != 0);
        mAnimations = (CheckBoxPreference) findPreference(KEY_ANIMATIONS);
        mAnimations.setPersistent(false);
        mAccelerometer = (CheckBoxPreference) findPreference(KEY_ACCELEROMETER);
        mAccelerometer.setPersistent(false);
        
        ListPreference screenTimeoutPreference =
            (ListPreference) findPreference(KEY_SCREEN_TIMEOUT);
        screenTimeoutPreference.setValue(String.valueOf(Settings.System.getInt(
                resolver, SCREEN_OFF_TIMEOUT, FALLBACK_SCREEN_TIMEOUT_VALUE)));
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
        final int ringerMode = mAudioManager.getRingerMode();
        final boolean silentOrVibrateMode =
                ringerMode != AudioManager.RINGER_MODE_NORMAL;
        
        if (silentOrVibrateMode != mSilent.isChecked() || force) {
            mSilent.setChecked(silentOrVibrateMode);
        }

        try {
            mPlayMediaNotificationSounds.setChecked(mMountService.getPlayNotificationSounds());
        } catch (RemoteException e) {
        }
       
        boolean vibrateSetting;
        if (silentOrVibrateMode) {
            vibrateSetting = ringerMode == AudioManager.RINGER_MODE_VIBRATE;
        } else {
            vibrateSetting = mAudioManager.getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER)
                    == AudioManager.VIBRATE_SETTING_ON;            
        }
        if (vibrateSetting != mVibrate.isChecked() || force) {
            mVibrate.setChecked(vibrateSetting);
        }
        
        boolean animations = true;
        try {
            mAnimationScales = mWindowManager.getAnimationScales();
        } catch (RemoteException e) {
        }
        if (mAnimationScales != null) {
            // We will leave the window animations alone (always set),
            // and only use this to change the transition animations.
            for (int i=1; i<mAnimationScales.length; i++) {
                if (mAnimationScales[i] == 0) {
                    animations = false;
                    break;
                }
            }
        }
        if (animations != mAnimations.isChecked() || force) {
            mAnimations.setChecked(animations);
        }
        mAccelerometer.setChecked(Settings.System.getInt(
                getContentResolver(), 
                Settings.System.ACCELEROMETER_ROTATION, 0) != 0);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        if (preference == mSilent) {
            final boolean silent = mSilent.isChecked();
            mAudioManager.setRingerMode(silent ? AudioManager.RINGER_MODE_SILENT
                    : AudioManager.RINGER_MODE_NORMAL);
            updateState(false);
            
        } else if (preference == mPlayMediaNotificationSounds) {
            try {
                mMountService.setPlayNotificationSounds(mPlayMediaNotificationSounds.isChecked());
            } catch (RemoteException e) {
            }
        } else if (preference == mVibrate) {
            final boolean vibrate = mVibrate.isChecked();
            final boolean silent = mSilent.isChecked();
            
            if (silent) {
                mAudioManager.setRingerMode(vibrate ? AudioManager.RINGER_MODE_VIBRATE :
                    AudioManager.RINGER_MODE_SILENT);                
            } else {
                mAudioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER,
                        vibrate ? AudioManager.VIBRATE_SETTING_ON
                                : AudioManager.VIBRATE_SETTING_OFF);
            }
            
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
            
        } else if (preference == mAnimations) {
            if (mAnimationScales.length > 0) {
                // Window animations are always on.
                mAnimationScales[0] = 1;
            }
            for (int i=1; i<mAnimationScales.length; i++) {
                mAnimationScales[i] = mAnimations.isChecked() ? 1 : 0;
            }
            try {
                mWindowManager.setAnimationScales(mAnimationScales);
            } catch (RemoteException e) {
            }
            
        } else if (preference == mAccelerometer) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION,
                    mAccelerometer.isChecked() ? 1 : 0);
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
