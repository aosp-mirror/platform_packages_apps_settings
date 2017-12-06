/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static android.app.admin.DevicePolicyManager.KEYGUARD_DISABLE_SECURE_NOTIFICATIONS;
import static android.app.admin.DevicePolicyManager.KEYGUARD_DISABLE_UNREDACTED_NOTIFICATIONS;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

import java.util.ArrayList;

public class LockScreenNotificationPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener,
        LifecycleObserver, OnResume, OnPause {

    private static final String TAG = "LockScreenNotifPref";

    private final String mSettingKey;
    private final String mWorkSettingCategoryKey;
    private final String mWorkSettingKey;

    private RestrictedDropDownPreference mLockscreen;
    private RestrictedDropDownPreference mLockscreenProfile;

    private final int mProfileChallengeUserId;
    private final boolean mSecure;
    private final boolean mSecureProfile;

    private SettingObserver mSettingObserver;
    private int mLockscreenSelectedValue;
    private int mLockscreenSelectedValueProfile;

    public LockScreenNotificationPreferenceController(Context context) {
        this(context, null, null, null);
    }

    public LockScreenNotificationPreferenceController(Context context,
            String settingKey, String workSettingCategoryKey, String workSettingKey) {
        super(context);
        mSettingKey = settingKey;
        mWorkSettingCategoryKey = workSettingCategoryKey;
        mWorkSettingKey = workSettingKey;

        mProfileChallengeUserId = Utils.getManagedProfileId(
                UserManager.get(context), UserHandle.myUserId());
        final LockPatternUtils utils = new LockPatternUtils(context);
        mSecure = utils.isSecure(UserHandle.myUserId());
        mSecureProfile = (mProfileChallengeUserId != UserHandle.USER_NULL)
                && (utils.isSecure(mProfileChallengeUserId)
                || (!utils.isSeparateProfileChallengeEnabled(mProfileChallengeUserId) && mSecure));
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mLockscreen =
                (RestrictedDropDownPreference) screen.findPreference(mSettingKey);
        if (mLockscreen == null) {
            Log.i(TAG, "Preference not found: " + mSettingKey);
            return;
        }
        if (mProfileChallengeUserId != UserHandle.USER_NULL) {
            mLockscreenProfile = (RestrictedDropDownPreference) screen.findPreference(
                    mWorkSettingKey);
        } else {
            removePreference(screen, mWorkSettingKey);
            removePreference(screen, mWorkSettingCategoryKey);
        }
        mSettingObserver = new SettingObserver();
        initLockScreenNotificationPrefDisplay();
        initLockscreenNotificationPrefForProfile();
    }

    private void initLockScreenNotificationPrefDisplay() {
        ArrayList<CharSequence> entries = new ArrayList<>();
        ArrayList<CharSequence> values = new ArrayList<>();

        String summaryShowEntry =
                mContext.getString(R.string.lock_screen_notifications_summary_show);
        String summaryShowEntryValue =
                Integer.toString(R.string.lock_screen_notifications_summary_show);
        entries.add(summaryShowEntry);
        values.add(summaryShowEntryValue);
        setRestrictedIfNotificationFeaturesDisabled(summaryShowEntry, summaryShowEntryValue,
                KEYGUARD_DISABLE_SECURE_NOTIFICATIONS | KEYGUARD_DISABLE_UNREDACTED_NOTIFICATIONS);

        if (mSecure) {
            String summaryHideEntry =
                    mContext.getString(R.string.lock_screen_notifications_summary_hide);
            String summaryHideEntryValue =
                    Integer.toString(R.string.lock_screen_notifications_summary_hide);
            entries.add(summaryHideEntry);
            values.add(summaryHideEntryValue);
            setRestrictedIfNotificationFeaturesDisabled(summaryHideEntry, summaryHideEntryValue,
                    KEYGUARD_DISABLE_SECURE_NOTIFICATIONS);
        }

        entries.add(mContext.getString(R.string.lock_screen_notifications_summary_disable));
        values.add(Integer.toString(R.string.lock_screen_notifications_summary_disable));


        mLockscreen.setEntries(entries.toArray(new CharSequence[entries.size()]));
        mLockscreen.setEntryValues(values.toArray(new CharSequence[values.size()]));
        updateLockscreenNotifications();

        if (mLockscreen.getEntries().length > 1) {
            mLockscreen.setOnPreferenceChangeListener(this);
        } else {
            // There is one or less option for the user, disable the drop down.
            mLockscreen.setEnabled(false);
        }
    }

    private void initLockscreenNotificationPrefForProfile() {
        if (mLockscreenProfile == null) {
            Log.i(TAG, "Preference not found: " + mWorkSettingKey);
            return;
        }
        ArrayList<CharSequence> entries = new ArrayList<>();
        ArrayList<CharSequence> values = new ArrayList<>();

        String summaryShowEntry = mContext.getString(
                R.string.lock_screen_notifications_summary_show_profile);
        String summaryShowEntryValue = Integer.toString(
                R.string.lock_screen_notifications_summary_show_profile);
        entries.add(summaryShowEntry);
        values.add(summaryShowEntryValue);
        setRestrictedIfNotificationFeaturesDisabled(summaryShowEntry, summaryShowEntryValue,
                KEYGUARD_DISABLE_SECURE_NOTIFICATIONS | KEYGUARD_DISABLE_UNREDACTED_NOTIFICATIONS);

        if (mSecureProfile) {
            String summaryHideEntry = mContext.getString(
                    R.string.lock_screen_notifications_summary_hide_profile);
            String summaryHideEntryValue = Integer.toString(
                    R.string.lock_screen_notifications_summary_hide_profile);
            entries.add(summaryHideEntry);
            values.add(summaryHideEntryValue);
            setRestrictedIfNotificationFeaturesDisabled(summaryHideEntry, summaryHideEntryValue,
                    KEYGUARD_DISABLE_SECURE_NOTIFICATIONS);
        }

        entries.add(mContext.getString(R.string.lock_screen_notifications_summary_disable_profile));
        values.add(Integer.toString(R.string.lock_screen_notifications_summary_disable_profile));

        mLockscreenProfile.setOnPreClickListener(
                (Preference p) -> Utils.startQuietModeDialogIfNecessary(mContext,
                        UserManager.get(mContext), mProfileChallengeUserId)
        );

        mLockscreenProfile.setEntries(entries.toArray(new CharSequence[entries.size()]));
        mLockscreenProfile.setEntryValues(values.toArray(new CharSequence[values.size()]));
        updateLockscreenNotificationsForProfile();
        if (mLockscreenProfile.getEntries().length > 1) {
            mLockscreenProfile.setOnPreferenceChangeListener(this);
        } else {
            // There is one or less option for the user, disable the drop down.
            mLockscreenProfile.setEnabled(false);
        }
    }

    @Override
    public String getPreferenceKey() {
        return null;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void onResume() {
        if (mSettingObserver != null) {
            mSettingObserver.register(mContext.getContentResolver(), true /* register */);
        }
    }

    @Override
    public void onPause() {
        if (mSettingObserver != null) {
            mSettingObserver.register(mContext.getContentResolver(), false /* register */);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final String key = preference.getKey();
        if (TextUtils.equals(mWorkSettingKey, key)) {
                final int val = Integer.parseInt((String) newValue);
                if (val == mLockscreenSelectedValueProfile) {
                    return false;
                }
                final boolean enabled =
                        val != R.string.lock_screen_notifications_summary_disable_profile;
                final boolean show =
                        val == R.string.lock_screen_notifications_summary_show_profile;
                Settings.Secure.putIntForUser(mContext.getContentResolver(),
                        Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS,
                        show ? 1 : 0, mProfileChallengeUserId);
                Settings.Secure.putIntForUser(mContext.getContentResolver(),
                        Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS,
                        enabled ? 1 : 0, mProfileChallengeUserId);
                mLockscreenSelectedValueProfile = val;
                return true;
        } else if (TextUtils.equals(mSettingKey, key)) {
                final int val = Integer.parseInt((String) newValue);
                if (val == mLockscreenSelectedValue) {
                    return false;
                }
                final boolean enabled =
                        val != R.string.lock_screen_notifications_summary_disable;
                final boolean show = val == R.string.lock_screen_notifications_summary_show;
                Settings.Secure.putInt(mContext.getContentResolver(),
                        Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, show ? 1 : 0);
                Settings.Secure.putInt(mContext.getContentResolver(),
                        Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, enabled ? 1 : 0);
                mLockscreenSelectedValue = val;
                return true;
        }
        return false;
    }

    private void setRestrictedIfNotificationFeaturesDisabled(CharSequence entry,
            CharSequence entryValue, int keyguardNotificationFeatures) {
        RestrictedLockUtils.EnforcedAdmin admin =
                RestrictedLockUtils.checkIfKeyguardFeaturesDisabled(
                        mContext, keyguardNotificationFeatures, UserHandle.myUserId());
        if (admin != null && mLockscreen != null) {
            RestrictedDropDownPreference.RestrictedItem item =
                    new RestrictedDropDownPreference.RestrictedItem(entry, entryValue, admin);
            mLockscreen.addRestrictedItem(item);
        }
        if (mProfileChallengeUserId != UserHandle.USER_NULL) {
            RestrictedLockUtils.EnforcedAdmin profileAdmin =
                    RestrictedLockUtils.checkIfKeyguardFeaturesDisabled(
                            mContext, keyguardNotificationFeatures, mProfileChallengeUserId);
            if (profileAdmin != null && mLockscreenProfile != null) {
                RestrictedDropDownPreference.RestrictedItem item =
                        new RestrictedDropDownPreference.RestrictedItem(
                                entry, entryValue, profileAdmin);
                mLockscreenProfile.addRestrictedItem(item);
            }
        }
    }

    public int getSummaryResource() {
        final boolean enabled = getLockscreenNotificationsEnabled(UserHandle.myUserId());
        final boolean allowPrivate = !mSecure
            || getLockscreenAllowPrivateNotifications(UserHandle.myUserId());
        return !enabled ? R.string.lock_screen_notifications_summary_disable :
            allowPrivate ? R.string.lock_screen_notifications_summary_show :
                R.string.lock_screen_notifications_summary_hide;
    }

    private void updateLockscreenNotifications() {
        if (mLockscreen == null) {
            return;
        }
        mLockscreenSelectedValue = getSummaryResource();
        mLockscreen.setSummary("%s");
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
        mLockscreenProfile.setSummary("%s");
        mLockscreenSelectedValueProfile = !enabled
                ? R.string.lock_screen_notifications_summary_disable_profile
                : (allowPrivate ? R.string.lock_screen_notifications_summary_show_profile
                        : R.string.lock_screen_notifications_summary_hide_profile);
        mLockscreenProfile.setValue(Integer.toString(mLockscreenSelectedValueProfile));
    }

    private boolean getLockscreenNotificationsEnabled(int userId) {
        return Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, 0, userId) != 0;
    }

    private boolean getLockscreenAllowPrivateNotifications(int userId) {
        return Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, 0, userId) != 0;
    }

    class SettingObserver extends ContentObserver {

        private final Uri LOCK_SCREEN_PRIVATE_URI =
                Settings.Secure.getUriFor(Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS);
        private final Uri LOCK_SCREEN_SHOW_URI =
                Settings.Secure.getUriFor(Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS);

        public SettingObserver() {
            super(new Handler());
        }

        public void register(ContentResolver cr, boolean register) {
            if (register) {
                cr.registerContentObserver(LOCK_SCREEN_PRIVATE_URI, false, this);
                cr.registerContentObserver(LOCK_SCREEN_SHOW_URI, false, this);
            } else {
                cr.unregisterContentObserver(this);
            }
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (LOCK_SCREEN_PRIVATE_URI.equals(uri) || LOCK_SCREEN_SHOW_URI.equals(uri)) {
                updateLockscreenNotifications();
                if (mProfileChallengeUserId != UserHandle.USER_NULL) {
                    updateLockscreenNotificationsForProfile();
                }
            }
        }
    }
}
