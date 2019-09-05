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

package com.android.settings.datetime.timezone;

import android.content.Context;

import androidx.preference.Preference;

import com.android.settings.R;

public class RegionZonePreferenceController extends BaseTimeZonePreferenceController {
    private static final String PREFERENCE_KEY = "region_zone";

    private TimeZoneInfo mTimeZoneInfo;
    private boolean mIsClickable;

    public RegionZonePreferenceController(Context context) {
        super(context, PREFERENCE_KEY);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setEnabled(isClickable());
    }

    @Override
    public CharSequence getSummary() {
        return mTimeZoneInfo == null ? ""
                : SpannableUtil.getResourcesText(mContext.getResources(),
                        R.string.zone_info_exemplar_location_and_offset,
                mTimeZoneInfo.getExemplarLocation(), mTimeZoneInfo.getGmtOffset());
    }

    public void setTimeZoneInfo(TimeZoneInfo timeZoneInfo) {
        mTimeZoneInfo = timeZoneInfo;
    }

    public TimeZoneInfo getTimeZoneInfo() {
        return mTimeZoneInfo;
    }

    public void setClickable(boolean clickable) {
        mIsClickable = clickable;
    }

    public boolean isClickable() {
        return mIsClickable;
    }
}
