/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.content.Context;
import android.net.INetworkStatsService;
import android.net.NetworkPolicy;
import android.net.NetworkPolicyManager;
import android.os.Bundle;
import android.os.INetworkManagementService;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.NetworkPolicyEditor;

public abstract class DataUsageBaseFragment extends DashboardFragment {
    private static final String TAG = "DataUsageBase";
    private static final String ETHERNET = "ethernet";

    protected final TemplatePreference.NetworkServices services =
            new TemplatePreference.NetworkServices();

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Context context = getContext();

        services.mNetworkService = INetworkManagementService.Stub.asInterface(
                ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE));
        services.mStatsService = INetworkStatsService.Stub.asInterface(
                ServiceManager.getService(Context.NETWORK_STATS_SERVICE));
        services.mPolicyManager = (NetworkPolicyManager) context
                .getSystemService(Context.NETWORK_POLICY_SERVICE);

        services.mPolicyEditor = new NetworkPolicyEditor(services.mPolicyManager);

        services.mTelephonyManager = context.getSystemService(TelephonyManager.class);
        services.mSubscriptionManager = SubscriptionManager.from(context);
        services.mUserManager = UserManager.get(context);
    }

    @Override
    public void onResume() {
        super.onResume();
        services.mPolicyEditor.read();
    }

    protected boolean isAdmin() {
        return services.mUserManager.isAdminUser();
    }

    protected boolean isMobileDataAvailable(int subId) {
        return services.mSubscriptionManager.getActiveSubscriptionInfo(subId) != null;
    }

    protected boolean isNetworkPolicyModifiable(NetworkPolicy policy, int subId) {
        return policy != null && isBandwidthControlEnabled() && services.mUserManager.isAdminUser()
                && isDataEnabled(subId);
    }

    private boolean isDataEnabled(int subId) {
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return true;
        }
        return services.mTelephonyManager.getDataEnabled(subId);
    }

    protected boolean isBandwidthControlEnabled() {
        try {
            return services.mNetworkService.isBandwidthControlEnabled();
        } catch (RemoteException e) {
            Log.w(TAG, "problem talking with INetworkManagementService: ", e);
            return false;
        }
    }
}
