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

package com.android.settings.deviceinfo.imei;

import android.app.Fragment;
import android.content.Context;

import com.android.settings.R;

public class ImeiInfoDualSimPreferenceController extends AbstractImeiInfoPreferenceController {

    private static final String KEY_IMEI_INFO_DUAL_SIM = "imei_info_sim_slot_2";
    private static final int SIM_SLOT = 1;

    public ImeiInfoDualSimPreferenceController(Context context, Fragment fragment) {
        super(context, fragment);
    }

    @Override
    public boolean isAvailable() {
        return super.isAvailable() && mIsMultiSim;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_IMEI_INFO_DUAL_SIM;
    }

    @Override
    protected String getTitleForCdmaPhone() {
        return mContext.getResources().getString(R.string.meid_multi_sim_sim_slot_2);
    }

    @Override
    protected String getTitleForGsmPhone() {
        return mContext.getResources().getString(R.string.imei_multi_sim_slot_2);
    }

    @Override
    protected int getSimSlot() {
        return SIM_SLOT;
    }
}
