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
    @VisibleForTesting ResetNetworkTask mResetNetworkTask;
    @VisibleForTesting Activity mActivity;
    @VisibleForTesting ResetNetworkRequest mResetNetworkRequest;
    private ProgressDialog mProgressDialog;
    private AlertDialog mAlertDialog;
    @VisibleForTesting ResetSubscriptionContract mResetSubscriptionContract;
    private OnSubscriptionsChangedListener mSubscriptionsChangedListener;

    /**
     * Async task used to do all reset task. If error happens during
     * erasing eSIM profiles or timeout, an error msg is shown.
     */
    private class ResetNetworkTask extends AsyncTask<Void, Void, Boolean> {
        private static final String TAG = "ResetNetworkTask";

        private final Context mContext;

        ResetNetworkTask(Context context) {
            mContext = context;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            final AtomicBoolean resetEsimSuccess = new AtomicBoolean(true);

            String resetEsimPackageName = mResetNetworkRequest.getResetEsimPackageName();
            ResetNetworkOperationBuilder builder = mResetNetworkRequest
                    .toResetNetworkOperationBuilder(mContext, Looper.getMainLooper());
            if (resetEsimPackageName != null) {
                // Override reset eSIM option for the result of reset operation
                builder = builder.resetEsim(resetEsimPackageName,
                        success -> { resetEsimSuccess.set(success); }
                        );
            }
            builder.build().run();

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
            Integer subId = mResetSubscriptionContract.getAnyMissingSubscriptionId();
            if (subId != null) {
                Log.w(TAG, "subId " + subId + " no longer active");
                getActivity().onBackPressed();
                return;
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
        if (mResetNetworkRequest.getResetEsimPackageName() != null) {
            ((TextView) mContentView.findViewById(R.id.reset_network_confirm))
                    .setText(R.string.reset_network_final_desc_esim);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = (new ResetNetworkRestrictionViewBuilder(mActivity)).build();
        if (view != null) {
            mResetSubscriptionContract.close();
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
        if (args == null) {
            args = savedInstanceState;
        }
        mResetNetworkRequest = new ResetNetworkRequest(args);

        mActivity = getActivity();

        mResetSubscriptionContract = new ResetSubscriptionContract(getContext(),
                mResetNetworkRequest) {
            @Override
            public void onSubscriptionInactive(int subscriptionId) {
                // close UI if subscription no longer active
                Log.w(TAG, "subId " + subscriptionId + " no longer active.");
                getActivity().onBackPressed();
            }
        };
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mResetNetworkRequest.writeIntoBundle(outState);
    }

    @Override
    public void onDestroy() {
        if (mResetNetworkTask != null) {
            mResetNetworkTask.cancel(true /* mayInterruptIfRunning */);
            mResetNetworkTask = null;
        }
        if (mResetSubscriptionContract != null) {
            mResetSubscriptionContract.close();
            mResetSubscriptionContract = null;
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
