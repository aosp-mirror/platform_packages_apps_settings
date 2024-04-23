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
        ZenModeConfig currentConfig = mNotificationManager.getZenModeConfig();
        modes.add(getManualDndMode(currentConfig));

        Map<String, AutomaticZenRule> zenRules = mNotificationManager.getAutomaticZenRules();
        for (Map.Entry<String, AutomaticZenRule> zenRuleEntry : zenRules.entrySet()) {
            String ruleId = zenRuleEntry.getKey();
            modes.add(new ZenMode(ruleId, zenRuleEntry.getValue(),
                    isRuleActive(ruleId, currentConfig)));
        }

        // TODO: b/331429435 - Sort modes.
        return modes;
    }

    @Nullable
    ZenMode getMode(String id) {
        ZenModeConfig currentConfig = mNotificationManager.getZenModeConfig();
        if (ZenMode.MANUAL_DND_MODE_ID.equals(id)) {
            // Regardless of its contents, non-null manualRule means that manual rule is active.
            return getManualDndMode(currentConfig);
        } else {
            AutomaticZenRule rule = mNotificationManager.getAutomaticZenRule(id);
            if (rule == null) {
                return null;
            }
            return new ZenMode(id, rule, isRuleActive(id, currentConfig));
        }
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
                .setTriggerDescription(mContext.getString(R.string.zen_mode_settings_summary))
                .setManualInvocationAllowed(true)
                .setConfigurationActivity(null) // No further settings
                .setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                .build();

        return ZenMode.manualDndMode(manualDndRule,
                config != null && config.manualRule != null);  // isActive
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
