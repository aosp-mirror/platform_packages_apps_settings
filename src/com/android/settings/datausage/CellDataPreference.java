/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.datausage;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkTemplate;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.preference.PreferenceViewHolder;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Checkable;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.CustomDialogPreference;
import com.android.settings.R;
import com.android.settings.Utils;

import java.util.List;

public class CellDataPreference extends CustomDialogPreference implements TemplatePreference {

    // TODO: Get Telephony to broadcast when this changes, and remove this.
    static final String ACTION_DATA_ENABLED_CHANGED =
            "com.android.settings.action.DATA_ENABLED_CHANGED";

    private static final String TAG = "CellDataPreference";

    public int mSubId;
    public boolean mChecked;
    public boolean mMultiSimDialog;
    private TelephonyManager mTelephonyManager;
    private SubscriptionManager mSubscriptionManager;

    public CellDataPreference(Context context, AttributeSet attrs) {
        super(context, attrs, android.R.attr.switchPreferenceStyle);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable s) {
        CellDataState state = (CellDataState) s;
        super.onRestoreInstanceState(state.getSuperState());
        mTelephonyManager = TelephonyManager.from(getContext());
        mChecked = state.mChecked;
        mMultiSimDialog = state.mMultiSimDialog;
        mSubId = state.mSubId;
        setKey(getKey() + mSubId);
        notifyChanged();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        CellDataState state = new CellDataState(super.onSaveInstanceState());
        state.mChecked = mChecked;
        state.mMultiSimDialog = mMultiSimDialog;
        state.mSubId = mSubId;
        return state;
    }

    @Override
    public void onAttached() {
        super.onAttached();
        getContext().registerReceiver(mReceiver, new IntentFilter(ACTION_DATA_ENABLED_CHANGED));
    }

    @Override
    public void onDetached() {
        getContext().unregisterReceiver(mReceiver);
        super.onDetached();
    }

    @Override
    public void setTemplate(NetworkTemplate template, int subId, NetworkServices services) {
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            throw new IllegalArgumentException("CellDataPreference needs a SubscriptionInfo");
        }
        mTelephonyManager = TelephonyManager.from(getContext());
        if (mSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            mSubId = subId;
            setKey(getKey() + subId);
        }
        updateChecked();
    }

    private void updateChecked() {
        setChecked(mTelephonyManager.getDataEnabled(mSubId));
    }

    @Override
    protected void performClick(View view) {
        super.performClick(view);
        MetricsLogger.action(getContext(), MetricsEvent.ACTION_CELL_DATA_TOGGLE, !mChecked);
        if (mChecked) {
            // disabling data; show confirmation dialog which eventually
            // calls setMobileDataEnabled() once user confirms.
            mMultiSimDialog = false;
            super.performClick(view);
        } else {
            // If we are showing the Sim Card tile then we are a Multi-Sim device.
            if (Utils.showSimCardTile(getContext())) {
                mMultiSimDialog = true;
                super.performClick(view);
            } else {
                setMobileDataEnabled(true);
            }
        }
    }

    private void setMobileDataEnabled(boolean enabled) {
        if (DataUsageSummary.LOGD) Log.d(TAG, "setMobileDataEnabled()");
        mTelephonyManager.setDataEnabled(mSubId, enabled);
        setChecked(enabled);
        getContext().sendBroadcast(new Intent(ACTION_DATA_ENABLED_CHANGED));
    }

    private void setChecked(boolean checked) {
        if (mChecked == checked) return;
        mChecked = checked;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        View switchView = holder.findViewById(android.R.id.switch_widget);
        switchView.setClickable(false);
        ((Checkable) switchView).setChecked(mChecked);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder,
            DialogInterface.OnClickListener listener) {
        if (mMultiSimDialog) {
            showMultiSimDialog(builder, listener);
        } else {
            showDisableDialog(builder, listener);
        }
    }

    private void showDisableDialog(AlertDialog.Builder builder,
            DialogInterface.OnClickListener listener) {
        builder.setTitle(null)
                .setMessage(R.string.data_usage_disable_mobile)
                .setPositiveButton(android.R.string.ok, listener)
                .setNegativeButton(android.R.string.cancel, null);
    }

    private void showMultiSimDialog(AlertDialog.Builder builder,
            DialogInterface.OnClickListener listener) {
        mSubscriptionManager = SubscriptionManager.from(getContext());
        final SubscriptionInfo currentSir = mSubscriptionManager.getActiveSubscriptionInfo(mSubId);

        final SubscriptionInfo nextSir = mSubscriptionManager.getDefaultDataSubscriptionInfo();

        // If the device is single SIM or is enabling data on the active data SIM then forgo
        // the pop-up.
        if (!Utils.showSimCardTile(getContext()) ||
                (nextSir != null && currentSir != null &&
                currentSir.getSubscriptionId() == nextSir.getSubscriptionId())) {
            setMobileDataEnabled(true);
            if (nextSir != null && currentSir != null &&
                currentSir.getSubscriptionId() == nextSir.getSubscriptionId()) {
                disableDataForOtherSubscriptions(mSubId);
            }
            return;
        }

        final String previousName = (nextSir == null)
            ? getContext().getResources().getString(R.string.sim_selection_required_pref)
            : nextSir.getDisplayName().toString();

        builder.setTitle(R.string.sim_change_data_title);
        builder.setMessage(getContext().getString(R.string.sim_change_data_message,
                String.valueOf(currentSir != null ? currentSir.getDisplayName() : null),
                previousName));

        builder.setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                mSubscriptionManager.setDefaultDataSubId(mSubId);
                setMobileDataEnabled(true);
                disableDataForOtherSubscriptions(mSubId);
            }
        });
        builder.setNegativeButton(R.string.cancel, null);

        builder.create().show();
    }

    private void disableDataForOtherSubscriptions(int subId) {
        List<SubscriptionInfo> subInfoList = mSubscriptionManager.getActiveSubscriptionInfoList();
        if (subInfoList != null) {
            for (SubscriptionInfo subInfo : subInfoList) {
                if (subInfo.getSubscriptionId() != subId) {
                    mTelephonyManager.setDataEnabled(subInfo.getSubscriptionId(), false);
                }
            }
        }
        getContext().sendBroadcast(new Intent(ACTION_DATA_ENABLED_CHANGED));
    }

    @Override
    protected void onClick(DialogInterface dialog, int which) {
        if (which != DialogInterface.BUTTON_POSITIVE) {
            return;
        }
        if (mMultiSimDialog) {
        } else {
            // TODO: extend to modify policy enabled flag.
            setMobileDataEnabled(false);
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateChecked();
        }
    };

    public static class CellDataState extends BaseSavedState {
        public int mSubId;
        public boolean mChecked;
        public boolean mMultiSimDialog;

        public CellDataState(Parcelable base) {
            super(base);
        }

        public CellDataState(Parcel source) {
            super(source);
            mChecked = source.readByte() != 0;
            mMultiSimDialog = source.readByte() != 0;
            mSubId = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeByte((byte) (mChecked ? 1 : 0));
            dest.writeByte((byte) (mMultiSimDialog ? 1 : 0));
            dest.writeInt(mSubId);
        }

        public static final Creator<CellDataState> CREATOR = new Creator<CellDataState>() {
            @Override
            public CellDataState createFromParcel(Parcel source) {
                return new CellDataState(source);
            }

            @Override
            public CellDataState[] newArray(int size) {
                return new CellDataState[size];
            }
        };
    }
}
