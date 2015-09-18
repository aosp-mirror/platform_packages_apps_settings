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
import android.provider.Settings.Global;
import android.service.notification.ConditionProviderService;
import android.service.notification.ZenModeConfig;
import android.service.notification.ZenModeConfig.ZenRule;
import android.util.Log;
import android.view.View;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.notification.ManagedServiceSettings.Config;
import com.android.settings.notification.ZenRuleNameDialog.RuleInfo;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;

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
        new ZenRuleNameDialog(mContext, mServiceListing, null, mConfig.getAutomaticRuleNames()) {
            @Override
            public void onOk(String ruleName, RuleInfo ri) {
                MetricsLogger.action(mContext, MetricsLogger.ACTION_ZEN_ADD_RULE_OK);
                final ZenRule rule = new ZenRule();
                rule.name = ruleName;
                rule.enabled = true;
                rule.zenMode = Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS;
                rule.conditionId = ri.defaultConditionId;
                rule.component = ri.serviceComponent;
                final ZenModeConfig newConfig = mConfig.copy();
                final String ruleId = newConfig.newRuleId();
                newConfig.automaticRules.put(ruleId, rule);
                if (setZenModeConfig(newConfig)) {
                    showRule(ri.settingsAction, ri.configurationActivity, ruleId, rule.name);
                }
            }
        }.show();
    }

    private void showDeleteRuleDialog(final String ruleName, final String ruleId) {
        new AlertDialog.Builder(mContext)
                .setMessage(getString(R.string.zen_mode_delete_rule_confirmation, ruleName))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.zen_mode_delete_rule_button,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MetricsLogger.action(mContext, MetricsLogger.ACTION_ZEN_DELETE_RULE_OK);
                        mConfig.automaticRules.remove(ruleId);
                        setZenModeConfig(mConfig);
                    }
                })
                .show();
    }

    private void showRule(String settingsAction, ComponentName configurationActivity,
            String ruleId, String ruleName) {
        if (DEBUG) Log.d(TAG, "showRule " + ruleId + " name=" + ruleName);
        mContext.startActivity(new Intent(settingsAction)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(ZenModeRuleSettingsBase.EXTRA_RULE_ID, ruleId));
    }

    private ZenRuleInfo[] sortedRules() {
        final ZenRuleInfo[] rt = new ZenRuleInfo[mConfig.automaticRules.size()];
        for (int i = 0; i < rt.length; i++) {
            final ZenRuleInfo zri = new ZenRuleInfo();
            zri.id = mConfig.automaticRules.keyAt(i);
            zri.rule = mConfig.automaticRules.valueAt(i);
            rt[i] = zri;
        }
        Arrays.sort(rt, RULE_COMPARATOR);
        return rt;
    }

    private void updateControls() {
        final PreferenceScreen root = getPreferenceScreen();
        root.removeAll();
        if (mConfig == null) return;
        final ZenRuleInfo[] sortedRules = sortedRules();
        for (int i = 0; i < sortedRules.length; i++) {
            final String id = sortedRules[i].id;
            final ZenRule rule = sortedRules[i].rule;
            root.addPreference(new ZenRulePreference(mContext, rule, id));
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

    private String computeRuleSummary(ZenRule rule, boolean isSystemRule,
            CharSequence providerLabel) {
        final String mode = computeZenModeCaption(getResources(), rule.zenMode);
        final String ruleState = (rule == null || !rule.enabled)
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
            case Global.ZEN_MODE_ALARMS:
                return res.getString(R.string.zen_mode_option_alarms);
            case Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS:
                return res.getString(R.string.zen_mode_option_important_interruptions);
            case Global.ZEN_MODE_NO_INTERRUPTIONS:
                return res.getString(R.string.zen_mode_option_no_interruptions);
            default:
                return null;
        }
    }

    private final ServiceListing.Callback mServiceListingCallback = new ServiceListing.Callback() {
        @Override
        public void onServicesReloaded(List<ServiceInfo> services) {
            for (ServiceInfo service : services) {
                final RuleInfo ri = ZenModeExternalRuleSettings.getRuleInfo(service);
                if (ri != null && ri.serviceComponent != null
                        && ri.settingsAction == ZenModeExternalRuleSettings.ACTION) {
                    if (!mServiceListing.isEnabled(ri.serviceComponent)) {
                        Log.i(TAG, "Enabling external condition provider: " + ri.serviceComponent);
                        mServiceListing.setEnabled(ri.serviceComponent, true);
                    }
                }
            }
        }
    };

    // TODO: Sort by creation date, once that data is available.
    private static final Comparator<ZenRuleInfo> RULE_COMPARATOR = new Comparator<ZenRuleInfo>() {
        @Override
        public int compare(ZenRuleInfo lhs, ZenRuleInfo rhs) {
            return key(lhs).compareTo(key(rhs));
        }

        private String key(ZenRuleInfo zri) {
            final ZenRule rule = zri.rule;
            final int type = ZenModeConfig.isValidScheduleConditionId(rule.conditionId) ? 1
                    : ZenModeConfig.isValidEventConditionId(rule.conditionId) ? 2
                    : 3;
            return type + rule.name;
        }
    };

    private static class ZenRuleInfo {
        String id;
        ZenRule rule;
    }

    private class ZenRulePreference extends Preference {
        final String mName;
        final String mId;

        public ZenRulePreference(Context context, final ZenRule rule, final String id) {
            super(context);

            mName = rule.name;
            this.mId = id;

            final boolean isSchedule = ZenModeConfig.isValidScheduleConditionId(rule.conditionId);
            final boolean isEvent = ZenModeConfig.isValidEventConditionId(rule.conditionId);
            final boolean isSystemRule = isSchedule || isEvent;

            try {
                ApplicationInfo info = mPm.getApplicationInfo(
                        rule.component.getPackageName(), 0);
                LoadIconTask task = new LoadIconTask(this);
                task.execute(info);
                setSummary(computeRuleSummary(rule, isSystemRule, info.loadLabel(mPm)));
            } catch (PackageManager.NameNotFoundException e) {
                setIcon(R.drawable.ic_label);
            }

            setTitle(rule.name);
            setPersistent(false);
            setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    final String action = isSchedule ? ZenModeScheduleRuleSettings.ACTION
                            : isEvent ? ZenModeEventRuleSettings.ACTION
                            : ZenModeExternalRuleSettings.ACTION;
                    showRule(action, null, id, rule.name);
                    return true;
                }
            });
            setWidgetLayoutResource(R.layout.zen_rule_widget);
        }

        @Override
        protected void onBindView(View view) {
            super.onBindView(view);

            View v = view.findViewById(R.id.delete_zen_rule);
            v.setOnClickListener(mDeleteListener);
        }

        private final View.OnClickListener mDeleteListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteRuleDialog(mName, mId);
            }
        };
    }

    private class LoadIconTask extends AsyncTask<ApplicationInfo, Void, Drawable> {
        private final WeakReference<Preference> prefReference;

        public LoadIconTask(Preference pref) {
            prefReference = new WeakReference<Preference>(pref);
        }

        @Override
        protected Drawable doInBackground(ApplicationInfo... params) {
            return params[0].loadIcon(mPm);
        }

        @Override
        protected void onPostExecute(Drawable icon) {
            if (prefReference != null && icon != null) {
                final Preference pref = prefReference.get();
                pref.setIcon(icon);
            }
        }
    }

}
