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

package com.android.settings.wifi.details2;

import android.content.Context;
import android.text.TextUtils;

import com.android.settings.core.BasePreferenceController;
import com.android.wifitrackerlib.WifiEntry;

/**
 * {@link BasePreferenceController} that display the second summary. If users click the preference,
 * @link ClickableSpan#onClick} of the first {@link ClickableSpan} in the summary will be called.
 */
public class WifiSecondSummaryController2 extends BasePreferenceController {

    private static final String KEY_WIFI_SECOND_SUMMARY = "second_summary";
    private CharSequence mSecondSummary;

    public WifiSecondSummaryController2(Context context) {
        super(context, KEY_WIFI_SECOND_SUMMARY);
    }

    public void setWifiEntry(WifiEntry wifiEntry) {
        mSecondSummary = wifiEntry.getSecondSummary();
    }

    @Override
    public int getAvailabilityStatus() {
        return TextUtils.isEmpty(mSecondSummary) ? CONDITIONALLY_UNAVAILABLE : AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        return mSecondSummary;
    }
}
