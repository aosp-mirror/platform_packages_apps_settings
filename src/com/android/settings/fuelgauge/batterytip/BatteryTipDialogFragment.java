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

package com.android.settings.fuelgauge.batterytip;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.fuelgauge.batterytip.BatteryTipPreferenceController.BatteryTipListener;
import com.android.settings.fuelgauge.batterytip.actions.BatteryTipAction;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.HighUsageTip;
import com.android.settings.fuelgauge.batterytip.tips.RestrictAppTip;
import com.android.settings.fuelgauge.batterytip.tips.UnrestrictAppTip;

import java.util.List;

/**
 * Dialog Fragment to show action dialog for each anomaly
 */
public class BatteryTipDialogFragment extends InstrumentedDialogFragment implements
        DialogInterface.OnClickListener {

    private static final String ARG_BATTERY_TIP = "battery_tip";
    private static final String ARG_METRICS_KEY = "metrics_key";

    @VisibleForTesting
    BatteryTip mBatteryTip;
    @VisibleForTesting
    int mMetricsKey;

    public static BatteryTipDialogFragment newInstance(BatteryTip batteryTip, int metricsKey) {
        BatteryTipDialogFragment dialogFragment = new BatteryTipDialogFragment();

        Bundle args = new Bundle(1);
        args.putParcelable(ARG_BATTERY_TIP, batteryTip);
        args.putInt(ARG_METRICS_KEY, metricsKey);
        dialogFragment.setArguments(args);

        return dialogFragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle bundle = getArguments();
        final Context context = getContext();

        mBatteryTip = bundle.getParcelable(ARG_BATTERY_TIP);
        mMetricsKey = bundle.getInt(ARG_METRICS_KEY);

        switch (mBatteryTip.getType()) {
            case BatteryTip.TipType.SUMMARY:
                return new AlertDialog.Builder(context)
                        .setMessage(R.string.battery_tip_dialog_summary_message)
                        .setPositiveButton(android.R.string.ok, null)
                        .create();
            case BatteryTip.TipType.HIGH_DEVICE_USAGE:
                final HighUsageTip highUsageTip = (HighUsageTip) mBatteryTip;
                final RecyclerView view = (RecyclerView) LayoutInflater.from(context).inflate(
                        R.layout.recycler_view,
                        null);
                view.setLayoutManager(new LinearLayoutManager(context));
                view.setAdapter(new HighUsageAdapter(context,
                        highUsageTip.getHighUsageAppList()));

                return new AlertDialog.Builder(context)
                        .setMessage(getString(R.string.battery_tip_dialog_message,
                                highUsageTip.getHighUsageAppList().size()))
                        .setView(view)
                        .setPositiveButton(android.R.string.ok, null)
                        .create();
            case BatteryTip.TipType.APP_RESTRICTION:
                final RestrictAppTip restrictAppTip = (RestrictAppTip) mBatteryTip;
                final List<AppInfo> restrictedAppList = restrictAppTip.getRestrictAppList();
                final int num = restrictedAppList.size();
                final CharSequence appLabel = Utils.getApplicationLabel(context,
                        restrictedAppList.get(0).packageName);

                final AlertDialog.Builder builder = new AlertDialog.Builder(context)
                        .setTitle(context.getResources().getQuantityString(
                                R.plurals.battery_tip_restrict_app_dialog_title, num, num))
                        .setPositiveButton(R.string.battery_tip_restrict_app_dialog_ok, this)
                        .setNegativeButton(android.R.string.cancel, null);
                if (num == 1) {
                    builder.setMessage(
                            getString(R.string.battery_tip_restrict_app_dialog_message, appLabel));
                } else if (num <= 5) {
                    builder.setMessage(
                            getString(
                                    R.string.battery_tip_restrict_apps_less_than_5_dialog_message));
                    final RecyclerView restrictionView = (RecyclerView) LayoutInflater.from(
                            context).inflate(R.layout.recycler_view, null);
                    restrictionView.setLayoutManager(new LinearLayoutManager(context));
                    restrictionView.setAdapter(new HighUsageAdapter(context, restrictedAppList));
                    builder.setView(restrictionView);
                } else {
                    builder.setMessage(context.getString(
                            R.string.battery_tip_restrict_apps_more_than_5_dialog_message,
                            restrictAppTip.getRestrictAppsString(context)));
                }

                return builder.create();
            case BatteryTip.TipType.REMOVE_APP_RESTRICTION:
                final UnrestrictAppTip unrestrictAppTip = (UnrestrictAppTip) mBatteryTip;
                final CharSequence name = Utils.getApplicationLabel(context,
                        unrestrictAppTip.getPackageName());

                return new AlertDialog.Builder(context)
                        .setTitle(getString(R.string.battery_tip_unrestrict_app_dialog_title))
                        .setMessage(R.string.battery_tip_unrestrict_app_dialog_message)
                        .setPositiveButton(R.string.battery_tip_unrestrict_app_dialog_ok, this)
                        .setNegativeButton(R.string.battery_tip_unrestrict_app_dialog_cancel, null)
                        .create();
            default:
                throw new IllegalArgumentException("unknown type " + mBatteryTip.getType());
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.FUELGAUGE_BATTERY_TIP_DIALOG;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        final BatteryTipListener lsn = (BatteryTipListener) getTargetFragment();
        if (lsn == null) {
            return;
        }
        final BatteryTipAction action = BatteryTipUtils.getActionForBatteryTip(mBatteryTip,
                (SettingsActivity) getActivity(),
                (InstrumentedPreferenceFragment) getTargetFragment());
        if (action != null) {
            action.handlePositiveAction(mMetricsKey);
        }
        lsn.onBatteryTipHandled(mBatteryTip);
    }

}
