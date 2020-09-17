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

import android.app.AlertDialog;
import android.app.AutomaticZenRule;
import android.app.NotificationManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.service.notification.ConditionProviderService;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.fragment.app.Fragment;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.utils.ManagedServiceSettings;
import com.android.settings.utils.ZenServiceListing;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SearchIndexable
public class ZenModeAutomationSettings extends ZenModeSettingsBase {
    public static final String DELETE = "DELETE_RULE";
    protected final ManagedServiceSettings.Config CONFIG = getConditionProviderConfig();
    private CharSequence[] mDeleteDialogRuleNames;
    private String[] mDeleteDialogRuleIds;
    private boolean[] mDeleteDialogChecked;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Bundle bundle = getArguments();
        if (bundle != null && bundle.containsKey(DELETE)) {
            mBackend.removeZenRule(bundle.getString(DELETE));
            bundle.remove(DELETE);
        }
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        ZenServiceListing serviceListing = new ZenServiceListing(getContext(), CONFIG);
        serviceListing.reloadApprovedServices();
        return buildPreferenceControllers(context, this, serviceListing, getSettingsLifecycle());
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Fragment parent, ZenServiceListing serviceListing, Lifecycle lifecycle) {
        List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new ZenModeAddAutomaticRulePreferenceController(context, parent,
                serviceListing, lifecycle));
        controllers.add(new ZenModeAutomaticRulesPreferenceController(context, parent, lifecycle));

        return controllers;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.zen_mode_automation_settings;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.NOTIFICATION_ZEN_MODE_AUTOMATION;
    }

    protected static ManagedServiceSettings.Config getConditionProviderConfig() {
        return new ManagedServiceSettings.Config.Builder()
                .setTag(TAG)
                .setIntentAction(ConditionProviderService.SERVICE_INTERFACE)
                .setConfigurationIntentAction(NotificationManager.ACTION_AUTOMATIC_ZEN_RULE)
                .setPermission(android.Manifest.permission.BIND_CONDITION_PROVIDER_SERVICE)
                .setNoun("condition provider")
                .build();
    }
    private final int DELETE_RULES = 1;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(Menu.NONE, DELETE_RULES, Menu.NONE, R.string.zen_mode_delete_automatic_rules);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case DELETE_RULES:
                Map.Entry<String, AutomaticZenRule>[] rules = mBackend.getAutomaticZenRules();
                mDeleteDialogRuleNames = new CharSequence[rules.length];
                mDeleteDialogRuleIds = new String[rules.length];
                mDeleteDialogChecked = new boolean[rules.length];
                for (int i = 0; i < rules.length; i++) {
                    mDeleteDialogRuleNames[i] = rules[i].getValue().getName();
                    mDeleteDialogRuleIds[i] = rules[i].getKey();
                }
                new AlertDialog.Builder(mContext)
                        .setTitle(R.string.zen_mode_delete_automatic_rules)
                        .setMultiChoiceItems(mDeleteDialogRuleNames, null,
                                new DialogInterface.OnMultiChoiceClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which,
                                            boolean isChecked) {
                                        mDeleteDialogChecked[which] = isChecked;
                                    }
                                })
                        .setPositiveButton(R.string.zen_mode_schedule_delete,
                                new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                for (int i = 0; i < rules.length; i++) {
                                    if (mDeleteDialogChecked[i]) {
                                        mBackend.removeZenRule(mDeleteDialogRuleIds[i]);
                                    }
                                }
                            }
                        }).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * For Search.
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.zen_mode_automation_settings) {

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    final List<String> keys = super.getNonIndexableKeys(context);
                    keys.add(ZenModeAddAutomaticRulePreferenceController.KEY);
                    keys.add(ZenModeAutomaticRulesPreferenceController.KEY);
                    return keys;
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return buildPreferenceControllers(context, null, null, null);
                }
            };
}
