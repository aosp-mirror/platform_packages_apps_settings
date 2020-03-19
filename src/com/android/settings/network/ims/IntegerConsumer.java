/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.network.ims;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

class IntegerConsumer extends Semaphore implements Consumer<Integer> {

    private static final String TAG = "IntegerConsumer";

    IntegerConsumer() {
        super(0);
        mValue = new AtomicInteger();
    }

    private volatile AtomicInteger mValue;

    /**
     * Get boolean value reported from callback
     *
     * @param timeout callback waiting time in milliseconds
     * @return int value reported
     * @throws InterruptedException when thread get interrupted
     */
    int get(long timeout) throws InterruptedException {
        tryAcquire(timeout, TimeUnit.MILLISECONDS);
        return mValue.get();
    }

    /**
     * Implementation of {@link Consumer#accept(Integer)}
     *
     * @param value int reported from {@link Consumer#accept(Integer)}
     */
    public void accept(Integer value) {
        if (value != null) {
            mValue.set(value.intValue());
        }
        release();
    }
}
