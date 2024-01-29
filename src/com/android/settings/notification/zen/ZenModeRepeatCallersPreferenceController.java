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

import android.app.NotificationManager.Policy;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class ZenModeRepeatCallersPreferenceController extends AbstractZenModePreferenceController
        implements Preference.OnPreferenceChangeListener {

    protected static final String KEY = "zen_mode_repeat_callers";

    private final ZenModeBackend mBackend;
    private final int mRepeatCallersThreshold;

    public ZenModeRepeatCallersPreferenceController(Context context, Lifecycle lifecycle,
            int repeatCallersThreshold) {
        super(context, KEY, lifecycle);

        mRepeatCallersThreshold = repeatCallersThreshold;
        mBackend = ZenModeBackend.getInstance(context);
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        setRepeatCallerSummary(screen.findPreference(KEY));
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        TwoStatePreference pref = (TwoStatePreference) preference;
        switch (getZenMode()) {
            case Settings.Global.ZEN_MODE_NO_INTERRUPTIONS:
            case Settings.Global.ZEN_MODE_ALARMS:
                pref.setEnabled(false);
                pref.setChecked(false);
                break;
            default:
                boolean anyCallersCanBypassDnd = (mBackend.isPriorityCategoryEnabled(
                        Policy.PRIORITY_CATEGORY_CALLS)
                        && mBackend.getPriorityCallSenders() == Policy.PRIORITY_SENDERS_ANY);
                // if any caller can bypass dnd then repeat callers preference is disabled
                if (anyCallersCanBypassDnd) {
                    pref.setEnabled(false);
                    pref.setChecked(true);
                } else {
                    pref.setEnabled(true);
                    pref.setChecked(mBackend.isPriorityCategoryEnabled(
                            Policy.PRIORITY_CATEGORY_REPEAT_CALLERS));
                }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean allowRepeatCallers = (Boolean) newValue;
        if (ZenModeSettingsBase.DEBUG) {
            Log.d(TAG, "onPrefChange allowRepeatCallers=" + allowRepeatCallers);
        }
        mMetricsFeatureProvider.action(mContext,
                SettingsEnums.ACTION_ZEN_ALLOW_REPEAT_CALLS, allowRepeatCallers);
        mBackend.saveSoundPolicy(Policy.PRIORITY_CATEGORY_REPEAT_CALLERS, allowRepeatCallers);
        return true;
    }

    private void setRepeatCallerSummary(Preference preference) {
        preference.setSummary(mContext.getString(R.string.zen_mode_repeat_callers_summary,
                mRepeatCallersThreshold));
    }
}
