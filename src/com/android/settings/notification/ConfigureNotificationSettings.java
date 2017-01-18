/**
 * Copyright (C) 2015 The Android Open Source Project
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

import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.TwoStatePreference;
import android.util.Log;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.RestrictedListPreference.RestrictedItem;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settingslib.RestrictedLockUtils;

import java.util.ArrayList;

import static android.app.admin.DevicePolicyManager.KEYGUARD_DISABLE_SECURE_NOTIFICATIONS;
import static android.app.admin.DevicePolicyManager.KEYGUARD_DISABLE_UNREDACTED_NOTIFICATIONS;
import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

public class ConfigureNotificationSettings extends SettingsPreferenceFragment {
    private static final String TAG = "ConfigNotiSettings";

    private static final String KEY_NOTIFICATION_PULSE = "notification_pulse";
    private static final String KEY_LOCK_SCREEN_NOTIFICATIONS = "lock_screen_notifications";
    private static final String KEY_LOCK_SCREEN_PROFILE_NOTIFICATIONS =
            "lock_screen_notifications_profile";

    private final SettingsObserver mSettingsObserver = new SettingsObserver();

    private Context mContext;

    private TwoStatePreference mNotificationPulse;
    private RestrictedDropDownPreference mLockscreen;
    private RestrictedDropDownPreference mLockscreenProfile;
    private boolean mSecure;
    private boolean mSecureProfile;
    private int mLockscreenSelectedValue;
    private int mLockscreenSelectedValueProfile;
    private int mProfileChallengeUserId;

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.CONFIGURE_NOTIFICATION;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        mProfileChallengeUserId = Utils.getManagedProfileId(
                UserManager.get(mContext), UserHandle.myUserId());

        final LockPatternUtils utils = new LockPatternUtils(getActivity());
        final boolean isUnified =
                !utils.isSeparateProfileChallengeEnabled(mProfileChallengeUserId);

        mSecure = utils.isSecure(UserHandle.myUserId());
        mSecureProfile = (mProfileChallengeUserId != UserHandle.USER_NULL)
                && (utils.isSecure(mProfileChallengeUserId) || (isUnified && mSecure));

        addPreferencesFromResource(R.xml.configure_notification_settings);

        initPulse();
        initLockscreenNotifications();

        if (mProfileChallengeUserId != UserHandle.USER_NULL) {
            addPreferencesFromResource(R.xml.configure_notification_settings_profile);
            initLockscreenNotificationsForProfile();
        }

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

    private void initPulse() {
        mNotificationPulse =
                (TwoStatePreference) getPreferenceScreen().findPreference(KEY_NOTIFICATION_PULSE);
        if (mNotificationPulse == null) {
            Log.i(TAG, "Preference not found: " + KEY_NOTIFICATION_PULSE);
            return;
        }
        if (!getResources()
                .getBoolean(com.android.internal.R.bool.config_intrusiveNotificationLed)) {
            getPreferenceScreen().removePreference(mNotificationPulse);
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
        if (mNotificationPulse == null) {
            return;
        }
        try {
            mNotificationPulse.setChecked(Settings.System.getInt(getContentResolver(),
                    Settings.System.NOTIFICATION_LIGHT_PULSE) == 1);
        } catch (Settings.SettingNotFoundException snfe) {
            Log.e(TAG, Settings.System.NOTIFICATION_LIGHT_PULSE + " not found");
        }
    }

    private void initLockscreenNotifications() {
        mLockscreen = (RestrictedDropDownPreference) getPreferenceScreen().findPreference(
                KEY_LOCK_SCREEN_NOTIFICATIONS);
        if (mLockscreen == null) {
            Log.i(TAG, "Preference not found: " + KEY_LOCK_SCREEN_NOTIFICATIONS);
            return;
        }

        ArrayList<CharSequence> entries = new ArrayList<>();
        ArrayList<CharSequence> values = new ArrayList<>();
        entries.add(getString(R.string.lock_screen_notifications_summary_disable));
        values.add(Integer.toString(R.string.lock_screen_notifications_summary_disable));

        String summaryShowEntry = getString(R.string.lock_screen_notifications_summary_show);
        String summaryShowEntryValue = Integer.toString(
                R.string.lock_screen_notifications_summary_show);
        entries.add(summaryShowEntry);
        values.add(summaryShowEntryValue);
        setRestrictedIfNotificationFeaturesDisabled(summaryShowEntry, summaryShowEntryValue,
                KEYGUARD_DISABLE_SECURE_NOTIFICATIONS | KEYGUARD_DISABLE_UNREDACTED_NOTIFICATIONS);

        if (mSecure) {
            String summaryHideEntry = getString(R.string.lock_screen_notifications_summary_hide);
            String summaryHideEntryValue = Integer.toString(
                    R.string.lock_screen_notifications_summary_hide);
            entries.add(summaryHideEntry);
            values.add(summaryHideEntryValue);
            setRestrictedIfNotificationFeaturesDisabled(summaryHideEntry, summaryHideEntryValue,
                    KEYGUARD_DISABLE_SECURE_NOTIFICATIONS);
        }

        mLockscreen.setEntries(entries.toArray(new CharSequence[entries.size()]));
        mLockscreen.setEntryValues(values.toArray(new CharSequence[values.size()]));
        updateLockscreenNotifications();
        if (mLockscreen.getEntries().length > 1) {
            mLockscreen.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final int val = Integer.parseInt((String) newValue);
                    if (val == mLockscreenSelectedValue) {
                        return false;
                    }
                    final boolean enabled =
                            val != R.string.lock_screen_notifications_summary_disable;
                    final boolean show = val == R.string.lock_screen_notifications_summary_show;
                    Settings.Secure.putInt(getContentResolver(),
                            Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, show ? 1 : 0);
                    Settings.Secure.putInt(getContentResolver(),
                            Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, enabled ? 1 : 0);
                    mLockscreenSelectedValue = val;
                    return true;
                }
            });
        } else {
            // There is one or less option for the user, disable the drop down.
            mLockscreen.setEnabled(false);
        }
    }

    // === Lockscreen (public / private) notifications ===
    private void initLockscreenNotificationsForProfile() {
        mLockscreenProfile = (RestrictedDropDownPreference) getPreferenceScreen()
                .findPreference(KEY_LOCK_SCREEN_PROFILE_NOTIFICATIONS);
        if (mLockscreenProfile == null) {
            Log.i(TAG, "Preference not found: " + KEY_LOCK_SCREEN_PROFILE_NOTIFICATIONS);
            return;
        }
        ArrayList<CharSequence> entries = new ArrayList<>();
        ArrayList<CharSequence> values = new ArrayList<>();
        entries.add(getString(R.string.lock_screen_notifications_summary_disable_profile));
        values.add(Integer.toString(R.string.lock_screen_notifications_summary_disable_profile));

        String summaryShowEntry = getString(
                R.string.lock_screen_notifications_summary_show_profile);
        String summaryShowEntryValue = Integer.toString(
                R.string.lock_screen_notifications_summary_show_profile);
        entries.add(summaryShowEntry);
        values.add(summaryShowEntryValue);
        setRestrictedIfNotificationFeaturesDisabled(summaryShowEntry, summaryShowEntryValue,
                KEYGUARD_DISABLE_SECURE_NOTIFICATIONS | KEYGUARD_DISABLE_UNREDACTED_NOTIFICATIONS);

        if (mSecureProfile) {
            String summaryHideEntry = getString(
                    R.string.lock_screen_notifications_summary_hide_profile);
            String summaryHideEntryValue = Integer.toString(
                    R.string.lock_screen_notifications_summary_hide_profile);
            entries.add(summaryHideEntry);
            values.add(summaryHideEntryValue);
            setRestrictedIfNotificationFeaturesDisabled(summaryHideEntry, summaryHideEntryValue,
                    KEYGUARD_DISABLE_SECURE_NOTIFICATIONS);
        }

        mLockscreenProfile.setOnPreClickListener(
                (Preference p) -> Utils.startQuietModeDialogIfNecessary(mContext,
                        UserManager.get(mContext),
                        mProfileChallengeUserId)
        );

        mLockscreenProfile.setEntries(entries.toArray(new CharSequence[entries.size()]));
        mLockscreenProfile.setEntryValues(values.toArray(new CharSequence[values.size()]));
        updateLockscreenNotificationsForProfile();
        if (mLockscreenProfile.getEntries().length > 1) {
            mLockscreenProfile.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final int val = Integer.parseInt((String) newValue);
                    if (val == mLockscreenSelectedValueProfile) {
                        return false;
                    }
                    final boolean enabled =
                            val != R.string.lock_screen_notifications_summary_disable_profile;
                    final boolean show =
                            val == R.string.lock_screen_notifications_summary_show_profile;
                    Settings.Secure.putIntForUser(getContentResolver(),
                            Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS,
                            show ? 1 : 0, mProfileChallengeUserId);
                    Settings.Secure.putIntForUser(getContentResolver(),
                            Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS,
                            enabled ? 1 : 0, mProfileChallengeUserId);
                    mLockscreenSelectedValueProfile = val;
                    return true;
                }
            });
        } else {
            // There is one or less option for the user, disable the drop down.
            mLockscreenProfile.setEnabled(false);
        }
    }

    private void setRestrictedIfNotificationFeaturesDisabled(CharSequence entry,
            CharSequence entryValue, int keyguardNotificationFeatures) {
        EnforcedAdmin admin = RestrictedLockUtils.checkIfKeyguardFeaturesDisabled(
                mContext, keyguardNotificationFeatures, UserHandle.myUserId());
        if (admin != null && mLockscreen != null) {
            RestrictedDropDownPreference.RestrictedItem item =
                    new RestrictedDropDownPreference.RestrictedItem(entry, entryValue, admin);
            mLockscreen.addRestrictedItem(item);
        }
        if (mProfileChallengeUserId != UserHandle.USER_NULL) {
            EnforcedAdmin profileAdmin = RestrictedLockUtils.checkIfKeyguardFeaturesDisabled(
                    mContext, keyguardNotificationFeatures, mProfileChallengeUserId);
            if (profileAdmin != null && mLockscreenProfile != null) {
                RestrictedDropDownPreference.RestrictedItem item =
                        new RestrictedDropDownPreference.RestrictedItem(
                                entry, entryValue, profileAdmin);
                mLockscreenProfile.addRestrictedItem(item);
            }
        }
    }

    private void updateLockscreenNotifications() {
        if (mLockscreen == null) {
            return;
        }
        final boolean enabled = getLockscreenNotificationsEnabled(UserHandle.myUserId());
        final boolean allowPrivate = !mSecure
                || getLockscreenAllowPrivateNotifications(UserHandle.myUserId());
        mLockscreenSelectedValue = !enabled ? R.string.lock_screen_notifications_summary_disable :
                allowPrivate ? R.string.lock_screen_notifications_summary_show :
                R.string.lock_screen_notifications_summary_hide;
        mLockscreen.setValue(Integer.toString(mLockscreenSelectedValue));
    }

    private void updateLockscreenNotificationsForProfile() {
        if (mProfileChallengeUserId == UserHandle.USER_NULL) {
            return;
        }
        if (mLockscreenProfile == null) {
            return;
        }
        final boolean enabled = getLockscreenNotificationsEnabled(mProfileChallengeUserId);
        final boolean allowPrivate = !mSecureProfile
                || getLockscreenAllowPrivateNotifications(mProfileChallengeUserId);
        mLockscreenSelectedValueProfile = !enabled
                ? R.string.lock_screen_notifications_summary_disable_profile
                        : (allowPrivate ? R.string.lock_screen_notifications_summary_show_profile
                                : R.string.lock_screen_notifications_summary_hide_profile);
        mLockscreenProfile.setValue(Integer.toString(mLockscreenSelectedValueProfile));
    }

    private boolean getLockscreenNotificationsEnabled(int userId) {
        return Settings.Secure.getIntForUser(getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, 0, userId) != 0;
    }

    private boolean getLockscreenAllowPrivateNotifications(int userId) {
        return Settings.Secure.getIntForUser(getContentResolver(),
                Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, 0, userId) != 0;
    }


    // === Callbacks ===

    private final class SettingsObserver extends ContentObserver {
        private final Uri NOTIFICATION_LIGHT_PULSE_URI =
                Settings.System.getUriFor(Settings.System.NOTIFICATION_LIGHT_PULSE);
        private final Uri LOCK_SCREEN_PRIVATE_URI =
                Settings.Secure.getUriFor(Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS);
        private final Uri LOCK_SCREEN_SHOW_URI =
                Settings.Secure.getUriFor(Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS);

        public SettingsObserver() {
            super(new Handler());
        }

        public void register(boolean register) {
            final ContentResolver cr = getContentResolver();
            if (register) {
                cr.registerContentObserver(NOTIFICATION_LIGHT_PULSE_URI, false, this);
                cr.registerContentObserver(LOCK_SCREEN_PRIVATE_URI, false, this);
                cr.registerContentObserver(LOCK_SCREEN_SHOW_URI, false, this);
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
                if (mProfileChallengeUserId != UserHandle.USER_NULL) {
                    updateLockscreenNotificationsForProfile();
                }
            }
        }
    }
}
