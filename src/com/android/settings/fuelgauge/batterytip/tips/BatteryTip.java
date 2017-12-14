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

package com.android.settings.fuelgauge.batterytip.tips;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.IntDef;
import android.support.v7.preference.Preference;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Base model for a battery tip(e.g. suggest user to turn on battery saver)
 *
 * Each {@link BatteryTip} contains basic data(e.g. title, summary, icon) as well as the
 * pre-defined action(e.g. turn on battery saver)
 */
public abstract class BatteryTip implements Comparable<BatteryTip> {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({StateType.NEW,
            StateType.HANDLED,
            StateType.INVISIBLE})
    public @interface StateType {
        int NEW = 0;
        int HANDLED = 1;
        int INVISIBLE = 2;
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({TipType.SUMMARY,
            TipType.BATTERY_SAVER,
            TipType.HIGH_DEVICE_USAGE,
            TipType.SMART_BATTERY_MANAGER,
            TipType.APP_RESTRICTION,
            TipType.REDUCED_BATTERY,
            TipType.LOW_BATTERY})
    public @interface TipType {
        int SUMMARY = 0;
        int BATTERY_SAVER = 1;
        int HIGH_DEVICE_USAGE = 2;
        int SMART_BATTERY_MANAGER = 3;
        int APP_RESTRICTION = 4;
        int REDUCED_BATTERY = 5;
        int LOW_BATTERY = 6;
    }

    private static final String KEY_PREFIX = "key_battery_tip";

    @TipType
    protected int mType;
    @StateType
    protected int mState;
    protected boolean mShowDialog;

    public abstract CharSequence getTitle(Context context);

    public abstract CharSequence getSummary(Context context);

    @IdRes
    public abstract int getIconId();

    /**
     * Update the current {@link #mState} using the new {@code tip}.
     * @param tip used to update
     */
    public abstract void updateState(BatteryTip tip);

    /**
     * Execute the action for this {@link BatteryTip}
     */
    public abstract void action();

    /**
     * Build the dialog to display either the info about {@link BatteryTip} or confirmation
     * about the action.
     */
    public abstract Dialog buildDialog();

    public Preference buildPreference(Context context) {
        Preference preference = new Preference(context);

        preference.setKey(getKey());
        preference.setTitle(getTitle(context));
        preference.setSummary(getSummary(context));
        preference.setIcon(getIconId());
        return preference;
    }

    public boolean shouldShowDialog() {
        return mShowDialog;
    }

    public String getKey() {
        return KEY_PREFIX + mType;
    }

    @StateType
    public int getState() {
        return mState;
    }

    public boolean isVisible() {
        return mState != StateType.INVISIBLE;
    }

    @Override
    public int compareTo(BatteryTip o) {
        return mType - o.mType;
    }
}
