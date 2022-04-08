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
import android.service.notification.ZenPolicy;
import android.util.Pair;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class ZenRuleVisEffectsNonePreferenceController extends
        AbstractZenCustomRulePreferenceController implements PreferenceControllerMixin {

    private ZenCustomRadioButtonPreference mPreference;

    public ZenRuleVisEffectsNonePreferenceController(Context context, Lifecycle lifecycle,
            String key) {
        super(context, key, lifecycle);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());

        mPreference.setOnRadioButtonClickListener(p -> {
            mMetricsFeatureProvider.action(mContext, SettingsEnums.ACTION_ZEN_SOUND_AND_VIS_EFFECTS,
                    Pair.create(MetricsProto.MetricsEvent.FIELD_ZEN_RULE_ID, mId));
            mRule.setZenPolicy(new ZenPolicy.Builder(mRule.getZenPolicy())
                    .hideAllVisualEffects()
                    .build());
            mBackend.updateZenRule(mId, mRule);
        });
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (mId == null || mRule == null || mRule.getZenPolicy() == null) {
            return;
        }

        mPreference.setChecked(mRule.getZenPolicy().shouldHideAllVisualEffects());
    }
}