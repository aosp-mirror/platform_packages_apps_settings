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
 * limitations under the License.
 */

package com.android.settings.network;

import static androidx.lifecycle.Lifecycle.Event.ON_DESTROY;
import static androidx.lifecycle.Lifecycle.Event.ON_START;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;

import android.content.Context;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * A proxy to the subscription manager
 */
public class ProxySubscriptionManager implements LifecycleObserver {

    /**
     * Interface for monitor active subscriptions list changing
     */
    public interface OnActiveSubscriptionChangedListener {
        /**
         * When active subscriptions list get changed
         */
        void onChanged();
        /**
         * get Lifecycle of listener
         *
         * @return Returns Lifecycle.
         */
        default Lifecycle getLifecycle() {
            return null;
        }
    }

    /**
     * Get proxy instance to subscription manager
     *
     * @return proxy to subscription manager
     */
    public static ProxySubscriptionManager getInstance(Context context) {
        if (sSingleton != null) {
            return sSingleton;
        }
        sSingleton = new ProxySubscriptionManager(context.getApplicationContext());
        return sSingleton;
    }

    private static ProxySubscriptionManager sSingleton;

    private ProxySubscriptionManager(Context context) {
        final Looper looper = Looper.getMainLooper();

        mActiveSubscriptionsListeners =
                new ArrayList<OnActiveSubscriptionChangedListener>();

        mSubsciptionsMonitor = new ActiveSubsciptionsListener(looper, context) {
            public void onChanged() {
                notifyAllListeners();
            }
        };
        mAirplaneModeMonitor = new GlobalSettingsChangeListener(looper,
                context, Settings.Global.AIRPLANE_MODE_ON) {
            public void onChanged(String field) {
                mSubsciptionsMonitor.clearCache();
                notifyAllListeners();
            }
        };

        mSubsciptionsMonitor.start();
    }

    private Lifecycle mLifecycle;
    private ActiveSubsciptionsListener mSubsciptionsMonitor;
    private GlobalSettingsChangeListener mAirplaneModeMonitor;

    private List<OnActiveSubscriptionChangedListener> mActiveSubscriptionsListeners;

    private void notifyAllListeners() {
        for (OnActiveSubscriptionChangedListener listener : mActiveSubscriptionsListeners) {
            final Lifecycle lifecycle = listener.getLifecycle();
            if ((lifecycle == null)
                    || (lifecycle.getCurrentState().isAtLeast(Lifecycle.State.STARTED))) {
                listener.onChanged();
            }
        }
    }

    /**
     * Lifecycle for data within proxy
     *
     * @param lifecycle life cycle to reference
     */
    public void setLifecycle(Lifecycle lifecycle) {
        if (mLifecycle == lifecycle) {
            return;
        }
        if (mLifecycle != null) {
            mLifecycle.removeObserver(this);
        }
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
        mLifecycle = lifecycle;
        mAirplaneModeMonitor.notifyChangeBasedOn(lifecycle);
    }

    @OnLifecycleEvent(ON_START)
    void onStart() {
        mSubsciptionsMonitor.start();
    }

    @OnLifecycleEvent(ON_STOP)
    void onStop() {
        mSubsciptionsMonitor.stop();
    }

    @OnLifecycleEvent(ON_DESTROY)
    void onDestroy() {
        mAirplaneModeMonitor.close();

        if (mLifecycle != null) {
            mLifecycle.removeObserver(this);
            mLifecycle = null;

            sSingleton = null;
        }
    }

    /**
     * Get SubscriptionManager
     *
     * @return a SubscriptionManager
     */
    public SubscriptionManager get() {
        return mSubsciptionsMonitor.getSubscriptionManager();
    }

    /**
     * Get current max. number active subscription info(s) been setup within device
     *
     * @return max. number of active subscription info(s)
     */
    public int getActiveSubscriptionInfoCountMax() {
        return mSubsciptionsMonitor.getActiveSubscriptionInfoCountMax();
    }

    /**
     * Get a list of active subscription info
     *
     * @return A list of active subscription info
     */
    public List<SubscriptionInfo> getActiveSubscriptionsInfo() {
        return mSubsciptionsMonitor.getActiveSubscriptionsInfo();
    }

    /**
     * Get an active subscription info with given subscription ID
     *
     * @param subId target subscription ID
     * @return A subscription info which is active list
     */
    public SubscriptionInfo getActiveSubscriptionInfo(int subId) {
        return mSubsciptionsMonitor.getActiveSubscriptionInfo(subId);
    }

    /**
     * Get a list of accessible subscription info
     *
     * @return A list of accessible subscription info
     */
    public List<SubscriptionInfo> getAccessibleSubscriptionsInfo() {
        return mSubsciptionsMonitor.getAccessibleSubscriptionsInfo();
    }

    /**
     * Get an accessible subscription info with given subscription ID
     *
     * @param subId target subscription ID
     * @return A subscription info which is accessible list
     */
    public SubscriptionInfo getAccessibleSubscriptionInfo(int subId) {
        return mSubsciptionsMonitor.getAccessibleSubscriptionInfo(subId);
    }

    /**
     * Clear data cached within proxy
     */
    public void clearCache() {
        mSubsciptionsMonitor.clearCache();
    }

    /**
     * Add listener to active subscriptions monitor list
     *
     * @param listener listener to active subscriptions change
     */
    public void addActiveSubscriptionsListener(OnActiveSubscriptionChangedListener listener) {
        if (mActiveSubscriptionsListeners.contains(listener)) {
            return;
        }
        mActiveSubscriptionsListeners.add(listener);
    }

    /**
     * Remove listener from active subscriptions monitor list
     *
     * @param listener listener to active subscriptions change
     */
    public void removeActiveSubscriptionsListener(OnActiveSubscriptionChangedListener listener) {
        mActiveSubscriptionsListeners.remove(listener);
    }
}
