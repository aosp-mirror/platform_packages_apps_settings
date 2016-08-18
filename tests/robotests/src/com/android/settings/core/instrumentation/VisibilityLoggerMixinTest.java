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

import android.content.Context;

import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@RunWith(RobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class VisibilityLoggerMixinTest {

    @Mock
    private EventLogWriter mLogger;
    private VisibilityLoggerMixin mMixin;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        mMixin = new VisibilityLoggerMixin(new TestInstrumentable(), mLogger);
    }

    @Test
    public void shouldLogVisibleOnResume() {
        mMixin.onResume(null);
        verify(mLogger, times(1))
                .visible(any(Context.class), eq(TestInstrumentable.TEST_METRIC));
    }

    @Test
    public void shouldLogHideOnPause() {
        mMixin.onPause(null);
        verify(mLogger, times(1))
                .hidden(any(Context.class), eq(TestInstrumentable.TEST_METRIC));
    }

    private final class TestInstrumentable implements Instrumentable {

        public static final int TEST_METRIC = 12345;

        @Override
        public int getMetricsCategory() {
            return TEST_METRIC;
        }
    }
}
