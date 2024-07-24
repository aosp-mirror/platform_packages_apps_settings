/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.core;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class InstrumentedActivityTest {

    /**
     * Verifies that the {@link InstrumentedActivity} class can be instantiated successfully.
     * Code added to the activity constructor had resulted in an NPE if resources are accessed
     * before onCreate().
     */
    @Test
    public void canInstantiate() {
        Robolectric.buildActivity(InstrumentedActivityTestable.class).setup().get();
    }

    public static class InstrumentedActivityTestable extends InstrumentedActivity {

        @Override
        public int getMetricsCategory() {
            return 0;
        }
    }
}
