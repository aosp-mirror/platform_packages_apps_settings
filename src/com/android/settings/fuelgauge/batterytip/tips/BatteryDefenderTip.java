/*
 * Copyright (C) 2020 The Android Open Source Project
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

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.CardPreference;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import kotlin.Unit;

/** Tip to show current battery is overheated */
public class BatteryDefenderTip extends BatteryTip {

    private static final String TAG = "BatteryDefenderTip";

    private boolean mIsPluggedIn;

    public BatteryDefenderTip(@StateType int state, boolean isPluggedIn) {
        super(TipType.BATTERY_DEFENDER, state, false /* showDialog */);
        mIsPluggedIn = isPluggedIn;
    }

    private BatteryDefenderTip(Parcel in) {
        super(in);
    }

    @Override
    public CharSequence getTitle(Context context) {
        return context.getString(R.string.battery_tip_limited_temporarily_title);
    }

    @Override
    public CharSequence getSummary(Context context) {
        return context.getString(R.string.battery_tip_limited_temporarily_summary);
    }

    @Override
    public int getIconId() {
        return R.drawable.ic_battery_defender_tip_shield;
    }

    @Override
    public void updateState(BatteryTip tip) {
        mState = tip.mState;
    }

    @Override
    public void log(Context context, MetricsFeatureProvider metricsFeatureProvider) {
        metricsFeatureProvider.action(context, SettingsEnums.ACTION_BATTERY_DEFENDER_TIP, mState);
    }

    @Override
    public void updatePreference(Preference preference) {
        super.updatePreference(preference);
        final Context context = preference.getContext();

        CardPreference cardPreference = castToCardPreferenceSafely(preference);
        if (cardPreference == null) {
            Log.e(TAG, "cast Preference to CardPreference failed");
            return;
        }

        cardPreference.setSelectable(false);
        cardPreference.setIconResId(getIconId());
        cardPreference.setPrimaryButtonText(context.getString(R.string.learn_more));
        cardPreference.setPrimaryButtonAction(
                () -> {
                    var helpIntent =
                            HelpUtils.getHelpIntent(
                                    context,
                                    context.getString(R.string.help_url_battery_defender),
                                    /* backupContext= */ "");
                    ActivityCompat.startActivityForResult(
                            (Activity) preference.getContext(),
                            helpIntent,
                            /* requestCode= */ 0,
                            /* options= */ null);

                    return Unit.INSTANCE;
                });
        cardPreference.setPrimaryButtonVisibility(true);
        cardPreference.setPrimaryButtonContentDescription(
                context.getString(
                        R.string.battery_tip_limited_temporarily_sec_button_content_description));

        cardPreference.setSecondaryButtonText(
                context.getString(R.string.battery_tip_charge_to_full_button));
        cardPreference.setSecondaryButtonAction(
                () -> {
                    resumeCharging(context);
                    preference.setVisible(false);

                    return Unit.INSTANCE;
                });
        cardPreference.setSecondaryButtonVisibility(mIsPluggedIn);
        cardPreference.buildContent();
    }

    private void resumeCharging(Context context) {
        final Intent intent =
                FeatureFactory.getFeatureFactory()
                        .getPowerUsageFeatureProvider()
                        .getResumeChargeIntent(false);
        if (intent != null) {
            context.sendBroadcast(intent);
        }

        Log.i(TAG, "send resume charging broadcast intent=" + intent);
    }

    public static final Creator CREATOR =
            new Creator() {
                public BatteryTip createFromParcel(Parcel in) {
                    return new BatteryDefenderTip(in);
                }

                public BatteryTip[] newArray(int size) {
                    return new BatteryDefenderTip[size];
                }
            };
}
