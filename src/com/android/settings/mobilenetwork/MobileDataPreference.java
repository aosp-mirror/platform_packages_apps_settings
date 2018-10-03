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

package com.android.settings.mobilenetwork;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.Settings.Global;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Checkable;

import androidx.preference.DialogPreference;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;

import java.util.List;

/**
 * Customized Preference to enable / disable mobile data.
 * Basically copy of with com.android.settings.CellDataPreference.
 */
public class MobileDataPreference extends DialogPreference implements
        DialogInterface.OnClickListener {

    private static final boolean DBG = false;
    private static final String TAG = "MobileDataPreference";

    public int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    public boolean mChecked;
    // Whether to show the dialog to ask switching default data subscription.
    // Should be true only when a multi-sim phone only supports data connection on a single phone,
    // and user is enabling data on the non-default phone.
    public boolean mMultiSimDialog;
    private TelephonyManager mTelephonyManager;
    private SubscriptionManager mSubscriptionManager;

    public MobileDataPreference(Context context, AttributeSet attrs) {
        super(context, attrs, com.android.internal.R.attr.switchPreferenceStyle);
    }

    // Must be called to avoid binder leakage.
    void dispose() {
        mListener.setListener(false, mSubId, getContext());
    }

    @Override
    protected void onRestoreInstanceState(Parcelable s) {
        CellDataState state = (CellDataState) s;
        super.onRestoreInstanceState(state.getSuperState());
        mTelephonyManager = TelephonyManager.from(getContext());
        mChecked = state.mChecked;
        mMultiSimDialog = state.mMultiSimDialog;
        if (mSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            mSubId = state.mSubId;
            setKey(getKey() + mSubId);
        }
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
        mListener.setListener(true, mSubId, getContext());
    }

    @Override
    protected void onPrepareForRemoval() {
        mListener.setListener(false, mSubId, getContext());
        super.onPrepareForRemoval();
    }

    /**
     * Initialize this preference with subId.
     */
    public void initialize(int subId) {
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            throw new IllegalArgumentException("MobileDataPreference needs a SubscriptionInfo");
        }
        mSubscriptionManager = SubscriptionManager.from(getContext());
        mTelephonyManager = TelephonyManager.from(getContext());
        if (mSubId != subId) {
            mSubId = subId;
            setKey(getKey() + subId);
        }
        updateChecked();
    }

    private void updateChecked() {
        setChecked(mTelephonyManager.getDataEnabled(mSubId));
    }

    @Override
    public void performClick() {
        if (!isEnabled() || !SubscriptionManager.isValidSubscriptionId(mSubId)) {
            return;
        }
        final SubscriptionInfo currentSir = mSubscriptionManager.getActiveSubscriptionInfo(
                mSubId);
        final SubscriptionInfo nextSir = mSubscriptionManager.getDefaultDataSubscriptionInfo();
        final boolean isMultiSim = (mTelephonyManager.getSimCount() > 1);
        final boolean isMultipleDataOnCapable =
                (mTelephonyManager.getNumberOfModemsWithSimultaneousDataConnections() > 1);
        final boolean isDefaultDataSubscription = (nextSir != null && currentSir != null
                && currentSir.getSubscriptionId() == nextSir.getSubscriptionId());
        if (mChecked) {
            if (!isMultiSim) {
                // disabling data; show confirmation dialog which eventually
                // calls setMobileDataEnabled() once user confirms.
                mMultiSimDialog = false;
                super.performClick();
            } else {
                // Don't show any dialog.
                setMobileDataEnabled(false /* enabled */, false /* disableOtherSubscriptions */);
            }
        } else {
            if (isMultiSim && !isMultipleDataOnCapable && !isDefaultDataSubscription) {
                // enabling data and setting to default; show confirmation dialog which eventually
                // calls setMobileDataEnabled() once user confirms.
                mMultiSimDialog = true;
                super.performClick();
            } else {
                // Don't show any dialog.
                setMobileDataEnabled(true /* enabled */, false /* disableOtherSubscriptions */);
            }
        }
    }

    private void setMobileDataEnabled(boolean enabled, boolean disableOtherSubscriptions) {
        if (DBG) Log.d(TAG, "setMobileDataEnabled(" + enabled + "," + mSubId + ")");

        MetricsLogger.action(getContext(), MetricsEvent.ACTION_MOBILE_NETWORK_MOBILE_DATA_TOGGLE,
                enabled);

        mTelephonyManager.setDataEnabled(mSubId, enabled);

        if (disableOtherSubscriptions) {
            disableDataForOtherSubscriptions(mSubId);
        }

        setChecked(enabled);
    }

    private void setChecked(boolean checked) {
        if (mChecked == checked) return;
        mChecked = checked;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        View checkableView = holder.findViewById(com.android.internal.R.id.switch_widget);
        checkableView.setClickable(false);
        ((Checkable) checkableView).setChecked(mChecked);
    }

    //TODO(b/114749736): move it to preference controller
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        if (mMultiSimDialog) {
            showMultiSimDialog(builder);
        } else {
            showDisableDialog(builder);
        }
    }

    private void showDisableDialog(AlertDialog.Builder builder) {
        builder.setTitle(null)
                .setMessage(R.string.data_usage_disable_mobile)
                .setPositiveButton(android.R.string.ok, this)
                .setNegativeButton(android.R.string.cancel, null);
    }

    private void showMultiSimDialog(AlertDialog.Builder builder) {
        final SubscriptionInfo currentSir = mSubscriptionManager.getActiveSubscriptionInfo(mSubId);
        final SubscriptionInfo nextSir = mSubscriptionManager.getDefaultDataSubscriptionInfo();

        final String previousName = (nextSir == null)
                ? getContext().getResources().getString(R.string.sim_selection_required_pref)
                : nextSir.getDisplayName().toString();

        builder.setTitle(R.string.sim_change_data_title);
        builder.setMessage(getContext().getString(R.string.sim_change_data_message,
                String.valueOf(currentSir != null ? currentSir.getDisplayName() : null),
                previousName));

        builder.setPositiveButton(android.R.string.ok, this);
        builder.setNegativeButton(R.string.cancel, null);
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
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which != DialogInterface.BUTTON_POSITIVE) {
            return;
        }
        if (mMultiSimDialog) {
            mSubscriptionManager.setDefaultDataSubId(mSubId);
            setMobileDataEnabled(true /* enabled */, true /* disableOtherSubscriptions */);
        } else {
            // TODO: extend to modify policy enabled flag.
            setMobileDataEnabled(false /* enabled */, false /* disableOtherSubscriptions */);
        }
    }

    private final DataStateListener mListener = new DataStateListener() {
        @Override
        public void onChange(boolean selfChange) {
            updateChecked();
        }
    };

    /**
     * Listener that listens mobile data state change.
     */
    public abstract static class DataStateListener extends ContentObserver {
        public DataStateListener() {
            super(new Handler(Looper.getMainLooper()));
        }

        /**
         * Set / Unset data state listening, specifying subId.
         */
        public void setListener(boolean listening, int subId, Context context) {
            if (listening) {
                Uri uri = Global.getUriFor(Global.MOBILE_DATA);
                if (TelephonyManager.getDefault().getSimCount() != 1) {
                    uri = Global.getUriFor(Global.MOBILE_DATA + subId);
                }
                context.getContentResolver().registerContentObserver(uri, false, this);
            } else {
                context.getContentResolver().unregisterContentObserver(this);
            }
        }
    }

    /**
     * Class that represents state of mobile data state.
     * Used by onSaveInstanceState and onRestoreInstanceState.
     */
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
