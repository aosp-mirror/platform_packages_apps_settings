/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SubscriptionManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;

/** This dialog activity advise the user to have connectivity if the eSIM uses a RAC. */
public class EuiccRacConnectivityDialogActivity extends FragmentActivity
        implements WarningDialogFragment.OnConfirmListener {

    private static final String TAG = "EuiccRacConnectivityDialogActivity";
    // Dialog tags
    private static final int DIALOG_TAG_ERASE_ANYWAY_CONFIRMATION = 1;
    private static final String ARG_SUB_ID = "sub_id";
    private static final String ARG_RESET_MOBILE_NETWORK_ID = "reset_mobile_netword_id";

    private int mSubId;
    @Nullable
    private Intent mResetMobileNetworkIntent;

    /**
     * Returns an intent of EuiccRacConnectivityDialogActivity for Settings: erase eSIM.
     *
     * @param context The context used to start the EuiccRacConnectivityDialogActivity.
     * @param subId The subscription ID of the subscription needs to be deleted. If the subscription
     *     belongs to a group of subscriptions, all subscriptions from the group will be deleted.
     */
    @NonNull
    public static Intent getIntent(@NonNull Context context, int subId) {
        Intent intent = new Intent(context, EuiccRacConnectivityDialogActivity.class);
        intent.putExtra(ARG_SUB_ID, subId);
        return intent;
    }

    /**
     * Returns an intent of EuiccRacConnectivityDialogActivity for Reset: Mobile network settings.
     *
     * @param context The context used to start the EuiccRacConnectivityDialogActivity.
     * @param resetMobileNetworkIntent The intent that will continue the reset of mobile network
     *     settings.
     */
    @NonNull
    public static Intent getIntent(@NonNull Context context,
            @NonNull Intent resetMobileNetworkIntent) {
        Intent intent = new Intent(context, EuiccRacConnectivityDialogActivity.class);
        intent.putExtra(ARG_RESET_MOBILE_NETWORK_ID, resetMobileNetworkIntent);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        mSubId = intent.getIntExtra(ARG_SUB_ID, SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        mResetMobileNetworkIntent =
                intent.getParcelableExtra(ARG_RESET_MOBILE_NETWORK_ID, Intent.class);

        if (savedInstanceState == null) {
            showConnectivityWarningDialog();
        }
    }

    @Override
    public void onConfirm(int tag, boolean confirmed) {
        if (!confirmed) {
            finish();
            return;
        }

        switch (tag) {
            case DIALOG_TAG_ERASE_ANYWAY_CONFIRMATION:
                finish();
                if (mResetMobileNetworkIntent != null) {
                    Log.i(TAG, "Show fragment activity that handles mobile network settings reset");
                    new SubSettingLauncher(this).launchWithIntent(mResetMobileNetworkIntent);
                } else {
                    Log.i(TAG, "Show dialogue activity that handles deleting eSIM profiles");
                    startActivity(DeleteEuiccSubscriptionDialogActivity.getIntent(this, mSubId));
                }
                break;
            default:
                Log.e(TAG, "Unrecognized confirmation dialog tag: " + tag);
                break;
        }
    }

    /* Displays warning to have connectivity because subscription is RAC dialog. */
    private void showConnectivityWarningDialog() {
        WarningDialogFragment.show(
                this,
                WarningDialogFragment.OnConfirmListener.class,
                DIALOG_TAG_ERASE_ANYWAY_CONFIRMATION,
                getString(R.string.wifi_warning_dialog_title),
                getString(R.string.wifi_warning_dialog_text),
                getString(R.string.wifi_warning_continue_button),
                getString(R.string.wifi_warning_return_button));
    }
}
