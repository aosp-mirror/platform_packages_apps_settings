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

import static android.provider.Settings.EXTRA_AUTOMATIC_ZEN_RULE_ID;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.service.notification.ZenPolicy;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

public class ZenModeAppsPreferenceController extends
        AbstractZenModePreferenceController implements Preference.OnPreferenceChangeListener {

    static final String KEY_PRIORITY = "zen_mode_apps_priority";
    static final String KEY_NONE = "zen_mode_apps_none";

    String mModeId;

    public ZenModeAppsPreferenceController(@NonNull Context context,
            @NonNull String key, @NonNull ZenModesBackend backend) {
        super(context, key, backend);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        SelectorWithWidgetPreference pref = screen.findPreference(getPreferenceKey());
        if (pref != null) {
            pref.setOnClickListener(mSelectorClickListener);

            // Adds the widget to only the priority category.
            if (getPreferenceKey().equals(KEY_PRIORITY)) {
                pref.setExtraWidgetOnClickListener(p -> {
                    launchPrioritySettings();
                });
            }
        }
        super.displayPreference(screen);
    }

    @Override
    public void updateState(Preference preference, @NonNull ZenMode zenMode) {
        mModeId = zenMode.getId();
        TwoStatePreference pref = (TwoStatePreference) preference;
        switch (getPreferenceKey()) {
            case KEY_PRIORITY:
                boolean policy_priority = zenMode.getPolicy().getAllowedChannels()
                        == ZenPolicy.CHANNEL_POLICY_PRIORITY;
                pref.setChecked(policy_priority);
                break;
            case KEY_NONE:
                boolean policy_none = zenMode.getPolicy().getAllowedChannels()
                        == ZenPolicy.CHANNEL_POLICY_NONE;
                pref.setChecked(policy_none);
                break;
        }
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        switch (getPreferenceKey()) {
            case KEY_PRIORITY:
                return savePolicy(p -> p.allowChannels(ZenPolicy.CHANNEL_POLICY_PRIORITY));
            case KEY_NONE:
                return savePolicy(p -> p.allowChannels(ZenPolicy.CHANNEL_POLICY_NONE));
        }
        return true;
    }

    @VisibleForTesting
    SelectorWithWidgetPreference.OnClickListener mSelectorClickListener =
            new SelectorWithWidgetPreference.OnClickListener() {
                @Override
                public void onRadioButtonClicked(SelectorWithWidgetPreference preference) {
                    onPreferenceChange(preference, true);
                }
            };

    private void launchPrioritySettings() {
        Bundle bundle = new Bundle();
        if (mModeId != null) {
            bundle.putString(EXTRA_AUTOMATIC_ZEN_RULE_ID, mModeId);
        }
        new SubSettingLauncher(mContext)
                .setDestination(ZenModeSelectBypassingAppsFragment.class.getName())
                .setSourceMetricsCategory(SettingsEnums.NOTIFICATION_ZEN_MODE_OVERRIDING_APPS)
                .setArguments(bundle)
                .launch();
    }
}
