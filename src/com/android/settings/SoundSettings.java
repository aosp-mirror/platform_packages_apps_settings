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

import com.android.settings.bluetooth.DockEventReceiver;

import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.List;

public class SoundSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "SoundSettings";

    private static final int DIALOG_NOT_DOCKED = 1;

    /** If there is no setting in the provider, use this. */
    private static final int FALLBACK_EMERGENCY_TONE_VALUE = 0;

    private static final String KEY_VIBRATE = "vibrate_when_ringing";
    private static final String KEY_RING_VOLUME = "ring_volume";
    private static final String KEY_MUSICFX = "musicfx";
    private static final String KEY_DTMF_TONE = "dtmf_tone";
    private static final String KEY_SOUND_EFFECTS = "sound_effects";
    private static final String KEY_HAPTIC_FEEDBACK = "haptic_feedback";
    private static final String KEY_EMERGENCY_TONE = "emergency_tone";
    private static final String KEY_SOUND_SETTINGS = "sound_settings";
    private static final String KEY_LOCK_SOUNDS = "lock_sounds";
    private static final String KEY_RINGTONE = "ringtone";
    private static final String KEY_NOTIFICATION_SOUND = "notification_sound";
    private static final String KEY_CATEGORY_CALLS = "category_calls_and_notification";
    private static final String KEY_DOCK_CATEGORY = "dock_category";
    private static final String KEY_DOCK_AUDIO_SETTINGS = "dock_audio";
    private static final String KEY_DOCK_SOUNDS = "dock_sounds";
    private static final String KEY_DOCK_AUDIO_MEDIA_ENABLED = "dock_audio_media_enabled";

    private static final String[] NEED_VOICE_CAPABILITY = {
            KEY_RINGTONE, KEY_DTMF_TONE, KEY_CATEGORY_CALLS,
            KEY_EMERGENCY_TONE, KEY_VIBRATE
    };

    private static final int MSG_UPDATE_RINGTONE_SUMMARY = 1;
    private static final int MSG_UPDATE_NOTIFICATION_SUMMARY = 2;

    private CheckBoxPreference mVibrateWhenRinging;
    private CheckBoxPreference mDtmfTone;
    private CheckBoxPreference mSoundEffects;
    private CheckBoxPreference mHapticFeedback;
    private Preference mMusicFx;
    private CheckBoxPreference mLockSounds;
    private Preference mRingtonePreference;
    private Preference mNotificationPreference;

    private Runnable mRingtoneLookupRunnable;

    private AudioManager mAudioManager;

    private Preference mDockAudioSettings;
    private CheckBoxPreference mDockSounds;
    private Intent mDockIntent;
    private CheckBoxPreference mDockAudioMediaEnabled;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_UPDATE_RINGTONE_SUMMARY:
                mRingtonePreference.setSummary((CharSequence) msg.obj);
                break;
            case MSG_UPDATE_NOTIFICATION_SUMMARY:
                mNotificationPreference.setSummary((CharSequence) msg.obj);
                break;
            }
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_DOCK_EVENT)) {
                handleDockChange(intent);
            }
        }
    };

    private PreferenceGroup mSoundSettings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContentResolver resolver = getContentResolver();
        int activePhoneType = TelephonyManager.getDefault().getCurrentPhoneType();

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        addPreferencesFromResource(R.xml.sound_settings);

        if (TelephonyManager.PHONE_TYPE_CDMA != activePhoneType) {
            // device is not CDMA, do not display CDMA emergency_tone
            getPreferenceScreen().removePreference(findPreference(KEY_EMERGENCY_TONE));
        }

        if (!getResources().getBoolean(R.bool.has_silent_mode)) {
            findPreference(KEY_RING_VOLUME).setDependency(null);
        }

        if (getResources().getBoolean(com.android.internal.R.bool.config_useFixedVolume)) {
            // device with fixed volume policy, do not display volumes submenu
            getPreferenceScreen().removePreference(findPreference(KEY_RING_VOLUME));
        }

        mVibrateWhenRinging = (CheckBoxPreference) findPreference(KEY_VIBRATE);
        mVibrateWhenRinging.setPersistent(false);
        mVibrateWhenRinging.setChecked(Settings.System.getInt(resolver,
                Settings.System.VIBRATE_WHEN_RINGING, 0) != 0);

        mDtmfTone = (CheckBoxPreference) findPreference(KEY_DTMF_TONE);
        mDtmfTone.setPersistent(false);
        mDtmfTone.setChecked(Settings.System.getInt(resolver,
                Settings.System.DTMF_TONE_WHEN_DIALING, 1) != 0);
        mSoundEffects = (CheckBoxPreference) findPreference(KEY_SOUND_EFFECTS);
        mSoundEffects.setPersistent(false);
        mSoundEffects.setChecked(Settings.System.getInt(resolver,
                Settings.System.SOUND_EFFECTS_ENABLED, 1) != 0);
        mHapticFeedback = (CheckBoxPreference) findPreference(KEY_HAPTIC_FEEDBACK);
        mHapticFeedback.setPersistent(false);
        mHapticFeedback.setChecked(Settings.System.getInt(resolver,
                Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0);
        mLockSounds = (CheckBoxPreference) findPreference(KEY_LOCK_SOUNDS);
        mLockSounds.setPersistent(false);
        mLockSounds.setChecked(Settings.System.getInt(resolver,
                Settings.System.LOCKSCREEN_SOUNDS_ENABLED, 1) != 0);

        mRingtonePreference = findPreference(KEY_RINGTONE);
        mNotificationPreference = findPreference(KEY_NOTIFICATION_SOUND);

        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) {
            removePreference(KEY_VIBRATE);
            removePreference(KEY_HAPTIC_FEEDBACK);
        }

        if (TelephonyManager.PHONE_TYPE_CDMA == activePhoneType) {
            ListPreference emergencyTonePreference =
                (ListPreference) findPreference(KEY_EMERGENCY_TONE);
            emergencyTonePreference.setValue(String.valueOf(Settings.Global.getInt(
                resolver, Settings.Global.EMERGENCY_TONE, FALLBACK_EMERGENCY_TONE_VALUE)));
            emergencyTonePreference.setOnPreferenceChangeListener(this);
        }

        mSoundSettings = (PreferenceGroup) findPreference(KEY_SOUND_SETTINGS);

        mMusicFx = mSoundSettings.findPreference(KEY_MUSICFX);
        Intent i = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
        PackageManager p = getPackageManager();
        List<ResolveInfo> ris = p.queryIntentActivities(i, PackageManager.GET_DISABLED_COMPONENTS);
        if (ris.size() <= 2) {
            // no need to show the item if there is no choice for the user to make
            // note: the built in musicfx panel has two activities (one being a
            // compatibility shim that launches either the other activity, or a
            // third party one), hence the check for <=2. If the implementation
            // of the compatbility layer changes, this check may need to be updated.
            mSoundSettings.removePreference(mMusicFx);
        }

        if (!Utils.isVoiceCapable(getActivity())) {
            for (String prefKey : NEED_VOICE_CAPABILITY) {
                Preference pref = findPreference(prefKey);
                if (pref != null) {
                    getPreferenceScreen().removePreference(pref);
                }
            }
        }

        mRingtoneLookupRunnable = new Runnable() {
            public void run() {
                if (mRingtonePreference != null) {
                    updateRingtoneName(RingtoneManager.TYPE_RINGTONE, mRingtonePreference,
                            MSG_UPDATE_RINGTONE_SUMMARY);
                }
                if (mNotificationPreference != null) {
                    updateRingtoneName(RingtoneManager.TYPE_NOTIFICATION, mNotificationPreference,
                            MSG_UPDATE_NOTIFICATION_SUMMARY);
                }
            }
        };

        initDockSettings();
    }

    @Override
    public void onResume() {
        super.onResume();

        lookupRingtoneNames();

        IntentFilter filter = new IntentFilter(Intent.ACTION_DOCK_EVENT);
        getActivity().registerReceiver(mReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();

        getActivity().unregisterReceiver(mReceiver);
    }

    private void updateRingtoneName(int type, Preference preference, int msg) {
        if (preference == null) return;
        Context context = getActivity();
        if (context == null) return;
        Uri ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context, type);
        CharSequence summary = context.getString(com.android.internal.R.string.ringtone_unknown);
        // Is it a silent ringtone?
        if (ringtoneUri == null) {
            summary = context.getString(com.android.internal.R.string.ringtone_silent);
        } else {
            // Fetch the ringtone title from the media provider
            try {
                Cursor cursor = context.getContentResolver().query(ringtoneUri,
                        new String[] { MediaStore.Audio.Media.TITLE }, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        summary = cursor.getString(0);
                    }
                    cursor.close();
                }
            } catch (SQLiteException sqle) {
                // Unknown title for the ringtone
            }
        }
        mHandler.sendMessage(mHandler.obtainMessage(msg, summary));
    }

    private void lookupRingtoneNames() {
        new Thread(mRingtoneLookupRunnable).start();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mVibrateWhenRinging) {
            Settings.System.putInt(getContentResolver(), Settings.System.VIBRATE_WHEN_RINGING,
                    mVibrateWhenRinging.isChecked() ? 1 : 0);
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

        } else if (preference == mMusicFx) {
            // let the framework fire off the intent
            return false;
        } else if (preference == mDockAudioSettings) {
            int dockState = mDockIntent != null
                    ? mDockIntent.getIntExtra(Intent.EXTRA_DOCK_STATE, 0)
                    : Intent.EXTRA_DOCK_STATE_UNDOCKED;

            if (dockState == Intent.EXTRA_DOCK_STATE_UNDOCKED) {
                showDialog(DIALOG_NOT_DOCKED);
            } else {
                boolean isBluetooth = mDockIntent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) != null;

                if (isBluetooth) {
                    Intent i = new Intent(mDockIntent);
                    i.setAction(DockEventReceiver.ACTION_DOCK_SHOW_UI);
                    i.setClass(getActivity(), DockEventReceiver.class);
                    getActivity().sendBroadcast(i);
                } else {
                    PreferenceScreen ps = (PreferenceScreen)mDockAudioSettings;
                    Bundle extras = ps.getExtras();
                    extras.putBoolean("checked",
                            Settings.Global.getInt(getContentResolver(),
                                    Settings.Global.DOCK_AUDIO_MEDIA_ENABLED, 0) == 1);
                    super.onPreferenceTreeClick(ps, ps);
                }
            }
        } else if (preference == mDockSounds) {
            Settings.Global.putInt(getContentResolver(), Settings.Global.DOCK_SOUNDS_ENABLED,
                    mDockSounds.isChecked() ? 1 : 0);
        } else if (preference == mDockAudioMediaEnabled) {
            Settings.Global.putInt(getContentResolver(), Settings.Global.DOCK_AUDIO_MEDIA_ENABLED,
                    mDockAudioMediaEnabled.isChecked() ? 1 : 0);
        }
        return true;
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (KEY_EMERGENCY_TONE.equals(key)) {
            try {
                int value = Integer.parseInt((String) objValue);
                Settings.Global.putInt(getContentResolver(),
                        Settings.Global.EMERGENCY_TONE, value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist emergency tone setting", e);
            }
        }

        return true;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_sound;
    }

    private boolean needsDockSettings() {
        return getResources().getBoolean(R.bool.has_dock_settings);
    }

    private void initDockSettings() {
        ContentResolver resolver = getContentResolver();

        if (needsDockSettings()) {
            mDockSounds = (CheckBoxPreference) findPreference(KEY_DOCK_SOUNDS);
            mDockSounds.setPersistent(false);
            mDockSounds.setChecked(Settings.Global.getInt(resolver,
                    Settings.Global.DOCK_SOUNDS_ENABLED, 0) != 0);
            mDockAudioSettings = findPreference(KEY_DOCK_AUDIO_SETTINGS);
            mDockAudioSettings.setEnabled(false);
        } else {
            getPreferenceScreen().removePreference(findPreference(KEY_DOCK_CATEGORY));
            getPreferenceScreen().removePreference(findPreference(KEY_DOCK_AUDIO_SETTINGS));
            getPreferenceScreen().removePreference(findPreference(KEY_DOCK_SOUNDS));
            Settings.Global.putInt(resolver, Settings.Global.DOCK_AUDIO_MEDIA_ENABLED, 1);
        }
    }

    private void handleDockChange(Intent intent) {
        if (mDockAudioSettings != null) {
            int dockState = intent.getIntExtra(Intent.EXTRA_DOCK_STATE, 0);

            boolean isBluetooth =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) != null;

            mDockIntent = intent;

            if (dockState != Intent.EXTRA_DOCK_STATE_UNDOCKED) {
                // remove undocked dialog if currently showing.
                try {
                    removeDialog(DIALOG_NOT_DOCKED);
                } catch (IllegalArgumentException iae) {
                    // Maybe it was already dismissed
                }

                if (isBluetooth) {
                    mDockAudioSettings.setEnabled(true);
                } else {
                    if (dockState == Intent.EXTRA_DOCK_STATE_LE_DESK) {
                        ContentResolver resolver = getContentResolver();
                        mDockAudioSettings.setEnabled(true);
                        if (Settings.Global.getInt(resolver,
                                Settings.Global.DOCK_AUDIO_MEDIA_ENABLED, -1) == -1) {
                            Settings.Global.putInt(resolver,
                                    Settings.Global.DOCK_AUDIO_MEDIA_ENABLED, 0);
                        }
                        mDockAudioMediaEnabled =
                                (CheckBoxPreference) findPreference(KEY_DOCK_AUDIO_MEDIA_ENABLED);
                        mDockAudioMediaEnabled.setPersistent(false);
                        mDockAudioMediaEnabled.setChecked(
                                Settings.Global.getInt(resolver,
                                        Settings.Global.DOCK_AUDIO_MEDIA_ENABLED, 0) != 0);
                    } else {
                        mDockAudioSettings.setEnabled(false);
                    }
                }
            } else {
                mDockAudioSettings.setEnabled(false);
            }
        }
    }

    @Override
    public Dialog onCreateDialog(int id) {
        if (id == DIALOG_NOT_DOCKED) {
            return createUndockedMessage();
        }
        return null;
    }

    private Dialog createUndockedMessage() {
        final AlertDialog.Builder ab = new AlertDialog.Builder(getActivity());
        ab.setTitle(R.string.dock_not_found_title);
        ab.setMessage(R.string.dock_not_found_text);
        ab.setPositiveButton(android.R.string.ok, null);
        return ab.create();
    }
}

