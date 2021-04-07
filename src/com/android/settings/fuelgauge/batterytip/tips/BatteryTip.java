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

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseIntArray;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.IntDef;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Base model for a battery tip(e.g. suggest user to turn on battery saver)
 *
 * Each {@link BatteryTip} contains basic data(e.g. title, summary, icon) as well as the
 * pre-defined action(e.g. turn on battery saver)
 */
public abstract class BatteryTip implements Comparable<BatteryTip>, Parcelable {
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
            TipType.LOW_BATTERY,
            TipType.REMOVE_APP_RESTRICTION,
            TipType.BATTERY_DEFENDER})
    public @interface TipType {
        int SMART_BATTERY_MANAGER = 0;
        int APP_RESTRICTION = 1;
        int HIGH_DEVICE_USAGE = 2;
        int BATTERY_SAVER = 3;
        int REDUCED_BATTERY = 4;
        int LOW_BATTERY = 5;
        int SUMMARY = 6;
        int REMOVE_APP_RESTRICTION = 7;
        int BATTERY_DEFENDER = 8;
    }

    @VisibleForTesting
    static final SparseIntArray TIP_ORDER;
    static {
        TIP_ORDER = new SparseIntArray();
        TIP_ORDER.append(TipType.BATTERY_DEFENDER, 0);
        TIP_ORDER.append(TipType.APP_RESTRICTION, 1);
        TIP_ORDER.append(TipType.BATTERY_SAVER, 2);
        TIP_ORDER.append(TipType.HIGH_DEVICE_USAGE, 3);
        TIP_ORDER.append(TipType.LOW_BATTERY, 4);
        TIP_ORDER.append(TipType.SUMMARY, 5);
        TIP_ORDER.append(TipType.SMART_BATTERY_MANAGER, 6);
        TIP_ORDER.append(TipType.REDUCED_BATTERY, 7);
        TIP_ORDER.append(TipType.REMOVE_APP_RESTRICTION, 8);
    }

    private static final String KEY_PREFIX = "key_battery_tip";

    protected int mType;
    protected int mState;
    protected boolean mShowDialog;
    /**
     * Whether we need to update battery tip when configuration change
     */
    protected boolean mNeedUpdate;

    BatteryTip(Parcel in) {
        mType = in.readInt();
        mState = in.readInt();
        mShowDialog = in.readBoolean();
        mNeedUpdate = in.readBoolean();
    }

    BatteryTip(int type, int state, boolean showDialog) {
        mType = type;
        mState = state;
        mShowDialog = showDialog;
        mNeedUpdate = true;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mType);
        dest.writeInt(mState);
        dest.writeBoolean(mShowDialog);
        dest.writeBoolean(mNeedUpdate);
    }

    public abstract CharSequence getTitle(Context context);

    public abstract CharSequence getSummary(Context context);

    @IdRes
    public abstract int getIconId();

    /**
     * Update the current {@link #mState} using the new {@code tip}.
     *
     * @param tip used to update
     */
    public abstract void updateState(BatteryTip tip);

    /**
     * Check whether data is still make sense. If not, try recover.
     * @param context used to do sanity check
     */
    public void sanityCheck(Context context) {
        // do nothing
    }

    /**
     * Log the battery tip
     */
    public abstract void log(Context context, MetricsFeatureProvider metricsFeatureProvider);

    public void updatePreference(Preference preference) {
        final Context context = preference.getContext();
        preference.setTitle(getTitle(context));
        preference.setSummary(getSummary(context));
        preference.setIcon(getIconId());
        @IdRes int iconTintColorId = getIconTintColorId();
        if (iconTintColorId != View.NO_ID) {
            preference.getIcon().setTint(context.getColor(iconTintColorId));
        }
    }

    /** Returns the color resid for tinting {@link #getIconId()} or {@link View#NO_ID} if none. */
    public @IdRes int getIconTintColorId() {
        return View.NO_ID;
    }

    public boolean shouldShowDialog() {
        return mShowDialog;
    }

    public boolean needUpdate() {
        return mNeedUpdate;
    }

    public String getKey() {
        return KEY_PREFIX + mType;
    }

    public int getType() {
        return mType;
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
        return TIP_ORDER.get(mType) - TIP_ORDER.get(o.mType);
    }

    @Override
    public String toString() {
        return "type=" + mType + " state=" + mState;
    }
}
