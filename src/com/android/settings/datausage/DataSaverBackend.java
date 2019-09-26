/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static android.net.NetworkPolicyManager.POLICY_ALLOW_METERED_BACKGROUND;
import static android.net.NetworkPolicyManager.POLICY_NONE;
import static android.net.NetworkPolicyManager.POLICY_REJECT_METERED_BACKGROUND;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.net.INetworkPolicyListener;
import android.net.NetworkPolicyManager;
import android.os.RemoteException;
import android.telephony.SubscriptionPlan;
import android.util.SparseIntArray;

import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.utils.ThreadUtils;

import java.util.ArrayList;

public class DataSaverBackend {

    private static final String TAG = "DataSaverBackend";

    private final Context mContext;
    private final MetricsFeatureProvider mMetricsFeatureProvider;

    private final NetworkPolicyManager mPolicyManager;
    private final ArrayList<Listener> mListeners = new ArrayList<>();
    private SparseIntArray mUidPolicies = new SparseIntArray();
    private boolean mWhitelistInitialized;
    private boolean mBlacklistInitialized;

    // TODO: Staticize into only one.
    public DataSaverBackend(Context context) {
        mContext = context;
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
        mPolicyManager = NetworkPolicyManager.from(context);
    }

    public void addListener(Listener listener) {
        mListeners.add(listener);
        if (mListeners.size() == 1) {
            mPolicyManager.registerListener(mPolicyListener);
        }
        listener.onDataSaverChanged(isDataSaverEnabled());
    }

    public void remListener(Listener listener) {
        mListeners.remove(listener);
        if (mListeners.size() == 0) {
            mPolicyManager.unregisterListener(mPolicyListener);
        }
    }

    public boolean isDataSaverEnabled() {
        return mPolicyManager.getRestrictBackground();
    }

    public void setDataSaverEnabled(boolean enabled) {
        mPolicyManager.setRestrictBackground(enabled);
        mMetricsFeatureProvider.action(
                mContext, SettingsEnums.ACTION_DATA_SAVER_MODE, enabled ? 1 : 0);
    }

    public void refreshWhitelist() {
        loadWhitelist();
    }

    public void setIsWhitelisted(int uid, String packageName, boolean whitelisted) {
        final int policy = whitelisted ? POLICY_ALLOW_METERED_BACKGROUND : POLICY_NONE;
        mPolicyManager.setUidPolicy(uid, policy);
        mUidPolicies.put(uid, policy);
        if (whitelisted) {
            mMetricsFeatureProvider.action(
                    mContext, SettingsEnums.ACTION_DATA_SAVER_WHITELIST, packageName);
        }
    }

    public boolean isWhitelisted(int uid) {
        loadWhitelist();
        return mUidPolicies.get(uid, POLICY_NONE) == POLICY_ALLOW_METERED_BACKGROUND;
    }

    private void loadWhitelist() {
        if (mWhitelistInitialized) {
            return;
        }

        for (int uid : mPolicyManager.getUidsWithPolicy(POLICY_ALLOW_METERED_BACKGROUND)) {
            mUidPolicies.put(uid, POLICY_ALLOW_METERED_BACKGROUND);
        }
        mWhitelistInitialized = true;
    }

    public void refreshBlacklist() {
        loadBlacklist();
    }

    public void setIsBlacklisted(int uid, String packageName, boolean blacklisted) {
        final int policy = blacklisted ? POLICY_REJECT_METERED_BACKGROUND : POLICY_NONE;
        mPolicyManager.setUidPolicy(uid, policy);
        mUidPolicies.put(uid, policy);
        if (blacklisted) {
            mMetricsFeatureProvider.action(
                    mContext, SettingsEnums.ACTION_DATA_SAVER_BLACKLIST, packageName);
        }
    }

    public boolean isBlacklisted(int uid) {
        loadBlacklist();
        return mUidPolicies.get(uid, POLICY_NONE) == POLICY_REJECT_METERED_BACKGROUND;
    }

    private void loadBlacklist() {
        if (mBlacklistInitialized) {
            return;
        }
        for (int uid : mPolicyManager.getUidsWithPolicy(POLICY_REJECT_METERED_BACKGROUND)) {
            mUidPolicies.put(uid, POLICY_REJECT_METERED_BACKGROUND);
        }
        mBlacklistInitialized = true;
    }

    private void handleRestrictBackgroundChanged(boolean isDataSaving) {
        for (int i = 0; i < mListeners.size(); i++) {
            mListeners.get(i).onDataSaverChanged(isDataSaving);
        }
    }

    private void handleWhitelistChanged(int uid, boolean isWhitelisted) {
        for (int i = 0; i < mListeners.size(); i++) {
            mListeners.get(i).onWhitelistStatusChanged(uid, isWhitelisted);
        }
    }

    private void handleBlacklistChanged(int uid, boolean isBlacklisted) {
        for (int i = 0; i < mListeners.size(); i++) {
            mListeners.get(i).onBlacklistStatusChanged(uid, isBlacklisted);
        }
    }

    private void handleUidPoliciesChanged(int uid, int newPolicy) {
        loadWhitelist();
        loadBlacklist();

        final int oldPolicy = mUidPolicies.get(uid, POLICY_NONE);
        if (newPolicy == POLICY_NONE) {
            mUidPolicies.delete(uid);
        } else {
            mUidPolicies.put(uid, newPolicy);
        }

        final boolean wasWhitelisted = oldPolicy == POLICY_ALLOW_METERED_BACKGROUND;
        final boolean wasBlacklisted = oldPolicy == POLICY_REJECT_METERED_BACKGROUND;
        final boolean isWhitelisted = newPolicy == POLICY_ALLOW_METERED_BACKGROUND;
        final boolean isBlacklisted = newPolicy == POLICY_REJECT_METERED_BACKGROUND;

        if (wasWhitelisted != isWhitelisted) {
            handleWhitelistChanged(uid, isWhitelisted);
        }

        if (wasBlacklisted != isBlacklisted) {
            handleBlacklistChanged(uid, isBlacklisted);
        }

    }

    private final INetworkPolicyListener mPolicyListener = new INetworkPolicyListener.Stub() {
        @Override
        public void onUidRulesChanged(int uid, int uidRules) throws RemoteException {
        }

        @Override
        public void onUidPoliciesChanged(final int uid, final int uidPolicies) {
            ThreadUtils.postOnMainThread(() -> handleUidPoliciesChanged(uid, uidPolicies));
        }

        @Override
        public void onMeteredIfacesChanged(String[] strings) throws RemoteException {
        }

        @Override
        public void onRestrictBackgroundChanged(final boolean isDataSaving) throws RemoteException {
            ThreadUtils.postOnMainThread(() -> handleRestrictBackgroundChanged(isDataSaving));
        }

        @Override
        public void onSubscriptionOverride(int subId, int overrideMask, int overrideValue) {
        }

        @Override
        public void onSubscriptionPlansChanged(int subId, SubscriptionPlan[] plans) {
        }
    };

    public interface Listener {
        void onDataSaverChanged(boolean isDataSaving);

        void onWhitelistStatusChanged(int uid, boolean isWhitelisted);

        void onBlacklistStatusChanged(int uid, boolean isBlacklisted);
    }
}
