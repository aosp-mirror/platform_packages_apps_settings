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

import android.app.NotificationManager;
import android.app.settings.SettingsEnums;
import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class ZenModeVisEffectsCustomPreferenceController
        extends AbstractZenModePreferenceController {

    private ZenCustomRadioButtonPreference mPreference;

    protected static final int INTERRUPTIVE_EFFECTS =
            NotificationManager.Policy.SUPPRESSED_EFFECT_AMBIENT
            | NotificationManager.Policy.SUPPRESSED_EFFECT_PEEK
            | NotificationManager.Policy.SUPPRESSED_EFFECT_LIGHTS
            | NotificationManager.Policy.SUPPRESSED_EFFECT_FULL_SCREEN_INTENT;

    public ZenModeVisEffectsCustomPreferenceController(Context context, Lifecycle lifecycle,
            String key) {
        super(context, key, lifecycle);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());

        mPreference.setOnGearClickListener(p -> {
            launchCustomSettings();

        });

        mPreference.setOnRadioButtonClickListener(p -> {
            launchCustomSettings();
        });
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        mPreference.setChecked(areCustomOptionsSelected());
    }

    protected boolean areCustomOptionsSelected() {
        boolean allEffectsSuppressed =
                NotificationManager.Policy.areAllVisualEffectsSuppressed(
                        mBackend.mPolicy.suppressedVisualEffects);
        boolean noEffectsSuppressed = mBackend.mPolicy.suppressedVisualEffects == 0;

        return !(allEffectsSuppressed || noEffectsSuppressed);
    }

    protected void select() {
        mMetricsFeatureProvider.action(mContext,
                SettingsEnums.ACTION_ZEN_CUSTOM, true);
    }

    private void launchCustomSettings() {
        select();
        new SubSettingLauncher(mContext)
                .setDestination(ZenModeBlockedEffectsSettings.class.getName())
                .setTitleRes(R.string.zen_mode_what_to_block_title)
                .setSourceMetricsCategory(SettingsEnums.SETTINGS_ZEN_NOTIFICATIONS)
                .launch();
    }
}