/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.notification.modes;

import static java.util.Objects.requireNonNull;

import android.annotation.Nullable;
import android.app.ActivityManager;
import android.app.AutomaticZenRule;
import android.app.INotificationManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ParceledListSlice;
import android.database.Cursor;
import android.net.Uri;
import android.os.ServiceManager;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.service.notification.Condition;
import android.service.notification.ConversationChannelWrapper;
import android.service.notification.ZenAdapters;
import android.service.notification.ZenModeConfig;

import android.util.Log;
import androidx.annotation.VisibleForTesting;
import com.android.settings.R;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Class used for Settings-NMS interactions related to Mode management.
 *
 * <p>This class converts {@link AutomaticZenRule} instances, as well as the manual zen mode,
 * into the unified {@link ZenMode} format.
 */
class ZenModesBackend {

    private static final String TAG = "ZenModeBackend";

    @Nullable // Until first usage
    private static ZenModesBackend sInstance;

    private final NotificationManager mNotificationManager;
    static INotificationManager sINM = INotificationManager.Stub.asInterface(
            ServiceManager.getService(Context.NOTIFICATION_SERVICE));

    private final Context mContext;

    static ZenModesBackend getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ZenModesBackend(context.getApplicationContext());
        }
        return sInstance;
    }

    ZenModesBackend(Context context) {
        mContext = context;
        mNotificationManager = context.getSystemService(NotificationManager.class);
    }

    List<ZenMode> getModes() {
        ArrayList<ZenMode> modes = new ArrayList<>();
        ZenModeConfig currentConfig = mNotificationManager.getZenModeConfig();
        modes.add(getManualDndMode(currentConfig));

        Map<String, AutomaticZenRule> zenRules = mNotificationManager.getAutomaticZenRules();
        for (Map.Entry<String, AutomaticZenRule> zenRuleEntry : zenRules.entrySet()) {
            String ruleId = zenRuleEntry.getKey();
            modes.add(new ZenMode(ruleId, zenRuleEntry.getValue(),
                    isRuleActive(ruleId, currentConfig)));
        }

        modes.sort((l, r) -> {
            if (l.isManualDnd()) {
                return -1;
            } else if (r.isManualDnd()) {
                return 1;
            }
            return l.getRule().getName().compareTo(r.getRule().getName());
        });

        return modes;
    }

    @Nullable
    ZenMode getMode(String id) {
        ZenModeConfig currentConfig = mNotificationManager.getZenModeConfig();
        if (ZenMode.MANUAL_DND_MODE_ID.equals(id)) {
            return getManualDndMode(currentConfig);
        } else {
            AutomaticZenRule rule = mNotificationManager.getAutomaticZenRule(id);
            if (rule == null) {
                return null;
            }
            return new ZenMode(id, rule, isRuleActive(id, currentConfig));
        }
    }

    public ParceledListSlice<ConversationChannelWrapper> getConversations(boolean onlyImportant) {
        try {
            return sINM.getConversations(onlyImportant);
        } catch (Exception e) {
            Log.w(TAG, "Error calling NoMan", e);
            return ParceledListSlice.emptyList();
        }
    }

    public List<String> getStarredContacts() {
        Cursor cursor = null;
        try {
            cursor = queryStarredContactsData();
            return getStarredContacts(cursor);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @VisibleForTesting
    List<String> getStarredContacts(Cursor cursor) {
        List<String> starredContacts = new ArrayList<>();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String contact = cursor.getString(0);
                starredContacts.add(contact != null ? contact :
                        mContext.getString(R.string.zen_mode_starred_contacts_empty_name));

            } while (cursor.moveToNext());
        }
        return starredContacts;
    }

    private Cursor queryStarredContactsData() {
        return mContext.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
                new String[]{ContactsContract.Contacts.DISPLAY_NAME_PRIMARY},
                ContactsContract.Data.STARRED + "=1", null,
                ContactsContract.Data.TIMES_CONTACTED);
    }

    Cursor queryAllContactsData() {
        return mContext.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
                new String[]{ContactsContract.Contacts.DISPLAY_NAME_PRIMARY},
                null, null, null);
    }

    private ZenMode getManualDndMode(ZenModeConfig config) {
        // TODO: b/333530553 - Read ZenDeviceEffects of manual DND.
        // TODO: b/333682392 - Replace with final strings for name & trigger description
        AutomaticZenRule manualDndRule = new AutomaticZenRule.Builder(
                mContext.getString(R.string.zen_mode_settings_title), Uri.EMPTY)
                .setType(AutomaticZenRule.TYPE_OTHER)
                .setZenPolicy(ZenAdapters.notificationPolicyToZenPolicy(
                        mNotificationManager.getNotificationPolicy()))
                .setDeviceEffects(null)
                .setManualInvocationAllowed(true)
                .setConfigurationActivity(null) // No further settings
                .setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                .build();

        // Regardless of its contents, non-null manualRule means that manual rule is active.
        return ZenMode.manualDndMode(manualDndRule,
                config != null && config.manualRule != null);
    }

    private static boolean isRuleActive(String id, ZenModeConfig config) {
        if (config == null) {
            // shouldn't happen if the config is coming from NM, but be safe
            return false;
        }
        ZenModeConfig.ZenRule configRule = config.automaticRules.get(id);
        return configRule != null && configRule.isAutomaticActive();
    }

    void updateMode(ZenMode mode) {
        if (mode.isManualDnd()) {
            NotificationManager.Policy dndPolicy =
                    new ZenModeConfig().toNotificationPolicy(requireNonNull(mode.getPolicy()));
            mNotificationManager.setNotificationPolicy(dndPolicy, /* fromUser= */ true);
            // TODO: b/333530553 - Update ZenDeviceEffects of the manual DND too.
        } else {
            mNotificationManager.updateAutomaticZenRule(mode.getId(), mode.getRule(),
                    /* fromUser= */ true);
        }
    }

    void activateMode(ZenMode mode, @Nullable Duration forDuration) {
        if (mode.isManualDnd()) {
            Uri durationConditionId = null;
            if (forDuration != null) {
                durationConditionId = ZenModeConfig.toTimeCondition(mContext,
                        (int) forDuration.toMinutes(), ActivityManager.getCurrentUser(), true).id;
            }
            mNotificationManager.setZenMode(Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS,
                    durationConditionId, TAG, /* fromUser= */ true);

        } else {
            if (forDuration != null) {
                throw new IllegalArgumentException(
                        "Only the manual DND mode can be activated for a specific duration");
            }
            mNotificationManager.setAutomaticZenRuleState(mode.getId(),
                    new Condition(mode.getRule().getConditionId(), "", Condition.STATE_TRUE,
                            Condition.SOURCE_USER_ACTION));
        }
    }

    void deactivateMode(ZenMode mode) {
        if (mode.isManualDnd()) {
            // When calling with fromUser=true this will not snooze other modes.
            mNotificationManager.setZenMode(Settings.Global.ZEN_MODE_OFF, null, TAG,
                    /* fromUser= */ true);
        } else {
            // TODO: b/333527800 - This should (potentially) snooze the rule if it was active.
            mNotificationManager.setAutomaticZenRuleState(mode.getId(),
                    new Condition(mode.getRule().getConditionId(), "", Condition.STATE_FALSE,
                            Condition.SOURCE_USER_ACTION));
        }
    }

    void removeMode(ZenMode mode) {
        if (!mode.canBeDeleted()) {
            throw new IllegalArgumentException("Mode " + mode + " cannot be deleted!");
        }
        mNotificationManager.removeAutomaticZenRule(mode.getId(), /* fromUser= */ true);
    }
}
