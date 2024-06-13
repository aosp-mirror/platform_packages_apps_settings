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
import android.util.Log;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A proxy to the subscription manager
 */
public class ProxySubscriptionManager implements LifecycleObserver {

    private static final String LOG_TAG = "ProxySubscriptionManager";

    private static final int LISTENER_END_OF_LIFE = -1;
    private static final int LISTENER_IS_INACTIVE = 0;
    private static final int LISTENER_IS_ACTIVE = 1;

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
        final Looper looper = context.getMainLooper();

        ActiveSubscriptionsListener subscriptionMonitor = new ActiveSubscriptionsListener(
                looper, context) {
            public void onChanged() {
                notifySubscriptionInfoMightChanged();
            }
        };
        GlobalSettingsChangeListener airplaneModeMonitor = new GlobalSettingsChangeListener(
                looper, context, Settings.Global.AIRPLANE_MODE_ON) {
            public void onChanged(String field) {
                subscriptionMonitor.clearCache();
                notifySubscriptionInfoMightChanged();
            }
        };

        init(context, subscriptionMonitor, airplaneModeMonitor);
    }

    @Keep
    @VisibleForTesting
    protected void init(Context context, ActiveSubscriptionsListener activeSubscriptionsListener,
            GlobalSettingsChangeListener airplaneModeOnSettingsChangeListener) {

        mActiveSubscriptionsListeners =
                new ArrayList<OnActiveSubscriptionChangedListener>();
        mPendingNotifyListeners =
                new ArrayList<OnActiveSubscriptionChangedListener>();

        mSubscriptionMonitor = activeSubscriptionsListener;
        mAirplaneModeMonitor = airplaneModeOnSettingsChangeListener;

        mSubscriptionMonitor.start();
    }

    private Lifecycle mLifecycle;
    private ActiveSubscriptionsListener mSubscriptionMonitor;
    private GlobalSettingsChangeListener mAirplaneModeMonitor;

    private List<OnActiveSubscriptionChangedListener> mActiveSubscriptionsListeners;
    private List<OnActiveSubscriptionChangedListener> mPendingNotifyListeners;

    @Keep
    @VisibleForTesting
    protected void notifySubscriptionInfoMightChanged() {
        // create a merged list for processing all listeners
        List<OnActiveSubscriptionChangedListener> listeners =
                new ArrayList<OnActiveSubscriptionChangedListener>(mPendingNotifyListeners);
        listeners.addAll(mActiveSubscriptionsListeners);

        mActiveSubscriptionsListeners.clear();
        mPendingNotifyListeners.clear();
        processStatusChangeOnListeners(listeners);
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
        mSubscriptionMonitor.start();

        // callback notify those listener(s) which back to active state
        List<OnActiveSubscriptionChangedListener> listeners = mPendingNotifyListeners;
        mPendingNotifyListeners = new ArrayList<OnActiveSubscriptionChangedListener>();
        processStatusChangeOnListeners(listeners);
    }

    @OnLifecycleEvent(ON_STOP)
    void onStop() {
        mSubscriptionMonitor.stop();
    }

    @OnLifecycleEvent(ON_DESTROY)
    void onDestroy() {
        mSubscriptionMonitor.close();
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
        return mSubscriptionMonitor.getSubscriptionManager();
    }

    /**
     * Get current max. number active subscription info(s) been setup within device
     *
     * @return max. number of active subscription info(s)
     */
    public int getActiveSubscriptionInfoCountMax() {
        return mSubscriptionMonitor.getActiveSubscriptionInfoCountMax();
    }

    /**
     * Get a list of active subscription info
     *
     * @return A list of active subscription info
     */
    public List<SubscriptionInfo> getActiveSubscriptionsInfo() {
        return mSubscriptionMonitor.getActiveSubscriptionsInfo();
    }

    /**
     * Get an active subscription info with given subscription ID
     *
     * @param subId target subscription ID
     * @return A subscription info which is active list
     */
    public SubscriptionInfo getActiveSubscriptionInfo(int subId) {
        return mSubscriptionMonitor.getActiveSubscriptionInfo(subId);
    }

    /**
     * Get a list of accessible subscription info
     *
     * @return A list of accessible subscription info
     */
    public List<SubscriptionInfo> getAccessibleSubscriptionsInfo() {
        return mSubscriptionMonitor.getAccessibleSubscriptionsInfo();
    }

    /**
     * Get an accessible subscription info with given subscription ID
     *
     * @param subId target subscription ID
     * @return A subscription info which is accessible list
     */
    public SubscriptionInfo getAccessibleSubscriptionInfo(int subId) {
        return mSubscriptionMonitor.getAccessibleSubscriptionInfo(subId);
    }

    /**
     * Gets a list of active, visible subscription Id(s) of the currently active SIM(s).
     *
     * @return the list of subId's that are active and visible; the length may be 0.
     */
    public @NonNull int[] getActiveSubscriptionIdList() {
        return mSubscriptionMonitor.getActiveSubscriptionIdList();
    }

    /**
     * Clear data cached within proxy
     */
    public void clearCache() {
        mSubscriptionMonitor.clearCache();
    }

    /**
     * Add listener to active subscriptions monitor list.
     * Note: listener only take place when change happens.
     *       No immediate callback performed after the invoke of this method.
     *
     * @param listener listener to active subscriptions change
     */
    @Keep
    public void addActiveSubscriptionsListener(OnActiveSubscriptionChangedListener listener) {
        removeSpecificListenerAndCleanList(listener, mPendingNotifyListeners);
        removeSpecificListenerAndCleanList(listener, mActiveSubscriptionsListeners);
        if ((listener == null) || (getListenerState(listener) == LISTENER_END_OF_LIFE)) {
            return;
        }
        mActiveSubscriptionsListeners.add(listener);
    }

    /**
     * Remove listener from active subscriptions monitor list
     *
     * @param listener listener to active subscriptions change
     */
    @Keep
    public void removeActiveSubscriptionsListener(OnActiveSubscriptionChangedListener listener) {
        removeSpecificListenerAndCleanList(listener, mPendingNotifyListeners);
        removeSpecificListenerAndCleanList(listener, mActiveSubscriptionsListeners);
    }

    private int getListenerState(OnActiveSubscriptionChangedListener listener) {
        Lifecycle lifecycle = listener.getLifecycle();
        if (lifecycle == null) {
            return LISTENER_IS_ACTIVE;
        }
        Lifecycle.State lifecycleState = lifecycle.getCurrentState();
        if (lifecycleState == Lifecycle.State.DESTROYED) {
            Log.d(LOG_TAG, "Listener dead detected - " + listener);
            return LISTENER_END_OF_LIFE;
        }
        return lifecycleState.isAtLeast(Lifecycle.State.STARTED) ?
                LISTENER_IS_ACTIVE : LISTENER_IS_INACTIVE;
    }

    private void removeSpecificListenerAndCleanList(OnActiveSubscriptionChangedListener listener,
            List<OnActiveSubscriptionChangedListener> list) {
        // also drop listener(s) which is end of life
        list.removeIf(it -> (it == listener) || (getListenerState(it) == LISTENER_END_OF_LIFE));
    }

    private void processStatusChangeOnListeners(
            List<OnActiveSubscriptionChangedListener> listeners) {
        // categorize listener(s), and end of life listener(s) been ignored
        Map<Integer, List<OnActiveSubscriptionChangedListener>> categorizedListeners =
                listeners.stream()
                .collect(Collectors.groupingBy(it -> getListenerState(it)));

        // have inactive listener(s) in pending list
        categorizedListeners.computeIfPresent(LISTENER_IS_INACTIVE, (category, list) -> {
            mPendingNotifyListeners.addAll(list);
            return list;
        });

        // get active listener(s)
        categorizedListeners.computeIfPresent(LISTENER_IS_ACTIVE, (category, list) -> {
            mActiveSubscriptionsListeners.addAll(list);
            // notify each one of them
            list.stream().forEach(it -> it.onChanged());
            return list;
        });
    }
}
