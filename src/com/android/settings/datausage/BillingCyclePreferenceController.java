/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.datausage;

import android.content.Context;
import android.net.NetworkTemplate;

import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.datausage.lib.DataUsageLib;

public class BillingCyclePreferenceController extends BasePreferenceController {
    private int mSubscriptionId;

    public BillingCyclePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    public void init(int subscriptionId) {
        mSubscriptionId = subscriptionId;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        BillingCyclePreference preference = screen.findPreference(getPreferenceKey());

        NetworkTemplate template = DataUsageLib.getMobileTemplate(mContext, mSubscriptionId);

        preference.setTemplate(template, mSubscriptionId);
    }

    @Override
    public int getAvailabilityStatus() {
        return DataUsageUtils.hasMobileData(mContext) ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }
}
