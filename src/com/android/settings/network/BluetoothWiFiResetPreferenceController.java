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

package com.android.settings.network;

import android.app.ProgressDialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.ResetNetworkRequest;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This is to show a preference regarding resetting Bluetooth and Wi-Fi.
 */
public class BluetoothWiFiResetPreferenceController extends BasePreferenceController
        implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {

    private static final String TAG = "BtWiFiResetPreferenceController";

    private final NetworkResetRestrictionChecker mRestrictionChecker;

    private DialogInterface mResetDialog;
    private ProgressDialog mProgressDialog;
    private ExecutorService mExecutorService;

    /**
     * Constructer.
     * @param context Context
     * @param preferenceKey is the key for Preference
     */
    public BluetoothWiFiResetPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);

        // restriction check
        mRestrictionChecker = new NetworkResetRestrictionChecker(context);
    }

    @Override
    public int getAvailabilityStatus() {
        return mRestrictionChecker.hasUserRestriction() ?
                CONDITIONALLY_UNAVAILABLE : AVAILABLE;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }
        buildResetDialog(preference);
        return true;
    }

    /**
     * This is a pop-up dialog showing detail of this reset option.
     */
    void buildResetDialog(Preference preference) {
        if (mResetDialog != null) {
            return;
        }
        mResetDialog = new AlertDialog.Builder(mContext)
                .setTitle(R.string.reset_bluetooth_wifi_title)
                .setMessage(R.string.reset_bluetooth_wifi_desc)
                .setPositiveButton(R.string.reset_bluetooth_wifi_button_text, this)
                .setNegativeButton(R.string.cancel, null /* OnClickListener */)
                .setOnDismissListener(this)
                .show();
    }

    public void onDismiss(DialogInterface dialog) {
        if (mResetDialog == dialog) {
            mResetDialog = null;
        }
    }

    /**
     * User pressed confirmation button, for starting reset operation.
     */
    public void onClick(DialogInterface dialog, int which) {
        if (mResetDialog != dialog) {
            return;
        }

        // User confirm the reset operation
        MetricsFeatureProvider provider = FeatureFactory.getFeatureFactory()
                .getMetricsFeatureProvider();
        provider.action(mContext, SettingsEnums.RESET_BLUETOOTH_WIFI_CONFIRM, true);

        // Non-cancelable progress dialog
        mProgressDialog = getProgressDialog(mContext);
        mProgressDialog.show();

        // Run reset in background thread
        mExecutorService = Executors.newSingleThreadExecutor();
        mExecutorService.execute(() -> {
            final AtomicReference<Exception> exceptionDuringReset =
                    new AtomicReference<Exception>();
            try {
                resetOperation().run();
            } catch (Exception exception) {
                exceptionDuringReset.set(exception);
            }
            mContext.getMainExecutor().execute(() -> endOfReset(exceptionDuringReset.get()));
        });
    }

    @VisibleForTesting
    protected ProgressDialog getProgressDialog(Context context) {
        final ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(
                context.getString(R.string.main_clear_progress_text));
        return progressDialog;
    }

    @VisibleForTesting
    protected Runnable resetOperation() throws Exception {
        if (SubscriptionUtil.isSimHardwareVisible(mContext)) {
            return new ResetNetworkRequest(
                    ResetNetworkRequest.RESET_WIFI_MANAGER |
                    ResetNetworkRequest.RESET_WIFI_P2P_MANAGER |
                    ResetNetworkRequest.RESET_BLUETOOTH_MANAGER)
                .toResetNetworkOperationBuilder(mContext, Looper.getMainLooper())
                .build();
        }

        /**
         * For device without SIMs visible to the user
         */
        return new ResetNetworkRequest(
                ResetNetworkRequest.RESET_CONNECTIVITY_MANAGER |
                ResetNetworkRequest.RESET_VPN_MANAGER |
                ResetNetworkRequest.RESET_WIFI_MANAGER |
                ResetNetworkRequest.RESET_WIFI_P2P_MANAGER |
                ResetNetworkRequest.RESET_BLUETOOTH_MANAGER)
            .toResetNetworkOperationBuilder(mContext, Looper.getMainLooper())
            .resetTelephonyAndNetworkPolicyManager(ResetNetworkRequest.ALL_SUBSCRIPTION_ID)
            .build();
    }

    @VisibleForTesting
    protected void endOfReset(Exception exceptionDuringReset) {
        if (mExecutorService != null) {
            mExecutorService.shutdown();
            mExecutorService = null;
        }
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
        if (exceptionDuringReset == null) {
            Toast.makeText(mContext, R.string.reset_bluetooth_wifi_complete_toast,
                    Toast.LENGTH_SHORT).show();
        } else {
            Log.e(TAG, "Exception during reset", exceptionDuringReset);
        }
    }
}
