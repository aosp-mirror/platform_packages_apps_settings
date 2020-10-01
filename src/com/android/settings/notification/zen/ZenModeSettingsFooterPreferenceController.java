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

package com.android.settings.notification.zen;

import android.app.Dialog;
import android.app.NotificationManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.icu.text.ListFormatter;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.ZenModeConfig;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.utils.AnnotationSpan;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ZenModeSettingsFooterPreferenceController extends AbstractZenModePreferenceController {
    static final String KEY = "footer_preference";
    private FragmentManager mFragment;

    public ZenModeSettingsFooterPreferenceController(Context context, Lifecycle lifecycle,
            FragmentManager fragment) {
        super(context, KEY, lifecycle);
        mFragment = fragment;
    }

    @Override
    public boolean isAvailable() {
        switch(getZenMode()) {
            case Settings.Global.ZEN_MODE_ALARMS:
            case Settings.Global.ZEN_MODE_NO_INTERRUPTIONS:
            case Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS:
                return true;
            case Settings.Global.ZEN_MODE_OFF:
            default:
                return false;
        }
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        boolean isAvailable = isAvailable();
        preference.setVisible(isAvailable);
        if (isAvailable) {
            preference.setTitle(getFooterText());
        }
    }

    protected CharSequence getFooterText() {
        ZenModeConfig config = getZenModeConfig();

        NotificationManager.Policy appliedPolicy = mBackend.getConsolidatedPolicy();
        NotificationManager.Policy defaultPolicy = config.toNotificationPolicy();
        final boolean usingCustomPolicy = !Objects.equals(appliedPolicy, defaultPolicy);

        if (usingCustomPolicy) {
            final List<ZenModeConfig.ZenRule> activeRules = getActiveRules(config);
            final List<String> rulesNames = new ArrayList<>();
            for (ZenModeConfig.ZenRule rule : activeRules) {
                if (rule.name != null) {
                    rulesNames.add(rule.name);
                }
            }
            if (rulesNames.size() > 0) {
                String rules = ListFormatter.getInstance().format(rulesNames);
                if (!rules.isEmpty()) {
                    final AnnotationSpan.LinkInfo linkInfo = new AnnotationSpan.LinkInfo(
                            AnnotationSpan.LinkInfo.DEFAULT_ANNOTATION, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    showCustomSettingsDialog();
                                }
                            });
                    return TextUtils.concat(mContext.getResources().getString(
                            R.string.zen_mode_settings_dnd_custom_settings_footer, rules),
                            AnnotationSpan.linkify(mContext.getResources().getText(
                            R.string.zen_mode_settings_dnd_custom_settings_footer_link),
                            linkInfo));
                }
            }
        }
        return getDefaultPolicyFooter(config);
    }

    private String getDefaultPolicyFooter(ZenModeConfig config) {
        String footerText = "";
        long latestEndTime = -1;

        // DND turned on by manual rule
        if (config.manualRule != null) {
            final Uri id = config.manualRule.conditionId;
            if (config.manualRule.enabler != null) {
                // app triggered manual rule
                String appOwner = mZenModeConfigWrapper.getOwnerCaption(config.manualRule.enabler);
                if (!appOwner.isEmpty()) {
                    footerText = mContext.getString(
                            R.string.zen_mode_settings_dnd_automatic_rule_app, appOwner);
                }
            } else {
                if (id == null) {
                    return mContext.getString(
                            R.string.zen_mode_settings_dnd_manual_indefinite);
                } else {
                    latestEndTime = mZenModeConfigWrapper.parseManualRuleTime(id);
                    if (latestEndTime > 0) {
                        final CharSequence formattedTime = mZenModeConfigWrapper.getFormattedTime(
                                latestEndTime, mContext.getUserId());
                        footerText = mContext.getString(
                                R.string.zen_mode_settings_dnd_manual_end_time,
                                formattedTime);
                    }
                }
            }
        }

        // DND turned on by an automatic rule
        for (ZenModeConfig.ZenRule automaticRule : config.automaticRules.values()) {
            if (automaticRule.isAutomaticActive()) {
                // set footer if 3rd party rule
                if (!mZenModeConfigWrapper.isTimeRule(automaticRule.conditionId)) {
                    return mContext.getString(R.string.zen_mode_settings_dnd_automatic_rule,
                            automaticRule.name);
                } else {
                    // set footer if automatic rule end time is the latest active rule end time
                    long endTime = mZenModeConfigWrapper.parseAutomaticRuleEndTime(
                            automaticRule.conditionId);
                    if (endTime > latestEndTime) {
                        latestEndTime = endTime;
                        footerText = mContext.getString(
                                R.string.zen_mode_settings_dnd_automatic_rule, automaticRule.name);
                    }
                }
            }
        }
        return footerText;
    }

    private List<ZenModeConfig.ZenRule> getActiveRules(ZenModeConfig config) {
        List<ZenModeConfig.ZenRule> zenRules = new ArrayList<>();
        if (config.manualRule != null) {
            zenRules.add(config.manualRule);
        }

        for (ZenModeConfig.ZenRule automaticRule : config.automaticRules.values()) {
            if (automaticRule.isAutomaticActive()) {
                zenRules.add(automaticRule);
            }
        }
        return zenRules;
    }

    private void showCustomSettingsDialog() {
        ZenCustomSettingsDialog dialog = new ZenCustomSettingsDialog();
        dialog.setNotificationPolicy(mBackend.getConsolidatedPolicy());
        dialog.show(mFragment, ZenCustomSettingsDialog.class.getName());
    }

    public static class ZenCustomSettingsDialog extends InstrumentedDialogFragment {
        private String KEY_POLICY = "policy";
        private NotificationManager.Policy mPolicy;
        private ZenModeSettings.SummaryBuilder mSummaryBuilder;

        public void setNotificationPolicy(NotificationManager.Policy policy) {
            mPolicy = policy;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Context context  = getActivity();
            if (savedInstanceState != null) {
                NotificationManager.Policy policy = savedInstanceState.getParcelable(KEY_POLICY);
                if (policy != null) {
                    mPolicy = policy;
                }
            }

            mSummaryBuilder = new ZenModeSettings.SummaryBuilder(context);

            AlertDialog customSettingsDialog = new AlertDialog.Builder(context)
                    .setTitle(R.string.zen_custom_settings_dialog_title)
                    .setNeutralButton(R.string.zen_custom_settings_dialog_review_schedule,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    new SubSettingLauncher(context)
                                            .setDestination(
                                                    ZenModeAutomationSettings.class.getName())
                                            .setSourceMetricsCategory(
                                                    SettingsEnums.NOTIFICATION_ZEN_MODE_AUTOMATION)
                                            .launch();
                                }
                            })
                    .setPositiveButton(R.string.zen_custom_settings_dialog_ok, null)
                    .setView(LayoutInflater.from(context).inflate(context.getResources().getLayout(
                            R.layout.zen_custom_settings_dialog), null, false))
                    .create();

            customSettingsDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    TextView allowCallsText = customSettingsDialog.findViewById(
                            R.id.zen_custom_settings_dialog_calls_allow);
                    TextView allowMessagesText = customSettingsDialog.findViewById(
                            R.id.zen_custom_settings_dialog_messages_allow);
                    TextView allowAlarmsText = customSettingsDialog.findViewById(
                            R.id.zen_custom_settings_dialog_alarms_allow);
                    TextView allowMediaText = customSettingsDialog.findViewById(
                            R.id.zen_custom_settings_dialog_media_allow);
                    TextView allowSystemText = customSettingsDialog.findViewById(
                            R.id.zen_custom_settings_dialog_system_allow);
                    TextView allowRemindersText = customSettingsDialog.findViewById(
                            R.id.zen_custom_settings_dialog_reminders_allow);
                    TextView allowEventsText = customSettingsDialog.findViewById(
                            R.id.zen_custom_settings_dialog_events_allow);
                    TextView notificationsText = customSettingsDialog.findViewById(
                            R.id.zen_custom_settings_dialog_show_notifications);

                    allowCallsText.setText(mSummaryBuilder.getCallsSettingSummary(mPolicy));
                    allowMessagesText.setText(mSummaryBuilder.getMessagesSettingSummary(mPolicy));
                    allowAlarmsText.setText(getAllowRes(mPolicy.allowAlarms()));
                    allowMediaText.setText(getAllowRes(mPolicy.allowMedia()));
                    allowSystemText.setText(getAllowRes(mPolicy.allowSystem()));
                    allowRemindersText.setText(getAllowRes(mPolicy.allowReminders()));
                    allowEventsText.setText(getAllowRes(mPolicy.allowEvents()));
                    notificationsText.setText(mSummaryBuilder.getBlockedEffectsSummary(mPolicy));
                }
            });

            return customSettingsDialog;
        }

        @Override
        public int getMetricsCategory() {
            return SettingsEnums.ZEN_CUSTOM_SETTINGS_DIALOG;
        }

        private int getAllowRes(boolean allow) {
            return allow ? R.string.zen_mode_sound_summary_on : R.string.zen_mode_sound_summary_off;
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putParcelable(KEY_POLICY, mPolicy);
        }
    }
}
