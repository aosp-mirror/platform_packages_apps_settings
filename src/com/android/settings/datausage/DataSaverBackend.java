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

import android.content.Context;
import android.net.INetworkPolicyListener;
import android.net.INetworkPolicyManager;
import android.net.NetworkPolicyManager;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.util.SparseBooleanArray;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;

import java.util.ArrayList;

import static android.net.NetworkPolicyManager.POLICY_NONE;
import static android.net.NetworkPolicyManager.POLICY_REJECT_METERED_BACKGROUND;

public class DataSaverBackend {

    private static final String TAG = "DataSaverBackend";

    private final Context mContext;

    private final Handler mHandler = new Handler();
    private final NetworkPolicyManager mPolicyManager;
    private final INetworkPolicyManager mIPolicyManager;
    private final ArrayList<Listener> mListeners = new ArrayList<>();
    private SparseBooleanArray mWhitelist;
    private SparseBooleanArray mBlacklist;

    // TODO: Staticize into only one.
    public DataSaverBackend(Context context) {
        mContext = context;
        mIPolicyManager = INetworkPolicyManager.Stub.asInterface(
                        ServiceManager.getService(Context.NETWORK_POLICY_SERVICE));
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
        MetricsLogger.action(mContext, MetricsEvent.ACTION_DATA_SAVER_MODE, enabled ? 1 : 0);
    }

    public void refreshWhitelist() {
        loadWhitelist();
    }

    public void setIsWhitelisted(int uid, String packageName, boolean whitelisted) {
        mWhitelist.put(uid, whitelisted);
        try {
            if (whitelisted) {
                mIPolicyManager.addRestrictBackgroundWhitelistedUid(uid);
            } else {
                mIPolicyManager.removeRestrictBackgroundWhitelistedUid(uid);
            }
        } catch (RemoteException e) {
            Log.w(TAG, "Can't reach policy manager", e);
        }
        if (whitelisted) {
            MetricsLogger.action(mContext, MetricsEvent.ACTION_DATA_SAVER_WHITELIST, packageName);
        }
    }

    public boolean isWhitelisted(int uid) {
        if (mWhitelist == null) {
            loadWhitelist();
        }
        return mWhitelist.get(uid);
    }

    public int getWhitelistedCount() {
        int count = 0;
        if (mWhitelist == null) {
            loadWhitelist();
        }
        for (int i = 0; i < mWhitelist.size(); i++) {
            if (mWhitelist.valueAt(i)) {
                count++;
            }
        }
        return count;
    }

    private void loadWhitelist() {
        mWhitelist = new SparseBooleanArray();
        try {
            for (int uid : mIPolicyManager.getRestrictBackgroundWhitelistedUids()) {
                mWhitelist.put(uid, true);
            }
        } catch (RemoteException e) {
        }
    }

    public void refreshBlacklist() {
        loadBlacklist();
    }

    public void setIsBlacklisted(int uid, String packageName, boolean blacklisted) {
        mPolicyManager.setUidPolicy(
                uid, blacklisted ? POLICY_REJECT_METERED_BACKGROUND : POLICY_NONE);
        if (blacklisted) {
            MetricsLogger.action(mContext, MetricsEvent.ACTION_DATA_SAVER_BLACKLIST, packageName);
        }
    }

    public boolean isBlacklisted(int uid) {
        if (mBlacklist == null) {
            loadBlacklist();
        }
        return mBlacklist.get(uid);
    }

    private void loadBlacklist() {
        mBlacklist = new SparseBooleanArray();
        try {
            for (int uid : mIPolicyManager.getUidsWithPolicy(POLICY_REJECT_METERED_BACKGROUND)) {
                mBlacklist.put(uid, true);
            }
        } catch (RemoteException e) {
        }
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

    private final INetworkPolicyListener mPolicyListener = new INetworkPolicyListener.Stub() {
        @Override
        public void onUidRulesChanged(final int uid, int uidRules) throws RemoteException {
        }

        @Override
        public void onRestrictBackgroundBlacklistChanged(int uid, boolean blacklisted) {
            if (mBlacklist == null) {
                loadBlacklist();
            }
            mBlacklist.put(uid, blacklisted);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    handleBlacklistChanged(uid, blacklisted);
                }
            });
        }

        @Override
        public void onRestrictBackgroundWhitelistChanged(final int uid, final boolean whitelisted) {
            if (mWhitelist == null) {
                loadWhitelist();
            }
            mWhitelist.put(uid, whitelisted);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    handleWhitelistChanged(uid, whitelisted);
                }
            });
        }

        @Override
        public void onMeteredIfacesChanged(String[] strings) throws RemoteException {
        }

        @Override
        public void onRestrictBackgroundChanged(final boolean isDataSaving) throws RemoteException {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    handleRestrictBackgroundChanged(isDataSaving);
                }
            });
        }
    };

    public interface Listener {
        void onDataSaverChanged(boolean isDataSaving);
        void onWhitelistStatusChanged(int uid, boolean isWhitelisted);
        void onBlacklistStatusChanged(int uid, boolean isBlacklisted);
    }
}
