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

import static android.net.NetworkPolicy.LIMIT_DISABLED;
import static android.net.NetworkPolicy.SNOOZE_NEVER;
import static android.net.NetworkPolicy.WARNING_DISABLED;
import static android.net.NetworkTemplate.MATCH_MOBILE_3G_LOWER;
import static android.net.NetworkTemplate.MATCH_MOBILE_4G;
import static android.net.NetworkTemplate.MATCH_WIFI;
import static android.net.NetworkTemplate.buildTemplateMobile3gLower;
import static android.net.NetworkTemplate.buildTemplateMobile4g;
import static android.net.NetworkTemplate.buildTemplateMobileAll;
import static com.android.internal.util.Preconditions.checkNotNull;

import android.net.INetworkPolicyManager;
import android.net.NetworkPolicy;
import android.net.NetworkTemplate;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.text.format.Time;

import com.android.internal.util.Objects;
import com.google.android.collect.Lists;
import com.google.android.collect.Sets;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Utility class to modify list of {@link NetworkPolicy}. Specifically knows
 * about which policies can coexist. Not thread safe.
 */
public class NetworkPolicyEditor {
    // TODO: be more robust when missing policies from service

    public static final boolean ENABLE_SPLIT_POLICIES = false;

    private INetworkPolicyManager mPolicyService;
    private ArrayList<NetworkPolicy> mPolicies = Lists.newArrayList();

    public NetworkPolicyEditor(INetworkPolicyManager policyService) {
        mPolicyService = checkNotNull(policyService);
    }

    public void read() {
        final NetworkPolicy[] policies;
        try {
            policies = mPolicyService.getNetworkPolicies();
        } catch (RemoteException e) {
            throw new RuntimeException("problem reading policies", e);
        }

        boolean modified = false;
        mPolicies.clear();
        for (NetworkPolicy policy : policies) {
            // TODO: find better place to clamp these
            if (policy.limitBytes < -1) {
                policy.limitBytes = LIMIT_DISABLED;
                modified = true;
            }
            if (policy.warningBytes < -1) {
                policy.warningBytes = WARNING_DISABLED;
                modified = true;
            }

            // drop any WIFI policies that were defined
            if (policy.template.getMatchRule() == MATCH_WIFI) {
                modified = true;
                continue;
            }

            mPolicies.add(policy);
        }

        // force combine any split policies when disabled
        if (!ENABLE_SPLIT_POLICIES) {
            modified |= forceMobilePolicyCombined();
        }

        // when we cleaned policies above, write back changes
        if (modified) writeAsync();
    }

    public void writeAsync() {
        // TODO: consider making more robust by passing through service
        final NetworkPolicy[] policies = mPolicies.toArray(new NetworkPolicy[mPolicies.size()]);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                write(policies);
                return null;
            }
        }.execute();
    }

    public void write(NetworkPolicy[] policies) {
        try {
            mPolicyService.setNetworkPolicies(policies);
        } catch (RemoteException e) {
            throw new RuntimeException("problem writing policies", e);
        }
    }

    public boolean hasLimitedPolicy(NetworkTemplate template) {
        final NetworkPolicy policy = getPolicy(template);
        return policy != null && policy.limitBytes != LIMIT_DISABLED;
    }

    public NetworkPolicy getOrCreatePolicy(NetworkTemplate template) {
        NetworkPolicy policy = getPolicy(template);
        if (policy == null) {
            policy = buildDefaultPolicy(template);
            mPolicies.add(policy);
        }
        return policy;
    }

    public NetworkPolicy getPolicy(NetworkTemplate template) {
        for (NetworkPolicy policy : mPolicies) {
            if (policy.template.equals(template)) {
                return policy;
            }
        }
        return null;
    }

    private static NetworkPolicy buildDefaultPolicy(NetworkTemplate template) {
        // TODO: move this into framework to share with NetworkPolicyManagerService
        final Time time = new Time();
        time.setToNow();
        final int cycleDay = time.monthDay;

        return new NetworkPolicy(template, cycleDay, WARNING_DISABLED, LIMIT_DISABLED, true);
    }

    public int getPolicyCycleDay(NetworkTemplate template) {
        return getPolicy(template).cycleDay;
    }

    public void setPolicyCycleDay(NetworkTemplate template, int cycleDay) {
        final NetworkPolicy policy = getOrCreatePolicy(template);
        policy.cycleDay = cycleDay;
        policy.clearSnooze();
        writeAsync();
    }

    public long getPolicyWarningBytes(NetworkTemplate template) {
        return getPolicy(template).warningBytes;
    }

    public void setPolicyWarningBytes(NetworkTemplate template, long warningBytes) {
        final NetworkPolicy policy = getOrCreatePolicy(template);
        policy.warningBytes = warningBytes;
        policy.clearSnooze();
        writeAsync();
    }

    public long getPolicyLimitBytes(NetworkTemplate template) {
        return getPolicy(template).limitBytes;
    }

    public void setPolicyLimitBytes(NetworkTemplate template, long limitBytes) {
        final NetworkPolicy policy = getOrCreatePolicy(template);
        policy.limitBytes = limitBytes;
        policy.clearSnooze();
        writeAsync();
    }

    /**
     * Remove any split {@link NetworkPolicy}.
     */
    private boolean forceMobilePolicyCombined() {
        final HashSet<String> subscriberIds = Sets.newHashSet();
        for (NetworkPolicy policy : mPolicies) {
            subscriberIds.add(policy.template.getSubscriberId());
        }

        boolean modified = false;
        for (String subscriberId : subscriberIds) {
            modified |= setMobilePolicySplitInternal(subscriberId, false);
        }
        return modified;
    }

    public boolean isMobilePolicySplit(String subscriberId) {
        boolean has3g = false;
        boolean has4g = false;
        for (NetworkPolicy policy : mPolicies) {
            final NetworkTemplate template = policy.template;
            if (Objects.equal(subscriberId, template.getSubscriberId())) {
                switch (template.getMatchRule()) {
                    case MATCH_MOBILE_3G_LOWER:
                        has3g = true;
                        break;
                    case MATCH_MOBILE_4G:
                        has4g = true;
                        break;
                }
            }
        }
        return has3g && has4g;
    }

    public void setMobilePolicySplit(String subscriberId, boolean split) {
        if (setMobilePolicySplitInternal(subscriberId, split)) {
            writeAsync();
        }
    }

    /**
     * Mutate {@link NetworkPolicy} for given subscriber, combining or splitting
     * the policy as requested.
     *
     * @return {@code true} when any {@link NetworkPolicy} was mutated.
     */
    private boolean setMobilePolicySplitInternal(String subscriberId, boolean split) {
        final boolean beforeSplit = isMobilePolicySplit(subscriberId);

        final NetworkTemplate template3g = buildTemplateMobile3gLower(subscriberId);
        final NetworkTemplate template4g = buildTemplateMobile4g(subscriberId);
        final NetworkTemplate templateAll = buildTemplateMobileAll(subscriberId);

        if (split == beforeSplit) {
            // already in requested state; skip
            return false;

        } else if (beforeSplit && !split) {
            // combine, picking most restrictive policy
            final NetworkPolicy policy3g = getPolicy(template3g);
            final NetworkPolicy policy4g = getPolicy(template4g);

            final NetworkPolicy restrictive = policy3g.compareTo(policy4g) < 0 ? policy3g
                    : policy4g;
            mPolicies.remove(policy3g);
            mPolicies.remove(policy4g);
            mPolicies.add(new NetworkPolicy(templateAll, restrictive.cycleDay,
                    restrictive.warningBytes, restrictive.limitBytes, restrictive.metered));
            return true;

        } else if (!beforeSplit && split) {
            // duplicate existing policy into two rules
            final NetworkPolicy policyAll = getPolicy(templateAll);
            mPolicies.remove(policyAll);
            mPolicies.add(new NetworkPolicy(template3g, policyAll.cycleDay, policyAll.warningBytes,
                    policyAll.limitBytes, policyAll.metered));
            mPolicies.add(new NetworkPolicy(template4g, policyAll.cycleDay, policyAll.warningBytes,
                    policyAll.limitBytes, policyAll.metered));
            return true;
        } else {
            return false;
        }
    }
}
