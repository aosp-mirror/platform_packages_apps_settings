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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.TwoStatePreference;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.SoundSettings;

public class NotificationSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, OnPreferenceClickListener {
    private static final String TAG = "NotificationSettings";

    private static final String KEY_NOTIFICATION_SOUND = "notification_sound";
    private static final String KEY_NOTIFICATION_PULSE = "notification_pulse";
    private static final String KEY_HEADS_UP = "heads_up";
    private static final String KEY_LOCK_SCREEN_NOTIFICATIONS = "toggle_lock_screen_notifications";
    private static final String KEY_NOTIFICATION_ACCESS = "manage_notification_access";

    private static final String KEY_TWEAKS_CATEGORY = "category_tweaks"; // power toys, eng only

    private static final int MSG_UPDATE_SOUND_SUMMARY = 2;

    private Context mContext;
    private PackageManager mPM;

    private Preference mNotificationSoundPreference;
    private Preference mNotificationAccess;
    private TwoStatePreference mLockscreenNotifications;
    private TwoStatePreference mHeadsUp;
    private TwoStatePreference mNotificationPulse;

    private final Runnable mRingtoneLookupRunnable = new Runnable() {
        @Override
        public void run() {
            if (mNotificationSoundPreference != null) {
                final CharSequence summary = SoundSettings.updateRingtoneName(
                        mContext, RingtoneManager.TYPE_NOTIFICATION);
                if (summary != null) {
                    mHandler.sendMessage(
                            mHandler.obtainMessage(MSG_UPDATE_SOUND_SUMMARY, summary));
                }
            }
        }
    };

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_SOUND_SUMMARY:
                    mNotificationSoundPreference.setSummary((CharSequence) msg.obj);
                    break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        final ContentResolver resolver = mContext.getContentResolver();

        mPM = mContext.getPackageManager();

        addPreferencesFromResource(R.xml.notification_settings);

        final PreferenceScreen root = getPreferenceScreen();

        PreferenceGroup tweaksCategory = (PreferenceGroup)
                root.findPreference(KEY_TWEAKS_CATEGORY);

        if (tweaksCategory != null
                && !(Build.TYPE.equals("eng") || Build.TYPE.equals("userdebug"))) {
            root.removePreference(tweaksCategory);
            tweaksCategory = null;
        }

        mNotificationSoundPreference = findPreference(KEY_NOTIFICATION_SOUND);

        mNotificationAccess = findPreference(KEY_NOTIFICATION_ACCESS);
        refreshNotificationListeners();

        mLockscreenNotifications
                = (TwoStatePreference) root.findPreference(KEY_LOCK_SCREEN_NOTIFICATIONS);
        if (mLockscreenNotifications != null) {
            if (!getDeviceLockscreenNotificationsEnabled()) {
                root.removePreference(mLockscreenNotifications);
            } else {
                mLockscreenNotifications.setChecked(getLockscreenAllowPrivateNotifications());
            }
        }

        mHeadsUp = (TwoStatePreference) findPreference(KEY_HEADS_UP);
        if (mHeadsUp != null) {
            updateHeadsUpMode(resolver);
            mHeadsUp.setOnPreferenceChangeListener(this);
            resolver.registerContentObserver(
                    Settings.Global.getUriFor(Settings.Global.HEADS_UP_NOTIFICATIONS_ENABLED),
                    false, new ContentObserver(mHandler) {
                @Override
                public void onChange(boolean selfChange) {
                    updateHeadsUpMode(resolver);
                }
            });
        }
        mNotificationPulse = (TwoStatePreference) findPreference(KEY_NOTIFICATION_PULSE);

        if (mNotificationPulse != null
                && getResources().getBoolean(
                com.android.internal.R.bool.config_intrusiveNotificationLed) == false) {
            getPreferenceScreen().removePreference(mNotificationPulse);
        } else {
            try {
                mNotificationPulse.setChecked(Settings.System.getInt(resolver,
                        Settings.System.NOTIFICATION_LIGHT_PULSE) == 1);
                mNotificationPulse.setOnPreferenceChangeListener(this);
            } catch (Settings.SettingNotFoundException snfe) {
                Log.e(TAG, Settings.System.NOTIFICATION_LIGHT_PULSE + " not found");
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        refreshNotificationListeners();
        lookupRingtoneNames();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        final String key = preference.getKey();

        if (KEY_LOCK_SCREEN_NOTIFICATIONS.equals(key)) {
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS,
                    mLockscreenNotifications.isChecked() ? 1 : 0);
        } else if (KEY_HEADS_UP.equals(key)) {
            setHeadsUpMode(getContentResolver(), mHeadsUp.isChecked());
        } else if (KEY_NOTIFICATION_PULSE.equals(key)) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.NOTIFICATION_LIGHT_PULSE,
                    mNotificationPulse.isChecked() ? 1 : 0);
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        return false;
    }

    // === Heads-up notifications ===

    private void updateHeadsUpMode(ContentResolver resolver) {
        mHeadsUp.setChecked(Settings.Global.HEADS_UP_ON == Settings.Global.getInt(resolver,
                Settings.Global.HEADS_UP_NOTIFICATIONS_ENABLED, Settings.Global.HEADS_UP_OFF));
    }

    private void setHeadsUpMode(ContentResolver resolver, boolean value) {
        Settings.Global.putInt(resolver, Settings.Global.HEADS_UP_NOTIFICATIONS_ENABLED,
                value ? Settings.Global.HEADS_UP_ON : Settings.Global.HEADS_UP_OFF);
    }

    // === Lockscreen (public / private) notifications ===

    private boolean getDeviceLockscreenNotificationsEnabled() {
        return 0 != Settings.Global.getInt(getContentResolver(),
                Settings.Global.LOCK_SCREEN_SHOW_NOTIFICATIONS, 0);
    }

    private boolean getLockscreenAllowPrivateNotifications() {
        return 0 != Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, 0);
    }

    // === Notification listeners ===

    private int getNumEnabledNotificationListeners() {
        final String flat = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_NOTIFICATION_LISTENERS);
        if (flat == null || "".equals(flat)) return 0;
        final String[] components = flat.split(":");
        return components.length;
    }

    private void refreshNotificationListeners() {
        if (mNotificationAccess != null) {
            final int total = NotificationAccessSettings.getListenersCount(mPM);
            if (total == 0) {
                getPreferenceScreen().removePreference(mNotificationAccess);
            } else {
                final int n = getNumEnabledNotificationListeners();
                if (n == 0) {
                    mNotificationAccess.setSummary(getResources().getString(
                            R.string.manage_notification_access_summary_zero));
                } else {
                    mNotificationAccess.setSummary(String.format(getResources().getQuantityString(
                            R.plurals.manage_notification_access_summary_nonzero,
                            n, n)));
                }
            }
        }
    }

    // === Ringtone ===

    private void lookupRingtoneNames() {
        new Thread(mRingtoneLookupRunnable).start();
    }
}
