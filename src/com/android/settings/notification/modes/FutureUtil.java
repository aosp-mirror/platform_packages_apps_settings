/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.notification.modes;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

class FutureUtil {

    private static final String TAG = "ZenFutureUtil";

    static <V> void whenDone(ListenableFuture<V> future, Consumer<V> consumer, Executor executor) {
        whenDone(future, consumer, executor, "Error in future");
    }

    static <V> void whenDone(ListenableFuture<V> future, Consumer<V> consumer, Executor executor,
            String errorLogMessage, Object... errorLogMessageArgs) {
        Futures.addCallback(future, new FutureCallback<V>() {
            @Override
            public void onSuccess(V v) {
                consumer.accept(v);
            }

            @Override
            public void onFailure(@NonNull Throwable throwable) {
                if (!(throwable instanceof CancellationException)) {
                    Log.e(TAG, String.format(errorLogMessage, errorLogMessageArgs), throwable);
                }
            }
        }, executor);
    }
}
