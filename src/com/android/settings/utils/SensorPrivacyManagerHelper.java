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

package com.android.settings.utils;

import android.content.Context;
import android.hardware.SensorPrivacyManager;
import android.hardware.SensorPrivacyManager.OnSensorPrivacyChangedListener;
import android.util.ArraySet;
import android.util.SparseArray;

import java.util.concurrent.Executor;

/**
 * A class to help with calls to the sensor privacy manager. This class caches state when needed and
 * multiplexes multiple listeners to a minimal set of binder calls.
 */
public class SensorPrivacyManagerHelper {

    public static final int MICROPHONE = SensorPrivacyManager.Sensors.MICROPHONE;
    public static final int CAMERA = SensorPrivacyManager.Sensors.CAMERA;

    private static SensorPrivacyManagerHelper sInstance;

    private final SensorPrivacyManager mSensorPrivacyManager;

    private final SparseArray<Boolean> mCurrentUserCachedState = new SparseArray<>();
    private final SparseArray<SparseArray<Boolean>> mCachedState = new SparseArray<>();

    private final SparseArray<OnSensorPrivacyChangedListener>
            mCurrentUserServiceListeners = new SparseArray<>();
    private final SparseArray<SparseArray<OnSensorPrivacyChangedListener>>
            mServiceListeners = new SparseArray<>();

    private final ArraySet<CallbackInfo> mCallbacks = new ArraySet<>();

    private final Object mLock = new Object();

    /**
     * Callback for when the state of the sensor privacy changes.
     */
    public interface Callback {

        /**
         * Method invoked when the sensor privacy changes.
         * @param sensor The sensor which changed
         * @param blocked If the sensor is blocked
         */
        void onSensorPrivacyChanged(int sensor, boolean blocked);
    }

    private static class CallbackInfo {
        static final int CURRENT_USER = -1;

        Callback mCallback;
        Executor mExecutor;
        int mSensor;
        int mUserId;

        CallbackInfo(Callback callback, Executor executor, int sensor, int userId) {
            mCallback = callback;
            mExecutor = executor;
            mSensor = sensor;
            mUserId = userId;
        }
    }

    /**
     * Gets the singleton instance
     * @param context The context which is needed if the instance hasn't been created
     * @return the instance
     */
    public static SensorPrivacyManagerHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new SensorPrivacyManagerHelper(context);
        }
        return sInstance;
    }

    /**
     * Only to be used in tests
     */
    private static void clearInstance() {
        sInstance = null;
    }

    private SensorPrivacyManagerHelper(Context context) {
        mSensorPrivacyManager = context.getSystemService(SensorPrivacyManager.class);
    }

    /**
     * Checks if the given toggle is supported on this device
     * @param sensor The sensor to check
     * @return whether the toggle for the sensor is supported on this device.
     */
    public boolean supportsSensorToggle(int sensor) {
        return mSensorPrivacyManager.supportsSensorToggle(sensor);
    }

    /**
     * Checks if the sensor is blocked for the current user. If the user switches and the state of
     * the new user is different, this value will change.
     * @param sensor the sensor to check
     * @return true if the sensor is blocked for the current user
     */
    public boolean isSensorBlocked(int sensor) {
        synchronized (mLock) {
            Boolean blocked = mCurrentUserCachedState.get(sensor);
            if (blocked == null) {
                registerCurrentUserListenerIfNeeded(sensor);

                blocked = mSensorPrivacyManager.isSensorPrivacyEnabled(sensor);
                mCurrentUserCachedState.put(sensor, blocked);
            }

            return blocked;
        }
    }

    /**
     * Checks if the sensor is or would be blocked if the given user is the foreground user
     * @param sensor the sensor to check
     * @param userId the user to check
     * @return true if the sensor is or would be blocked if the given user is the foreground user
     */
    public boolean isSensorBlocked(int sensor, int userId) {
        synchronized (mLock) {
            SparseArray<Boolean> userCachedState = createUserCachedStateIfNeededLocked(userId);
            Boolean blocked = userCachedState.get(sensor);
            if (blocked == null) {
                registerListenerIfNeeded(sensor, userId);

                blocked = mSensorPrivacyManager.isSensorPrivacyEnabled(sensor);
                userCachedState.put(sensor, blocked);
            }

            return blocked;
        }
    }

    /**
     * Sets the sensor privacy for the current user.
     * @param source The source with which sensor privacy is toggled.
     * @param sensor The sensor to set for
     * @param blocked The state to set to
     */
    public void setSensorBlocked(int source, int sensor, boolean blocked) {
        mSensorPrivacyManager.setSensorPrivacy(source, sensor, blocked);
    }

    /**
     * Sets the sensor privacy for the given user.
     * @param source The source with which sensor privacy is toggled.
     * @param sensor The sensor to set for
     * @param blocked The state to set to
     * @param userId The user to set for
     */
    public void setSensorBlocked(int source, int sensor, boolean blocked, int userId) {
        mSensorPrivacyManager.setSensorPrivacy(source, sensor, blocked, userId);
    }

    /**
     * Sets the sensor privacy for the current profile group.
     * @param source The source with which sensor privacy is toggled.
     * @param sensor The sensor to set for
     * @param blocked The state to set to
     */
    public void setSensorBlockedForProfileGroup(int source, int sensor, boolean blocked) {
        mSensorPrivacyManager.setSensorPrivacyForProfileGroup(source, sensor, blocked);
    }

    /**
     * Sets the sensor privacy for the given user's profile group.
     * @param source The source with which sensor privacy is toggled.
     * @param sensor The sensor to set for
     * @param blocked The state to set to
     */
    public void setSensorBlockedForProfileGroup(int source, int sensor, boolean blocked,
            int userId) {
        mSensorPrivacyManager.setSensorPrivacyForProfileGroup(source, sensor, blocked, userId);
    }

    /**
     * Adds a listener for the state of the current user. If the current user changes and the state
     * of the new user is different, a callback will be received.
     * @param sensor The sensor to watch
     * @param callback The callback to invoke
     * @param executor The executor to invoke on
     */
    public void addSensorBlockedListener(int sensor, Callback callback, Executor executor) {
        synchronized (mLock) {
            mCallbacks.add(new CallbackInfo(callback, executor, sensor, CallbackInfo.CURRENT_USER));
        }
    }

    /**
     * Adds a listener for the state of the given user
     * @param sensor The sensor to watch
     * @param callback The callback to invoke
     * @param executor The executor to invoke on
     */
    public void addSensorBlockedListener(int sensor, int userId, Callback callback,
            Executor executor) {
        synchronized (mLock) {
            mCallbacks.add(new CallbackInfo(callback, executor, sensor, userId));
        }
    }

    /**
     * Removes a callback
     * @param callback The callback to remove
     */
    public void removeBlockedListener(Callback callback) {
        synchronized (mLock) {
            mCallbacks.removeIf(callbackInfo -> callbackInfo.mCallback == callback);
        }
    }

    private void registerCurrentUserListenerIfNeeded(int sensor) {
        synchronized (mLock) {
            if (!mCurrentUserServiceListeners.contains(sensor)) {
                OnSensorPrivacyChangedListener listener = (s, enabled) -> {
                    mCurrentUserCachedState.put(sensor, enabled);
                    dispatchStateChangedLocked(sensor, enabled, CallbackInfo.CURRENT_USER);
                };
                mCurrentUserServiceListeners.put(sensor, listener);
                mSensorPrivacyManager.addSensorPrivacyListener(sensor, listener);
            }
        }
    }

    private void registerListenerIfNeeded(int sensor, int userId) {
        synchronized (mLock) {
            SparseArray<OnSensorPrivacyChangedListener>
                    userServiceListeners = createUserServiceListenersIfNeededLocked(userId);

            if (!userServiceListeners.contains(sensor)) {
                OnSensorPrivacyChangedListener listener = (s, enabled) -> {
                    SparseArray<Boolean> userCachedState =
                            createUserCachedStateIfNeededLocked(userId);
                    userCachedState.put(sensor, enabled);
                    dispatchStateChangedLocked(sensor, enabled, userId);
                };
                mCurrentUserServiceListeners.put(sensor, listener);
                mSensorPrivacyManager.addSensorPrivacyListener(sensor, listener);
            }
        }
    }

    private void dispatchStateChangedLocked(int sensor, boolean blocked, int userId) {
        for (CallbackInfo callbackInfo : mCallbacks) {
            if (callbackInfo.mUserId == userId && callbackInfo.mSensor == sensor) {
                Callback callback = callbackInfo.mCallback;
                Executor executor = callbackInfo.mExecutor;

                executor.execute(() -> callback.onSensorPrivacyChanged(sensor, blocked));
            }
        }
    }

    private SparseArray<Boolean> createUserCachedStateIfNeededLocked(int userId) {
        SparseArray<Boolean> userCachedState = mCachedState.get(userId);
        if (userCachedState == null) {
            userCachedState = new SparseArray<>();
            mCachedState.put(userId, userCachedState);
        }
        return userCachedState;
    }

    private SparseArray<OnSensorPrivacyChangedListener> createUserServiceListenersIfNeededLocked(
            int userId) {
        SparseArray<OnSensorPrivacyChangedListener> userServiceListeners =
                mServiceListeners.get(userId);
        if (userServiceListeners == null) {
            userServiceListeners = new SparseArray<>();
            mServiceListeners.put(userId, userServiceListeners);
        }
        return userServiceListeners;
    }
}
