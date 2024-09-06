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

import static android.app.AutomaticZenRule.TYPE_BEDTIME;
import static android.app.AutomaticZenRule.TYPE_DRIVING;
import static android.app.AutomaticZenRule.TYPE_SCHEDULE_CALENDAR;
import static android.app.AutomaticZenRule.TYPE_SCHEDULE_TIME;
import static android.service.notification.ZenModeConfig.tryParseScheduleConditionId;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.service.notification.SystemZenRules;
import android.service.notification.ZenModeConfig;
import android.util.Log;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settingslib.PrimarySwitchPreference;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;

import com.google.common.base.Strings;

class ZenModeTriggerUpdatePreferenceController extends AbstractZenModePreferenceController {

    private static final String TAG = "ZenModeTriggerUpdate";

    private final PackageManager mPackageManager;
    private final ConfigurationActivityHelper mConfigurationActivityHelper;
    private final ZenServiceListing mServiceListing;

    private String mModeName;

    ZenModeTriggerUpdatePreferenceController(Context context, String key,
            ZenModesBackend backend) {
        this(context, key, backend, context.getPackageManager(),
                new ConfigurationActivityHelper(context.getPackageManager()),
                new ZenServiceListing(context));
    }

    @VisibleForTesting
    ZenModeTriggerUpdatePreferenceController(Context context, String key,
            ZenModesBackend backend, PackageManager packageManager,
            ConfigurationActivityHelper configurationActivityHelper,
            ZenServiceListing serviceListing) {
        super(context, key, backend);
        mPackageManager = packageManager;
        mConfigurationActivityHelper = configurationActivityHelper;
        mServiceListing = serviceListing;
    }

    @Override
    public boolean isAvailable(@NonNull ZenMode zenMode) {
        return !zenMode.isCustomManual() && !zenMode.isManualDnd();
    }

    @Override
    void updateState(Preference preference, @NonNull ZenMode zenMode) {
        if (!isAvailable(zenMode)) {
            return;
        }

        mModeName = zenMode.getName();
        PrimarySwitchPreference triggerPref = (PrimarySwitchPreference) preference;
        triggerPref.setChecked(zenMode.isEnabled());
        triggerPref.setOnPreferenceChangeListener(mSwitchChangeListener);
        if (zenMode.isSystemOwned()) {
            setUpForSystemOwnedTrigger(triggerPref, zenMode);
        } else {
            setUpForAppTrigger(triggerPref, zenMode);
        }
    }

    private void setUpForSystemOwnedTrigger(Preference preference, ZenMode mode) {
        if (mode.getType() == TYPE_SCHEDULE_TIME) {
            preference.setIntent(ZenSubSettingLauncher.forModeFragment(mContext,
                    ZenModeSetScheduleFragment.class, mode.getId(),
                    SettingsEnums.ZEN_PRIORITY_MODE).toIntent());

            // [Clock Icon] 9:00 - 17:00 / Sun-Mon
            preference.setIcon(com.android.internal.R.drawable.ic_zen_mode_type_schedule_time);
            ZenModeConfig.ScheduleInfo schedule =
                    tryParseScheduleConditionId(mode.getRule().getConditionId());
            if (schedule != null) {
                preference.setTitle(SystemZenRules.getTimeSummary(mContext, schedule));
                preference.setSummary(SystemZenRules.getShortDaysSummary(mContext, schedule));
            } else {
                // Fallback, but shouldn't happen.
                Log.wtf(TAG, "SCHEDULE_TIME mode without schedule: " + mode);
                preference.setTitle(R.string.zen_mode_set_schedule_link);
                preference.setSummary(null);
            }
        } else if (mode.getType() == TYPE_SCHEDULE_CALENDAR) {
            preference.setIntent(ZenSubSettingLauncher.forModeFragment(mContext,
                    ZenModeSetCalendarFragment.class, mode.getId(),
                    SettingsEnums.ZEN_PRIORITY_MODE).toIntent());

            // [Event Icon] Calendar Events / <Calendar name>
            preference.setIcon(
                    com.android.internal.R.drawable.ic_zen_mode_type_schedule_calendar);
            preference.setTitle(R.string.zen_mode_trigger_title_schedule_calendar);
            preference.setSummary(mode.getTriggerDescription());
        } else {
            Log.wtf(TAG, "Unexpected type for system-owned mode: " + mode);
        }
    }

    @SuppressLint("SwitchIntDef")
    private void setUpForAppTrigger(Preference preference, ZenMode mode) {
        // App-owned mode may have triggerDescription, configurationActivity, or both/neither.
        mServiceListing.loadApprovedComponents(mode.getRule().getPackageName());
        Intent configurationIntent =
                mConfigurationActivityHelper.getConfigurationActivityIntentForMode(
                        mode, mServiceListing::findService);

        @StringRes int title = switch (mode.getType()) {
            case TYPE_BEDTIME -> R.string.zen_mode_trigger_title_bedtime;
            case TYPE_DRIVING -> R.string.zen_mode_trigger_title_driving;
            default -> R.string.zen_mode_trigger_title_generic;
        };

        String summary;
        if (!Strings.isNullOrEmpty(mode.getTriggerDescription())) {
            summary = mode.getTriggerDescription();
        } else if (!Strings.isNullOrEmpty(mode.getRule().getPackageName())) {
            String appName = null;
            try {
                ApplicationInfo appInfo = mPackageManager.getApplicationInfo(
                        mode.getRule().getPackageName(), 0);
                appName = appInfo.loadLabel(mPackageManager).toString();
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Couldn't resolve owner for mode: " + mode);
            }

            if (appName != null) {
                summary = mContext.getString(
                        configurationIntent != null
                                ? R.string.zen_mode_trigger_summary_settings_in_app
                                : R.string.zen_mode_trigger_summary_managed_by_app,
                        appName);
            } else {
                summary = null;
            }
        } else {
            Log.e(TAG, "Mode without package! " + mode);
            summary = null;
        }

        @DrawableRes int icon;
        if (mode.getType() == TYPE_BEDTIME) {
            icon = com.android.internal.R.drawable.ic_zen_mode_type_schedule_time; // Clock
        } else if (mode.getType() == TYPE_DRIVING) {
            icon = com.android.internal.R.drawable.ic_zen_mode_type_driving; // Car
        } else {
            icon = configurationIntent != null ? R.drawable.ic_zen_mode_trigger_with_activity
                    : R.drawable.ic_zen_mode_trigger_without_activity;
        }

        preference.setTitle(title);
        preference.setSummary(summary);
        preference.setIcon(icon);
        preference.setIntent(configurationIntent);
    }

    private final Preference.OnPreferenceChangeListener mSwitchChangeListener = (p, newValue) -> {
        confirmChangeEnabled(p, (boolean) newValue);
        return true;
    };

    private void confirmChangeEnabled(Preference preference, boolean enabled) {
        @StringRes int titleFormat = enabled ? R.string.zen_mode_confirm_enable_mode_title
                : R.string.zen_mode_confirm_disable_mode_title;
        @StringRes int message = enabled ? R.string.zen_mode_confirm_enable_message
                : R.string.zen_mode_confirm_disable_message;
        @StringRes int confirmButton = enabled ? R.string.zen_mode_action_enable
                : R.string.zen_mode_action_disable;

        new AlertDialog.Builder(mContext)
                .setTitle(mContext.getString(titleFormat, mModeName))
                .setMessage(message)
                .setPositiveButton(confirmButton,
                        (dialog, which) -> setModeEnabled(enabled))
                .setNegativeButton(R.string.cancel,
                        (dialog, which) -> undoToggleSwitch(preference, enabled))
                .setOnCancelListener(dialog -> undoToggleSwitch(preference, enabled))
                .show();
    }

    private void setModeEnabled(boolean enabled) {
        saveMode((zenMode) -> {
            if (enabled != zenMode.isEnabled()) {
                zenMode.getRule().setEnabled(enabled);
            }
            return zenMode;
        });
        getMetricsFeatureProvider().action(mContext, SettingsEnums.ACTION_ZEN_MODE_ENABLE_TOGGLE,
                enabled);
    }

    private void undoToggleSwitch(Preference preference, boolean wasSwitchedTo) {
        PrimarySwitchPreference switchPreference = (PrimarySwitchPreference) preference;
        switchPreference.setChecked(!wasSwitchedTo);
    }
}
