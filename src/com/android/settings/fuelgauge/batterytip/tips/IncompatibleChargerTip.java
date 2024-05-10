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

package com.android.settings.fuelgauge.batterytip.tips;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Parcel;
import android.util.Log;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.widget.CardPreference;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

/** Tip to show incompatible charger state */
public final class IncompatibleChargerTip extends BatteryTip {
    private static final String TAG = "IncompatibleChargerTip";

    public IncompatibleChargerTip(@StateType int state) {
        super(TipType.INCOMPATIBLE_CHARGER, state, /* showDialog */ false);
    }

    private IncompatibleChargerTip(Parcel in) {
        super(in);
    }

    @Override
    public CharSequence getTitle(Context context) {
        return context.getString(R.string.battery_tip_incompatible_charging_title);
    }

    @Override
    public CharSequence getSummary(Context context) {
        return context.getString(R.string.battery_tip_incompatible_charging_message);
    }

    @Override
    public int getIconId() {
        return R.drawable.ic_battery_charger;
    }

    @Override
    public void updateState(BatteryTip tip) {
        mState = tip.mState;
    }

    @Override
    public void log(Context context, MetricsFeatureProvider metricsFeatureProvider) {
        metricsFeatureProvider.action(
                context, SettingsEnums.ACTION_INCOMPATIBLE_CHARGING_TIP, mState);
    }

    @Override
    public void updatePreference(Preference preference) {
        super.updatePreference(preference);
        final Context context = preference.getContext();
        final CardPreference cardPreference = castToCardPreferenceSafely(preference);
        if (cardPreference == null) {
            Log.e(TAG, "cast Preference to CardPreference failed");
            return;
        }

        cardPreference.setSelectable(false);
        cardPreference.setPrimaryButtonText(context.getString(R.string.learn_more));
        cardPreference.setPrimaryButtonClickListener(
                button ->
                        button.startActivityForResult(
                                HelpUtils.getHelpIntent(
                                        context,
                                        context.getString(R.string.help_url_incompatible_charging),
                                        /* backupContext */ ""), /* requestCode */
                                0));
        cardPreference.setPrimaryButtonVisible(true);
        cardPreference.setPrimaryButtonContentDescription(
                context.getString(R.string.battery_tip_incompatible_charging_content_description));
    }

    public static final Creator CREATOR =
            new Creator() {
                public BatteryTip createFromParcel(Parcel in) {
                    return new IncompatibleChargerTip(in);
                }

                public BatteryTip[] newArray(int size) {
                    return new IncompatibleChargerTip[size];
                }
            };
}
