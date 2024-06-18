/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.sim;

import static android.telephony.SubscriptionManager.PROFILE_CLASS_PROVISIONING;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.DialogInterface;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;

import com.android.internal.telephony.flags.Flags;
import com.android.settings.R;
import com.android.settings.network.SubscriptionUtil;

import java.util.List;

/**
 * Presents a dialog asking the user if they want to switch the data to another sim
 */
public class SelectSpecificDataSimDialogFragment extends SimDialogFragment implements
        DialogInterface.OnClickListener {
    private static final String TAG = "PreferredSimDialogFrag";

    private SubscriptionInfo mSubscriptionInfo;

    /**
     * @return the dialog fragment.
     */
    public static SelectSpecificDataSimDialogFragment newInstance() {
        final SelectSpecificDataSimDialogFragment
                fragment = new SelectSpecificDataSimDialogFragment();
        final Bundle args = initArguments(SimDialogActivity.DATA_PICK,
                R.string.select_specific_sim_for_data_title);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setNegativeButton(R.string.sim_action_no_thanks, null)
                .create();
        updateDialog(dialog);
        return dialog;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        Log.d(TAG, "Dialog onDismiss, dismiss status: " + mWasDismissed);
        if (!mWasDismissed) {
            // This dialog might be called onDismiss twice due to first time called by onDismiss()
            // as a consequence of user action. We need this fragment alive so the activity
            // doesn't end, which allows the following dialog to attach. Upon the second dialog
            // dismiss, this fragment is removed from SimDialogActivity.onFragmentDismissed to
            // end the activity.
            mWasDismissed = true;
            final SimDialogActivity activity = (SimDialogActivity) getActivity();
            activity.showEnableAutoDataSwitchDialog();
            // Not using super.onDismiss because it will result in an immediate end of the activity,
            // before the second auto data switch dialog can attach.
            if (getDialog() != null) getDialog().dismiss();
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int buttonClicked) {
        final SimDialogActivity activity = (SimDialogActivity) getActivity();
        if (buttonClicked == DialogInterface.BUTTON_POSITIVE) {
            final SubscriptionInfo info = getTargetSubscriptionInfo();
            if (info != null) {
                activity.onSubscriptionSelected(getDialogType(), info.getSubscriptionId());
            }
        }
    }

    private SubscriptionInfo getNonDefaultDataSubscriptionInfo(SubscriptionInfo dds) {
        List<SubscriptionInfo> subInfos = getSubscriptionManager().getActiveSubscriptionInfoList();
        if (subInfos == null || dds == null) {
            return null;
        }
        return subInfos.stream().filter(subinfo -> subinfo.getSubscriptionId()
                != dds.getSubscriptionId()).findFirst().orElse(null);
    }

    private SubscriptionInfo getDefaultDataSubInfo() {
        return getSubscriptionManager().getDefaultDataSubscriptionInfo();
    }

    private void updateDialog(AlertDialog dialog) {
        Log.d(TAG, "Dialog updated, dismiss status: " + mWasDismissed);
        if (mWasDismissed) {
            return;
        }

        if (dialog == null) {
            Log.d(TAG, "Dialog is null.");
            dismiss();
        }

        SubscriptionInfo currentDataSubInfo = getDefaultDataSubInfo();
        SubscriptionInfo newSubInfo = getNonDefaultDataSubscriptionInfo(currentDataSubInfo);

        if (newSubInfo == null || currentDataSubInfo == null) {
            Log.d(TAG, "one of target SubscriptionInfos is null");
            dismiss();
            return;
        }

        if ((newSubInfo.isEmbedded()
            && (newSubInfo.getProfileClass() == PROFILE_CLASS_PROVISIONING
                || (Flags.oemEnabledSatelliteFlag()
                    && newSubInfo.isOnlyNonTerrestrialNetwork())))
            || (currentDataSubInfo.isEmbedded()
            && (currentDataSubInfo.getProfileClass() == PROFILE_CLASS_PROVISIONING
                || (Flags.oemEnabledSatelliteFlag()
                    && currentDataSubInfo.isOnlyNonTerrestrialNetwork())))) {
            Log.d(TAG, "do not set the provisioning or satellite eSIM");
            dismiss();
            return;
        }

        Log.d(TAG, "newSubId: " + newSubInfo.getSubscriptionId()
                + "currentDataSubID: " + currentDataSubInfo.getSubscriptionId());
        setTargetSubscriptionInfo(newSubInfo);

        CharSequence newDataCarrierName = SubscriptionUtil.getUniqueSubscriptionDisplayName(
                newSubInfo, getContext());
        CharSequence currentDataCarrierName = SubscriptionUtil.getUniqueSubscriptionDisplayName(
                currentDataSubInfo, getContext());

        String positive = getContext().getString(
                R.string.select_specific_sim_for_data_button, newDataCarrierName);
        String message = getContext().getString(R.string.select_specific_sim_for_data_msg,
                newDataCarrierName, currentDataCarrierName);

        View content = LayoutInflater.from(getContext()).inflate(
                R.layout.sim_confirm_dialog_multiple_enabled_profiles_supported, null);
        TextView dialogMessage = content != null ? content.findViewById(R.id.msg) : null;
        if (!TextUtils.isEmpty(message) && dialogMessage != null) {
            dialogMessage.setText(message);
            dialogMessage.setVisibility(View.VISIBLE);
        }
        dialog.setView(content);

        View titleView = LayoutInflater.from(getContext()).inflate(
                R.layout.sim_confirm_dialog_title_multiple_enabled_profiles_supported, null);
        TextView titleTextView = titleView.findViewById(R.id.title);
        titleTextView.setText(getContext().getString(getTitleResId(), newDataCarrierName));

        dialog.setCustomTitle(titleTextView);
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, positive, this);
    }

    private void setTargetSubscriptionInfo(SubscriptionInfo subInfo) {
        mSubscriptionInfo = subInfo;
    }

    private SubscriptionInfo getTargetSubscriptionInfo() {
        return mSubscriptionInfo;
    }

    @Override
    public void updateDialog() {
        updateDialog((AlertDialog) getDialog());
    }

    @VisibleForTesting
    protected SubscriptionManager getSubscriptionManager() {
        return getContext().getSystemService(SubscriptionManager.class).createForAllUserProfiles();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_SPECIFIC_DDS_SIM_PICKER;
    }
}
