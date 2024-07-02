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

import static android.app.AutomaticZenRule.TYPE_SCHEDULE_CALENDAR;
import static android.app.AutomaticZenRule.TYPE_SCHEDULE_TIME;
import static android.app.NotificationManager.EXTRA_AUTOMATIC_RULE_ID;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.service.notification.ConditionProviderService;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.utils.ManagedServiceSettings;
import com.android.settings.utils.ZenServiceListing;
import com.android.settingslib.PrimarySwitchPreference;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;

import java.util.List;

/**
 * Preference controller for the link to an individual mode's configuration page.
 */
class ZenModeSetTriggerLinkPreferenceController extends AbstractZenModePreferenceController {
    private static final String TAG = "ZenModeSetTriggerLink";

    @VisibleForTesting
    protected static final String AUTOMATIC_TRIGGER_PREF_KEY = "zen_automatic_trigger_settings";

    private static final ManagedServiceSettings.Config CONFIG =
            ZenModesListFragment.getConditionProviderConfig();

    private ZenServiceListing mServiceListing;
    private final PackageManager mPm;
    private final DashboardFragment mFragment;

    ZenModeSetTriggerLinkPreferenceController(Context context, String key,
            DashboardFragment fragment, ZenModesBackend backend,
            PackageManager packageManager) {
        super(context, key, backend);
        mFragment = fragment;
        mPm = packageManager;
    }

    @VisibleForTesting
    protected void setServiceListing(ZenServiceListing serviceListing) {
        mServiceListing = serviceListing;
    }

    @Override
    public boolean isAvailable(@NonNull ZenMode zenMode) {
        return !zenMode.isManualDnd();
    }

    @Override
    public void displayPreference(PreferenceScreen screen, @NonNull ZenMode zenMode) {
        if (mServiceListing == null) {
            mServiceListing = new ZenServiceListing(
                    mContext, CONFIG, zenMode.getRule().getPackageName());
        }
        mServiceListing.reloadApprovedServices();
    }

    @Override
    public void updateState(Preference preference, @NonNull ZenMode zenMode) {
        // This controller is expected to govern a preference category so that it controls the
        // availability of the entire preference category if the mode doesn't have a way to
        // automatically trigger (such as manual DND).
        PrimarySwitchPreference switchPref = ((PreferenceCategory) preference).findPreference(
                AUTOMATIC_TRIGGER_PREF_KEY);
        if (switchPref == null) {
            return;
        }
        switchPref.setChecked(zenMode.getRule().isEnabled());
        switchPref.setOnPreferenceChangeListener(mSwitchChangeListener);
        switchPref.setSummary(zenMode.getRule().getTriggerDescription());
        switchPref.setIcon(null);
        switchPref.setOnPreferenceClickListener(null);
        switchPref.setIntent(null);

        if (zenMode.isSystemOwned()) {
            if (zenMode.getType() == TYPE_SCHEDULE_TIME) {
                switchPref.setTitle(R.string.zen_mode_set_schedule_link);
                // TODO: b/332937635 - set correct metrics category
                switchPref.setIntent(ZenSubSettingLauncher.forModeFragment(mContext,
                        ZenModeSetScheduleFragment.class, zenMode.getId(), 0).toIntent());
            } else if (zenMode.getType() == TYPE_SCHEDULE_CALENDAR) {
                switchPref.setTitle(R.string.zen_mode_set_calendar_link);
                switchPref.setIcon(null);
                // TODO: b/332937635 - set correct metrics category
                switchPref.setIntent(ZenSubSettingLauncher.forModeFragment(mContext,
                        ZenModeSetCalendarFragment.class, zenMode.getId(), 0).toIntent());
            } else {
                switchPref.setTitle(R.string.zen_mode_select_schedule);
                switchPref.setIcon(R.drawable.ic_add_24dp);
                switchPref.setSummary("");
                // TODO: b/342156843 - Hide the switch (needs support in SettingsLib).
                switchPref.setOnPreferenceClickListener(clickedPreference -> {
                    ZenModeScheduleChooserDialog.show(mFragment, mOnScheduleOptionListener);
                    return true;
                });
            }
        } else {
            Intent intent = getAppRuleIntent(zenMode);
            if (intent != null && isValidIntent(intent)) {
                preference.setVisible(true);
                switchPref.setTitle(R.string.zen_mode_configuration_link_title);
                switchPref.setSummary(zenMode.getRule().getTriggerDescription());
                switchPref.setIntent(intent);
            } else {
                Log.i(TAG, "No intent found for " + zenMode.getRule().getName());
                preference.setVisible(false);
            }
        }
    }

    @VisibleForTesting
    final ZenModeScheduleChooserDialog.OnScheduleOptionListener mOnScheduleOptionListener =
            conditionId -> saveMode(mode -> {
                mode.setCustomModeConditionId(mContext, conditionId);
                return mode;
            });

    @VisibleForTesting
    protected Preference.OnPreferenceChangeListener mSwitchChangeListener = (p, newValue) -> {
        final boolean newEnabled = (Boolean) newValue;
        return saveMode((zenMode) -> {
            if (newEnabled != zenMode.getRule().isEnabled()) {
                zenMode.getRule().setEnabled(newEnabled);
            }
            return zenMode;
        });
        // TODO: b/342156843 - Do we want to jump to the corresponding schedule editing screen?
    };

    @VisibleForTesting
    protected @Nullable Intent getAppRuleIntent(ZenMode zenMode) {
        Intent intent = new Intent().addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(ConditionProviderService.EXTRA_RULE_ID, zenMode.getId())
                .putExtra(EXTRA_AUTOMATIC_RULE_ID, zenMode.getId());
        String owner = zenMode.getRule().getPackageName();
        ComponentName configActivity = null;
        if (zenMode.getRule().getConfigurationActivity() != null) {
            // If a configuration activity is present, use that directly in the intent
            configActivity = zenMode.getRule().getConfigurationActivity();
        } else {
            // Otherwise, look for a condition provider service for the rule's package
            ComponentInfo ci = mServiceListing.findService(zenMode.getRule().getOwner());
            if (ci == null) {
                // do nothing
            } else if (ci instanceof ActivityInfo) {
                // new activity backed rule
                intent.setComponent(new ComponentName(ci.packageName, ci.name));
                return intent;
            } else if (ci.metaData != null) {
                // old service backed rule
                final String configurationActivity = ci.metaData.getString(
                        ConditionProviderService.META_DATA_CONFIGURATION_ACTIVITY);
                if (configurationActivity != null) {
                    configActivity = ComponentName.unflattenFromString(configurationActivity);
                }
            }
        }

        if (configActivity != null) {
            // verify that the owner of the rule owns the configuration activity, but only if
            // owner exists
            intent.setComponent(configActivity);
            if (owner == null) {
                return intent;
            }
            try {
                int ownerUid = mPm.getPackageUid(owner, 0);
                int configActivityOwnerUid = mPm.getPackageUid(configActivity.getPackageName(), 0);
                if (ownerUid == configActivityOwnerUid) {
                    return intent;
                } else {
                    Log.w(TAG, "Config activity not in owner package for "
                            + zenMode.getRule().getName());
                    return null;
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Failed to find config activity");
                return null;
            }
        }
        return null;
    }

    private boolean isValidIntent(Intent intent) {
        List<ResolveInfo> results = mPm.queryIntentActivities(
                intent, PackageManager.ResolveInfoFlags.of(0));
        if (intent.resolveActivity(mPm) == null || results.size() == 0) {
            Log.w(TAG, "intent for zen rule invalid: " + intent);
            return false;
        }
        return true;
    }
}
