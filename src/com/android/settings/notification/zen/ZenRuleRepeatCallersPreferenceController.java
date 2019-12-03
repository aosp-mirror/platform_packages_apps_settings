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
import android.util.Log;
import android.util.Pair;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class ZenRuleRepeatCallersPreferenceController extends
        AbstractZenCustomRulePreferenceController implements Preference.OnPreferenceChangeListener {

    private final int mRepeatCallersThreshold;

    public ZenRuleRepeatCallersPreferenceController(Context context,
            String key, Lifecycle lifecycle, int repeatCallersThreshold) {
        super(context, key, lifecycle);
        mRepeatCallersThreshold = repeatCallersThreshold;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        setRepeatCallerSummary(screen.findPreference(KEY));
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (mRule == null || mRule.getZenPolicy() == null) {
            return;
        }

        SwitchPreference pref = (SwitchPreference) preference;
        boolean anyCallersCanBypassDnd = mRule.getZenPolicy().getPriorityCallSenders()
                == ZenPolicy.PEOPLE_TYPE_ANYONE;

        // if any caller can bypass dnd then repeat callers preference is disabled
        if (anyCallersCanBypassDnd) {
            pref.setEnabled(false);
            pref.setChecked(true);
        } else {
            pref.setEnabled(true);
            pref.setChecked(mRule.getZenPolicy().getPriorityCategoryRepeatCallers()
                    == ZenPolicy.STATE_ALLOW);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean allow = (Boolean) newValue;
        if (ZenModeSettingsBase.DEBUG) {
            Log.d(TAG, KEY + " onPrefChange mRule=" + mRule + " mCategory="
                    + ZenPolicy.PRIORITY_CATEGORY_REPEAT_CALLERS + " allow=" + allow);
        }
        mMetricsFeatureProvider.action(mContext, SettingsEnums.ACTION_ZEN_ALLOW_REPEAT_CALLS,
                Pair.create(MetricsProto.MetricsEvent.FIELD_ZEN_TOGGLE_EXCEPTION, allow ? 1 : 0),
                Pair.create(MetricsProto.MetricsEvent.FIELD_ZEN_RULE_ID, mId));
        mRule.setZenPolicy(new ZenPolicy.Builder(mRule.getZenPolicy())
                .allowRepeatCallers(allow)
                .build());
        mBackend.updateZenRule(mId, mRule);
        return true;
    }

    private void setRepeatCallerSummary(Preference preference) {
        preference.setSummary(mContext.getString(
                com.android.settings.R.string.zen_mode_repeat_callers_summary,
                mRepeatCallersThreshold));
    }
}
