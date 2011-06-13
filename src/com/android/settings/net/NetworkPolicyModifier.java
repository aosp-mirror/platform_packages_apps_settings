/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings.net;

import static android.net.TrafficStats.TEMPLATE_MOBILE_3G_LOWER;
import static android.net.TrafficStats.TEMPLATE_MOBILE_4G;
import static android.net.TrafficStats.TEMPLATE_MOBILE_ALL;
import static com.android.internal.util.Preconditions.checkNotNull;

import android.net.INetworkPolicyManager;
import android.net.NetworkPolicy;
import android.os.AsyncTask;
import android.os.RemoteException;

import com.android.internal.util.Objects;
import com.google.android.collect.Lists;

import java.util.ArrayList;

/**
 * Utility class to modify list of {@link NetworkPolicy}. Specifically knows
 * about which policies can coexist.
 */
public class NetworkPolicyModifier {

    private INetworkPolicyManager mPolicyService;
    private String mSubscriberId;

    private ArrayList<NetworkPolicy> mPolicies = Lists.newArrayList();

    public NetworkPolicyModifier(INetworkPolicyManager policyService, String subscriberId) {
        mPolicyService = checkNotNull(policyService);
        mSubscriberId = subscriberId;
    }

    public void read() {
        try {
            final NetworkPolicy[] policies = mPolicyService.getNetworkPolicies();
            mPolicies.clear();
            for (NetworkPolicy policy : policies) {
                mPolicies.add(policy);
            }
        } catch (RemoteException e) {
            throw new RuntimeException("problem reading policies", e);
        }
    }

    public void writeAsync() {
        // TODO: consider making more robust by passing through service
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                write();
                return null;
            }
        }.execute();
    }

    public void write() {
        try {
            final NetworkPolicy[] policies = mPolicies.toArray(new NetworkPolicy[mPolicies.size()]);
            mPolicyService.setNetworkPolicies(policies);
        } catch (RemoteException e) {
            throw new RuntimeException("problem reading policies", e);
        }
    }

    public NetworkPolicy getPolicy(int networkTemplate) {
        for (NetworkPolicy policy : mPolicies) {
            if (policy.networkTemplate == networkTemplate
                    && Objects.equal(policy.subscriberId, mSubscriberId)) {
                return policy;
            }
        }
        return null;
    }

    public void setPolicyCycleDay(int networkTemplate, int cycleDay) {
        getPolicy(networkTemplate).cycleDay = cycleDay;
        writeAsync();
    }

    public void setPolicyWarningBytes(int networkTemplate, long warningBytes) {
        getPolicy(networkTemplate).warningBytes = warningBytes;
        writeAsync();
    }

    public void setPolicyLimitBytes(int networkTemplate, long limitBytes) {
        getPolicy(networkTemplate).limitBytes = limitBytes;
        writeAsync();
    }

    public boolean isMobilePolicySplit() {
        return getPolicy(TEMPLATE_MOBILE_3G_LOWER) != null && getPolicy(TEMPLATE_MOBILE_4G) != null;
    }

    public void setMobilePolicySplit(boolean split) {
        final boolean beforeSplit = isMobilePolicySplit();
        if (split == beforeSplit) {
            // already in requested state; skip
            return;

        } else if (beforeSplit && !split) {
            // combine, picking most restrictive policy
            final NetworkPolicy policy3g = getPolicy(TEMPLATE_MOBILE_3G_LOWER);
            final NetworkPolicy policy4g = getPolicy(TEMPLATE_MOBILE_4G);

            final NetworkPolicy restrictive = policy3g.compareTo(policy4g) < 0 ? policy3g
                    : policy4g;
            mPolicies.remove(policy3g);
            mPolicies.remove(policy4g);
            mPolicies.add(new NetworkPolicy(TEMPLATE_MOBILE_ALL, restrictive.subscriberId,
                    restrictive.cycleDay, restrictive.warningBytes, restrictive.limitBytes));
            writeAsync();

        } else if (!beforeSplit && split) {
            // duplicate existing policy into two rules
            final NetworkPolicy policyAll = getPolicy(TEMPLATE_MOBILE_ALL);
            mPolicies.remove(policyAll);
            mPolicies.add(
                    new NetworkPolicy(TEMPLATE_MOBILE_3G_LOWER, policyAll.subscriberId,
                            policyAll.cycleDay, policyAll.warningBytes, policyAll.limitBytes));
            mPolicies.add(
                    new NetworkPolicy(TEMPLATE_MOBILE_4G, policyAll.subscriberId,
                            policyAll.cycleDay, policyAll.warningBytes, policyAll.limitBytes));
            writeAsync();

        }
    }

}
