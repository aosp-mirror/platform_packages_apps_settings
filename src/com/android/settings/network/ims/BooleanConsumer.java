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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

class BooleanConsumer extends Semaphore implements Consumer<Boolean> {

    private static final String TAG = "BooleanConsumer";

    BooleanConsumer() {
        super(0);
        mValue = new AtomicBoolean();
    }

    private volatile AtomicBoolean mValue;

    /**
     * Get boolean value reported from callback
     *
     * @param timeout callback waiting time in milliseconds
     * @return boolean value reported
     * @throws InterruptedException when thread get interrupted
     */
    boolean get(long timeout) throws InterruptedException {
        tryAcquire(timeout, TimeUnit.MILLISECONDS);
        return mValue.get();
    }

    /**
     * Implementation of {@link Consumer#accept(Boolean)}
     *
     * @param value boolean reported from {@link Consumer#accept(Boolean)}
     */
    public void accept(Boolean value) {
        if (value != null) {
            mValue.set(value.booleanValue());
        }
        release();
    }
}
