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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.util.Pair;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class ZenRuleVisEffectsCustomPreferenceController extends
        AbstractZenCustomRulePreferenceController implements PreferenceControllerMixin {

    private ZenCustomRadioButtonPreference mPreference;

    public ZenRuleVisEffectsCustomPreferenceController(Context context, Lifecycle lifecycle,
            String key) {
        super(context, key, lifecycle);
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
        if (mId == null || mRule == null || mRule.getZenPolicy() == null) {
            return;
        }

        mPreference.setChecked(!mRule.getZenPolicy().shouldHideAllVisualEffects()
                && !mRule.getZenPolicy().shouldShowAllVisualEffects());
    }

    private void launchCustomSettings() {
        mMetricsFeatureProvider.action(mContext, SettingsEnums.ACTION_ZEN_SHOW_CUSTOM,
                Pair.create(MetricsProto.MetricsEvent.FIELD_ZEN_RULE_ID, mId));
        new SubSettingLauncher(mContext)
                .setDestination(ZenCustomRuleBlockedEffectsSettings.class.getName())
                .setArguments(createBundle())
                .setSourceMetricsCategory(SettingsEnums.ZEN_CUSTOM_RULE_VIS_EFFECTS)
                .launch();
    }
}