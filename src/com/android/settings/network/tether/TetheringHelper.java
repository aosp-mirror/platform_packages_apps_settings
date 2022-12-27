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

package com.android.settings.network.tether;

import android.annotation.NonNull;
import android.annotation.TestApi;
import android.content.Context;
import android.net.TetheringManager;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.android.internal.annotations.GuardedBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helper class for TetheringManager.
 */
public class TetheringHelper implements DefaultLifecycleObserver {
    private static final String TAG = "TetheringHelper";

    private static final Object sInstanceLock = new Object();
    @TestApi
    @GuardedBy("sInstanceLock")
    private static Map<Context, TetheringHelper> sTestInstances;

    protected static Context sAppContext;
    protected static TetheringManager sTetheringManager;
    protected static TetheringEventCallback sTetheringEventCallback = new TetheringEventCallback();
    protected static ArrayList<TetheringManager.TetheringEventCallback> sEventCallbacks =
            new ArrayList<>();

    protected TetheringManager.TetheringEventCallback mCallback;

    /**
     * Static method to create a singleton class for TetheringHelper.
     *
     * @param context The Context this is associated with.
     * @return an instance of {@link TetheringHelper} object.
     */
    @NonNull
    public static TetheringHelper getInstance(@NonNull Context context,
            @NonNull TetheringManager.TetheringEventCallback callback,
            @NonNull Lifecycle lifecycle) {
        synchronized (sInstanceLock) {
            if (sTestInstances != null && sTestInstances.containsKey(context)) {
                TetheringHelper testInstance = sTestInstances.get(context);
                Log.w(TAG, "The context owner use a test instance:" + testInstance);
                return testInstance;
            }

            if (sAppContext == null) {
                sAppContext = context.getApplicationContext();
                sTetheringManager = sAppContext.getSystemService(TetheringManager.class);
            }
            TetheringHelper helper = new TetheringHelper();
            helper.mCallback = callback;
            lifecycle.addObserver(helper);
            return helper;
        }
    }

    /**
     * A convenience method to set pre-prepared instance or mock class for testing.
     *
     * @param context  The Context this is associated with.
     * @param instance of {@link TetheringHelper} object.
     * @hide
     */
    @TestApi
    @VisibleForTesting
    public static void setTestInstance(@NonNull Context context, TetheringHelper instance) {
        synchronized (sInstanceLock) {
            if (sTestInstances == null) sTestInstances = new ConcurrentHashMap<>();
            Log.w(TAG, "Set a test instance by context:" + context);
            sTestInstances.put(context, instance);
        }
    }

    /**
     * The constructor can only be accessed from static method inside the class itself, this is
     * to avoid creating a class by adding a private constructor.
     */
    private TetheringHelper() {
        // Do nothing.
    }

    /**
     * Returns the TetheringManager If the system service is successfully obtained.
     */
    public TetheringManager getTetheringManager() {
        return sTetheringManager;
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (sEventCallbacks.contains(mCallback)) {
            Log.w(TAG, "The callback already contains, don't register callback:" + mCallback);
            return;
        }
        sEventCallbacks.add(mCallback);
        if (sEventCallbacks.size() == 1) {
            sTetheringManager.registerTetheringEventCallback(sAppContext.getMainExecutor(),
                    sTetheringEventCallback);
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (!sEventCallbacks.remove(mCallback)) {
            Log.w(TAG, "The callback does not contain, don't unregister callback:" + mCallback);
            return;
        }
        if (sEventCallbacks.size() == 0) {
            sTetheringManager.unregisterTetheringEventCallback(sTetheringEventCallback);
        }
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        mCallback = null;
    }

    protected static final class TetheringEventCallback implements
            TetheringManager.TetheringEventCallback {
        @Override
        public void onTetheredInterfacesChanged(List<String> interfaces) {
            for (TetheringManager.TetheringEventCallback callback : sEventCallbacks) {
                if (callback == null) continue;
                callback.onTetheredInterfacesChanged(interfaces);
            }
        }
    }
}


