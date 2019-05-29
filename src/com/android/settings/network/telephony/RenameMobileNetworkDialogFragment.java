/*
 * Copyright (C) 2019 The Android Open Source Project
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
import android.os.Bundle;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.DeviceInfoUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;

/** A dialog allowing the display name of a mobile network subscription to be changed */
public class RenameMobileNetworkDialogFragment extends InstrumentedDialogFragment {
    public static final String TAG ="RenameMobileNetwork";

    private static final String KEY_SUBSCRIPTION_ID = "subscription_id";

    private TelephonyManager mTelephonyManager;
    private SubscriptionManager mSubscriptionManager;
    private int mSubId;
    private EditText mNameView;

    public static RenameMobileNetworkDialogFragment newInstance(int subscriptionId) {
        final Bundle args = new Bundle(1);
        args.putInt(KEY_SUBSCRIPTION_ID, subscriptionId);
        final RenameMobileNetworkDialogFragment fragment = new RenameMobileNetworkDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @VisibleForTesting
    protected TelephonyManager getTelephonyManager(Context context) {
        return context.getSystemService(TelephonyManager.class);
    }

    @VisibleForTesting
    protected SubscriptionManager getSubscriptionManager(Context context) {
        return context.getSystemService(SubscriptionManager.class);
    }

    @VisibleForTesting
    protected EditText getNameView() {
        return mNameView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mTelephonyManager = getTelephonyManager(context);
        mSubscriptionManager = getSubscriptionManager(context);
        mSubId = getArguments().getInt(KEY_SUBSCRIPTION_ID);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final LayoutInflater layoutInflater = builder.getContext().getSystemService(
                LayoutInflater.class);
        final View view = layoutInflater.inflate(R.layout.dialog_mobile_network_rename, null);
        populateView(view);
        builder.setTitle(R.string.mobile_network_sim_name)
                .setView(view)
                .setPositiveButton(R.string.mobile_network_sim_name_rename, (dialog, which) -> {
                    String newName = mNameView.getText().toString();
                    mSubscriptionManager.setDisplayName(newName, mSubId,
                            SubscriptionManager.NAME_SOURCE_USER_INPUT);
                })
                .setNegativeButton(android.R.string.cancel, null);
        return builder.create();
    }

    @VisibleForTesting
    protected void populateView(View view) {
        mNameView = view.findViewById(R.id.edittext);
        final SubscriptionInfo info = mSubscriptionManager.getActiveSubscriptionInfo(mSubId);
        if (info == null) {
            Log.w(TAG, "got null SubscriptionInfo for mSubId:" + mSubId);
            return;
        }
        final CharSequence displayName = info.getDisplayName();
        mNameView.setText(displayName);
        if (!TextUtils.isEmpty(displayName)) {
            mNameView.setSelection(displayName.length());
        }

        final TextView operatorName = view.findViewById(R.id.operator_name_value);
        final ServiceState serviceState = mTelephonyManager.getServiceStateForSubscriber(mSubId);
        operatorName.setText(serviceState.getOperatorAlphaLong());

        final TextView phoneTitle = view.findViewById(R.id.number_label);
        phoneTitle.setVisibility(info.isOpportunistic() ? View.GONE : View.VISIBLE);

        final TextView phoneNumber = view.findViewById(R.id.number_value);
        final String formattedNumber = DeviceInfoUtils.getFormattedPhoneNumber(getContext(), info);
        phoneNumber.setText(BidiFormatter.getInstance().unicodeWrap(formattedNumber,
                TextDirectionHeuristics.LTR));
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.MOBILE_NETWORK_RENAME_DIALOG;
    }
}
