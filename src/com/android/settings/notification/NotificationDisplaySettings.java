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
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.TwoStatePreference;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class NotificationDisplaySettings extends SettingsPreferenceFragment {
    private static final String TAG = "NotificationDisplaySettings";

    private static final String KEY_NOTIFICATION_PULSE = "notification_pulse";
    private static final String KEY_LOCK_SCREEN_NOTIFICATIONS = "lock_screen_notifications";
    private static final String KEY_ZEN_MODE_NOTIFICATIONS = "zen_mode_notifications";

    private final Handler mHandler = new Handler();
    private final SettingsObserver mSettingsObserver = new SettingsObserver();

    private TwoStatePreference mNotificationPulse;
    private DropDownPreference mLockscreen;
    private DropDownPreference mZenModeNotifications;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.notification_display_settings);

        final PreferenceScreen root = getPreferenceScreen();
        initPulse(root);
        initLockscreenNotifications(root);
        initZenModeNotifications(root);
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

    // === Pulse notification light ===

    private void initPulse(PreferenceScreen parent) {
        mNotificationPulse = (TwoStatePreference) parent.findPreference(KEY_NOTIFICATION_PULSE);
        if (mNotificationPulse == null) return;
        if (!getResources()
                .getBoolean(com.android.internal.R.bool.config_intrusiveNotificationLed)) {
            parent.removePreference(mNotificationPulse);
        } else {
            updatePulse();
            mNotificationPulse.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final boolean val = (Boolean)newValue;
                    return Settings.System.putInt(getContentResolver(),
                            Settings.System.NOTIFICATION_LIGHT_PULSE,
                            val ? 1 : 0);
                }
            });
        }
    }

    private void updatePulse() {
        if (mNotificationPulse == null) return;
        try {
            mNotificationPulse.setChecked(Settings.System.getInt(getContentResolver(),
                    Settings.System.NOTIFICATION_LIGHT_PULSE) == 1);
        } catch (Settings.SettingNotFoundException snfe) {
            Log.e(TAG, Settings.System.NOTIFICATION_LIGHT_PULSE + " not found");
        }
    }

    // === Lockscreen (public / private) notifications ===

    private void initLockscreenNotifications(PreferenceScreen parent) {
        mLockscreen = (DropDownPreference) parent.findPreference(KEY_LOCK_SCREEN_NOTIFICATIONS);
        if (mLockscreen == null) return;
        mLockscreen.addItem(R.string.lock_screen_notifications_summary_show,
                R.string.lock_screen_notifications_summary_show);
        mLockscreen.addItem(R.string.lock_screen_notifications_summary_hide,
                R.string.lock_screen_notifications_summary_hide);
        mLockscreen.addItem(R.string.lock_screen_notifications_summary_disable,
                R.string.lock_screen_notifications_summary_disable);
        updateLockscreenNotifications();
        mLockscreen.setCallback(new DropDownPreference.Callback() {
            @Override
            public boolean onItemSelected(int pos, Object value) {
                final int val = (Integer) value;
                final boolean enabled = val != R.string.lock_screen_notifications_summary_disable;
                final boolean show = val == R.string.lock_screen_notifications_summary_show;
                Settings.Secure.putInt(getContentResolver(),
                        Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, show ? 1 : 0);
                Settings.Global.putInt(getContentResolver(),
                        Settings.Global.LOCK_SCREEN_SHOW_NOTIFICATIONS, enabled ? 1 : 0);
                return true;
            }
        });
    }

    private void updateLockscreenNotifications() {
        if (mLockscreen == null) return;
        final boolean allowPrivate = getLockscreenAllowPrivateNotifications();
        final boolean enabled = getLockscreenNotificationsEnabled();
        final int val = !enabled ? R.string.lock_screen_notifications_summary_disable :
                allowPrivate ? R.string.lock_screen_notifications_summary_show :
                R.string.lock_screen_notifications_summary_hide;
        mLockscreen.setSelectedValue(val);
    }

    private boolean getLockscreenNotificationsEnabled() {
        return Settings.Global.getInt(getContentResolver(),
                Settings.Global.LOCK_SCREEN_SHOW_NOTIFICATIONS, 0) != 0;
    }

    private boolean getLockscreenAllowPrivateNotifications() {
        return Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, 0) != 0;
    }

    // === Zen mode notifications ===

    private void initZenModeNotifications(PreferenceScreen parent) {
        mZenModeNotifications = (DropDownPreference)
                parent.findPreference(KEY_ZEN_MODE_NOTIFICATIONS);
        if (mZenModeNotifications == null) return;
        mZenModeNotifications.addItem(R.string.zen_mode_notifications_summary_hide, 0);
        mZenModeNotifications.addItem(R.string.zen_mode_notifications_summary_show, 1);
        updateZenModeNotifications();
        mZenModeNotifications.setCallback(new DropDownPreference.Callback() {
            @Override
            public boolean onItemSelected(int pos, Object value) {
                final int val = (Integer) value;
                Settings.Secure.putInt(getContentResolver(),
                        Settings.Secure.DISPLAY_INTERCEPTED_NOTIFICATIONS, val);
                return true;
            }
        });
    }

    private void updateZenModeNotifications() {
        if (mZenModeNotifications == null) return;
        mZenModeNotifications.setSelectedValue(Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.DISPLAY_INTERCEPTED_NOTIFICATIONS, 0));
    }

    // === Callbacks ===

    private final class SettingsObserver extends ContentObserver {
        private final Uri NOTIFICATION_LIGHT_PULSE_URI =
                Settings.System.getUriFor(Settings.System.NOTIFICATION_LIGHT_PULSE);
        private final Uri LOCK_SCREEN_PRIVATE_URI =
                Settings.Secure.getUriFor(Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS);
        private final Uri LOCK_SCREEN_SHOW_URI =
                Settings.Global.getUriFor(Settings.Global.LOCK_SCREEN_SHOW_NOTIFICATIONS);
        private final Uri DISPLAY_INTERCEPTED_NOTIFICATIONS_URI =
                Settings.Secure.getUriFor(Settings.Secure.DISPLAY_INTERCEPTED_NOTIFICATIONS);

        public SettingsObserver() {
            super(mHandler);
        }

        public void register(boolean register) {
            final ContentResolver cr = getContentResolver();
            if (register) {
                cr.registerContentObserver(NOTIFICATION_LIGHT_PULSE_URI, false, this);
                cr.registerContentObserver(LOCK_SCREEN_PRIVATE_URI, false, this);
                cr.registerContentObserver(LOCK_SCREEN_SHOW_URI, false, this);
                cr.registerContentObserver(DISPLAY_INTERCEPTED_NOTIFICATIONS_URI, false, this);
            } else {
                cr.unregisterContentObserver(this);
            }
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (NOTIFICATION_LIGHT_PULSE_URI.equals(uri)) {
                updatePulse();
            }
            if (LOCK_SCREEN_PRIVATE_URI.equals(uri) || LOCK_SCREEN_SHOW_URI.equals(uri)) {
                updateLockscreenNotifications();
            }
            if (DISPLAY_INTERCEPTED_NOTIFICATIONS_URI.equals(uri)) {
                updateZenModeNotifications();
            }
        }
    }
}
