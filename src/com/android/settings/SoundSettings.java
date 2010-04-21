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
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.IWindowManager;

public class SoundSettings extends PreferenceActivity implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "SoundAndDisplaysSettings";

    /** If there is no setting in the provider, use this. */
    private static final int FALLBACK_SCREEN_TIMEOUT_VALUE = 30000;
    private static final int FALLBACK_EMERGENCY_TONE_VALUE = 0;

    private static final String KEY_SILENT = "silent";
    private static final String KEY_VIBRATE = "vibrate";
    private static final String KEY_DTMF_TONE = "dtmf_tone";
    private static final String KEY_SOUND_EFFECTS = "sound_effects";
    private static final String KEY_HAPTIC_FEEDBACK = "haptic_feedback";
    private static final String KEY_EMERGENCY_TONE = "emergency_tone";
    private static final String KEY_SOUND_SETTINGS = "sound_settings";
    private static final String KEY_NOTIFICATION_PULSE = "notification_pulse";
    private static final String KEY_LOCK_SOUNDS = "lock_sounds";

    private static final String VALUE_VIBRATE_NEVER = "never";
    private static final String VALUE_VIBRATE_ALWAYS = "always";
    private static final String VALUE_VIBRATE_ONLY_SILENT = "silent";
    private static final String VALUE_VIBRATE_UNLESS_SILENT = "notsilent";

    private CheckBoxPreference mSilent;

    /*
     * If we are currently in one of the silent modes (the ringer mode is set to either
     * "silent mode" or "vibrate mode"), then toggling the "Phone vibrate"
     * preference will switch between "silent mode" and "vibrate mode".
     * Otherwise, it will adjust the normal ringer mode's ring or ring+vibrate
     * setting.
     */
    private ListPreference mVibrate;
    private CheckBoxPreference mDtmfTone;
    private CheckBoxPreference mSoundEffects;
    private CheckBoxPreference mHapticFeedback;
    private CheckBoxPreference mNotificationPulse;
    private CheckBoxPreference mLockSounds;

    private AudioManager mAudioManager;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(AudioManager.RINGER_MODE_CHANGED_ACTION)) {
                updateState(false);
            }
        }
    };

    private PreferenceGroup mSoundSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContentResolver resolver = getContentResolver();
        int activePhoneType = TelephonyManager.getDefault().getPhoneType();

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        addPreferencesFromResource(R.xml.sound_settings);

        if (TelephonyManager.PHONE_TYPE_CDMA != activePhoneType) {
            // device is not CDMA, do not display CDMA emergency_tone
            getPreferenceScreen().removePreference(findPreference(KEY_EMERGENCY_TONE));
        }

        mSilent = (CheckBoxPreference) findPreference(KEY_SILENT);

        mVibrate = (ListPreference) findPreference(KEY_VIBRATE);
        mVibrate.setOnPreferenceChangeListener(this);

        mDtmfTone = (CheckBoxPreference) findPreference(KEY_DTMF_TONE);
        mDtmfTone.setPersistent(false);
        mDtmfTone.setChecked(Settings.System.getInt(resolver,
                Settings.System.DTMF_TONE_WHEN_DIALING, 1) != 0);
        mSoundEffects = (CheckBoxPreference) findPreference(KEY_SOUND_EFFECTS);
        mSoundEffects.setPersistent(false);
        mSoundEffects.setChecked(Settings.System.getInt(resolver,
                Settings.System.SOUND_EFFECTS_ENABLED, 0) != 0);
        mHapticFeedback = (CheckBoxPreference) findPreference(KEY_HAPTIC_FEEDBACK);
        mHapticFeedback.setPersistent(false);
        mHapticFeedback.setChecked(Settings.System.getInt(resolver,
                Settings.System.HAPTIC_FEEDBACK_ENABLED, 0) != 0);
        mLockSounds = (CheckBoxPreference) findPreference(KEY_LOCK_SOUNDS);
        mLockSounds.setPersistent(false);
        mLockSounds.setChecked(Settings.System.getInt(resolver,
                Settings.System.LOCKSCREEN_SOUNDS_ENABLED, 1) != 0);

        if (TelephonyManager.PHONE_TYPE_CDMA == activePhoneType) {
            ListPreference emergencyTonePreference =
                (ListPreference) findPreference(KEY_EMERGENCY_TONE);
            emergencyTonePreference.setValue(String.valueOf(Settings.System.getInt(
                resolver, Settings.System.EMERGENCY_TONE, FALLBACK_EMERGENCY_TONE_VALUE)));
            emergencyTonePreference.setOnPreferenceChangeListener(this);
        }

        mSoundSettings = (PreferenceGroup) findPreference(KEY_SOUND_SETTINGS);
        mNotificationPulse = (CheckBoxPreference)
                mSoundSettings.findPreference(KEY_NOTIFICATION_PULSE);
        if (mNotificationPulse != null &&
                getResources().getBoolean(R.bool.has_intrusive_led) == false) {
            mSoundSettings.removePreference(mNotificationPulse);
        } else {
            try {
                mNotificationPulse.setChecked(Settings.System.getInt(resolver,
                        Settings.System.NOTIFICATION_LIGHT_PULSE) == 1);
                mNotificationPulse.setOnPreferenceChangeListener(this);
            } catch (SettingNotFoundException snfe) {
                Log.e(TAG, Settings.System.NOTIFICATION_LIGHT_PULSE + " not found");
            }
        }

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

    private String getPhoneVibrateSettingValue() {
        boolean vibeInSilent = (Settings.System.getInt(
            getContentResolver(),
            Settings.System.VIBRATE_IN_SILENT,
            1) == 1);

        // Control phone vibe independent of silent mode
        int callsVibrateSetting = 
            mAudioManager.getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER);

        if (vibeInSilent) {
            if (callsVibrateSetting == AudioManager.VIBRATE_SETTING_OFF) {
                // this state does not make sense; fix it up for the user
                mAudioManager.setVibrateSetting(
                    AudioManager.VIBRATE_TYPE_RINGER,
                    AudioManager.VIBRATE_SETTING_ONLY_SILENT);
            }
            if (callsVibrateSetting == AudioManager.VIBRATE_SETTING_ON) {
                return VALUE_VIBRATE_ALWAYS;
            } else {
                return VALUE_VIBRATE_ONLY_SILENT;
            }
        } else {
            if (callsVibrateSetting == AudioManager.VIBRATE_SETTING_ONLY_SILENT) {
                // this state does not make sense; fix it up
                mAudioManager.setVibrateSetting(
                    AudioManager.VIBRATE_TYPE_RINGER,
                    AudioManager.VIBRATE_SETTING_OFF);
            }
            if (callsVibrateSetting == AudioManager.VIBRATE_SETTING_ON) {
                return VALUE_VIBRATE_UNLESS_SILENT;
            } else {
                return VALUE_VIBRATE_NEVER;
            }
        }
    }

    private void setPhoneVibrateSettingValue(String value) {
        boolean vibeInSilent;
        int callsVibrateSetting;

        if (value.equals(VALUE_VIBRATE_UNLESS_SILENT)) {
            callsVibrateSetting = AudioManager.VIBRATE_SETTING_ON;
            vibeInSilent = false;
        } else if (value.equals(VALUE_VIBRATE_NEVER)) {
            callsVibrateSetting = AudioManager.VIBRATE_SETTING_OFF;
            vibeInSilent = false;
        } else if (value.equals(VALUE_VIBRATE_ONLY_SILENT)) {
            callsVibrateSetting = AudioManager.VIBRATE_SETTING_ONLY_SILENT;
            vibeInSilent = true;
        } else { //VALUE_VIBRATE_ALWAYS
            callsVibrateSetting = AudioManager.VIBRATE_SETTING_ON;
            vibeInSilent = true;
        }

        Settings.System.putInt(getContentResolver(),
            Settings.System.VIBRATE_IN_SILENT,
            vibeInSilent ? 1 : 0);

        // might need to switch the ringer mode from one kind of "silent" to
        // another
        if (mSilent.isChecked()) {
            mAudioManager.setRingerMode(
                vibeInSilent ? AudioManager.RINGER_MODE_VIBRATE
                             : AudioManager.RINGER_MODE_SILENT);
        }

        mAudioManager.setVibrateSetting(
            AudioManager.VIBRATE_TYPE_RINGER,
            callsVibrateSetting);
    }

    // updateState in fact updates the UI to reflect the system state
    private void updateState(boolean force) {
        final int ringerMode = mAudioManager.getRingerMode();

        // NB: in the UI we now simply call this "silent mode". A separate
        // setting controls whether we're in RINGER_MODE_SILENT or
        // RINGER_MODE_VIBRATE.
        final boolean silentOrVibrateMode =
                ringerMode != AudioManager.RINGER_MODE_NORMAL;

        if (silentOrVibrateMode != mSilent.isChecked() || force) {
            mSilent.setChecked(silentOrVibrateMode);
        }

        String phoneVibrateSetting = getPhoneVibrateSettingValue();

        if (! phoneVibrateSetting.equals(mVibrate.getValue()) || force) {
            mVibrate.setValue(phoneVibrateSetting);
        }
        mVibrate.setSummary(mVibrate.getEntry());

        int silentModeStreams = Settings.System.getInt(getContentResolver(),
                Settings.System.MODE_RINGER_STREAMS_AFFECTED, 0);
        boolean isAlarmInclSilentMode = (silentModeStreams & (1 << AudioManager.STREAM_ALARM)) != 0;
        mSilent.setSummary(isAlarmInclSilentMode ?
                R.string.silent_mode_incl_alarm_summary :
                R.string.silent_mode_summary);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mSilent) {
            if (mSilent.isChecked()) {
                boolean vibeInSilent = (1 == Settings.System.getInt(
                    getContentResolver(),
                    Settings.System.VIBRATE_IN_SILENT,
                    1));
                mAudioManager.setRingerMode(
                    vibeInSilent ? AudioManager.RINGER_MODE_VIBRATE
                                 : AudioManager.RINGER_MODE_SILENT);
            } else {
                mAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            }
            updateState(false);
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

        } else if (preference == mHapticFeedback) {
            Settings.System.putInt(getContentResolver(), Settings.System.HAPTIC_FEEDBACK_ENABLED,
                    mHapticFeedback.isChecked() ? 1 : 0);

        } else if (preference == mLockSounds) {
            Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_SOUNDS_ENABLED,
                    mLockSounds.isChecked() ? 1 : 0);

        } else if (preference == mNotificationPulse) {
            boolean value = mNotificationPulse.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.NOTIFICATION_LIGHT_PULSE, value ? 1 : 0);
        }

        return true;
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (KEY_EMERGENCY_TONE.equals(key)) {
            int value = Integer.parseInt((String) objValue);
            try {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.EMERGENCY_TONE, value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist emergency tone setting", e);
            }
        } else if (preference == mVibrate) {
            setPhoneVibrateSettingValue(objValue.toString());
            updateState(false);
        }

        return true;
    }
}
