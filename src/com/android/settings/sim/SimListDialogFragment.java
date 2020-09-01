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
 * limitations under the License
 */

package com.android.settings.sim;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows a dialog consisting of a list of SIMs (aka subscriptions), possibly including an additional
 * entry indicating "ask me every time".
 */
public class SimListDialogFragment extends SimDialogFragment implements
        DialogInterface.OnClickListener {
    protected static final String KEY_INCLUDE_ASK_EVERY_TIME = "include_ask_every_time";

    protected SelectSubscriptionAdapter mAdapter;
    @VisibleForTesting
    List<SubscriptionInfo> mSubscriptions;

    public static SimListDialogFragment newInstance(int dialogType, int titleResId,
            boolean includeAskEveryTime) {
        final SimListDialogFragment fragment = new SimListDialogFragment();
        final Bundle args = initArguments(dialogType, titleResId);
        args.putBoolean(KEY_INCLUDE_ASK_EVERY_TIME, includeAskEveryTime);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        mSubscriptions = new ArrayList<>();

        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getTitleResId());

        mAdapter = new SelectSubscriptionAdapter(builder.getContext(), mSubscriptions);

        setAdapter(builder);
        final Dialog dialog = builder.create();
        updateDialog();
        return dialog;
    }

    @Override
    public void onClick(DialogInterface dialog, int selectionIndex) {
        if (selectionIndex >= 0 && selectionIndex < mSubscriptions.size()) {
            int subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
            final SubscriptionInfo subscription = mSubscriptions.get(selectionIndex);
            if (subscription != null) {
                subId = subscription.getSubscriptionId();
            }
            final SimDialogActivity activity = (SimDialogActivity) getActivity();
            activity.onSubscriptionSelected(getDialogType(), subId);
        }
    }

    protected List<SubscriptionInfo> getCurrentSubscriptions() {
        final SubscriptionManager manager = getContext().getSystemService(
                SubscriptionManager.class);
        return manager.getActiveSubscriptionInfoList(true);
    }

    @Override
    public void updateDialog() {
        List<SubscriptionInfo> currentSubscriptions = getCurrentSubscriptions();
        if (currentSubscriptions == null) {
            dismiss();
            return;
        }
        if (getArguments().getBoolean(KEY_INCLUDE_ASK_EVERY_TIME)) {
            final List<SubscriptionInfo> tmp = new ArrayList<>(currentSubscriptions.size() + 1);
            tmp.add(null);
            tmp.addAll(currentSubscriptions);
            currentSubscriptions = tmp;
        }
        if (currentSubscriptions.equals(mSubscriptions)) {
            return;
        }
        mSubscriptions.clear();
        mSubscriptions.addAll(currentSubscriptions);
        mAdapter.notifyDataSetChanged();
    }

    @VisibleForTesting
    void setAdapter(AlertDialog.Builder builder) {
        builder.setAdapter(mAdapter, this);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_SIM_LIST;
    }

    private static class SelectSubscriptionAdapter extends BaseAdapter {
        private Context mContext;
        private LayoutInflater mInflater;
        List<SubscriptionInfo> mSubscriptions;

        public SelectSubscriptionAdapter(Context context, List<SubscriptionInfo> subscriptions) {
            mSubscriptions = subscriptions;
            mContext = context;
        }

        @Override
        public int getCount() {
            return mSubscriptions.size();
        }

        @Override
        public SubscriptionInfo getItem(int position) {
            return mSubscriptions.get(position);
        }

        @Override
        public long getItemId(int position) {
            final SubscriptionInfo info = mSubscriptions.get(position);
            if (info == null) {
                return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
            }
            return info.getSubscriptionId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                if (mInflater == null) {
                    mInflater = LayoutInflater.from(parent.getContext());
                }
                convertView = mInflater.inflate(R.layout.select_account_list_item, parent, false);
            }
            final SubscriptionInfo sub = getItem(position);

            final TextView title = convertView.findViewById(R.id.title);
            final TextView summary = convertView.findViewById(R.id.summary);
            final ImageView icon = convertView.findViewById(R.id.icon);

            if (sub == null) {
                title.setText(R.string.sim_calls_ask_first_prefs_title);
                summary.setText("");
                icon.setImageDrawable(mContext.getDrawable(R.drawable.ic_feedback_24dp));
                icon.setImageTintList(
                        Utils.getColorAttr(mContext, android.R.attr.textColorSecondary));
            } else {
                title.setText(sub.getDisplayName());
                summary.setText(isMdnProvisioned(sub.getNumber()) ? sub.getNumber() : "");
                icon.setImageBitmap(sub.createIconBitmap(mContext));

            }
            return convertView;
        }

        // An MDN is considered not provisioned if it's empty or all 0's
        private boolean isMdnProvisioned(String mdn) {
            return !(TextUtils.isEmpty(mdn) || mdn.matches("[\\D0]+"));
        }
    }
}
