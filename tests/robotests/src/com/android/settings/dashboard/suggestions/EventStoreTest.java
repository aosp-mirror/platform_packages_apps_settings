/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.dashboard.suggestions;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;

import org.robolectric.RuntimeEnvironment;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class EventStoreTest {

    private EventStore mEventStore;

    @Before
    public void setUp() {
        mEventStore = new EventStore(RuntimeEnvironment.application);
    }

    @Test
    public void testWriteRead() {
        mEventStore.writeEvent("pkg", EventStore.EVENT_SHOWN);
        long timeMs = System.currentTimeMillis();
        assertThat(mEventStore.readMetric("pkg", EventStore.EVENT_SHOWN, EventStore.METRIC_COUNT))
            .isEqualTo(1);
        assertThat(Math.abs(timeMs - mEventStore
            .readMetric("pkg", EventStore.EVENT_SHOWN, EventStore.METRIC_LAST_EVENT_TIME)) < 10000)
            .isTrue();
    }

    @Test
    public void testWriteRead_shouldHaveLatestValues() {
        mEventStore.writeEvent("pkg", EventStore.EVENT_DISMISSED);
        mEventStore.writeEvent("pkg", EventStore.EVENT_DISMISSED);
        assertThat(
            mEventStore.readMetric("pkg", EventStore.EVENT_DISMISSED, EventStore.METRIC_COUNT))
            .isEqualTo(2);
    }

    @Test
    public void testWriteRead_shouldReturnDefaultIfNotAvailable() {
        assertThat(mEventStore.readMetric("pkg", EventStore.EVENT_SHOWN, EventStore.METRIC_COUNT))
            .isEqualTo(0);
        assertThat(
            mEventStore
                .readMetric("pkg", EventStore.EVENT_SHOWN, EventStore.METRIC_LAST_EVENT_TIME))
            .isEqualTo(0);
    }

}
