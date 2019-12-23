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

package com.android.settings.network.telephony.cdma;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;

import androidx.preference.ListPreference;

/**
 * {@link ListPreference} that will launch ECM dialog when in ECM mode
 */
public class CdmaListPreference extends ListPreference {
    private TelephonyManager mTelephonyManager;

    public CdmaListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onClick() {
        // Only show dialog when it is not in ECM
        if (mTelephonyManager == null || !mTelephonyManager.getEmergencyCallbackMode()) {
            super.onClick();
        }
    }

    public void setSubId(int subId) {
        mTelephonyManager = getContext().getSystemService(TelephonyManager.class)
                .createForSubscriptionId(subId);
    }
}
