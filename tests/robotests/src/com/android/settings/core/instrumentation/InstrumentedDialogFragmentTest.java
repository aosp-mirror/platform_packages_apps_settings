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

import static com.google.common.truth.Truth.assertThat;

import android.os.Bundle;

import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class InstrumentedDialogFragmentTest {

    public static class TestDialogFragment extends InstrumentedDialogFragment {
        static final int TEST_METRIC = 1234;

        public MetricsFeatureProvider getMetricsFeatureProvider() {
            return mMetricsFeatureProvider;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            assertThat(mMetricsFeatureProvider).isNotNull();
        }

        @Override
        public int getMetricsCategory() {
            return TEST_METRIC;
        }
    }

    @Test
    public void runThroughFragmentLifecycles_shouldHaveMetricsFeatureProviderOnAttach() {
        final TestDialogFragment fragment = new TestDialogFragment();

        // Precondition: no metrics feature
        assertThat(fragment.getMetricsFeatureProvider()).isNull();

        fragment.onAttach(RuntimeEnvironment.application);

        // Verify: has metrics feature
        assertThat(fragment.getMetricsFeatureProvider()).isNotNull();
    }
}
