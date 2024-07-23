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

import static com.google.common.base.Preconditions.checkNotNull;

import android.annotation.SuppressLint;
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
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.PrimarySwitchPreference;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;

import com.google.common.base.Strings;

/**
 * Preference controller for the link to an individual mode's configuration page.
 */
class ZenModeSetTriggerLinkPreferenceController extends AbstractZenModePreferenceController {
    private static final String TAG = "ZenModeSetTriggerLink";

    @VisibleForTesting
    static final String AUTOMATIC_TRIGGER_KEY = "zen_automatic_trigger_settings";
    static final String ADD_TRIGGER_KEY = "zen_add_automatic_trigger";

    private final DashboardFragment mFragment;
    private final PackageManager mPackageManager;
    private final ConfigurationActivityHelper mConfigurationActivityHelper;
    private final ZenServiceListing mServiceListing;

    ZenModeSetTriggerLinkPreferenceController(Context context, String key,
            DashboardFragment fragment, ZenModesBackend backend) {
        this(context, key, fragment, backend, context.getPackageManager(),
                new ConfigurationActivityHelper(context.getPackageManager()),
                new ZenServiceListing(context));
    }

    @VisibleForTesting
    ZenModeSetTriggerLinkPreferenceController(Context context, String key,
            DashboardFragment fragment, ZenModesBackend backend, PackageManager packageManager,
            ConfigurationActivityHelper configurationActivityHelper,
            ZenServiceListing serviceListing) {
        super(context, key, backend);
        mFragment = fragment;
        mPackageManager = packageManager;
        mConfigurationActivityHelper = configurationActivityHelper;
        mServiceListing = serviceListing;
    }

    @Override
    public boolean isAvailable(@NonNull ZenMode zenMode) {
        return !zenMode.isManualDnd();
    }

    @Override
    public void displayPreference(PreferenceScreen screen, @NonNull ZenMode zenMode) {
        // Preload approved components, but only for the package that owns the rule (since it's the
        // only package that can have a valid configurationActivity).
        mServiceListing.loadApprovedComponents(zenMode.getRule().getPackageName());
    }

    @Override
    public void updateState(Preference preference, @NonNull ZenMode zenMode) {
        // This controller is expected to govern a preference category so that it controls the
        // availability of the entire preference category if the mode doesn't have a way to
        // automatically trigger (such as manual DND).
        if (zenMode.isManualDnd()) {
            return;
        }
        PrimarySwitchPreference triggerPref = checkNotNull(
                ((PreferenceCategory) preference).findPreference(AUTOMATIC_TRIGGER_KEY));
        Preference addTriggerPref = checkNotNull(
                ((PreferenceCategory) preference).findPreference(ADD_TRIGGER_KEY));

        boolean isAddTrigger = zenMode.isSystemOwned() && zenMode.getType() != TYPE_SCHEDULE_TIME
                && zenMode.getType() != TYPE_SCHEDULE_CALENDAR;

        if (isAddTrigger) {
            triggerPref.setVisible(false);
            addTriggerPref.setVisible(true);
            addTriggerPref.setOnPreferenceClickListener(unused -> {
                ZenModeScheduleChooserDialog.show(mFragment, mOnScheduleOptionListener);
                return true;
            });
        } else {
            addTriggerPref.setVisible(false);
            triggerPref.setVisible(true);
            triggerPref.setChecked(zenMode.getRule().isEnabled());
            triggerPref.setOnPreferenceChangeListener(mSwitchChangeListener);

            if (zenMode.isSystemOwned()) {
                setUpForSystemOwnedTrigger(triggerPref, zenMode);
            } else {
                setUpForAppTrigger(triggerPref, zenMode);
            }
        }
    }

    private void setUpForSystemOwnedTrigger(Preference preference, ZenMode mode) {
        if (mode.getType() == TYPE_SCHEDULE_TIME) {
            // TODO: b/332937635 - set correct metrics category
            preference.setIntent(ZenSubSettingLauncher.forModeFragment(mContext,
                    ZenModeSetScheduleFragment.class, mode.getId(), 0).toIntent());

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
            // TODO: b/332937635 - set correct metrics category
            preference.setIntent(ZenSubSettingLauncher.forModeFragment(mContext,
                    ZenModeSetCalendarFragment.class, mode.getId(), 0).toIntent());

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

    @VisibleForTesting
    final ZenModeScheduleChooserDialog.OnScheduleOptionListener mOnScheduleOptionListener =
            conditionId -> saveMode(mode -> {
                mode.setCustomModeConditionId(mContext, conditionId);
                return mode;
                // TODO: b/342156843 - Maybe jump to the corresponding schedule editing screen?
            });

    private final Preference.OnPreferenceChangeListener mSwitchChangeListener = (p, newValue) -> {
        final boolean newEnabled = (Boolean) newValue;
        return saveMode((zenMode) -> {
            if (newEnabled != zenMode.getRule().isEnabled()) {
                zenMode.getRule().setEnabled(newEnabled);
            }
            return zenMode;
        });
    };
}
