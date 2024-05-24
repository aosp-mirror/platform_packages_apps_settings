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

import static com.android.settings.notification.modes.ZenModeFragmentBase.MODE_ID;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.service.notification.ZenPolicy;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

public class ZenModeAppsPreferenceController extends
        AbstractZenModePreferenceController implements Preference.OnPreferenceChangeListener {

    static final String KEY_PRIORITY = "zen_mode_apps_priority";
    static final String KEY_NONE = "zen_mode_apps_none";
    static final String KEY_ALL = "zen_mode_apps_all";

    String mModeId;


    public ZenModeAppsPreferenceController(@NonNull Context context,
            @NonNull String key, @Nullable ZenModesBackend backend) {
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
            case KEY_ALL:
                // A UI-only setting; the underlying policy never actually has this value,
                // but ZenMode acts as though it does for the sake of UI consistency.
                boolean policy_all = zenMode.getPolicy().getAllowedChannels()
                        == ZenMode.CHANNEL_POLICY_ALL;
                pref.setChecked(policy_all);
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
            case KEY_ALL:
                return savePolicy(p -> p.allowChannels(ZenMode.CHANNEL_POLICY_ALL));
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
            bundle.putString(MODE_ID, mModeId);
        }
        // TODO(b/332937635): Update metrics category
        new SubSettingLauncher(mContext)
                .setDestination(ZenModeSelectBypassingAppsFragment.class.getName())
                .setSourceMetricsCategory(SettingsEnums.SETTINGS_ZEN_NOTIFICATIONS)
                .setArguments(bundle)
                .launch();
    }
}
