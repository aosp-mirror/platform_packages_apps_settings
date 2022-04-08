/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.notification.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;

import androidx.preference.Preference;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.RestrictedListPreference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.notification.NotificationBackend;
import com.android.settings.notification.app.NotificationPreferenceController;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;

import java.util.ArrayList;

public class VisibilityPreferenceController extends NotificationPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String TAG = "ChannelVisPrefContr";
    private static final String KEY_VISIBILITY_OVERRIDE = "visibility_override";
    private LockPatternUtils mLockPatternUtils;

    public VisibilityPreferenceController(Context context, LockPatternUtils utils,
            NotificationBackend backend) {
        super(context, backend);
        mLockPatternUtils = utils;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_VISIBILITY_OVERRIDE;
    }

    @Override
    public boolean isAvailable() {
        if (!super.isAvailable()) {
            return false;
        }
        if (mChannel == null || mAppRow.banned) {
            return false;
        }
        return checkCanBeVisible(NotificationManager.IMPORTANCE_LOW) && isLockScreenSecure();
    }

    public void updateState(Preference preference) {
        if (mChannel != null && mAppRow != null) {
            RestrictedListPreference pref = (RestrictedListPreference) preference;
            ArrayList<CharSequence> entries = new ArrayList<>();
            ArrayList<CharSequence> values = new ArrayList<>();

            pref.clearRestrictedItems();
            if (getLockscreenNotificationsEnabled() && getLockscreenAllowPrivateNotifications()) {
                final String summaryShowEntry =
                        mContext.getString(R.string.lock_screen_notifications_summary_show);
                final String summaryShowEntryValue =
                        Integer.toString(NotificationManager.VISIBILITY_NO_OVERRIDE);
                entries.add(summaryShowEntry);
                values.add(summaryShowEntryValue);
                setRestrictedIfNotificationFeaturesDisabled(pref, summaryShowEntry,
                        summaryShowEntryValue,
                        DevicePolicyManager.KEYGUARD_DISABLE_SECURE_NOTIFICATIONS
                                | DevicePolicyManager.KEYGUARD_DISABLE_UNREDACTED_NOTIFICATIONS);
            }

            if (getLockscreenNotificationsEnabled()) {
                final String summaryHideEntry =
                        mContext.getString(R.string.lock_screen_notifications_summary_hide);
                final String summaryHideEntryValue = Integer.toString(
                        Notification.VISIBILITY_PRIVATE);
                entries.add(summaryHideEntry);
                values.add(summaryHideEntryValue);
                setRestrictedIfNotificationFeaturesDisabled(pref,
                        summaryHideEntry, summaryHideEntryValue,
                        DevicePolicyManager.KEYGUARD_DISABLE_SECURE_NOTIFICATIONS);
            }
            entries.add(mContext.getString(R.string.lock_screen_notifications_summary_disable));
            values.add(Integer.toString(Notification.VISIBILITY_SECRET));
            pref.setEntries(entries.toArray(new CharSequence[entries.size()]));
            pref.setEntryValues(values.toArray(new CharSequence[values.size()]));

            if (mChannel.getLockscreenVisibility()
                    == NotificationListenerService.Ranking.VISIBILITY_NO_OVERRIDE) {
                pref.setValue(Integer.toString(getGlobalVisibility()));
            } else {
                pref.setValue(Integer.toString(mChannel.getLockscreenVisibility()));
            }
            pref.setSummary("%s");
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mChannel != null) {
            int sensitive = Integer.parseInt((String) newValue);
            if (sensitive == getGlobalVisibility()) {
                sensitive = NotificationListenerService.Ranking.VISIBILITY_NO_OVERRIDE;
            }
            mChannel.setLockscreenVisibility(sensitive);
            mChannel.lockFields(NotificationChannel.USER_LOCKED_VISIBILITY);
            saveChannel();
        }
        return true;
    }

    private void setRestrictedIfNotificationFeaturesDisabled(RestrictedListPreference pref,
            CharSequence entry, CharSequence entryValue, int keyguardNotificationFeatures) {
        RestrictedLockUtils.EnforcedAdmin admin =
                RestrictedLockUtilsInternal.checkIfKeyguardFeaturesDisabled(
                        mContext, keyguardNotificationFeatures, mAppRow.userId);
        if (admin != null) {
            RestrictedListPreference.RestrictedItem item =
                    new RestrictedListPreference.RestrictedItem(entry, entryValue, admin);
            pref.addRestrictedItem(item);
        }
    }

    private int getGlobalVisibility() {
        int globalVis = NotificationListenerService.Ranking.VISIBILITY_NO_OVERRIDE;
        if (!getLockscreenNotificationsEnabled()) {
            globalVis = Notification.VISIBILITY_SECRET;
        } else if (!getLockscreenAllowPrivateNotifications()) {
            globalVis = Notification.VISIBILITY_PRIVATE;
        }
        return globalVis;
    }

    private boolean getLockscreenNotificationsEnabled() {
        final UserInfo parentUser = mUm.getProfileParent(UserHandle.myUserId());
        final int primaryUserId = parentUser != null ? parentUser.id : UserHandle.myUserId();
        return Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, 0, primaryUserId) != 0;
    }

    private boolean getLockscreenAllowPrivateNotifications() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, 0) != 0;
    }

    protected boolean isLockScreenSecure() {
        boolean lockscreenSecure = mLockPatternUtils.isSecure(UserHandle.myUserId());
        UserInfo parentUser = mUm.getProfileParent(UserHandle.myUserId());
        if (parentUser != null){
            lockscreenSecure |= mLockPatternUtils.isSecure(parentUser.id);
        }

        return lockscreenSecure;
    }

}
