/*
 * Copyright (C) 2022 The Android Open Source Project
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
import android.content.Intent;
import android.os.Parcel;
import android.util.Log;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.BatteryUtils.DockDefenderMode;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.CardPreference;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

/**
 * Tip to show dock defender status
 */
public class DockDefenderTip extends BatteryTip {
    private static final String TAG = "DockDefenderTip";
    private int mMode;

    public DockDefenderTip(@StateType int state, @DockDefenderMode int mode) {
        super(TipType.DOCK_DEFENDER, state, false);
        mMode = mode;
    }

    private DockDefenderTip(Parcel in) {
        super(in);
    }

    public int getMode() {
        return mMode;
    }

    @Override
    public CharSequence getTitle(Context context) {
        switch (mMode) {
            case DockDefenderMode.FUTURE_BYPASS:
                return context.getString(R.string.battery_tip_dock_defender_future_bypass_title);
            case DockDefenderMode.ACTIVE:
                return context.getString(R.string.battery_tip_dock_defender_active_title);
            case DockDefenderMode.TEMPORARILY_BYPASSED:
                return context.getString(
                        R.string.battery_tip_dock_defender_temporarily_bypassed_title);
            default:
                return null;
        }
    }

    @Override
    public CharSequence getSummary(Context context) {
        switch (mMode) {
            case DockDefenderMode.FUTURE_BYPASS:
                return context.getString(R.string.battery_tip_dock_defender_future_bypass_summary);
            case DockDefenderMode.ACTIVE:
                return context.getString(R.string.battery_tip_dock_defender_active_summary);
            case DockDefenderMode.TEMPORARILY_BYPASSED:
                return context.getString(
                        R.string.battery_tip_dock_defender_temporarily_bypassed_summary);
            default:
                return null;
        }
    }

    @Override
    public int getIconId() {
        return mMode == DockDefenderMode.ACTIVE ? R.drawable.ic_battery_status_protected_24dp :
                R.drawable.ic_battery_dock_defender_untriggered_24dp;
    }

    @Override
    public void updateState(BatteryTip tip) {
        mState = tip.mState;
        if (tip instanceof DockDefenderTip) {
            mMode = ((DockDefenderTip) tip).mMode;
        }
    }

    @Override
    public void log(Context context, MetricsFeatureProvider metricsFeatureProvider) {
        metricsFeatureProvider.action(context, SettingsEnums.ACTION_DOCK_DEFENDER_TIP,
                mState);
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
        switch (mMode) {
            case DockDefenderMode.FUTURE_BYPASS:
            case DockDefenderMode.ACTIVE:
                cardPreference.setPrimaryButtonText(
                        context.getString(R.string.battery_tip_charge_to_full_button));
                cardPreference.setPrimaryButtonClickListener(unused -> {
                    resumeCharging(context);
                    mMode = DockDefenderMode.TEMPORARILY_BYPASSED;
                    context.sendBroadcast(new Intent().setAction(
                            BatteryUtils.BYPASS_DOCK_DEFENDER_ACTION).setPackage(
                            context.getPackageName()).addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY
                            | Intent.FLAG_RECEIVER_FOREGROUND));
                    updatePreference(preference);
                });
                cardPreference.setPrimaryButtonVisible(true);
                break;
            case DockDefenderMode.TEMPORARILY_BYPASSED:
                cardPreference.setPrimaryButtonVisible(false);
                break;
            default:
                cardPreference.setVisible(false);
                return;
        }

        cardPreference.setSecondaryButtonText(context.getString(R.string.learn_more));
        cardPreference.setSecondaryButtonClickListener(
                button -> button.startActivityForResult(
                        HelpUtils.getHelpIntent(
                                context,
                                context.getString(R.string.help_url_dock_defender),
                                /* backupContext */ ""), /* requestCode */ 0));
        cardPreference.setSecondaryButtonVisible(true);
        cardPreference.setSecondaryButtonContentDescription(context.getString(
                R.string.battery_tip_limited_temporarily_sec_button_content_description));

    }

    private CardPreference castToCardPreferenceSafely(Preference preference) {
        return preference instanceof CardPreference ? (CardPreference) preference : null;
    }

    private void resumeCharging(Context context) {
        final Intent intent =
                FeatureFactory.getFactory(context)
                        .getPowerUsageFeatureProvider(context)
                        .getResumeChargeIntent(true);
        if (intent != null) {
            context.sendBroadcast(intent);
        }

        Log.i(TAG, "send resume charging broadcast intent=" + intent);
    }

    public static final Creator CREATOR = new Creator() {
        public BatteryTip createFromParcel(Parcel in) {
            return new DockDefenderTip(in);
        }

        public BatteryTip[] newArray(int size) {
            return new DockDefenderTip[size];
        }
    };
}
