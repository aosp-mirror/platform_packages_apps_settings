/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.deviceinfo.batteryinfo;

import android.content.Context;
import android.os.BatteryManager;

import java.text.DateFormat;
import java.util.Date;

/** Implementation of {@code BatteryInfoFeatureProvider} */
public class BatteryInfoFeatureProviderImpl implements BatteryInfoFeatureProvider {

    private BatteryManager mBatteryManager;
    private Context mContext;
    private long mManufactureDateInSec;
    private long mFirstUseDateInSec;

    public BatteryInfoFeatureProviderImpl(Context context) {
        mContext = context;
        mBatteryManager = mContext.getSystemService(BatteryManager.class);
    }

    @Override
    public boolean isManufactureDateAvailable() {
        return false;
    }

    @Override
    public boolean isFirstUseDateAvailable() {
        return false;
    }

    @Override
    public CharSequence getManufactureDateSummary() {
        if (!isManufactureDateAvailable()) {
            return null;
        }
        final long manufactureDateInSec = getManufactureDate();

        return getFormattedDate(manufactureDateInSec * 1000L);
    }

    @Override
    public CharSequence getFirstUseDateSummary() {
        if (!isFirstUseDateAvailable()) {
            return null;
        }
        final long firstUseDateInSec = getFirstUseDate();

        return getFormattedDate(firstUseDateInSec * 1000L);
    }

    protected long getManufactureDate() {
        if (mManufactureDateInSec == 0L) {
            mManufactureDateInSec = mBatteryManager.getLongProperty(
                    BatteryManager.BATTERY_PROPERTY_MANUFACTURING_DATE);
        }
        return mManufactureDateInSec;
    }

    protected long getFirstUseDate() {
        if (mFirstUseDateInSec == 0L) {
            mFirstUseDateInSec = mBatteryManager.getLongProperty(
                    BatteryManager.BATTERY_PROPERTY_FIRST_USAGE_DATE);
        }
        return mFirstUseDateInSec;
    }

    private CharSequence getFormattedDate(long dateInMs) {
        final Date date = new Date(dateInMs);
        final CharSequence formattedDate =
                DateFormat.getDateInstance(DateFormat.LONG).format(date.getTime());

        return formattedDate;
    }
}
