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

package com.android.settings.fuelgauge.batterytip.tips;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.icu.text.ListFormatter;
import android.os.Parcel;
import android.util.ArrayMap;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.fuelgauge.batterytip.AppInfo;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.utils.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Tip to suggest user to restrict some bad apps */
public class RestrictAppTip extends BatteryTip {
    private List<AppInfo> mRestrictAppList;

    public RestrictAppTip(@StateType int state, List<AppInfo> restrictApps) {
        super(TipType.APP_RESTRICTION, state, state == StateType.NEW /* showDialog */);
        mRestrictAppList = restrictApps;
        mNeedUpdate = false;
    }

    public RestrictAppTip(@StateType int state, AppInfo appInfo) {
        super(TipType.APP_RESTRICTION, state, state == StateType.NEW /* showDialog */);
        mRestrictAppList = new ArrayList<>();
        mRestrictAppList.add(appInfo);
        mNeedUpdate = false;
    }

    @VisibleForTesting
    RestrictAppTip(Parcel in) {
        super(in);
        mRestrictAppList = in.createTypedArrayList(AppInfo.CREATOR);
    }

    @Override
    public CharSequence getTitle(Context context) {
        final int num = mRestrictAppList.size();
        final CharSequence appLabel =
                num > 0
                        ? Utils.getApplicationLabel(context, mRestrictAppList.get(0).packageName)
                        : "";

        Map<String, Object> arguments = new ArrayMap<>();
        arguments.put("count", num);
        arguments.put("label", appLabel);
        return mState == StateType.HANDLED
                ? StringUtil.getIcuPluralsString(
                        context, arguments, R.string.battery_tip_restrict_handled_title)
                : StringUtil.getIcuPluralsString(
                        context, arguments, R.string.battery_tip_restrict_title);
    }

    @Override
    public CharSequence getSummary(Context context) {
        final int num = mRestrictAppList.size();
        final CharSequence appLabel =
                num > 0
                        ? Utils.getApplicationLabel(context, mRestrictAppList.get(0).packageName)
                        : "";
        final int resId =
                mState == StateType.HANDLED
                        ? R.string.battery_tip_restrict_handled_summary
                        : R.string.battery_tip_restrict_summary;
        Map<String, Object> arguments = new ArrayMap<>();
        arguments.put("count", num);
        arguments.put("label", appLabel);
        return StringUtil.getIcuPluralsString(context, arguments, resId);
    }

    @Override
    public int getIconId() {
        return mState == StateType.HANDLED
                ? R.drawable.ic_perm_device_information_theme
                : R.drawable.ic_battery_alert_theme;
    }

    @Override
    public void updateState(BatteryTip tip) {
        if (tip.mState == StateType.NEW) {
            // Display it if new anomaly comes
            mState = StateType.NEW;
            mRestrictAppList = ((RestrictAppTip) tip).mRestrictAppList;
            mShowDialog = true;
        } else if (mState == StateType.NEW && tip.mState == StateType.INVISIBLE) {
            // If anomaly becomes invisible, show it as handled
            mState = StateType.HANDLED;
            mShowDialog = false;
        } else {
            mState = tip.getState();
            mShowDialog = tip.shouldShowDialog();
            mRestrictAppList = ((RestrictAppTip) tip).mRestrictAppList;
        }
    }

    @Override
    public void validateCheck(Context context) {
        super.validateCheck(context);

        // Set it invisible if there is no valid app
        mRestrictAppList.removeIf(AppLabelPredicate.getInstance(context));
        if (mRestrictAppList.isEmpty()) {
            mState = StateType.INVISIBLE;
        }
    }

    @Override
    public void log(Context context, MetricsFeatureProvider metricsFeatureProvider) {
        metricsFeatureProvider.action(context, SettingsEnums.ACTION_APP_RESTRICTION_TIP, mState);
        if (mState == StateType.NEW) {
            for (int i = 0, size = mRestrictAppList.size(); i < size; i++) {
                final AppInfo appInfo = mRestrictAppList.get(i);
                for (Integer anomalyType : appInfo.anomalyTypes) {
                    metricsFeatureProvider.action(
                            SettingsEnums.PAGE_UNKNOWN,
                            SettingsEnums.ACTION_APP_RESTRICTION_TIP_LIST,
                            SettingsEnums.PAGE_UNKNOWN,
                            appInfo.packageName,
                            anomalyType);
                }
            }
        }
    }

    public List<AppInfo> getRestrictAppList() {
        return mRestrictAppList;
    }

    /** Construct the app list string(e.g. app1, app2, and app3) */
    public CharSequence getRestrictAppsString(Context context) {
        final List<CharSequence> appLabels = new ArrayList<>();
        for (int i = 0, size = mRestrictAppList.size(); i < size; i++) {
            appLabels.add(Utils.getApplicationLabel(context, mRestrictAppList.get(i).packageName));
        }

        return ListFormatter.getInstance().format(appLabels);
    }

    @Override
    public String toString() {
        final StringBuilder stringBuilder = new StringBuilder(super.toString());
        stringBuilder.append(" {");
        for (int i = 0, size = mRestrictAppList.size(); i < size; i++) {
            final AppInfo appInfo = mRestrictAppList.get(i);
            stringBuilder.append(" " + appInfo.toString() + " ");
        }
        stringBuilder.append('}');

        return stringBuilder.toString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeTypedList(mRestrictAppList);
    }

    public static final Creator CREATOR =
            new Creator() {
                public BatteryTip createFromParcel(Parcel in) {
                    return new RestrictAppTip(in);
                }

                public BatteryTip[] newArray(int size) {
                    return new RestrictAppTip[size];
                }
            };
}
