/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings;

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ContentResolver;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkPolicyManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RecoverySystem;
import android.os.UserHandle;
import android.os.UserManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;

import com.android.settings.core.InstrumentedFragment;
import com.android.settings.enterprise.ActionDisabledByAdminDialogHelper;
import com.android.settings.network.ApnSettings;
import com.android.settingslib.RestrictedLockUtilsInternal;

/**
 * Confirm and execute a reset of the network settings to a clean "just out of the box"
 * state.  Multiple confirmations are required: first, a general "are you sure
 * you want to do this?" prompt, followed by a keyguard pattern trace if the user
 * has defined one, followed by a final strongly-worded "THIS WILL RESET EVERYTHING"
 * prompt.  If at any time the phone is allowed to go to sleep, is
 * locked, et cetera, then the confirmation sequence is abandoned.
 *
 * This is the confirmation screen.
 */
public class ResetNetworkConfirm extends InstrumentedFragment {

    @VisibleForTesting View mContentView;
    @VisibleForTesting boolean mEraseEsim;
    @VisibleForTesting ResetNetworkTask mResetNetworkTask;
    @VisibleForTesting Activity mActivity;
    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    private ProgressDialog mProgressDialog;
    private AlertDialog mAlertDialog;

    /**
     * Async task used to do all reset task. If error happens during
     * erasing eSIM profiles or timeout, an error msg is shown.
     */
    private class ResetNetworkTask extends AsyncTask<Void, Void, Boolean> {
        private final Context mContext;
        private final String mPackageName;

        ResetNetworkTask(Context context) {
            mContext = context;
            mPackageName = context.getPackageName();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean isResetSucceed = true;
            ConnectivityManager connectivityManager = (ConnectivityManager)
                    mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                connectivityManager.factoryReset();
            }

            WifiManager wifiManager = (WifiManager)
                    mContext.getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                wifiManager.factoryReset();
            }

            p2pFactoryReset(mContext);

            if (mEraseEsim) {
                isResetSucceed = RecoverySystem.wipeEuiccData(mContext, mPackageName);
            }

            TelephonyManager telephonyManager = (TelephonyManager)
                    mContext.getSystemService(TelephonyManager.class)
                            .createForSubscriptionId(mSubId);
            if (telephonyManager != null) {
                telephonyManager.resetSettings();
            }

            NetworkPolicyManager policyManager = (NetworkPolicyManager)
                    mContext.getSystemService(Context.NETWORK_POLICY_SERVICE);
            if (policyManager != null) {
                String subscriberId = telephonyManager.getSubscriberId();
                policyManager.factoryReset(subscriberId);
            }

            BluetoothManager btManager = (BluetoothManager)
                    mContext.getSystemService(Context.BLUETOOTH_SERVICE);
            if (btManager != null) {
                BluetoothAdapter btAdapter = btManager.getAdapter();
                if (btAdapter != null) {
                    btAdapter.factoryReset();
                }
            }

            restoreDefaultApn(mContext);
            return isResetSucceed;
        }

        @Override
        protected void onPostExecute(Boolean succeeded) {
            mProgressDialog.dismiss();
            if (succeeded) {
                Toast.makeText(mContext, R.string.reset_network_complete_toast, Toast.LENGTH_SHORT)
                        .show();
            } else {
                mAlertDialog = new AlertDialog.Builder(mContext)
                        .setTitle(R.string.reset_esim_error_title)
                        .setMessage(R.string.reset_esim_error_msg)
                        .setPositiveButton(android.R.string.ok, null /* listener */)
                        .show();
            }
        }
    }

    /**
     * The user has gone through the multiple confirmation, so now we go ahead
     * and reset the network settings to its factory-default state.
     */
    @VisibleForTesting
    Button.OnClickListener mFinalClickListener = new Button.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (Utils.isMonkeyRunning()) {
                return;
            }

            mProgressDialog = getProgressDialog(mActivity);
            mProgressDialog.show();

            mResetNetworkTask = new ResetNetworkTask(mActivity);
            mResetNetworkTask.execute();
        }
    };

    @VisibleForTesting
    void p2pFactoryReset(Context context) {
        WifiP2pManager wifiP2pManager = (WifiP2pManager)
                context.getSystemService(Context.WIFI_P2P_SERVICE);
        if (wifiP2pManager != null) {
            WifiP2pManager.Channel channel = wifiP2pManager.initialize(
                    context.getApplicationContext(), context.getMainLooper(),
                    null /* listener */);
            if (channel != null) {
                wifiP2pManager.factoryReset(channel, null /* listener */);
            }
        }
    }

    private ProgressDialog getProgressDialog(Context context) {
        final ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(
                context.getString(R.string.master_clear_progress_text));
        return progressDialog;
    }

    /**
     * Restore APN settings to default.
     */
    private void restoreDefaultApn(Context context) {
        Uri uri = Uri.parse(ApnSettings.RESTORE_CARRIERS_URI);

        if (SubscriptionManager.isUsableSubscriptionId(mSubId)) {
            uri = Uri.withAppendedPath(uri, "subId/" + String.valueOf(mSubId));
        }

        ContentResolver resolver = context.getContentResolver();
        resolver.delete(uri, null, null);
    }

    /**
     * Configure the UI for the final confirmation interaction
     */
    private void establishFinalConfirmationState() {
        mContentView.findViewById(R.id.execute_reset_network)
                .setOnClickListener(mFinalClickListener);
    }

    @VisibleForTesting
    void setSubtitle() {
        if (mEraseEsim) {
            ((TextView) mContentView.findViewById(R.id.reset_network_confirm))
                    .setText(R.string.reset_network_final_desc_esim);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final EnforcedAdmin admin = RestrictedLockUtilsInternal.checkIfRestrictionEnforced(
                mActivity, UserManager.DISALLOW_NETWORK_RESET, UserHandle.myUserId());
        if (RestrictedLockUtilsInternal.hasBaseUserRestriction(mActivity,
                UserManager.DISALLOW_NETWORK_RESET, UserHandle.myUserId())) {
            return inflater.inflate(R.layout.network_reset_disallowed_screen, null);
        } else if (admin != null) {
            new ActionDisabledByAdminDialogHelper(mActivity)
                    .prepareDialogBuilder(UserManager.DISALLOW_NETWORK_RESET, admin)
                    .setOnDismissListener(__ -> mActivity.finish())
                    .show();
            return new View(mActivity);
        }
        mContentView = inflater.inflate(R.layout.reset_network_confirm, null);
        establishFinalConfirmationState();
        setSubtitle();
        return mContentView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            mSubId = args.getInt(SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX,
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID);
            mEraseEsim = args.getBoolean(MasterClear.ERASE_ESIMS_EXTRA);
        }

        mActivity = getActivity();
    }

    @Override
    public void onDestroy() {
        if (mResetNetworkTask != null) {
            mResetNetworkTask.cancel(true /* mayInterruptIfRunning */);
            mResetNetworkTask = null;
        }
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
        if (mAlertDialog != null) {
            mAlertDialog.dismiss();
        }
        super.onDestroy();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.RESET_NETWORK_CONFIRM;
    }
}
