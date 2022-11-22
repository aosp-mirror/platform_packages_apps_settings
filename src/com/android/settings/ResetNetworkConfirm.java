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

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;

import com.android.settings.core.InstrumentedFragment;
import com.android.settings.network.ResetNetworkOperationBuilder;
import com.android.settings.network.ResetNetworkRestrictionViewBuilder;

import java.util.concurrent.atomic.AtomicBoolean;

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
    private static final String TAG = "ResetNetworkConfirm";

    @VisibleForTesting View mContentView;
    @VisibleForTesting boolean mEraseEsim;
    @VisibleForTesting ResetNetworkTask mResetNetworkTask;
    @VisibleForTesting Activity mActivity;
    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    private ProgressDialog mProgressDialog;
    private AlertDialog mAlertDialog;
    private OnSubscriptionsChangedListener mSubscriptionsChangedListener;

    /**
     * Async task used to do all reset task. If error happens during
     * erasing eSIM profiles or timeout, an error msg is shown.
     */
    private class ResetNetworkTask extends AsyncTask<Void, Void, Boolean> {
        private static final String TAG = "ResetNetworkTask";

        private final Context mContext;
        private final String mPackageName;

        ResetNetworkTask(Context context) {
            mContext = context;
            mPackageName = context.getPackageName();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            final AtomicBoolean resetEsimSuccess = new AtomicBoolean(true);
            ResetNetworkOperationBuilder builder =
                    (new ResetNetworkOperationBuilder(mContext))
                    .resetConnectivityManager()
                    .resetVpnManager()
                    .resetWifiManager()
                    .resetWifiP2pManager(Looper.getMainLooper());
            if (mEraseEsim) {
                builder = builder.resetEsim(mContext.getPackageName(),
                        success -> { resetEsimSuccess.set(success); }
                        );
            }
            builder.resetTelephonyAndNetworkPolicyManager(mSubId)
                    .resetBluetoothManager()
                    .resetApn(mSubId)
                    .build()
                    .run();

            boolean isResetSucceed = resetEsimSuccess.get();
            Log.d(TAG, "network factoryReset complete. succeeded: "
                    + String.valueOf(isResetSucceed));
            return isResetSucceed;
        }

        @Override
        protected void onPostExecute(Boolean succeeded) {
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }

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

            // abandon execution if subscription no longer active
            if (mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                SubscriptionManager mgr = getSubscriptionManager();
                // always remove listener
                stopMonitorSubscriptionChange(mgr);
                if (!isSubscriptionRemainActive(mgr, mSubId)) {
                    Log.w(TAG, "subId " + mSubId + " disappear when confirm");
                    mActivity.finish();
                    return;
                }
            }

            // Should dismiss the progress dialog firstly if it is showing
            // Or not the progress dialog maybe not dismissed in fast clicking.
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }

            mProgressDialog = getProgressDialog(mActivity);
            mProgressDialog.show();

            mResetNetworkTask = new ResetNetworkTask(mActivity);
            mResetNetworkTask.execute();
        }
    };

    private ProgressDialog getProgressDialog(Context context) {
        final ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(
                context.getString(R.string.main_clear_progress_text));
        return progressDialog;
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
        View view = (new ResetNetworkRestrictionViewBuilder(mActivity)).build();
        if (view != null) {
            Log.w(TAG, "Access deny.");
            return view;
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
            mEraseEsim = args.getBoolean(MainClear.ERASE_ESIMS_EXTRA);
        }

        mActivity = getActivity();

        if (mSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return;
        }
        // close confirmation dialog when reset specific subscription
        // but removed priori to the confirmation button been pressed
        startMonitorSubscriptionChange(getSubscriptionManager());
    }

    private SubscriptionManager getSubscriptionManager() {
        SubscriptionManager mgr = mActivity.getSystemService(SubscriptionManager.class);
        if (mgr == null) {
            Log.w(TAG, "No SubscriptionManager");
        }
        return mgr;
    }

    private void startMonitorSubscriptionChange(SubscriptionManager mgr) {
        if (mgr == null) {
            return;
        }
        // update monitor listener
        mSubscriptionsChangedListener = new OnSubscriptionsChangedListener(
                Looper.getMainLooper()) {
            @Override
            public void onSubscriptionsChanged() {
                SubscriptionManager mgr = getSubscriptionManager();
                if (isSubscriptionRemainActive(mgr, mSubId)) {
                    return;
                }
                // close UI if subscription no longer active
                Log.w(TAG, "subId " + mSubId + " no longer active.");
                stopMonitorSubscriptionChange(mgr);
                mActivity.finish();
            }
        };
        mgr.addOnSubscriptionsChangedListener(
                mActivity.getMainExecutor(), mSubscriptionsChangedListener);
    }

    private boolean isSubscriptionRemainActive(SubscriptionManager mgr, int subscriptionId) {
        return (mgr == null) ? false : (mgr.getActiveSubscriptionInfo(subscriptionId) != null);
    }

    private void stopMonitorSubscriptionChange(SubscriptionManager mgr) {
        if ((mgr == null) || (mSubscriptionsChangedListener == null)) {
            return;
        }
        mgr.removeOnSubscriptionsChangedListener(mSubscriptionsChangedListener);
        mSubscriptionsChangedListener = null;
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
        stopMonitorSubscriptionChange(getSubscriptionManager());
        super.onDestroy();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.RESET_NETWORK_CONFIRM;
    }
}
