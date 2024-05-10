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

package com.android.settings.network.helper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;
import java.util.function.Consumer;

/**
 * A {@link BroadcastReceiver} for {@link Intent}.
 *
 * This is {@link BroadcastReceiver} supported by {@link LifecycleCallbackConverter},
 * and only register when state is either START or RESUME.
 */
@VisibleForTesting
public class LifecycleCallbackIntentReceiver extends LifecycleCallbackConverter<Intent> {
    private static final String TAG = "LifecycleCallbackIntentReceiver";

    @VisibleForTesting
    protected final BroadcastReceiver mReceiver;

    private final Runnable mRegisterCallback;
    private final Runnable mUnRegisterCallback;

    /**
     * Constructor
     * @param lifecycle {@link Lifecycle} to monitor
     * @param context for this BroadcastReceiver
     * @param filter the IntentFilter for BroadcastReceiver
     * @param broadcastPermission for permission when listening
     * @param scheduler for running in background thread
     * @param resultCallback for the Intent from BroadcastReceiver
     */
    @VisibleForTesting
    public LifecycleCallbackIntentReceiver(@NonNull Lifecycle lifecycle,
            @NonNull Context context, @NonNull IntentFilter filter,
            String broadcastPermission, Handler scheduler,
            @NonNull Consumer<Intent> resultCallback) {
        super(lifecycle, resultCallback);

        // BroadcastReceiver
        mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (isInitialStickyBroadcast()) {
                    return;
                }
                final String action = intent.getAction();
                if ((action == null) || (action.length() <= 0)) {
                    return;
                }
                postResult(intent);
            }
        };

        // Register operation
        mRegisterCallback = () -> {
            Intent initIntent = context.registerReceiver(mReceiver,
                    filter, broadcastPermission, scheduler);
            if (initIntent != null) {
                postResult(initIntent);
            }
        };

        // Un-Register operation
        mUnRegisterCallback = () -> {
            context.unregisterReceiver(mReceiver);
        };
    }

    @Override
    public void setCallbackActive(boolean isActive) {
        super.setCallbackActive(isActive);
        Runnable op = (isActive) ? mRegisterCallback : mUnRegisterCallback;
        op.run();
    }

    @Override
    public void close() {
        super.close();
        if (isCallbackActive()) {
            setCallbackActive(false);
        }
    }
}
