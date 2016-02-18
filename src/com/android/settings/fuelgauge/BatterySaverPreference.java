/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.fuelgauge;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.support.v7.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import com.android.settings.R;
import com.android.settings.Utils;

public class BatterySaverPreference extends Preference {

    private PowerManager mPowerManager;

    public BatterySaverPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void performClick(View view) {
        Utils.startWithFragment(getContext(), getFragment(), null, null, 0, 0, getTitle());
    }

    @Override
    public void onAttached() {
        super.onAttached();
        mPowerManager = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
        mObserver.onChange(true);
        getContext().getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(Global.LOW_POWER_MODE_TRIGGER_LEVEL), true, mObserver);
        getContext().getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(Global.LOW_POWER_MODE), true, mObserver);
    }

    @Override
    public void onDetached() {
        super.onDetached();
        getContext().getContentResolver().unregisterContentObserver(mObserver);
    }

    private void updateSwitch() {
        final Context context = getContext();
        final boolean mode = mPowerManager.isPowerSaveMode();
        int format = mode ? R.string.battery_saver_on_summary
                : R.string.battery_saver_off_summary;
        int percent = Settings.Global.getInt(context.getContentResolver(),
                Global.LOW_POWER_MODE_TRIGGER_LEVEL, 0);
        int percentFormat = percent > 0 ? R.string.battery_saver_desc_turn_on_auto_pct
                : R.string.battery_saver_desc_turn_on_auto_never;
        setSummary(context.getString(format, context.getString(percentFormat,
                Utils.formatPercentage(percent))));
    }

    private final ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            updateSwitch();
        }
    };

}
