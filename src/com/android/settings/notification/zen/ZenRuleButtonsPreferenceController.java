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

import android.app.AutomaticZenRule;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.ActionButtonsPreference;

public class ZenRuleButtonsPreferenceController extends AbstractZenModePreferenceController
    implements PreferenceControllerMixin {
    public static final String KEY = "zen_action_buttons";

    private AutomaticZenRule mRule;
    private String mId;
    private PreferenceFragmentCompat mFragment;
    private ActionButtonsPreference mButtonsPref;


    public ZenRuleButtonsPreferenceController(Context context, PreferenceFragmentCompat fragment,
            Lifecycle lc) {
        super(context, KEY, lc);
        mFragment = fragment;
    }


    @Override
    public boolean isAvailable() {
        return mRule != null;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        if (isAvailable()) {
            mButtonsPref = ((ActionButtonsPreference) screen.findPreference(KEY))
                    .setButton1Text(R.string.zen_mode_rule_name_edit)
                    .setButton1Icon(com.android.internal.R.drawable.ic_mode_edit)
                    .setButton1OnClickListener(new EditRuleNameClickListener())
                    .setButton2Text(R.string.zen_mode_delete_rule_button)
                    .setButton2Icon(R.drawable.ic_settings_delete)
                    .setButton2OnClickListener(new DeleteRuleClickListener());
        }
    }

    public class EditRuleNameClickListener implements View.OnClickListener {
        public EditRuleNameClickListener() {}

        @Override
        public void onClick(View v) {
            ZenRuleNameDialog.show(mFragment, mRule.getName(), null,
                    new ZenRuleNameDialog.PositiveClickListener() {
                        @Override
                        public void onOk(String ruleName, Fragment parent) {
                            if (TextUtils.equals(ruleName, mRule.getName())) {
                                return;
                            }
                            mMetricsFeatureProvider.action(mContext,
                                    SettingsEnums.ACTION_ZEN_MODE_RULE_NAME_CHANGE_OK);
                            mRule.setName(ruleName);
                            mRule.setModified(true);
                            mBackend.updateZenRule(mId, mRule);
                        }
                    });
        }
    }

    public class DeleteRuleClickListener implements View.OnClickListener {
        public DeleteRuleClickListener() {}

        @Override
        public void onClick(View v) {
            ZenDeleteRuleDialog.show(mFragment, mRule.getName(), mId,
                    new ZenDeleteRuleDialog.PositiveClickListener() {
                        @Override
                        public void onOk(String id) {
                            Bundle bundle = new Bundle();
                            bundle.putString(ZenModeAutomationSettings.DELETE, id);
                            mMetricsFeatureProvider.action(mContext,
                                    SettingsEnums.ACTION_ZEN_DELETE_RULE_OK);
                            new SubSettingLauncher(mContext)
                                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                    .setDestination(ZenModeAutomationSettings.class.getName())
                                    .setSourceMetricsCategory(MetricsProto.MetricsEvent
                                            .NOTIFICATION_ZEN_MODE_AUTOMATION)
                                    .setArguments(bundle)
                                    .launch();
                        }
            });
        }
    }

    protected void onResume(AutomaticZenRule rule, String id) {
        mRule = rule;
        mId = id;
    }
}
