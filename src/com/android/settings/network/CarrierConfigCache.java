/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static android.telephony.CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED;
import static android.telephony.SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX;
import static android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID;

import android.annotation.TestApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is a singleton class for Carrier-Configuration cache.
 */
public class CarrierConfigCache {
    private static final String TAG = "CarrConfCache";

    private static final Object sInstanceLock = new Object();
    /**
     * A singleton {@link CarrierConfigCache} object is used to share with all sub-settings.
     */
    @GuardedBy("sInstanceLock")
    private static CarrierConfigCache sInstance;
    @TestApi
    @GuardedBy("sInstanceLock")
    private static Map<Context, CarrierConfigCache> sTestInstances;

    /**
     * Manages mapping data from the subscription ID to the Carrier-Configuration
     * {@link PersistableBundle} object.
     *
     * The Carrier-Configurations are used to share with all sub-settings.
     */
    @VisibleForTesting
    protected static final Map<Integer, PersistableBundle> sCarrierConfigs =
            new ConcurrentHashMap<>();
    @VisibleForTesting
    protected static CarrierConfigManager sCarrierConfigManager;

    /**
     * Static method to create a singleton class for Carrier-Configuration cache.
     *
     * @param context The Context this is associated with.
     * @return an instance of {@link CarrierConfigCache} object.
     */
    @NonNull
    public static CarrierConfigCache getInstance(@NonNull Context context) {
        synchronized (sInstanceLock) {
            if (sTestInstances != null && sTestInstances.containsKey(context)) {
                CarrierConfigCache testInstance = sTestInstances.get(context);
                Log.w(TAG, "The context owner try to use a test instance:" + testInstance);
                return testInstance;
            }

            if (sInstance != null) return sInstance;

            sInstance = new CarrierConfigCache();
            final CarrierConfigChangeReceiver receiver = new CarrierConfigChangeReceiver();
            final Context appContext = context.getApplicationContext();
            sCarrierConfigManager = appContext.getSystemService(CarrierConfigManager.class);
            appContext.registerReceiver(receiver, new IntentFilter(ACTION_CARRIER_CONFIG_CHANGED),
                    Context.RECEIVER_EXPORTED/*UNAUDITED*/);
            return sInstance;
        }
    }

    /**
     * A convenience method to set pre-prepared instance or mock(CarrierConfigCache.class) for
     * testing.
     *
     * @param context The Context this is associated with.
     * @param instance of {@link CarrierConfigCache} object.
     * @hide
     */
    @TestApi
    @VisibleForTesting
    public static void setTestInstance(@NonNull Context context, CarrierConfigCache instance) {
        synchronized (sInstanceLock) {
            if (sTestInstances == null) sTestInstances = new ConcurrentHashMap<>();

            Log.w(TAG, "Try to set a test instance by context:" + context);
            sTestInstances.put(context, instance);
        }
    }

    /**
     * The constructor can only be accessed from static method inside the class itself, this is
     * to avoid creating a class by adding a private constructor.
     */
    private CarrierConfigCache() {
        // Do nothing.
    }

    /**
     * Returns the boolean If the system service is successfully obtained.
     *
     * @return true value, if the system service is successfully obtained.
     */
    public boolean hasCarrierConfigManager() {
        return (sCarrierConfigManager != null);
    }

    /**
     * Gets the Carrier-Configuration for a particular subscription, which is associated with a
     * specific SIM card. If an invalid subId is used, the returned config will contain default
     * values.
     *
     * @param subId the subscription ID, normally obtained from {@link SubscriptionManager}.
     * @return A {@link PersistableBundle} containing the config for the given subId, or default
     * values for an invalid subId.
     */
    public PersistableBundle getConfigForSubId(int subId) {
        if (sCarrierConfigManager == null) return null;

        synchronized (sCarrierConfigs) {
            if (sCarrierConfigs.containsKey(subId)) {
                return sCarrierConfigs.get(subId);
            }
            final PersistableBundle config = sCarrierConfigManager.getConfigForSubId(subId);
            if (config == null) {
                Log.e(TAG, "Could not get carrier config, subId:" + subId);
                return null;
            }
            sCarrierConfigs.put(subId, config);
            return config;
        }
    }

    /**
     * Gets the Carrier-Configuration for the default subscription.
     *
     * @see #getConfigForSubId
     */
    public PersistableBundle getConfig() {
        if (sCarrierConfigManager == null) return null;

        return getConfigForSubId(SubscriptionManager.getDefaultSubscriptionId());
    }

    private static class CarrierConfigChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!ACTION_CARRIER_CONFIG_CHANGED.equals(intent.getAction())) return;

            final int subId = intent.getIntExtra(EXTRA_SUBSCRIPTION_INDEX, INVALID_SUBSCRIPTION_ID);
            synchronized (sCarrierConfigs) {
                if (SubscriptionManager.isValidSubscriptionId(subId)) {
                    sCarrierConfigs.remove(subId);
                } else {
                    sCarrierConfigs.clear();
                }
            }
        }
    }
}
