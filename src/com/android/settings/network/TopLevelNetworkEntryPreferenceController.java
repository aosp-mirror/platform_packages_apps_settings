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

package com.android.settings.network;

import android.content.Context;
import android.icu.text.ListFormatter;
import android.text.BidiFormatter;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.wifi.WifiMasterSwitchPreferenceController;

import java.util.ArrayList;
import java.util.List;

public class TopLevelNetworkEntryPreferenceController extends BasePreferenceController {

    private final WifiMasterSwitchPreferenceController mWifiPreferenceController;
    private final MobileNetworkPreferenceController mMobileNetworkPreferenceController;
    private final TetherPreferenceController mTetherPreferenceController;

    public TopLevelNetworkEntryPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mMobileNetworkPreferenceController = new MobileNetworkPreferenceController(mContext);
        mTetherPreferenceController = new TetherPreferenceController(
                mContext, null /* lifecycle */);
        mWifiPreferenceController = new WifiMasterSwitchPreferenceController(
                mContext, null /* metrics */);
    }

    @Override
    public int getAvailabilityStatus() {
        return Utils.isDemoUser(mContext) ? UNSUPPORTED_ON_DEVICE : AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        final String wifiSummary = BidiFormatter.getInstance()
                .unicodeWrap(mContext.getString(R.string.wifi_settings_title));
        final String mobileSummary = mContext.getString(
                R.string.network_dashboard_summary_mobile);
        final String dataUsageSummary = mContext.getString(
                R.string.network_dashboard_summary_data_usage);
        final String hotspotSummary = mContext.getString(
                R.string.network_dashboard_summary_hotspot);

        final List<String> summaries = new ArrayList<>();
        if (mWifiPreferenceController.isAvailable()
                && !TextUtils.isEmpty(wifiSummary)) {
            summaries.add(wifiSummary);
        }
        if (mMobileNetworkPreferenceController.isAvailable() && !TextUtils.isEmpty(mobileSummary)) {
            summaries.add(mobileSummary);
        }
        if (!TextUtils.isEmpty(dataUsageSummary)) {
            summaries.add(dataUsageSummary);
        }
        if (mTetherPreferenceController.isAvailable()
                && !TextUtils.isEmpty(hotspotSummary)) {
            summaries.add(hotspotSummary);
        }
        return ListFormatter.getInstance().format(summaries);
    }
}
