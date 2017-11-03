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

package com.android.settings.deviceinfo.simstatus;

import android.app.Fragment;
import android.content.Context;

import com.android.settings.R;

public class SimStatusPreferenceControllerV2 extends AbstractSimStatusPreferenceController {

    public static final int SIM_SLOT = 0;

    private static final String KEY_SIM_1_STATUS = "sim_status_sim_1";

    public SimStatusPreferenceControllerV2(Context context, Fragment fragment) {
        super(context, fragment);
    }

    @Override
    protected String getPreferenceTitle() {
        return mIsMultiSim ? mContext.getResources().getString(R.string.sim_status_title_sim_slot_1)
                : mContext.getResources().getString(R.string.sim_status_title);
    }

    @Override
    protected int getSimSlot() {
        return SIM_SLOT;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_SIM_1_STATUS;
    }
}
