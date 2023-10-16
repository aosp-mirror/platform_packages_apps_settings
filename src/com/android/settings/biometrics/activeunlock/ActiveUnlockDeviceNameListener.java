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

package com.android.settings.biometrics.activeunlock;

import android.content.Context;

/** Listens to device name updates from the content provider and fetches the latest value. */
public class ActiveUnlockDeviceNameListener  {
    private static final String TAG = "ActiveUnlockDeviceNameListener";
    private static final String METHOD_NAME = "getDeviceName";
    private static final String DEVICE_NAME_KEY = "com.android.settings.active_unlock.device_name";

    private final ActiveUnlockContentListener mActiveUnlockContentListener;
    public ActiveUnlockDeviceNameListener(
            Context context, ActiveUnlockContentListener.OnContentChangedListener listener) {
        mActiveUnlockContentListener = new ActiveUnlockContentListener(
            context, listener, TAG, METHOD_NAME, DEVICE_NAME_KEY);
    }

    /** Returns whether a device is enrolled in Active Unlock. */
    public boolean hasEnrolled() {
        return mActiveUnlockContentListener.getContent() != null;
    }

    /** Subscribes to device name updates. */
    public void subscribe() {
        mActiveUnlockContentListener.subscribe();
    }

    /** Unsubscribes from device name updates. */
    public void unsubscribe() {
        mActiveUnlockContentListener.unsubscribe();
    }
}
