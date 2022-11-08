/*
 * Copyright (C) 2014 The Android Open Source Project
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

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.android.settings.R;
import com.android.settings.network.SubscriptionUtil;
import com.android.settings.network.telephony.SubscriptionActionDialogActivity;

import java.util.List;

/**
 * This activity provides singleton semantics per dialog type for showing various kinds of
 * dialogs asking the user to make choices about which SIM to use for various services
 * (calls, SMS, and data).
 */
public class SimDialogActivity extends FragmentActivity {
    private static String TAG = "SimDialogActivity";

    public static String PREFERRED_SIM = "preferred_sim";
    public static String DIALOG_TYPE_KEY = "dialog_type";
    // sub ID returned from startActivityForResult
    public static String RESULT_SUB_ID = "result_sub_id";
    public static final int INVALID_PICK = -1;
    public static final int DATA_PICK = 0;
    public static final int CALLS_PICK = 1;
    public static final int SMS_PICK = 2;
    public static final int PREFERRED_PICK = 3;
    // Show the "select SMS subscription" dialog, but don't save as default, just return a result
    public static final int SMS_PICK_FOR_MESSAGE = 4;
    // Dismiss the current dialog and finish the activity.
    public static final int PICK_DISMISS = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!SubscriptionUtil.isSimHardwareVisible(this)) {
            Log.d(TAG, "Not support on device without SIM.");
            finish();
            return;
        }
        SimDialogProhibitService.supportDismiss(this);

        getWindow().addSystemFlags(
                WindowManager.LayoutParams.SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS);
        showOrUpdateDialog();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        showOrUpdateDialog();
    }

    private int getProgressState() {
        final SharedPreferences prefs = getSharedPreferences(
                SubscriptionActionDialogActivity.SIM_ACTION_DIALOG_PREFS, MODE_PRIVATE);
        return prefs.getInt(SubscriptionActionDialogActivity.KEY_PROGRESS_STATE,
                SubscriptionActionDialogActivity.PROGRESS_IS_NOT_SHOWING);
    }

    private void showOrUpdateDialog() {
        final int dialogType = getIntent().getIntExtra(DIALOG_TYPE_KEY, INVALID_PICK);

        if (dialogType == PICK_DISMISS) {
            finishAndRemoveTask();
            return;
        }

        if (dialogType == PREFERRED_PICK
                && getProgressState() == SubscriptionActionDialogActivity.PROGRESS_IS_SHOWING) {
            Log.d(TAG, "Finish the sim dialog since the sim action dialog is showing the progress");
            finish();
            return;
        }

        final String tag = Integer.toString(dialogType);
        final FragmentManager fragmentManager = getSupportFragmentManager();
        SimDialogFragment fragment = (SimDialogFragment) fragmentManager.findFragmentByTag(tag);

        if (fragment == null) {
            fragment = createFragment(dialogType);
            fragment.show(fragmentManager, tag);
        } else {
            fragment.updateDialog();
        }
    }

    private SimDialogFragment createFragment(int dialogType) {
        switch (dialogType) {
            case DATA_PICK:
                return getDataPickDialogFramgent();
            case CALLS_PICK:
                return CallsSimListDialogFragment.newInstance(dialogType,
                        R.string.select_sim_for_calls,
                        true /* includeAskEveryTime */,
                        false /* isCancelItemShowed */);
            case SMS_PICK:
                return SimListDialogFragment.newInstance(dialogType, R.string.select_sim_for_sms,
                        true /* includeAskEveryTime */,
                        false /* isCancelItemShowed */);
            case PREFERRED_PICK:
                if (!getIntent().hasExtra(PREFERRED_SIM)) {
                    throw new IllegalArgumentException("Missing required extra " + PREFERRED_SIM);
                }
                return PreferredSimDialogFragment.newInstance();
            case SMS_PICK_FOR_MESSAGE:
                return SimListDialogFragment.newInstance(dialogType, R.string.select_sim_for_sms,
                        false /* includeAskEveryTime */,
                        false /* isCancelItemShowed */);
            default:
                throw new IllegalArgumentException("Invalid dialog type " + dialogType + " sent.");
        }
    }

    private SimDialogFragment getDataPickDialogFramgent() {
        if (SubscriptionManager.getDefaultDataSubscriptionId()
                == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return SimListDialogFragment.newInstance(DATA_PICK, R.string.select_sim_for_data,
                    false /* includeAskEveryTime */,
                    true /* isCancelItemShowed */);
        }
        return SelectSpecificDataSimDialogFragment.newInstance();
    }

    public void onSubscriptionSelected(int dialogType, int subId) {
        if (getSupportFragmentManager().findFragmentByTag(Integer.toString(dialogType)) == null) {
            Log.w(TAG, "onSubscriptionSelected ignored because stored fragment was null");
            return;
        }
        switch (dialogType) {
            case DATA_PICK:
                setDefaultDataSubId(subId);
                break;
            case CALLS_PICK:
                setDefaultCallsSubId(subId);
                break;
            case SMS_PICK:
                setDefaultSmsSubId(subId);
                break;
            case PREFERRED_PICK:
                setPreferredSim(subId);
                break;
            case SMS_PICK_FOR_MESSAGE:
                // Don't set a default here.
                // The caller has created this dialog waiting for a result.
                Intent intent = new Intent();
                intent.putExtra(RESULT_SUB_ID, subId);
                setResult(Activity.RESULT_OK, intent);
                break;
            default:
                throw new IllegalArgumentException(
                        "Invalid dialog type " + dialogType + " sent.");
        }
    }

    public void onFragmentDismissed(SimDialogFragment simDialogFragment) {
        final List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments.size() == 1 && fragments.get(0) == simDialogFragment) {
            finishAndRemoveTask();
        }
    }

    private void setDefaultDataSubId(final int subId) {
        final SubscriptionManager subscriptionManager = getSystemService(SubscriptionManager.class);
        final TelephonyManager telephonyManager = getSystemService(
                TelephonyManager.class).createForSubscriptionId(subId);
        subscriptionManager.setDefaultDataSubId(subId);
        if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            telephonyManager.setDataEnabled(true);
            Toast.makeText(this, R.string.data_switch_started, Toast.LENGTH_LONG).show();
        }
    }

    private void setDefaultCallsSubId(final int subId) {
        final PhoneAccountHandle phoneAccount = subscriptionIdToPhoneAccountHandle(subId);
        final TelecomManager telecomManager = getSystemService(TelecomManager.class);
        telecomManager.setUserSelectedOutgoingPhoneAccount(phoneAccount);
    }

    private void setDefaultSmsSubId(final int subId) {
        final SubscriptionManager subscriptionManager = getSystemService(SubscriptionManager.class);
        subscriptionManager.setDefaultSmsSubId(subId);
    }

    private void setPreferredSim(final int subId) {
        setDefaultDataSubId(subId);
        setDefaultSmsSubId(subId);
        setDefaultCallsSubId(subId);
    }

    private PhoneAccountHandle subscriptionIdToPhoneAccountHandle(final int subId) {
        final TelecomManager telecomManager = getSystemService(TelecomManager.class);
        final TelephonyManager telephonyManager = getSystemService(TelephonyManager.class);

        for (PhoneAccountHandle handle : telecomManager.getCallCapablePhoneAccounts()) {
            if (subId == telephonyManager.getSubscriptionId(handle)) {
                return handle;
            }
        }
        return null;
    }

    /*
     * Force dismiss this Activity.
     */
    protected void forceClose() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        Log.d(TAG, "Dismissed by Service");
        finishAndRemoveTask();
    }
}
