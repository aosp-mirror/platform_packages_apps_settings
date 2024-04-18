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
import android.app.NotificationManager;
import android.content.Context;
import android.net.Uri;
import android.provider.Settings;
import android.service.notification.Condition;
import android.service.notification.ZenAdapters;
import android.service.notification.ZenModeConfig;

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
        modes.add(getManualDndMode());

        Map<String, AutomaticZenRule> zenRules = mNotificationManager.getAutomaticZenRules();
        for (Map.Entry<String, AutomaticZenRule> zenRuleEntry : zenRules.entrySet()) {
            modes.add(new ZenMode(zenRuleEntry.getKey(), zenRuleEntry.getValue()));
        }

        // TODO: b/331429435 - Sort modes.
        return modes;
    }

    @Nullable
    ZenMode getMode(String id) {
        if (ZenMode.MANUAL_DND_MODE_ID.equals(id)) {
            return getManualDndMode();
        } else {
            AutomaticZenRule rule = mNotificationManager.getAutomaticZenRule(id);
            return rule != null ? new ZenMode(id, rule) : null;
        }
    }

    private ZenMode getManualDndMode() {
        // TODO: b/333530553 - Read ZenDeviceEffects of manual DND.
        // TODO: b/333682392 - Replace with final strings for name & trigger description
        AutomaticZenRule manualDndRule = new AutomaticZenRule.Builder(
                mContext.getString(R.string.zen_mode_settings_title), Uri.EMPTY)
                .setType(AutomaticZenRule.TYPE_OTHER)
                .setZenPolicy(ZenAdapters.notificationPolicyToZenPolicy(
                        mNotificationManager.getNotificationPolicy()))
                .setDeviceEffects(null)
                .setTriggerDescription(mContext.getString(R.string.zen_mode_settings_summary))
                .setManualInvocationAllowed(true)
                .setConfigurationActivity(null) // No further settings
                .setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                .build();

        return ZenMode.manualDndMode(manualDndRule);
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
