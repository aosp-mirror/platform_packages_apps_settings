/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.network.telephony;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyDisplayInfo;
import android.telephony.TelephonyManager;
import android.util.ArraySet;

import com.google.common.collect.Sets;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Help to listen telephony display info change to subscriptions.
 * TODO(b/177647571): unit test is needed.
 */
public class TelephonyDisplayInfoListener {

    private TelephonyManager mBaseTelephonyManager;
    private Callback mCallback;
    private Map<Integer, PhoneStateListener> mListeners;
    private Map<Integer, TelephonyDisplayInfo> mDisplayInfos;

    private static final TelephonyDisplayInfo mDefaultTelephonyDisplayInfo =
            new TelephonyDisplayInfo(TelephonyManager.NETWORK_TYPE_UNKNOWN,
                    TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE);
    /**
     * Interface of callback and to use notify TelephonyDisplayInfo change.
     */
    public interface Callback {
        /**
         * Used to notify TelephonyDisplayInfo change.
         */
        void onTelephonyDisplayInfoChanged(int subId, TelephonyDisplayInfo telephonyDisplayInfo);
    }

    public TelephonyDisplayInfoListener(Context context, Callback callback) {
        mBaseTelephonyManager = context.getSystemService(TelephonyManager.class);
        mCallback = callback;
        mListeners = new HashMap<>();
        mDisplayInfos = new HashMap<>();
    }
    /**
     * Get TelephonyDisplayInfo.
     */
    public TelephonyDisplayInfo getTelephonyDisplayInfo(int subId) {
        return mDisplayInfos.get(subId);
    }

    /** Resumes listening telephony display info changes to the set of ids from the last call to
     * {@link #updateSubscriptionIds(Set)}  */
    public void resume() {
        for (int subId : mListeners.keySet()) {
            startListening(subId);
        }
    }

    /** Pauses listening for telephony display info changes */
    public void pause() {
        for (int subId : mListeners.keySet()) {
            stopListening(subId);
        }
    }

    /** Updates the set of ids we want to be listening for, beginning to listen for any new ids and
     * stopping listening for any ids not contained in the new set */
    public void updateSubscriptionIds(Set<Integer> ids) {
        Set<Integer> currentIds = new ArraySet<>(mListeners.keySet());
        for (int idToRemove : Sets.difference(currentIds, ids)) {
            stopListening(idToRemove);
            mListeners.remove(idToRemove);
            mDisplayInfos.remove(idToRemove);
        }
        for (int idToAdd : Sets.difference(ids, currentIds)) {
            PhoneStateListener listener = new PhoneStateListener() {
                @Override
                public void onDisplayInfoChanged(TelephonyDisplayInfo telephonyDisplayInfo) {
                    mDisplayInfos.put(idToAdd, telephonyDisplayInfo);
                    mCallback.onTelephonyDisplayInfoChanged(idToAdd, telephonyDisplayInfo);
                }
            };
            mDisplayInfos.put(idToAdd, mDefaultTelephonyDisplayInfo);
            mListeners.put(idToAdd, listener);
            startListening(idToAdd);
        }
    }

    private void startListening(int subId) {
        TelephonyManager mgr = mBaseTelephonyManager.createForSubscriptionId(subId);
        mgr.listen(mListeners.get(subId), PhoneStateListener.LISTEN_DISPLAY_INFO_CHANGED);
    }

    private void stopListening(int subId) {
        TelephonyManager mgr = mBaseTelephonyManager.createForSubscriptionId(subId);
        mgr.listen(mListeners.get(subId), PhoneStateListener.LISTEN_NONE);
    }
}
