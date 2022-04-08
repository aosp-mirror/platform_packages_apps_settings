/*
 * Copyright (C) 2019 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.network.telephony;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.ArraySet;

import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Helper class to manage listening to signal strength changes on a set of mobile network
 *  subscriptions */
public class SignalStrengthListener {

    private TelephonyManager mBaseTelephonyManager;
    private Callback mCallback;
    private Map<Integer, PhoneStateListener> mListeners;

    public interface Callback {
        void onSignalStrengthChanged();
    }

    public SignalStrengthListener(Context context, Callback callback) {
        mBaseTelephonyManager = context.getSystemService(TelephonyManager.class);
        mCallback = callback;
        mListeners = new TreeMap<>();
    }

    /** Resumes listening for signal strength changes for the set of ids from the last call to
     * {@link #updateSubscriptionIds(Set)}  */
    public void resume() {
        for (int subId : mListeners.keySet()) {
            startListening(subId);
        }
    }

    /** Pauses listening for signal strength changes */
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
        }
        for (int idToAdd : Sets.difference(ids, currentIds)) {
            PhoneStateListener listener = new PhoneStateListener() {
                @Override
                public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                    mCallback.onSignalStrengthChanged();
                }
            };
            mListeners.put(idToAdd, listener);
            startListening(idToAdd);
        }
    }

    private void startListening(int subId) {
        TelephonyManager mgr = mBaseTelephonyManager.createForSubscriptionId(subId);
        mgr.listen(mListeners.get(subId), PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
    }

    private void stopListening(int subId) {
        TelephonyManager mgr = mBaseTelephonyManager.createForSubscriptionId(subId);
        mgr.listen(mListeners.get(subId), PhoneStateListener.LISTEN_NONE);
    }
}
