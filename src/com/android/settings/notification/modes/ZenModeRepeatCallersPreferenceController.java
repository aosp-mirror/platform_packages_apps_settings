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

import static android.service.notification.ZenPolicy.PEOPLE_TYPE_ANYONE;
import static android.service.notification.ZenPolicy.PEOPLE_TYPE_NONE;
import static android.service.notification.ZenPolicy.STATE_ALLOW;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.provider.Settings;
import android.service.notification.ZenPolicy;
import android.util.Log;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;
import com.android.settings.R;

public class ZenModeRepeatCallersPreferenceController extends AbstractZenModePreferenceController
        implements Preference.OnPreferenceChangeListener {

    private final int mRepeatCallersThreshold;

    public ZenModeRepeatCallersPreferenceController(Context context,
            String key, ZenModesBackend backend, int repeatCallersThreshold) {
        super(context, key, backend);

        mRepeatCallersThreshold = repeatCallersThreshold;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        TwoStatePreference pref = (TwoStatePreference) preference;

        boolean anyCallersCanBypassDnd =
                getMode().getPolicy().getPriorityCategoryCalls() == STATE_ALLOW
                && getMode().getPolicy().getPriorityCallSenders() == PEOPLE_TYPE_ANYONE;
        // if any caller can bypass dnd then repeat callers preference is disabled
        if (anyCallersCanBypassDnd) {
            pref.setEnabled(false);
            pref.setChecked(true);
        } else {
            pref.setEnabled(true);
            pref.setChecked(
                    getMode().getPolicy().getPriorityCategoryRepeatCallers() == STATE_ALLOW);
        }

        setRepeatCallerSummary(preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean allowRepeatCallers = (Boolean) newValue;
        ZenPolicy diffPolicy = new ZenPolicy.Builder()
                .allowRepeatCallers(allowRepeatCallers)
                .build();
        getMode().setPolicy(diffPolicy);
        mBackend.updateMode(getMode());
        return true;
    }

    private void setRepeatCallerSummary(Preference preference) {
        preference.setSummary(mContext.getString(R.string.zen_mode_repeat_callers_summary,
                mRepeatCallersThreshold));
    }
}
