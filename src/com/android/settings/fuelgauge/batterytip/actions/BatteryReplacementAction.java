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

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.android.settings.R;
import com.android.settingslib.HelpUtils;

/**
 * Action to know details about battery replacement tip.
 */
public class BatteryReplacementAction extends BatteryTipAction {
    private static final String TAG = "BatteryReplacementAction";

    public BatteryReplacementAction(Context context) {
        super(context);
    }

    @Override
    public void handlePositiveAction(int metricsKey) {
        final Intent helpIntent = HelpUtils.getHelpIntent(mContext,
                mContext.getString(R.string.help_url_battery_replacement), /* backupContext */ "");
        if (helpIntent == null) {
            return;
        }

        try {
            mContext.startActivity(helpIntent);
        } catch (Exception e) {
            Log.w(TAG, "can't start action: " + helpIntent, e);
        }
    }
}
