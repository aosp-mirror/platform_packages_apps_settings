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

import static com.android.settings.widget.EntityHeaderController.PREF_KEY_APP_HEADER;

import android.app.AutomaticZenRule;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.service.notification.ZenModeConfig;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.Preference;
import android.util.Slog;
import android.view.View;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class ZenAutomaticRuleHeaderPreferenceController extends AbstractZenModePreferenceController
        implements PreferenceControllerMixin {

    private final String KEY = PREF_KEY_APP_HEADER;
    private final PreferenceFragment mFragment;
    private AutomaticZenRule mRule;
    private String mId;
    private EntityHeaderController mController;

    public ZenAutomaticRuleHeaderPreferenceController(Context context, PreferenceFragment fragment,
            Lifecycle lifecycle) {
        super(context, PREF_KEY_APP_HEADER, lifecycle);
        mFragment = fragment;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public boolean isAvailable() {
        return mRule != null;
    }

    public void updateState(Preference preference) {
        if (mRule == null) {
            return;
        }

        if (mFragment != null) {
            LayoutPreference pref = (LayoutPreference) preference;

            if (mController == null) {
                mController = EntityHeaderController
                        .newInstance(mFragment.getActivity(), mFragment,
                                pref.findViewById(R.id.entity_header));

                mController.setEditZenRuleNameListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ZenRuleNameDialog.show(mFragment, mRule.getName(), null,
                                new RuleNameChangeListener());
                    }
                });
            }

            pref = mController.setIcon(getIcon())
                    .setLabel(mRule.getName())
                    .setPackageName(mRule.getOwner().getPackageName())
                    .setUid(mContext.getUserId())
                    .setHasAppInfoLink(false)
                    .setButtonActions(EntityHeaderController.ActionType.ACTION_DND_RULE_PREFERENCE,
                            EntityHeaderController.ActionType.ACTION_NONE)
                    .done(mFragment.getActivity(), mContext);

            pref.findViewById(R.id.entity_header).setVisibility(View.VISIBLE);
        }
    }

    private Drawable getIcon() {
        try {
            PackageManager packageManager =  mContext.getPackageManager();
            ApplicationInfo info = packageManager.getApplicationInfo(
                    mRule.getOwner().getPackageName(), 0);
            if (info.isSystemApp()) {
                if (ZenModeConfig.isValidScheduleConditionId(mRule.getConditionId())) {
                    return mContext.getDrawable(R.drawable.ic_timelapse);
                } else if (ZenModeConfig.isValidEventConditionId(mRule.getConditionId())) {
                    return mContext.getDrawable(R.drawable.ic_event);
                }
            }
            return info.loadIcon(packageManager);
        } catch (PackageManager.NameNotFoundException e) {
           Slog.w(TAG, "Unable to load icon - PackageManager.NameNotFoundException");
        }

        return null;
    }

    protected void onResume(AutomaticZenRule rule, String id) {
        mRule = rule;
        mId = id;
    }

    public class RuleNameChangeListener implements ZenRuleNameDialog.PositiveClickListener {
        public RuleNameChangeListener() {}

        @Override
        public void onOk(String ruleName, Fragment parent) {
            mMetricsFeatureProvider.action(mContext,
                    MetricsProto.MetricsEvent.ACTION_ZEN_MODE_RULE_NAME_CHANGE_OK);
            mRule.setName(ruleName);
            mBackend.setZenRule(mId, mRule);
        }
    }
}
