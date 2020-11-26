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

package com.android.settings.network.telephony;

import android.content.Context;

/**
 * Preference controller for "Cross SIM Calling"
 **/
public class CrossSimCallingPreferenceController extends TelephonyTogglePreferenceController {

    /**
     * Class constructor of cross sim calling.
     *
     * @param context of settings
     * @param key assigned within UI entry of XML file
     **/
    public CrossSimCallingPreferenceController(Context context, String key) {
        super(context, key);
    }

    /**
     * Initialization based on given subscription id.
     *
     * @param subId is the subscription id
     * @return this instance after initialization
     **/
    public CrossSimCallingPreferenceController init(int subId) {
        return this;
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        return CONDITIONALLY_UNAVAILABLE;
    }

    /**
     * Implementation of abstract methods
     **/
    public boolean setChecked(boolean isChecked) {
        return false;
    }

    /**
     * Implementation of abstract methods
     **/
    public boolean isChecked() {
        return false;
    }

}
