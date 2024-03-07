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

import static android.telephony.SubscriptionManager.PROFILE_CLASS_PROVISIONING;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;

import com.android.internal.telephony.flags.Flags;
import com.android.settings.R;
import com.android.settings.network.SubscriptionUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows a dialog consisting of a list of SIMs (aka subscriptions), possibly including an additional
 * entry indicating "ask me every time".
 */
public class SimListDialogFragment extends SimDialogFragment {
    private static final String TAG = "SimListDialogFragment";
    protected static final String KEY_INCLUDE_ASK_EVERY_TIME = "include_ask_every_time";
    protected static final String KEY_SHOW_CANCEL_ITEM = "show_cancel_item";

    protected SelectSubscriptionAdapter mAdapter;
    @VisibleForTesting
    @NonNull
    List<SubscriptionInfo> mSubscriptions = new ArrayList<>();

    public static SimListDialogFragment newInstance(int dialogType, int titleResId,
            boolean includeAskEveryTime, boolean isCancelItemShowed) {
        final SimListDialogFragment fragment = new SimListDialogFragment();
        final Bundle args = initArguments(dialogType, titleResId);
        args.putBoolean(KEY_INCLUDE_ASK_EVERY_TIME, includeAskEveryTime);
        args.putBoolean(KEY_SHOW_CANCEL_ITEM, isCancelItemShowed);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View titleView = LayoutInflater.from(getContext()).inflate(
                R.layout.sim_confirm_dialog_title_multiple_enabled_profiles_supported, null);
        TextView titleTextView = titleView.findViewById(R.id.title);
        titleTextView.setText(getContext().getString(getTitleResId()));
        builder.setCustomTitle(titleTextView);
        mAdapter = new SelectSubscriptionAdapter(builder.getContext(), mSubscriptions);

        final AlertDialog dialog = builder.create();

        View content = LayoutInflater.from(getContext()).inflate(
                R.layout.sim_confirm_dialog_multiple_enabled_profiles_supported, null);

        final ListView lvItems = content != null ? content.findViewById(R.id.carrier_list) : null;
        if (lvItems != null) {
            setAdapter(lvItems);
            lvItems.setVisibility(View.VISIBLE);
            lvItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    onClick(position);
                }
            });
        }

        dialog.setView(content);
        updateDialog();

        return dialog;
    }

    /**
     * If the user click the item at the list, then it sends the callback.
     * @param selectionIndex the index of item in the list.
     */
    public void onClick(int selectionIndex) {
        final SimDialogActivity activity = (SimDialogActivity) getActivity();
        if (selectionIndex >= 0 && selectionIndex < mSubscriptions.size()) {
            int subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
            final SubscriptionInfo subscription = mSubscriptions.get(selectionIndex);
            if (subscription != null) {
                subId = subscription.getSubscriptionId();
            }
            activity.onSubscriptionSelected(getDialogType(), subId);
        }
        Log.d(TAG, "Start showing auto data switch dialog");
        activity.showEnableAutoDataSwitchDialog();
        if (getDialog() != null) getDialog().dismiss();
    }

    protected List<SubscriptionInfo> getCurrentSubscriptions() {
        final SubscriptionManager manager = getContext().getSystemService(
                SubscriptionManager.class);
        return manager.getActiveSubscriptionInfoList();
    }

    @Override
    public void updateDialog() {
        Log.d(TAG, "Dialog updated, dismiss status: " + mWasDismissed);
        if (mWasDismissed) {
            return;
        }

        List<SubscriptionInfo> currentSubscriptions = getCurrentSubscriptions();
        if (currentSubscriptions == null) {
            dismiss();
            return;
        }

        // Remove the provisioning or satellite eSIM from the subscription list.
        currentSubscriptions.removeIf(info -> info.isEmbedded()
            && (info.getProfileClass() == PROFILE_CLASS_PROVISIONING
                || (Flags.oemEnabledSatelliteFlag() && info.isOnlyNonTerrestrialNetwork())));

        boolean includeAskEveryTime = getArguments().getBoolean(KEY_INCLUDE_ASK_EVERY_TIME);
        boolean isCancelItemShowed = getArguments().getBoolean(KEY_SHOW_CANCEL_ITEM);
        if (includeAskEveryTime || isCancelItemShowed) {
            int arraySize = currentSubscriptions.size()
                    + (includeAskEveryTime ? 1 : 0)
                    + (isCancelItemShowed ? 1 : 0);
            final List<SubscriptionInfo> tmp = new ArrayList<>(arraySize);
            if (includeAskEveryTime) {
                // add the value of 'AskEveryTime' item
                tmp.add(null);
            }
            tmp.addAll(currentSubscriptions);
            if (isCancelItemShowed) {
                // add the value of 'Cancel' item
                tmp.add(null);
            }
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
    void setAdapter(ListView lvItems) {
        lvItems.setAdapter(mAdapter);
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

            if (sub == null) {
                if (position == 0) {
                    title.setText(R.string.sim_calls_ask_first_prefs_title);
                } else {
                    title.setText(R.string.sim_action_cancel);
                }
                summary.setVisibility(View.GONE);
            } else {
                title.setText(SubscriptionUtil.getUniqueSubscriptionDisplayName(sub, mContext));
                String phoneNumber = isMdnProvisioned(sub.getNumber()) ? sub.getNumber() : "";
                if (!TextUtils.isEmpty(phoneNumber)) {
                    summary.setVisibility(View.VISIBLE);
                    summary.setText(phoneNumber);
                } else {
                    summary.setVisibility(View.GONE);
                }
            }
            return convertView;
        }

        // An MDN is considered not provisioned if it's empty or all 0's
        private boolean isMdnProvisioned(String mdn) {
            return !(TextUtils.isEmpty(mdn) || mdn.matches("[\\D0]+"));
        }
    }
}
