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

import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.RestrictedListPreference;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;
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

    private RestrictedListPreference mLockscreen;
    private RestrictedListPreference mLockscreenProfile;

    private final int mProfileUserId;
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

        mProfileUserId = Utils.getManagedProfileId(UserManager.get(context), UserHandle.myUserId());
        final LockPatternUtils utils = FeatureFactory.getFactory(context)
                .getSecurityFeatureProvider()
                .getLockPatternUtils(context);
        mSecure = utils.isSecure(UserHandle.myUserId());
        mSecureProfile = (mProfileUserId != UserHandle.USER_NULL) && utils.isSecure(mProfileUserId);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mLockscreen = screen.findPreference(mSettingKey);
        if (mLockscreen == null) {
            Log.i(TAG, "Preference not found: " + mSettingKey);
            return;
        }
        if (mProfileUserId != UserHandle.USER_NULL) {
            mLockscreenProfile = screen.findPreference(mWorkSettingKey);
            mLockscreenProfile.setRequiresActiveUnlockedProfile(true);
            mLockscreenProfile.setProfileUserId(mProfileUserId);
        } else {
            setVisible(screen, mWorkSettingKey, false /* visible */);
            setVisible(screen, mWorkSettingCategoryKey, false /* visible */);
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
            final boolean show = val == R.string.lock_screen_notifications_summary_show_profile;
            Settings.Secure.putIntForUser(mContext.getContentResolver(),
                    Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS,
                    show ? 1 : 0, mProfileUserId);
            mLockscreenSelectedValueProfile = val;
            return true;
        } else if (TextUtils.equals(mSettingKey, key)) {
            final int val = Integer.parseInt((String) newValue);
            if (val == mLockscreenSelectedValue) {
                return false;
            }
            final boolean enabled = val != R.string.lock_screen_notifications_summary_disable;
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
                RestrictedLockUtilsInternal.checkIfKeyguardFeaturesDisabled(
                        mContext, keyguardNotificationFeatures, UserHandle.myUserId());
        if (admin != null && mLockscreen != null) {
            RestrictedListPreference.RestrictedItem item =
                    new RestrictedListPreference.RestrictedItem(entry, entryValue, admin);
            mLockscreen.addRestrictedItem(item);
        }
        if (mProfileUserId != UserHandle.USER_NULL) {
            RestrictedLockUtils.EnforcedAdmin profileAdmin =
                    RestrictedLockUtilsInternal.checkIfKeyguardFeaturesDisabled(
                            mContext, keyguardNotificationFeatures, mProfileUserId);
            if (profileAdmin != null && mLockscreenProfile != null) {
                RestrictedListPreference.RestrictedItem item =
                        new RestrictedListPreference.RestrictedItem(
                                entry, entryValue, profileAdmin);
                mLockscreenProfile.addRestrictedItem(item);
            }
        }
    }

    public static int getSummaryResource(Context context) {
        final boolean enabled = getLockscreenNotificationsEnabled(context);
        final boolean secure = FeatureFactory.getFactory(context)
                .getSecurityFeatureProvider()
                .getLockPatternUtils(context)
                .isSecure(UserHandle.myUserId());
        final boolean allowPrivate = !secure
            || getAllowPrivateNotifications(context, UserHandle.myUserId());
        return !enabled ? R.string.lock_screen_notifications_summary_disable :
            allowPrivate ? R.string.lock_screen_notifications_summary_show :
                R.string.lock_screen_notifications_summary_hide;
    }

    private void updateLockscreenNotifications() {
        if (mLockscreen == null) {
            return;
        }
        mLockscreenSelectedValue = getSummaryResource(mContext);
        mLockscreen.setSummary("%s");
        mLockscreen.setValue(Integer.toString(mLockscreenSelectedValue));
    }

    private boolean adminAllowsUnredactedNotifications(int userId) {
        final int dpmFlags = mContext.getSystemService(DevicePolicyManager.class)
                .getKeyguardDisabledFeatures(null/* admin */, userId);
        return (dpmFlags & KEYGUARD_DISABLE_UNREDACTED_NOTIFICATIONS) == 0;
    }

    private void updateLockscreenNotificationsForProfile() {
        if (mProfileUserId == UserHandle.USER_NULL) {
            return;
        }
        if (mLockscreenProfile == null) {
            return;
        }
        final boolean allowPrivate = adminAllowsUnredactedNotifications(mProfileUserId) &&
                (!mSecureProfile || getAllowPrivateNotifications(mContext, mProfileUserId));
        mLockscreenProfile.setSummary("%s");
        mLockscreenSelectedValueProfile = allowPrivate
                        ? R.string.lock_screen_notifications_summary_show_profile
                        : R.string.lock_screen_notifications_summary_hide_profile;
        mLockscreenProfile.setValue(Integer.toString(mLockscreenSelectedValueProfile));
    }

    private static boolean getLockscreenNotificationsEnabled(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, 0) != 0;
    }

    private static boolean getAllowPrivateNotifications(Context context, int userId) {
        return Settings.Secure.getIntForUser(context.getContentResolver(),
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
                if (mProfileUserId != UserHandle.USER_NULL) {
                    updateLockscreenNotificationsForProfile();
                }
            }
        }
    }
}
