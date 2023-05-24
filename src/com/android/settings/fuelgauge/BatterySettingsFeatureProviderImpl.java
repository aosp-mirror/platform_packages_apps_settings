/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.fuelgauge;

import android.content.Context;
import android.os.BatteryManager;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.concurrent.TimeUnit;

/** Feature provider implementation for battery settings usage. */
public class BatterySettingsFeatureProviderImpl implements BatterySettingsFeatureProvider {

    protected Context mContext;

    private BatteryManager mBatteryManager;
    private long mManufactureDateInMs;
    private long mFirstUseDateInMs;

    public BatterySettingsFeatureProviderImpl(Context context) {
        mContext = context.getApplicationContext();
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
        return isManufactureDateAvailable()
                ? getFormattedDate(getManufactureDate())
                : null;
    }

    @Override
    public CharSequence getFirstUseDateSummary() {
        return isFirstUseDateAvailable()
                ? getFormattedDate(getFirstUseDate())
                : null;
    }

    protected long getManufactureDate() {
        if (mManufactureDateInMs == 0L) {
            final long manufactureDateInSec = mBatteryManager.getLongProperty(
                    BatteryManager.BATTERY_PROPERTY_MANUFACTURING_DATE);
            mManufactureDateInMs = TimeUnit.MILLISECONDS.convert(manufactureDateInSec,
                    TimeUnit.SECONDS);
        }
        return mManufactureDateInMs;
    }

    protected long getFirstUseDate() {
        if (mFirstUseDateInMs == 0L) {
            final long firstUseDateInSec = mBatteryManager.getLongProperty(
                    BatteryManager.BATTERY_PROPERTY_FIRST_USAGE_DATE);
            mFirstUseDateInMs = TimeUnit.MILLISECONDS.convert(firstUseDateInSec, TimeUnit.SECONDS);
        }
        return mFirstUseDateInMs;
    }

    private CharSequence getFormattedDate(long dateInMs) {
        final Instant instant = Instant.ofEpochMilli(dateInMs);
        final String localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate().format(
                DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG));

        return localDate;
    }
}
