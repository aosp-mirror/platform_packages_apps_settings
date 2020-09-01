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

import android.content.Context;
import android.service.notification.ZenPolicy;
import android.util.Log;
import android.util.Pair;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class ZenRuleCustomSwitchPreferenceController extends
        AbstractZenCustomRulePreferenceController implements Preference.OnPreferenceChangeListener {

    private @ZenPolicy.PriorityCategory int mCategory;
    private int mMetricsCategory;

    public ZenRuleCustomSwitchPreferenceController(Context context, Lifecycle lifecycle,
            String key, @ZenPolicy.PriorityCategory int category, int metricsCategory) {
        super(context, key, lifecycle);
        mCategory = category;
        mMetricsCategory = metricsCategory;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (mRule == null || mRule.getZenPolicy() == null) {
            return;
        }

        SwitchPreference pref = (SwitchPreference) preference;
        pref.setChecked(mRule.getZenPolicy().isCategoryAllowed(mCategory, false));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean allow = (Boolean) newValue;
        if (ZenModeSettingsBase.DEBUG) {
            Log.d(TAG, KEY + " onPrefChange mRule=" + mRule + " mCategory=" + mCategory
                    + " allow=" + allow);
        }
        mMetricsFeatureProvider.action(mContext, mMetricsCategory,
                Pair.create(MetricsProto.MetricsEvent.FIELD_ZEN_TOGGLE_EXCEPTION, allow ? 1 : 0),
                Pair.create(MetricsProto.MetricsEvent.FIELD_ZEN_RULE_ID, mId));
        mRule.setZenPolicy(new ZenPolicy.Builder(mRule.getZenPolicy())
                .allowCategory(mCategory, allow)
                .build());
        mBackend.updateZenRule(mId, mRule);
        return true;
    }
}
