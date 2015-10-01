/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.app.AlertDialog;
import android.app.AutomaticZenRule;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.service.notification.ConditionProviderService;
import android.service.notification.ZenModeConfig;
import android.util.Log;
import android.view.View;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.notification.ManagedServiceSettings.Config;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class ZenModeAutomationSettings extends ZenModeSettingsBase {

    static final Config CONFIG = getConditionProviderConfig();

    private PackageManager mPm;
    private ServiceListing mServiceListing;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.zen_mode_automation_settings);
        mServiceListing = new ServiceListing(mContext, CONFIG);
        mServiceListing.addCallback(mServiceListingCallback);
        mServiceListing.reload();
        mServiceListing.setListening(true);
        mPm = mContext.getPackageManager();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mServiceListing.setListening(false);
        mServiceListing.removeCallback(mServiceListingCallback);
    }

    @Override
    protected void onZenModeChanged() {
        // don't care
    }

    @Override
    protected void onZenModeConfigChanged() {
        updateControls();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateControls();
    }

    private void showAddRuleDialog() {
        new ZenRuleSelectionDialog(mContext, mServiceListing) {
            @Override
            public void onSystemRuleSelected(ZenRuleInfo ri) {
                showNameRuleDialog(ri);
            }

            @Override
            public void onExternalRuleSelected(ZenRuleInfo ri) {
                Intent intent = new Intent().setComponent(ri.configurationActivity);
                startActivity(intent);
            }
        }.show();
    }

    private void showNameRuleDialog(final ZenRuleInfo ri) {
        new ZenRuleNameDialog(mContext, mServiceListing, null, mRules) {
            @Override
            public void onOk(String ruleName) {
                MetricsLogger.action(mContext, MetricsLogger.ACTION_ZEN_ADD_RULE_OK);
                AutomaticZenRule rule = new AutomaticZenRule(ruleName, ri.serviceComponent,
                        ri.defaultConditionId, NotificationManager.INTERRUPTION_FILTER_PRIORITY,
                        true);
                if (setZenRule(rule)) {
                    startActivity(getRuleIntent(ri.settingsAction, null, rule.getName()));
                }
            }
        }.show();
    }

    private void showDeleteRuleDialog(final String ruleName) {
        new AlertDialog.Builder(mContext)
                .setMessage(getString(R.string.zen_mode_delete_rule_confirmation, ruleName))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.zen_mode_delete_rule_button,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MetricsLogger.action(mContext, MetricsLogger.ACTION_ZEN_DELETE_RULE_OK);
                        removeZenRule(ruleName);
                    }
                })
                .show();
    }

    private Intent getRuleIntent(String settingsAction, ComponentName configurationActivity,
            String ruleName) {
        Intent intent = new Intent()
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(ConditionProviderService.EXTRA_RULE_NAME, ruleName);
        if (configurationActivity != null) {
            intent.setComponent(configurationActivity);
        } else {
            intent.setAction(settingsAction);
        }
        return intent;
    }

    private AutomaticZenRule[] sortedRules() {
        final AutomaticZenRule[] rt = mRules.toArray(new AutomaticZenRule[mRules.size()]);
        Arrays.sort(rt, RULE_COMPARATOR);
        return rt;
    }

    private void updateControls() {
        final PreferenceScreen root = getPreferenceScreen();
        root.removeAll();
        if (mRules.size() == 0) return;
        final AutomaticZenRule[] sortedRules = sortedRules();
        for (AutomaticZenRule sortedRule : sortedRules) {
            root.addPreference(new ZenRulePreference(mContext, sortedRule));
        }
        final Preference p = new Preference(mContext);
        p.setIcon(R.drawable.ic_add);
        p.setTitle(R.string.zen_mode_add_rule);
        p.setPersistent(false);
        p.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                MetricsLogger.action(mContext, MetricsLogger.ACTION_ZEN_ADD_RULE);
                showAddRuleDialog();
                return true;
            }
        });
        root.addPreference(p);
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.NOTIFICATION_ZEN_MODE_AUTOMATION;
    }

    private String computeRuleSummary(AutomaticZenRule rule, boolean isSystemRule,
            CharSequence providerLabel) {
        final String mode = computeZenModeCaption(getResources(), rule.getInterruptionFilter());
        final String ruleState = (rule == null || !rule.isEnabled())
                ? getString(R.string.switch_off_text)
                : getString(R.string.zen_mode_rule_summary_enabled_combination, mode);

        return isSystemRule ? ruleState
                : getString(R.string.zen_mode_rule_summary_provider_combination,
                        providerLabel, ruleState);
    }

    private static Config getConditionProviderConfig() {
        final Config c = new Config();
        c.tag = TAG;
        c.setting = Settings.Secure.ENABLED_CONDITION_PROVIDERS;
        c.intentAction = ConditionProviderService.SERVICE_INTERFACE;
        c.permission = android.Manifest.permission.BIND_CONDITION_PROVIDER_SERVICE;
        c.noun = "condition provider";
        return c;
    }

    private static String computeZenModeCaption(Resources res, int zenMode) {
        switch (zenMode) {
            case NotificationManager.INTERRUPTION_FILTER_ALARMS:
                return res.getString(R.string.zen_mode_option_alarms);
            case NotificationManager.INTERRUPTION_FILTER_PRIORITY:
                return res.getString(R.string.zen_mode_option_important_interruptions);
            case NotificationManager.INTERRUPTION_FILTER_NONE:
                return res.getString(R.string.zen_mode_option_no_interruptions);
            default:
                return null;
        }
    }

    private final ServiceListing.Callback mServiceListingCallback = new ServiceListing.Callback() {
        @Override
        public void onServicesReloaded(List<ServiceInfo> services) {
            for (ServiceInfo service : services) {
                final ZenRuleInfo ri = getRuleInfo(service);
                if (ri != null && ri.serviceComponent != null
                        && Objects.equals(ri.settingsAction,
                        Settings.ACTION_ZEN_MODE_EXTERNAL_RULE_SETTINGS)) {
                    if (!mServiceListing.isEnabled(ri.serviceComponent)) {
                        Log.i(TAG, "Enabling external condition provider: " + ri.serviceComponent);
                        mServiceListing.setEnabled(ri.serviceComponent, true);
                    }
                }
            }
        }
    };

    public static ZenRuleInfo getRuleInfo(ServiceInfo si) {
        if (si == null || si.metaData == null) return null;
        final String ruleType = si.metaData.getString(ConditionProviderService.META_DATA_RULE_TYPE);
        final String defaultConditionId =
                si.metaData.getString(ConditionProviderService.META_DATA_DEFAULT_CONDITION_ID);
        if (ruleType != null && !ruleType.trim().isEmpty() && defaultConditionId != null) {
            final ZenRuleInfo ri = new ZenRuleInfo();
            ri.settingsAction = Settings.ACTION_ZEN_MODE_EXTERNAL_RULE_SETTINGS;
            ri.title = ruleType;
            ri.packageName = si.packageName;
            ri.configurationActivity = getSettingsActivity(si);

            return ri;
        }
        return null;
    }

    private static ComponentName getSettingsActivity(ServiceInfo si) {
        if (si == null || si.metaData == null) return null;
        final String configurationActivity =
                si.metaData.getString(ConditionProviderService.META_DATA_CONFIGURATION_ACTIVITY);
        if (configurationActivity != null) {
            return ComponentName.unflattenFromString(configurationActivity);
        }
        return null;
    }

    // TODO: Sort by creation date, once that data is available.
    private static final Comparator<AutomaticZenRule> RULE_COMPARATOR =
            new Comparator<AutomaticZenRule>() {
        @Override
        public int compare(AutomaticZenRule lhs, AutomaticZenRule rhs) {
            return key(lhs).compareTo(key(rhs));
        }

        private String key(AutomaticZenRule rule) {
            final int type = ZenModeConfig.isValidScheduleConditionId(rule.getConditionId()) ? 1
                    : ZenModeConfig.isValidEventConditionId(rule.getConditionId()) ? 2
                    : 3;
            return type + rule.getName();
        }
    };

    private class ZenRulePreference extends Preference {
        final String mName;

        public ZenRulePreference(Context context, final AutomaticZenRule rule) {
            super(context);

            mName = rule.getName();

            final boolean isSchedule = ZenModeConfig.isValidScheduleConditionId(
                    rule.getConditionId());
            final boolean isEvent = ZenModeConfig.isValidEventConditionId(rule.getConditionId());
            final boolean isSystemRule = isSchedule || isEvent;

            try {
                ApplicationInfo info = mPm.getApplicationInfo(rule.getOwner().getPackageName(), 0);
                LoadIconTask task = new LoadIconTask(this);
                task.execute(info);
                setSummary(computeRuleSummary(rule, isSystemRule, info.loadLabel(mPm)));
            } catch (PackageManager.NameNotFoundException e) {
                setIcon(R.drawable.ic_label);
            }

            setTitle(rule.getName());
            setPersistent(false);

            final String action = isSchedule ? ZenModeScheduleRuleSettings.ACTION
                    : isEvent ? ZenModeEventRuleSettings.ACTION : "";
            ServiceInfo si = mServiceListing.findService(mContext, CONFIG, rule.getOwner());
            ComponentName settingsActivity = getSettingsActivity(si);
            setIntent(getRuleIntent(action, settingsActivity, rule.getName()));

            setWidgetLayoutResource(R.layout.zen_rule_widget);
        }

        @Override
        protected void onBindView(View view) {
            super.onBindView(view);

            View v = view.findViewById(R.id.delete_zen_rule);
            if (v != null) {
                v.setOnClickListener(mDeleteListener);
            }
        }

        private final View.OnClickListener mDeleteListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteRuleDialog(mName);
            }
        };
    }

    private class LoadIconTask extends AsyncTask<ApplicationInfo, Void, Drawable> {
        private final WeakReference<Preference> prefReference;

        public LoadIconTask(Preference pref) {
            prefReference = new WeakReference<>(pref);
        }

        @Override
        protected Drawable doInBackground(ApplicationInfo... params) {
            return params[0].loadIcon(mPm);
        }

        @Override
        protected void onPostExecute(Drawable icon) {
            if (icon != null) {
                final Preference pref = prefReference.get();
                pref.setIcon(icon);
            }
        }
    }

}
