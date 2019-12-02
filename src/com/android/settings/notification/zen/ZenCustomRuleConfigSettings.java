/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.notification.zen;

import android.app.NotificationManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.service.notification.ZenPolicy;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

public class ZenCustomRuleConfigSettings extends ZenCustomRuleSettingsBase {
    private static final String CALLS_KEY = "zen_rule_calls_settings";
    private static final String MESSAGES_KEY = "zen_rule_messages_settings";
    private static final String ALARMS_KEY = "zen_rule_alarms";
    private static final String MEDIA_KEY = "zen_rule_media";
    private static final String SYSTEM_KEY = "zen_rule_system";
    private static final String REMINDERS_KEY = "zen_rule_reminders";
    private static final String EVENTS_KEY = "zen_rule_events";
    private static final String NOTIFICATIONS_KEY = "zen_rule_notifications";
    private static final String PREFERENCE_CATEGORY_KEY = "zen_custom_rule_configuration_category";

    private Preference mCallsPreference;
    private Preference mMessagesPreference;
    private Preference mNotificationsPreference;
    private ZenModeSettings.SummaryBuilder mSummaryBuilder;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mSummaryBuilder = new ZenModeSettings.SummaryBuilder(mContext);

        mCallsPreference = getPreferenceScreen().findPreference(CALLS_KEY);
        mCallsPreference.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        new SubSettingLauncher(mContext)
                                .setDestination(ZenCustomRuleCallsSettings.class.getName())
                                .setArguments(createZenRuleBundle())
                                .setSourceMetricsCategory(SettingsEnums.ZEN_CUSTOM_RULE_CALLS)
                                .launch();
                        return true;
                    }
                });

        mMessagesPreference = getPreferenceScreen().findPreference(MESSAGES_KEY);
        mMessagesPreference.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        new SubSettingLauncher(mContext)
                                .setDestination(ZenCustomRuleMessagesSettings.class.getName())
                                .setArguments(createZenRuleBundle())
                                .setSourceMetricsCategory(SettingsEnums.ZEN_CUSTOM_RULE_MESSAGES)
                                .launch();
                        return true;
                    }
                });

        mNotificationsPreference = getPreferenceScreen().findPreference(NOTIFICATIONS_KEY);
        mNotificationsPreference.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        new SubSettingLauncher(mContext)
                                .setDestination(ZenCustomRuleNotificationsSettings.class.getName())
                                .setArguments(createZenRuleBundle())
                                .setSourceMetricsCategory
                                        (SettingsEnums.ZEN_CUSTOM_RULE_NOTIFICATION_RESTRICTIONS)
                                .launch();
                        return true;
                    }
                });

        updateSummaries();
    }

    @Override
    public void onZenModeConfigChanged() {
        super.onZenModeConfigChanged();
        updateSummaries();
    }

    /**
     * Updates summaries of preferences without preference controllers
     */
    private void updateSummaries() {
        NotificationManager.Policy noManPolicy = mBackend.toNotificationPolicy(
                mRule.getZenPolicy());

        mCallsPreference.setSummary(mSummaryBuilder.getCallsSettingSummary(noManPolicy));
        mMessagesPreference.setSummary(mSummaryBuilder.getMessagesSettingSummary(noManPolicy));
        mNotificationsPreference.setSummary(mSummaryBuilder.getBlockedEffectsSummary(noManPolicy));
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.zen_mode_custom_rule_configuration;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ZEN_CUSTOM_RULE_SOUND_SETTINGS;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        mControllers = new ArrayList<>();
        mControllers.add(new ZenRuleCustomSwitchPreferenceController(context,
                getSettingsLifecycle(), ALARMS_KEY, ZenPolicy.PRIORITY_CATEGORY_ALARMS,
                SettingsEnums.ACTION_ZEN_ALLOW_ALARMS));
        mControllers.add(new ZenRuleCustomSwitchPreferenceController(context,
                getSettingsLifecycle(), MEDIA_KEY, ZenPolicy.PRIORITY_CATEGORY_MEDIA,
                SettingsEnums.ACTION_ZEN_ALLOW_MEDIA));
        mControllers.add(new ZenRuleCustomSwitchPreferenceController(context,
                getSettingsLifecycle(), SYSTEM_KEY, ZenPolicy.PRIORITY_CATEGORY_SYSTEM,
                SettingsEnums.ACTION_ZEN_ALLOW_SYSTEM));
        mControllers.add(new ZenRuleCustomSwitchPreferenceController(context,
                getSettingsLifecycle(), REMINDERS_KEY, ZenPolicy.PRIORITY_CATEGORY_REMINDERS,
                SettingsEnums.ACTION_ZEN_ALLOW_REMINDERS));
        mControllers.add(new ZenRuleCustomSwitchPreferenceController(context,
                getSettingsLifecycle(), EVENTS_KEY, ZenPolicy.PRIORITY_CATEGORY_EVENTS,
                SettingsEnums.ACTION_ZEN_ALLOW_EVENTS));
        return mControllers;
    }

    @Override
    String getPreferenceCategoryKey() {
        return PREFERENCE_CATEGORY_KEY;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSummaries();
    }
}
