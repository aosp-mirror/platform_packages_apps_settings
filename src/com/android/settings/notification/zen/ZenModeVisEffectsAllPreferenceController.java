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

import android.app.NotificationManager.Policy;
import android.app.settings.SettingsEnums;
import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.core.lifecycle.Lifecycle;

public class ZenModeVisEffectsAllPreferenceController
        extends AbstractZenModePreferenceController
        implements ZenCustomRadioButtonPreference.OnRadioButtonClickListener {

    private ZenCustomRadioButtonPreference mPreference;

    protected static final int EFFECTS = Policy.SUPPRESSED_EFFECT_SCREEN_OFF
            | Policy.SUPPRESSED_EFFECT_SCREEN_ON
            | Policy.SUPPRESSED_EFFECT_FULL_SCREEN_INTENT
            | Policy.SUPPRESSED_EFFECT_LIGHTS
            | Policy.SUPPRESSED_EFFECT_PEEK
            | Policy.SUPPRESSED_EFFECT_STATUS_BAR
            | Policy.SUPPRESSED_EFFECT_BADGE
            | Policy.SUPPRESSED_EFFECT_AMBIENT
            | Policy.SUPPRESSED_EFFECT_NOTIFICATION_LIST;

    public ZenModeVisEffectsAllPreferenceController(Context context, Lifecycle lifecycle,
            String key) {
        super(context, key, lifecycle);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        mPreference.setOnRadioButtonClickListener(this);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        boolean everythingBlocked = Policy.areAllVisualEffectsSuppressed(
                mBackend.mPolicy.suppressedVisualEffects);
        mPreference.setChecked(everythingBlocked);
    }

    @Override
    public void onRadioButtonClick(ZenCustomRadioButtonPreference p) {
        mMetricsFeatureProvider.action(mContext,
                SettingsEnums.ACTION_ZEN_SOUND_AND_VIS_EFFECTS, true);
        mBackend.saveVisualEffectsPolicy(EFFECTS, true);
    }
}
