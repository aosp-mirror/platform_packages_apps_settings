/*
 * Copyright (C) 2020 The Android Open Source Project
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
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.lifecycle.Lifecycle;

/**
 * Controls the summary for preference found at:
 *  Settings > Sound > Do Not Disturb > Alarms & other interruptions
 */
public class ZenModeSoundVibrationPreferenceController extends
        AbstractZenModePreferenceController implements PreferenceControllerMixin {
    private final String mKey;
    private final ZenModeSettings.SummaryBuilder mSummaryBuilder;

    public ZenModeSoundVibrationPreferenceController(Context context, Lifecycle lifecycle,
            String key) {
        super(context, key, lifecycle);
        mKey = key;
        mSummaryBuilder = new ZenModeSettings.SummaryBuilder(context);
    }

    @Override
    public String getPreferenceKey() {
        return mKey;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        switch (getZenMode()) {
            case Settings.Global.ZEN_MODE_NO_INTERRUPTIONS:
                preference.setEnabled(false);
                preference.setSummary(mContext.getString(R.string.zen_mode_other_sounds_none));
                break;
            case Settings.Global.ZEN_MODE_ALARMS:
                preference.setEnabled(false);
                preference.setSummary(mContext.getString(R.string.zen_mode_behavior_alarms_only));
                break;
            default:
                preference.setEnabled(true);
                preference.setSummary(mSummaryBuilder.getOtherSoundCategoriesSummary(getPolicy()));
        }
    }
}
