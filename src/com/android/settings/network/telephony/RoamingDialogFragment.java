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
package com.android.settings.network.telephony;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.TelephonyManager;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/**
 * A dialog fragment that asks the user if they are sure they want to turn on data roaming
 * to avoid accidental charges.
 */
public class RoamingDialogFragment extends InstrumentedDialogFragment implements OnClickListener {

    public static final String SUB_ID_KEY = "sub_id_key";

    private CarrierConfigManager mCarrierConfigManager;
    private int mSubId;

    public static RoamingDialogFragment newInstance(int subId) {
        final RoamingDialogFragment dialogFragment = new RoamingDialogFragment();
        final Bundle args = new Bundle();
        args.putInt(SUB_ID_KEY, subId);
        dialogFragment.setArguments(args);

        return dialogFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        final Bundle args = getArguments();
        mSubId = args.getInt(SUB_ID_KEY);
        mCarrierConfigManager = context.getSystemService(CarrierConfigManager.class);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        final int title = R.string.roaming_alert_title;
        int message = R.string.roaming_warning;
        final PersistableBundle carrierConfig = mCarrierConfigManager.getConfigForSubId(mSubId);
        if (carrierConfig != null && carrierConfig.getBoolean(
                CarrierConfigManager.KEY_CHECK_PRICING_WITH_CARRIER_FOR_DATA_ROAMING_BOOL)) {
            message = R.string.roaming_check_price_warning;
        }
        builder.setMessage(getResources().getString(message))
                .setTitle(title)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setPositiveButton(android.R.string.yes, this)
                .setNegativeButton(android.R.string.no, this);
        return builder.create();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.MOBILE_ROAMING_DIALOG;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        // let the host know that the positive button has been clicked
        if (which == dialog.BUTTON_POSITIVE) {
            final TelephonyManager telephonyManager =
                    getContext().getSystemService(TelephonyManager.class)
                    .createForSubscriptionId(mSubId);
            if (telephonyManager == null) {
                return;
            }
            telephonyManager.setDataRoamingEnabled(true);
        }
    }
}
