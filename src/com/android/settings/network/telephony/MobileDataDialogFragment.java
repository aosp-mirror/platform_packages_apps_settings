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

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;


/**
 * Dialog Fragment to show dialog for "mobile data"
 *
 * 1. When user want to disable data in single sim case, show dialog to confirm
 * 2. When user want to enable data in multiple sim case, show dialog to confirm to disable other
 * sim
 */
public class MobileDataDialogFragment extends InstrumentedDialogFragment implements
        DialogInterface.OnClickListener {

    public static final int TYPE_DISABLE_DIALOG = 0;
    public static final int TYPE_MULTI_SIM_DIALOG = 1;

    private static final String ARG_DIALOG_TYPE = "dialog_type";
    private static final String ARG_SUB_ID = "subId";

    private SubscriptionManager mSubscriptionManager;
    private int mType;
    private int mSubId;

    public static MobileDataDialogFragment newInstance(int type, int subId) {
        final MobileDataDialogFragment dialogFragment = new MobileDataDialogFragment();

        Bundle args = new Bundle();
        args.putInt(ARG_DIALOG_TYPE, type);
        args.putInt(ARG_SUB_ID, subId);
        dialogFragment.setArguments(args);

        return dialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSubscriptionManager = getContext().getSystemService(SubscriptionManager.class);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle bundle = getArguments();
        final Context context = getContext();

        mType = bundle.getInt(ARG_DIALOG_TYPE);
        mSubId = bundle.getInt(ARG_SUB_ID);

        switch (mType) {
            case TYPE_DISABLE_DIALOG:
                return new AlertDialog.Builder(context)
                        .setMessage(R.string.data_usage_disable_mobile)
                        .setPositiveButton(android.R.string.ok, this)
                        .setNegativeButton(android.R.string.cancel, null)
                        .create();
            case TYPE_MULTI_SIM_DIALOG:
                final SubscriptionInfo currentSubInfo =
                        mSubscriptionManager.getActiveSubscriptionInfo(mSubId);
                final SubscriptionInfo nextSubInfo =
                        mSubscriptionManager.getActiveSubscriptionInfo(
                                mSubscriptionManager.getDefaultDataSubscriptionId());

                final String previousName = (nextSubInfo == null)
                        ? getContext().getResources().getString(
                        R.string.sim_selection_required_pref)
                        : nextSubInfo.getDisplayName().toString();

                final String newName = (currentSubInfo == null)
                        ? getContext().getResources().getString(
                        R.string.sim_selection_required_pref)
                        : currentSubInfo.getDisplayName().toString();

                return new AlertDialog.Builder(context)
                        .setTitle(context.getString(R.string.sim_change_data_title, newName))
                        .setMessage(context.getString(R.string.sim_change_data_message,
                                newName, previousName))
                        .setPositiveButton(
                                context.getString(R.string.sim_change_data_ok, newName),
                                this)
                        .setNegativeButton(R.string.cancel, null)
                        .create();
            default:
                throw new IllegalArgumentException("unknown type " + mType);
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.MOBILE_DATA_DIALOG;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (mType) {
            case TYPE_DISABLE_DIALOG:
                MobileNetworkUtils.setMobileDataEnabled(getContext(), mSubId, false /* enabled */,
                        false /* disableOtherSubscriptions */);
                break;
            case TYPE_MULTI_SIM_DIALOG:
                mSubscriptionManager.setDefaultDataSubId(mSubId);
                MobileNetworkUtils.setMobileDataEnabled(getContext(), mSubId, true /* enabled */,
                        true /* disableOtherSubscriptions */);
                break;
            default:
                throw new IllegalArgumentException("unknown type " + mType);
        }
    }

}
