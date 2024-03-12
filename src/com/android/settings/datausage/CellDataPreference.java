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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.net.NetworkTemplate;
import android.os.Parcel;
import android.os.Parcelable;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.network.MobileDataEnabledListener;
import com.android.settings.network.ProxySubscriptionManager;
import com.android.settings.network.SubscriptionUtil;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.CustomDialogPreferenceCompat;

/**
 * Preference of cellular data control within Data Usage
 */
public class CellDataPreference extends CustomDialogPreferenceCompat
        implements TemplatePreference, MobileDataEnabledListener.Client {

    private static final String TAG = "CellDataPreference";

    public int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    public boolean mChecked;
    public boolean mMultiSimDialog;
    private final MobileDataEnabledListener mDataStateListener;

    public CellDataPreference(Context context, AttributeSet attrs) {
        super(context, attrs, androidx.preference.R.attr.switchPreferenceCompatStyle);
        mDataStateListener = new MobileDataEnabledListener(context, this);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable s) {
        final CellDataState state = (CellDataState) s;
        super.onRestoreInstanceState(state.getSuperState());
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
        final CellDataState state = new CellDataState(super.onSaveInstanceState());
        state.mChecked = mChecked;
        state.mMultiSimDialog = mMultiSimDialog;
        state.mSubId = mSubId;
        return state;
    }

    @Override
    public void onAttached() {
        super.onAttached();
        mDataStateListener.start(mSubId);
        getProxySubscriptionManager()
                .addActiveSubscriptionsListener(mOnSubscriptionsChangeListener);
    }

    @Override
    public void onDetached() {
        mDataStateListener.stop();
        getProxySubscriptionManager()
                .removeActiveSubscriptionsListener(mOnSubscriptionsChangeListener);
        super.onDetached();
    }

    @Override
    public void setTemplate(NetworkTemplate template, int subId) {
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            throw new IllegalArgumentException("CellDataPreference needs a SubscriptionInfo");
        }

        getProxySubscriptionManager()
                .addActiveSubscriptionsListener(mOnSubscriptionsChangeListener);

        if (mSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            mSubId = subId;
            setKey(getKey() + subId);
        }
        updateEnabled();
        updateChecked();
    }

    @VisibleForTesting
    ProxySubscriptionManager getProxySubscriptionManager() {
        return ProxySubscriptionManager.getInstance(getContext());
    }

    @VisibleForTesting
    SubscriptionInfo getActiveSubscriptionInfo(int subId) {
        return getProxySubscriptionManager().getActiveSubscriptionInfo(subId);
    }

    private void updateChecked() {
        setChecked(getContext().getSystemService(TelephonyManager.class).getDataEnabled(mSubId));
    }

    private void updateEnabled() {
        // If this subscription is not active, for example, SIM card is taken out, we disable
        // the button.
        setEnabled(getActiveSubscriptionInfo(mSubId) != null);
    }

    @Override
    protected void performClick(View view) {
        final Context context = getContext();
        FeatureFactory.getFeatureFactory().getMetricsFeatureProvider()
                .action(context, SettingsEnums.ACTION_CELL_DATA_TOGGLE, !mChecked);
        final SubscriptionInfo currentSir = getActiveSubscriptionInfo(mSubId);
        final SubscriptionInfo nextSir = getActiveSubscriptionInfo(
                SubscriptionManager.getDefaultDataSubscriptionId());
        if (mChecked) {
            setMobileDataEnabled(false);
            if (nextSir != null && currentSir != null
                    && currentSir.getSubscriptionId() == nextSir.getSubscriptionId()) {
                disableDataForOtherSubscriptions(mSubId);
            }
        } else {
            setMobileDataEnabled(true);
        }
    }

    private void setMobileDataEnabled(boolean enabled) {
        if (DataUsageSummary.LOGD) Log.d(TAG, "setMobileDataEnabled(" + enabled + ","
                + mSubId + ")");
        getContext().getSystemService(TelephonyManager.class).setDataEnabled(mSubId, enabled);
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
        final CompoundButton switchView =
                (CompoundButton) holder.findViewById(androidx.preference.R.id.switchWidget);
        switchView.setClickable(false);
        switchView.setChecked(mChecked);
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder,
            DialogInterface.OnClickListener listener) {
        if (mMultiSimDialog) {
            showMultiSimDialog(builder, listener);
        } else {
            showDisableDialog(builder, listener);
        }
    }

    private void showDisableDialog(Builder builder,
            DialogInterface.OnClickListener listener) {
        builder.setTitle(null)
                .setMessage(R.string.data_usage_disable_mobile)
                .setPositiveButton(android.R.string.ok, listener)
                .setNegativeButton(android.R.string.cancel, null);
    }

    private void showMultiSimDialog(Builder builder,
            DialogInterface.OnClickListener listener) {
        final SubscriptionInfo currentSir = getActiveSubscriptionInfo(mSubId);
        final SubscriptionInfo nextSir = getActiveSubscriptionInfo(
                SubscriptionManager.getDefaultDataSubscriptionId());

        final String previousName = (nextSir == null)
            ? getContext().getResources().getString(R.string.sim_selection_required_pref)
                : SubscriptionUtil.getUniqueSubscriptionDisplayName(
                        nextSir, getContext()).toString();

        builder.setTitle(R.string.sim_change_data_title);
        builder.setMessage(getContext().getString(R.string.sim_change_data_message,
                String.valueOf(currentSir != null
                    ? SubscriptionUtil.getUniqueSubscriptionDisplayName(currentSir, getContext())
                    : null), previousName));

        builder.setPositiveButton(R.string.okay, listener);
        builder.setNegativeButton(R.string.cancel, null);
    }

    private void disableDataForOtherSubscriptions(int subId) {
        final SubscriptionInfo subInfo = getActiveSubscriptionInfo(subId);
        if (subInfo != null) {
            getContext().getSystemService(TelephonyManager.class).setDataEnabled(subId, false);
        }
    }

    @Override
    protected void onClick(DialogInterface dialog, int which) {
        if (which != DialogInterface.BUTTON_POSITIVE) {
            return;
        }
        if (mMultiSimDialog) {
            getProxySubscriptionManager().get().setDefaultDataSubId(mSubId);
            setMobileDataEnabled(true);
            disableDataForOtherSubscriptions(mSubId);
        } else {
            // TODO: extend to modify policy enabled flag.
            setMobileDataEnabled(false);
        }
    }

    @VisibleForTesting
    final ProxySubscriptionManager.OnActiveSubscriptionChangedListener
            mOnSubscriptionsChangeListener =
            new ProxySubscriptionManager.OnActiveSubscriptionChangedListener() {
                public void onChanged() {
                    if (DataUsageSummary.LOGD) {
                        Log.d(TAG, "onSubscriptionsChanged");
                    }
                    updateEnabled();
                }
            };

    /**
     * Implementation of {@code MobileDataEnabledListener.Client}
    */
    @VisibleForTesting
    public void onMobileDataEnabledChange() {
        updateChecked();
    }

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
