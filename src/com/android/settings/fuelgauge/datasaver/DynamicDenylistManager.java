/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.fuelgauge.datasaver;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.NetworkPolicyManager;

import androidx.annotation.VisibleForTesting;

/** A class to dynamically manage per apps {@link NetworkPolicyManager} POLICY_ flags. */
public final class DynamicDenylistManager {

    private static final String TAG = "DynamicDenylistManager";
    private static final String PREF_KEY_MANUAL_DENY = "manual_denylist_preference";
    private static final String PREF_KEY_DYNAMIC_DENY = "dynamic_denylist_preference";

    private final Context mContext;
    private final NetworkPolicyManager mNetworkPolicyManager;

    private static DynamicDenylistManager sInstance;

    /** @return a DynamicDenylistManager object */
    public static DynamicDenylistManager getInstance(Context context) {
        synchronized (DynamicDenylistManager.class) {
            if (sInstance == null) {
                sInstance = new DynamicDenylistManager(context);
            }
            return sInstance;
        }
    }

    DynamicDenylistManager(Context context) {
        mContext = context.getApplicationContext();
        mNetworkPolicyManager = NetworkPolicyManager.from(mContext);
    }

    /** Update the target uid policy in {@link #getManualDenylistPref()}. */
    public void updateManualDenylist(String uid, int policy) {
        if (policy != NetworkPolicyManager.POLICY_REJECT_METERED_BACKGROUND) {
            getManualDenylistPref().edit().remove(uid).apply();
        } else {
            getManualDenylistPref().edit().putInt(uid, policy).apply();
        }
    }

    /** Return true if the target uid is in {@link #getManualDenylistPref()}. */
    public boolean isInManualDenylist(String uid) {
        return getManualDenylistPref().contains(uid);
    }

    /** Clear all data in {@link #getManualDenylistPref()} */
    public void clearManualDenylistPref() {
        getManualDenylistPref().edit().clear().apply();
    }

    /** Clear all data in {@link #getDynamicDenylistPref()} */
    public void clearDynamicDenylistPref() {
        getDynamicDenylistPref().edit().clear().apply();
    }

    @VisibleForTesting
    SharedPreferences getManualDenylistPref() {
        return mContext.getSharedPreferences(PREF_KEY_MANUAL_DENY, Context.MODE_PRIVATE);
    }

    @VisibleForTesting
    SharedPreferences getDynamicDenylistPref() {
        return mContext.getSharedPreferences(PREF_KEY_DYNAMIC_DENY, Context.MODE_PRIVATE);
    }
}
