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

package com.android.settings.testutils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/** A fake {@link Clock} class for testing. */
public final class FakeClock extends Clock {
    private long mCurrentTimeMillis;

    public FakeClock() {}

    /** Sets the time in millis for {@link Clock#millis()} method. */
    public void setCurrentTime(Duration duration) {
        mCurrentTimeMillis = duration.toMillis();
    }

    @Override
    public ZoneId getZone() {
        throw new UnsupportedOperationException("unsupported!");
    }

    @Override
    public Clock withZone(ZoneId zone) {
        throw new UnsupportedOperationException("unsupported!");
    }

    @Override
    public Instant instant() {
        throw new UnsupportedOperationException("unsupported!");
    }

    @Override
    public long millis() {
        return mCurrentTimeMillis;
    }
}
