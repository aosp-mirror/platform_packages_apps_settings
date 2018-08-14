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

package com.android.settings.notification;

import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_SCREEN_OFF;
import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_SCREEN_ON;

import android.app.ActivityManager;
import android.app.AutomaticZenRule;
import android.app.NotificationManager;
import android.content.Context;
import android.net.Uri;
import android.provider.Settings;
import android.service.notification.ZenModeConfig;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.android.settings.R;

public class ZenModeBackend {
    @VisibleForTesting
    protected static final String ZEN_MODE_FROM_ANYONE = "zen_mode_from_anyone";
    @VisibleForTesting
    protected static final String ZEN_MODE_FROM_CONTACTS = "zen_mode_from_contacts";
    @VisibleForTesting
    protected static final String ZEN_MODE_FROM_STARRED = "zen_mode_from_starred";
    @VisibleForTesting
    protected static final String ZEN_MODE_FROM_NONE = "zen_mode_from_none";
    protected static final int SOURCE_NONE = -1;

    private static ZenModeBackend sInstance;

    protected int mZenMode;
    /** gets policy last set by updatePolicy **/
    protected NotificationManager.Policy mPolicy;
    private final NotificationManager mNotificationManager;

    private String TAG = "ZenModeSettingsBackend";
    private final Context mContext;

    public static ZenModeBackend getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ZenModeBackend(context);
        }
        return sInstance;
    }

    public ZenModeBackend(Context context) {
        mContext = context;
        mNotificationManager = (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE);
        updateZenMode();
        updatePolicy();
    }

    protected void updatePolicy() {
        if (mNotificationManager != null) {
            mPolicy = mNotificationManager.getNotificationPolicy();
        }
    }

    protected void updateZenMode() {
        mZenMode = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.ZEN_MODE, mZenMode);
    }

    protected boolean setZenRule(String id, AutomaticZenRule rule) {
        return NotificationManager.from(mContext).updateAutomaticZenRule(id, rule);
    }

    protected void setZenMode(int zenMode) {
        NotificationManager.from(mContext).setZenMode(zenMode, null, TAG);
        mZenMode = getZenMode();
    }

    protected void setZenModeForDuration(int minutes) {
        Uri conditionId = ZenModeConfig.toTimeCondition(mContext, minutes,
                ActivityManager.getCurrentUser(), true).id;
        mNotificationManager.setZenMode(Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS,
                conditionId, TAG);
        mZenMode = getZenMode();
    }

    protected int getZenMode() {
        mZenMode = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.ZEN_MODE, mZenMode);
        return mZenMode;
    }

    protected boolean isVisualEffectSuppressed(int visualEffect) {
        return (mPolicy.suppressedVisualEffects & visualEffect) != 0;
    }

    protected boolean isPriorityCategoryEnabled(int categoryType) {
        return (mPolicy.priorityCategories & categoryType) != 0;
    }

    protected int getNewPriorityCategories(boolean allow, int categoryType) {
        int priorityCategories = mPolicy.priorityCategories;
        if (allow) {
            priorityCategories |= categoryType;
        } else {
            priorityCategories &= ~categoryType;
        }
        return priorityCategories;
    }

    protected int getPriorityCallSenders() {
        if (isPriorityCategoryEnabled(NotificationManager.Policy.PRIORITY_CATEGORY_CALLS)) {
            return mPolicy.priorityCallSenders;
        }

        return SOURCE_NONE;
    }

    protected int getPriorityMessageSenders() {
        if (isPriorityCategoryEnabled(NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES)) {
            return mPolicy.priorityMessageSenders;
        }
        return SOURCE_NONE;
    }

    protected void saveVisualEffectsPolicy(int category, boolean suppress) {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.ZEN_SETTINGS_UPDATED, 1);

        int suppressedEffects = getNewSuppressedEffects(suppress, category);
        savePolicy(mPolicy.priorityCategories, mPolicy.priorityCallSenders,
                mPolicy.priorityMessageSenders, suppressedEffects);
    }

    protected void saveSoundPolicy(int category, boolean allow) {
        int priorityCategories = getNewPriorityCategories(allow, category);
        savePolicy(priorityCategories, mPolicy.priorityCallSenders,
                mPolicy.priorityMessageSenders, mPolicy.suppressedVisualEffects);
    }

    protected void savePolicy(int priorityCategories, int priorityCallSenders,
            int priorityMessageSenders, int suppressedVisualEffects) {
        mPolicy = new NotificationManager.Policy(priorityCategories, priorityCallSenders,
                priorityMessageSenders, suppressedVisualEffects);
        mNotificationManager.setNotificationPolicy(mPolicy);
    }

    private int getNewSuppressedEffects(boolean suppress, int effectType) {
        int effects = mPolicy.suppressedVisualEffects;

        if (suppress) {
            effects |= effectType;
        } else {
            effects &= ~effectType;
        }

        return clearDeprecatedEffects(effects);
    }

    private int clearDeprecatedEffects(int effects) {
        return effects & ~(SUPPRESSED_EFFECT_SCREEN_ON | SUPPRESSED_EFFECT_SCREEN_OFF);
    }

    protected boolean isEffectAllowed(int effect) {
        return (mPolicy.suppressedVisualEffects & effect) == 0;
    }

    protected void saveSenders(int category, int val) {
        int priorityCallSenders = getPriorityCallSenders();
        int priorityMessagesSenders = getPriorityMessageSenders();
        int categorySenders = getPrioritySenders(category);

        final boolean allowSenders = val != SOURCE_NONE;
        final int allowSendersFrom = val == SOURCE_NONE ? categorySenders : val;

        String stringCategory = "";
        if (category == NotificationManager.Policy.PRIORITY_CATEGORY_CALLS) {
            stringCategory = "Calls";
            priorityCallSenders = allowSendersFrom;
        }

        if (category == NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES) {
            stringCategory = "Messages";
            priorityMessagesSenders = allowSendersFrom;
        }

        savePolicy(getNewPriorityCategories(allowSenders, category),
            priorityCallSenders, priorityMessagesSenders, mPolicy.suppressedVisualEffects);

        if (ZenModeSettingsBase.DEBUG) Log.d(TAG, "onPrefChange allow" +
                stringCategory + "=" + allowSenders + " allow" + stringCategory + "From="
                + ZenModeConfig.sourceToString(allowSendersFrom));
    }

    protected String getSendersKey(int category) {
        switch (getZenMode()) {
            case Settings.Global.ZEN_MODE_NO_INTERRUPTIONS:
            case Settings.Global.ZEN_MODE_ALARMS:
                return getKeyFromSetting(SOURCE_NONE);
            default:
                int prioritySenders = getPrioritySenders(category);
                return getKeyFromSetting(isPriorityCategoryEnabled(category)
                        ? prioritySenders : SOURCE_NONE);
            }
    }

    private int getPrioritySenders(int category) {
        int categorySenders = -1;

        if (category == NotificationManager.Policy.PRIORITY_CATEGORY_CALLS) {
            return getPriorityCallSenders();
        }

        if (category == NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES) {
            return getPriorityMessageSenders();
        }

        return categorySenders;
    }

    protected static String getKeyFromSetting(int contactType) {
        switch (contactType) {
            case NotificationManager.Policy.PRIORITY_SENDERS_ANY:
                return ZEN_MODE_FROM_ANYONE;
            case NotificationManager.Policy.PRIORITY_SENDERS_CONTACTS:
                return ZEN_MODE_FROM_CONTACTS;
            case NotificationManager.Policy.PRIORITY_SENDERS_STARRED:
                return ZEN_MODE_FROM_STARRED;
            case SOURCE_NONE:
            default:
                return ZEN_MODE_FROM_NONE;
        }
    }

    protected int getContactsSummary(int category) {
        int contactType = -1;

        // SOURCE_NONE can be used when in total silence or alarms only
        // (policy is based on user's preferences but the UI displayed is based on zenMode)
        if (category == SOURCE_NONE) {
            return R.string.zen_mode_from_none;
        }

        if (category == NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES) {
            if (isPriorityCategoryEnabled(category)) {
                contactType = getPriorityMessageSenders();
            }
        } else if (category == NotificationManager.Policy.PRIORITY_CATEGORY_CALLS) {
            if (isPriorityCategoryEnabled(category)) {
                contactType = getPriorityCallSenders();
            }
        }

        switch (contactType) {
            case NotificationManager.Policy.PRIORITY_SENDERS_ANY:
                return R.string.zen_mode_from_anyone;
            case NotificationManager.Policy.PRIORITY_SENDERS_CONTACTS:
                return  R.string.zen_mode_from_contacts;
            case NotificationManager.Policy.PRIORITY_SENDERS_STARRED:
                return  R.string.zen_mode_from_starred;
            case SOURCE_NONE:
            default:
                return R.string.zen_mode_from_none;
        }
    }

    protected static int getSettingFromPrefKey(String key) {
        switch (key) {
            case ZEN_MODE_FROM_ANYONE:
                return NotificationManager.Policy.PRIORITY_SENDERS_ANY;
            case ZEN_MODE_FROM_CONTACTS:
                return NotificationManager.Policy.PRIORITY_SENDERS_CONTACTS;
            case ZEN_MODE_FROM_STARRED:
                return NotificationManager.Policy.PRIORITY_SENDERS_STARRED;
            case ZEN_MODE_FROM_NONE:
            default:
                return SOURCE_NONE;
        }
    }

    public boolean removeZenRule(String ruleId) {
        return NotificationManager.from(mContext).removeAutomaticZenRule(ruleId);
    }

    protected String addZenRule(AutomaticZenRule rule) {
        try {
            String id = NotificationManager.from(mContext).addAutomaticZenRule(rule);
            NotificationManager.from(mContext).getAutomaticZenRule(id);
            return id;
        } catch (Exception e) {
            return null;
        }
    }
}
