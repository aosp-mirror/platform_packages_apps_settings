/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.os.UserHandle;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.android.settings.Utils;

/**
 * Activity which acts as a proxy to the tether provisioning app for sanity checks and permission
 * restrictions. Specifically, the provisioning apps require
 * {@link android.permission.CONNECTIVITY_INTERNAL}, while this activity can be started by a caller
 * with {@link android.permission.TETHER_PRIVILEGED}.
 */
public class TetherProvisioningActivity extends Activity {
    private static final int PROVISION_REQUEST = 0;
    private static final String TAG = "TetherProvisioningAct";
    private static final String EXTRA_TETHER_TYPE = "TETHER_TYPE";
    private static final String EXTRA_SUBID = "subId";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);
    private ResultReceiver mResultReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mResultReceiver = (ResultReceiver)getIntent().getParcelableExtra(
                ConnectivityManager.EXTRA_PROVISION_CALLBACK);

        final int tetherType = getIntent().getIntExtra(ConnectivityManager.EXTRA_ADD_TETHER_TYPE,
                ConnectivityManager.TETHERING_INVALID);

        final int tetherSubId = getIntent().getIntExtra(EXTRA_SUBID,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        final int subId = SubscriptionManager.getActiveDataSubscriptionId();
        if (tetherSubId != subId) {
            Log.e(TAG, "This Provisioning request is outdated, current subId: " + subId);
            return;
        }
        final Resources res = Utils.getResourcesForSubId(this, subId);
        final String[] provisionApp = res.getStringArray(
                com.android.internal.R.array.config_mobile_hotspot_provision_app);

        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName(provisionApp[0], provisionApp[1]);
        intent.putExtra(EXTRA_TETHER_TYPE, tetherType);
        intent.putExtra(SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX, subId);
        if (DEBUG) {
            Log.d(TAG, "Starting provisioning app: " + provisionApp[0] + "." + provisionApp[1]);
        }

        if (getPackageManager().queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY).isEmpty()) {
            Log.e(TAG, "Provisioning app is configured, but not available.");
            mResultReceiver.send(ConnectivityManager.TETHER_ERROR_PROVISION_FAILED, null);
            finish();
            return;
        }

        startActivityForResultAsUser(intent, PROVISION_REQUEST, UserHandle.CURRENT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == PROVISION_REQUEST) {
            if (DEBUG) Log.d(TAG, "Got result from app: " + resultCode);
            int result = resultCode == Activity.RESULT_OK ?
                    ConnectivityManager.TETHER_ERROR_NO_ERROR :
                    ConnectivityManager.TETHER_ERROR_PROVISION_FAILED;
            mResultReceiver.send(result, null);
            finish();
        }
    }
}
