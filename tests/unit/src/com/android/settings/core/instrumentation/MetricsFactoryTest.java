/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.settings.core.instrumentation;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class MetricsFactoryTest {

    @Test
    public void factoryShouldReuseCachedInstance() {
        MetricsFactory factory1 = MetricsFactory.get();
        MetricsFactory factory2 = MetricsFactory.get();
        assertTrue(factory1 == factory2);
    }

    @Test
    public void factoryShouldCacheLogger() {
        MetricsFactory factory = MetricsFactory.get();
        EventLogWriter logger1 = factory.getLogger();
        EventLogWriter logger2 = factory.getLogger();
        assertTrue(logger1 == logger2);
    }
}
