/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.datetime;

import static android.app.time.Capabilities.CAPABILITY_POSSESSED;

import android.app.time.TimeManager;
import android.app.time.TimeZoneCapabilities;
import android.content.Context;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.datetime.ZoneGetter;

import java.util.Calendar;

public class TimeZonePreferenceController extends BasePreferenceController {

    private final TimeManager mTimeManager;

    public TimeZonePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mTimeManager = context.getSystemService(TimeManager.class);
    }

    @Override
    public CharSequence getSummary() {
        return getTimeZoneOffsetAndName();
    }

    @Override
    public int getAvailabilityStatus() {
        return shouldEnableManualTimeZoneSelection() ? AVAILABLE : DISABLED_DEPENDENT_SETTING;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        if (preference instanceof RestrictedPreference
                && ((RestrictedPreference) preference).isDisabledByAdmin()) {
            return;
        }

        preference.setEnabled(shouldEnableManualTimeZoneSelection());
    }

    @VisibleForTesting
    CharSequence getTimeZoneOffsetAndName() {
        final Calendar now = Calendar.getInstance();
        return ZoneGetter.getTimeZoneOffsetAndName(mContext,
                now.getTimeZone(), now.getTime());
    }

    private boolean shouldEnableManualTimeZoneSelection() {
        TimeZoneCapabilities timeZoneCapabilities =
                mTimeManager.getTimeZoneCapabilitiesAndConfig().getCapabilities();
        int suggestManualTimeZoneCapability =
                timeZoneCapabilities.getSetManualTimeZoneCapability();
        return suggestManualTimeZoneCapability == CAPABILITY_POSSESSED;
    }
}
