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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.fuelgauge.batterytip.BatteryTipPreferenceController.BatteryTipListener;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.HighUsageTip;

/**
 * Dialog Fragment to show action dialog for each anomaly
 */
public class BatteryTipDialogFragment extends InstrumentedDialogFragment implements
        DialogInterface.OnClickListener {

    private static final String ARG_BATTERY_TIP = "battery_tip";

    @VisibleForTesting
    BatteryTip mBatteryTip;

    public static BatteryTipDialogFragment newInstance(BatteryTip batteryTip) {
        BatteryTipDialogFragment dialogFragment = new BatteryTipDialogFragment();

        Bundle args = new Bundle(1);
        args.putParcelable(ARG_BATTERY_TIP, batteryTip);
        dialogFragment.setArguments(args);

        return dialogFragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle bundle = getArguments();
        final Context context = getContext();

        mBatteryTip = bundle.getParcelable(ARG_BATTERY_TIP);

        switch (mBatteryTip.getType()) {
            case BatteryTip.TipType.SUMMARY:
            case BatteryTip.TipType.LOW_BATTERY:
                //TODO(b/70570352): add dialog
                return null;
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
                                highUsageTip.getScreenTimeMs()))
                        .setView(view)
                        .setPositiveButton(android.R.string.ok, null)
                        .create();
            default:
                throw new IllegalArgumentException("unknown type " + mBatteryTip.getType());
        }
    }

    @Override
    public int getMetricsCategory() {
        //TODO(b/70570352): add correct metric id
        return 0;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        final BatteryTipListener lsn = (BatteryTipListener) getTargetFragment();
        if (lsn == null) {
            return;
        }
        mBatteryTip.action();
        lsn.onBatteryTipHandled(mBatteryTip);
    }

}
