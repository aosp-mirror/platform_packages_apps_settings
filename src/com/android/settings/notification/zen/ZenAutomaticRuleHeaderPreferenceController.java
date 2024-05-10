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

import static com.android.settings.widget.EntityHeaderController.PREF_KEY_APP_HEADER;

import android.app.AutomaticZenRule;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.service.notification.ZenModeConfig;
import android.util.Slog;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.LayoutPreference;

public class ZenAutomaticRuleHeaderPreferenceController extends AbstractZenModePreferenceController
        implements PreferenceControllerMixin {

    private final PreferenceFragmentCompat mFragment;
    private AutomaticZenRule mRule;
    private EntityHeaderController mController;

    public ZenAutomaticRuleHeaderPreferenceController(Context context,
            PreferenceFragmentCompat fragment, Lifecycle lifecycle) {
        super(context, PREF_KEY_APP_HEADER, lifecycle);
        mFragment = fragment;
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY_APP_HEADER;
    }

    void setRule(AutomaticZenRule rule) {
        mRule = rule;
    }

    @Override
    public boolean isAvailable() {
        return mRule != null;
    }

    @Override
    public void updateState(Preference preference) {
        if (mRule == null || mFragment == null) {
            return;
        }

        if (mController == null) {
            final LayoutPreference pref = (LayoutPreference) preference;
            mController = EntityHeaderController.newInstance(mFragment.getActivity(), mFragment,
                    pref.findViewById(R.id.entity_header));
        }

        mController.setIcon(getIcon())
                .setLabel(mRule.getName())
                .done(false /* rebindActions */);
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
}
