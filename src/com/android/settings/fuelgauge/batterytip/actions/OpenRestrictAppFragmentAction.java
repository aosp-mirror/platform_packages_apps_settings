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

package com.android.settings.fuelgauge.batterytip.actions;

import android.app.Fragment;

import com.android.settings.SettingsActivity;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.RestrictedAppDetails;
import com.android.settings.fuelgauge.batterytip.AppInfo;
import com.android.settings.fuelgauge.batterytip.tips.RestrictAppTip;

import java.util.List;

/**
 * Action to open the {@link com.android.settings.fuelgauge.RestrictedAppDetails}
 */
public class OpenRestrictAppFragmentAction extends BatteryTipAction {
    private final RestrictAppTip mRestrictAppTip;
    private final BatteryUtils mBatteryUtils;
    private final SettingsActivity mSettingsActivity;
    private final InstrumentedPreferenceFragment mFragment;

    public OpenRestrictAppFragmentAction(SettingsActivity settingsActivity,
            InstrumentedPreferenceFragment fragment, RestrictAppTip tip) {
        super(fragment.getContext());
        mSettingsActivity = settingsActivity;
        mFragment = fragment;
        mRestrictAppTip = tip;
        mBatteryUtils = BatteryUtils.getInstance(mContext);
    }

    /**
     * Handle the action when user clicks positive button
     */
    @Override
    public void handlePositiveAction() {
        final List<AppInfo> mAppInfos = mRestrictAppTip.getRestrictAppList();
        RestrictedAppDetails.startRestrictedAppDetails(mSettingsActivity, mFragment,
                mAppInfos);
    }
}
