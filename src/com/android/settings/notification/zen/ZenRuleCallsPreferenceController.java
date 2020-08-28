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
import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class ZenRuleCallsPreferenceController extends AbstractZenCustomRulePreferenceController
        implements Preference.OnPreferenceChangeListener {

    private final String[] mListValues;

    public ZenRuleCallsPreferenceController(Context context, String key, Lifecycle lifecycle) {
        super(context, key, lifecycle);
        mListValues = context.getResources().getStringArray(
                com.android.settings.R.array.zen_mode_contacts_values);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        updateFromContactsValue(preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object selectedContactsFrom) {
        int allowCalls = ZenModeBackend.getZenPolicySettingFromPrefKey(
                selectedContactsFrom.toString());
        mMetricsFeatureProvider.action(mContext, SettingsEnums.ACTION_ZEN_ALLOW_CALLS,
                Pair.create(MetricsProto.MetricsEvent.FIELD_ZEN_TOGGLE_EXCEPTION, allowCalls),
                Pair.create(MetricsProto.MetricsEvent.FIELD_ZEN_RULE_ID, mId));
        mRule.setZenPolicy(new ZenPolicy.Builder(mRule.getZenPolicy())
                .allowCalls(allowCalls)
                .build());
        mBackend.updateZenRule(mId, mRule);
        updateFromContactsValue(preference);
        return true;
    }

    private void updateFromContactsValue(Preference preference) {
        if (mRule == null || mRule.getZenPolicy() == null) {
            return;
        }
        ListPreference listPreference = (ListPreference) preference;
        listPreference.setSummary(mBackend.getContactsCallsSummary(mRule.getZenPolicy()));
        final String currentVal = ZenModeBackend.getKeyFromZenPolicySetting(
                mRule.getZenPolicy().getPriorityCallSenders());
        listPreference.setValue(mListValues[getIndexOfSendersValue(currentVal)]);

    }

    @VisibleForTesting
    protected int getIndexOfSendersValue(String currentVal) {
        int index = 3; // defaults to "none" based on R.array.zen_mode_contacts_values
        for (int i = 0; i < mListValues.length; i++) {
            if (TextUtils.equals(currentVal, mListValues[i])) {
                return i;
            }
        }

        return index;
    }
}
