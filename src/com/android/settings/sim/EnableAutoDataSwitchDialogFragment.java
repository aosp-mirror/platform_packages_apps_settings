/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.network.SubscriptionUtil;

import java.util.List;

/**
 * Show a dialog prompting the user to enable auto data switch following the dialog where user chose
 * default data SIM.
 */
public class EnableAutoDataSwitchDialogFragment extends SimDialogFragment implements
        DialogInterface.OnClickListener {
    private static final String TAG = "EnableAutoDataSwitchDialogFragment";
    /** Sub Id of the non-default data SIM */
    private int mBackupDataSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

    /** @return a new instance of this fragment */
    public static EnableAutoDataSwitchDialogFragment newInstance() {
        final EnableAutoDataSwitchDialogFragment fragment =
                new EnableAutoDataSwitchDialogFragment();
        final Bundle args = initArguments(SimDialogActivity.ENABLE_AUTO_DATA_SWITCH,
                R.string.enable_auto_data_switch_dialog_title);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setPositiveButton(R.string.yes, this)
                .setNegativeButton(R.string.sim_action_no_thanks, null)
                .create();
        updateDialog(dialog);
        return dialog;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_AUTO_DATA_SWITCH;
    }

    /** update dialog */
    public void updateDialog(AlertDialog dialog) {
        Log.d(TAG, "Dialog updated, dismiss status: " + mWasDismissed);

        if (mWasDismissed) {
            return;
        }

        if (dialog == null) {
            Log.d(TAG, "Dialog is null.");
            dismiss();
            return;
        }

        // Set message
        View content = LayoutInflater.from(getContext()).inflate(
                R.layout.sim_confirm_dialog_multiple_enabled_profiles_supported, null);
        TextView dialogMessage = content != null ? content.findViewById(R.id.msg) : null;
        final String message = getMessage();
        if (TextUtils.isEmpty(message) || dialogMessage == null) {
            onDismiss(dialog);
            return;
        }
        dialogMessage.setText(message);
        dialogMessage.setVisibility(View.VISIBLE);
        dialog.setView(content);

        // Set title
        View titleView = LayoutInflater.from(getContext()).inflate(
                R.layout.sim_confirm_dialog_title_multiple_enabled_profiles_supported, null);
        TextView titleTextView = titleView.findViewById(R.id.title);
        titleTextView.setText(getContext().getString(getTitleResId()));
        dialog.setCustomTitle(titleTextView);
    }

    /**
     * @return The message of the dialog. {@code null} if the dialog shouldn't be displayed.
     */
    @VisibleForTesting
    protected String getMessage() {
        int ddsSubId = getDefaultDataSubId();
        if (ddsSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) return null;
        Log.d(TAG, "DDS SubId: " + ddsSubId);

        SubscriptionManager subscriptionManager = getSubscriptionManager();
        List<SubscriptionInfo> activeSubscriptions  = subscriptionManager
                .getActiveSubscriptionInfoList();
        if (activeSubscriptions == null) return null;

        // Find if a backup data sub exists.
        SubscriptionInfo backupSubInfo = activeSubscriptions.stream()
                .filter(subInfo -> subInfo.getSubscriptionId() != ddsSubId)
                .findFirst()
                .orElse(null);
        if (backupSubInfo == null) return null;
        mBackupDataSubId = backupSubInfo.getSubscriptionId();

        // Check if auto data switch is already enabled
        final TelephonyManager telephonyManager = getTelephonyManagerForSub(mBackupDataSubId);
        if (telephonyManager == null) {
            Log.d(TAG, "telephonyManager for " + mBackupDataSubId + " is null");
            return null;
        }
        if (telephonyManager.isMobileDataPolicyEnabled(
                TelephonyManager.MOBILE_DATA_POLICY_AUTO_DATA_SWITCH)) {
            Log.d(TAG, "AUTO_DATA_SWITCH already enabled");
            return null;
        }

        Log.d(TAG, "Backup data sub Id: " + mBackupDataSubId);
        // The description of the feature
        String message =
                getContext().getString(
                        R.string.enable_auto_data_switch_dialog_message,
                        SubscriptionUtil.getUniqueSubscriptionDisplayName(
                                backupSubInfo, getContext()));
        UserManager userManager = getUserManager();
        if (userManager == null) return message;

        // If one of the sub is dedicated to work profile(enterprise-managed), which means we might
        // switching between personal & work profile, append a warning to the message.
        UserHandle ddsUserHandle = subscriptionManager.getSubscriptionUserHandle(ddsSubId);
        UserHandle nDdsUserHandle = subscriptionManager.getSubscriptionUserHandle(mBackupDataSubId);
        boolean isDdsManaged = ddsUserHandle != null && userManager.isManagedProfile(
                ddsUserHandle.getIdentifier());
        boolean isNDdsManaged = nDdsUserHandle != null && userManager.isManagedProfile(
                nDdsUserHandle.getIdentifier());
        Log.d(TAG, "isDdsManaged= " + isDdsManaged + " isNDdsManaged=" + isNDdsManaged);
        if (isDdsManaged ^ isNDdsManaged) {
            message += getContext().getString(
                    R.string.auto_data_switch_dialog_managed_profile_warning);
        }

        return message;
    }

    @Override
    public void updateDialog() {
        updateDialog((AlertDialog) getDialog());
    }

    @Override
    public void onClick(DialogInterface dialog, int buttonClicked) {
        if (buttonClicked != DialogInterface.BUTTON_POSITIVE) {
            return;
        }
        final SimDialogActivity activity = (SimDialogActivity) getActivity();
        if (mBackupDataSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            activity.onSubscriptionSelected(getDialogType(), mBackupDataSubId);
        }
    }

    private TelephonyManager getTelephonyManagerForSub(int subId) {
        return getContext().getSystemService(TelephonyManager.class)
                .createForSubscriptionId(subId);
    }

    private SubscriptionManager getSubscriptionManager() {
        return getContext().getSystemService(SubscriptionManager.class).createForAllUserProfiles();
    }

    @VisibleForTesting
    protected int getDefaultDataSubId() {
        return SubscriptionManager.getDefaultDataSubscriptionId();
    }

    private UserManager getUserManager() {
        return getContext().getSystemService(UserManager.class);
    }
}
