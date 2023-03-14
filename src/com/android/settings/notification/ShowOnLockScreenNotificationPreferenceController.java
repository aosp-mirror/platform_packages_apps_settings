/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.RestrictedListPreference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.core.AbstractPreferenceController;

import com.google.common.annotations.VisibleForTesting;

import java.util.ArrayList;

public class ShowOnLockScreenNotificationPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String TAG = "LockScreenNotifPref";

    private final String mSettingKey;
    private DevicePolicyManager mDpm;

    public ShowOnLockScreenNotificationPreferenceController(Context context, String settingKey) {
        super(context);
        mSettingKey = settingKey;
        mDpm = context.getSystemService(DevicePolicyManager.class);
    }

    @VisibleForTesting
    void setDpm(DevicePolicyManager dpm) {
        mDpm = dpm;
    }

    @Override
    public String getPreferenceKey() {
        return mSettingKey;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        RestrictedListPreference pref = screen.findPreference(mSettingKey);
        pref.clearRestrictedItems();
        ArrayList<CharSequence> entries = new ArrayList<>();
        ArrayList<CharSequence> values = new ArrayList<>();

        String showAllEntry =
                mContext.getString(R.string.lock_screen_notifs_show_all);
        String showAllEntryValue =
                Integer.toString(R.string.lock_screen_notifs_show_all);
        entries.add(showAllEntry);
        values.add(showAllEntryValue);
        setRestrictedIfNotificationFeaturesDisabled(pref, showAllEntry, showAllEntryValue,
                KEYGUARD_DISABLE_SECURE_NOTIFICATIONS);

        String alertingEntry = mContext.getString(R.string.lock_screen_notifs_show_alerting);
        String alertingEntryValue = Integer.toString(R.string.lock_screen_notifs_show_alerting);
        entries.add(alertingEntry);
        values.add(alertingEntryValue);
        setRestrictedIfNotificationFeaturesDisabled(pref, alertingEntry, alertingEntryValue,
                KEYGUARD_DISABLE_SECURE_NOTIFICATIONS);

        entries.add(mContext.getString(R.string.lock_screen_notifs_show_none));
        values.add(Integer.toString(R.string.lock_screen_notifs_show_none));

        pref.setEntries(entries.toArray(new CharSequence[entries.size()]));
        pref.setEntryValues(values.toArray(new CharSequence[values.size()]));

        if (!adminAllowsNotifications() || !getLockscreenNotificationsEnabled()) {
            pref.setValue(Integer.toString(R.string.lock_screen_notifs_show_none));
        } else if (!getLockscreenSilentNotificationsEnabled()) {
            pref.setValue(Integer.toString(R.string.lock_screen_notifs_show_alerting));
        } else {
            pref.setValue(Integer.toString(R.string.lock_screen_notifs_show_all));
        }

        pref.setOnPreferenceChangeListener(this);

        refreshSummary(pref);
    }

    @Override
    public CharSequence getSummary() {
        if (!adminAllowsNotifications() || !getLockscreenNotificationsEnabled()) {
            return mContext.getString(R.string.lock_screen_notifs_show_none);
        } else if (!getLockscreenSilentNotificationsEnabled()) {
            return mContext.getString(R.string.lock_screen_notifs_show_alerting);
        } else {
            return mContext.getString(R.string.lock_screen_notifs_show_all_summary);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final int val = Integer.parseInt((String) newValue);
        final boolean enabled = val != R.string.lock_screen_notifs_show_none;
        final boolean show = val == R.string.lock_screen_notifs_show_all;
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_SILENT_NOTIFICATIONS, show ? 1 : 0);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, enabled ? 1 : 0);
        refreshSummary(preference);
        return true;
    }

    private void setRestrictedIfNotificationFeaturesDisabled(RestrictedListPreference pref,
            CharSequence entry, CharSequence entryValue, int keyguardNotificationFeatures) {
        RestrictedLockUtils.EnforcedAdmin admin =
                RestrictedLockUtilsInternal.checkIfKeyguardFeaturesDisabled(
                        mContext, keyguardNotificationFeatures, UserHandle.myUserId());
        if (admin != null && pref != null) {
            RestrictedListPreference.RestrictedItem item =
                    new RestrictedListPreference.RestrictedItem(entry, entryValue, admin);
            pref.addRestrictedItem(item);
        }
    }

    private boolean adminAllowsNotifications() {
        final int dpmFlags = mDpm.getKeyguardDisabledFeatures(null/* admin */);
        return (dpmFlags & KEYGUARD_DISABLE_SECURE_NOTIFICATIONS) == 0;
    }

    private boolean getLockscreenNotificationsEnabled() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, 1) != 0;
    }

    private boolean getLockscreenSilentNotificationsEnabled() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_SILENT_NOTIFICATIONS, 0) != 0;
    }
}
