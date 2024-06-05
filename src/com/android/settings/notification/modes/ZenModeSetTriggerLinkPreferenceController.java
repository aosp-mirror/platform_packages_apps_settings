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

import static com.android.settings.notification.modes.ZenModeFragmentBase.MODE_ID;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.PrimarySwitchPreference;

/**
 * Preference controller for the link
 */
public class ZenModeSetTriggerLinkPreferenceController extends AbstractZenModePreferenceController {
    @VisibleForTesting
    protected static final String AUTOMATIC_TRIGGER_PREF_KEY = "zen_automatic_trigger_settings";

    public ZenModeSetTriggerLinkPreferenceController(Context context, String key,
            ZenModesBackend backend) {
        super(context, key, backend);
    }

    @Override
    public boolean isAvailable(@NonNull ZenMode zenMode) {
        return !zenMode.isManualDnd();
    }

    @Override
    public void updateState(Preference preference, @NonNull ZenMode zenMode) {
        // This controller is expected to govern a preference category so that it controls the
        // availability of the entire preference category if the mode doesn't have a way to
        // automatically trigger (such as manual DND).
        Preference switchPref = ((PreferenceCategory) preference).findPreference(
                AUTOMATIC_TRIGGER_PREF_KEY);
        if (switchPref == null) {
            return;
        }
        ((PrimarySwitchPreference) switchPref).setChecked(zenMode.getRule().isEnabled());
        switchPref.setOnPreferenceChangeListener(mSwitchChangeListener);

        Bundle bundle = new Bundle();
        bundle.putString(MODE_ID, zenMode.getId());

        // TODO: b/341961712 - direct preference to app-owned intent if available
        switch (zenMode.getRule().getType()) {
            case TYPE_SCHEDULE_CALENDAR:
                switchPref.setTitle(R.string.zen_mode_set_calendar_link);
                switchPref.setSummary(zenMode.getRule().getTriggerDescription());
                switchPref.setIntent(new SubSettingLauncher(mContext)
                        .setDestination(ZenModeSetCalendarFragment.class.getName())
                        // TODO: b/332937635 - set correct metrics category
                        .setSourceMetricsCategory(0)
                        .setArguments(bundle)
                        .toIntent());
                break;
            default:
                // TODO: b/342156843 - change this to allow adding a trigger condition for system
                //                     rules that don't yet have a type selected
                switchPref.setTitle("not implemented");
        }
    }

    @VisibleForTesting
    protected Preference.OnPreferenceChangeListener mSwitchChangeListener = (p, newValue) -> {
        final boolean newEnabled = (Boolean) newValue;
        return saveMode((zenMode) -> {
            if (newEnabled != zenMode.getRule().isEnabled()) {
                zenMode.getRule().setEnabled(newEnabled);
            }
            return zenMode;
        });
    };
}
