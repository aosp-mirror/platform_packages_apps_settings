/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.settings.testutils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.settings.SettingsMetricsLogger
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider
import com.android.settingslib.metadata.PreferenceScreenRegistry
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Test rule for metrics. */
class MetricsRule : TestWatcher() {
    val metricsFeatureProvider: MetricsFeatureProvider =
        FakeFeatureFactory.setupForTest().metricsFeatureProvider

    override fun starting(description: Description) {
        val context: Context = ApplicationProvider.getApplicationContext()
        PreferenceScreenRegistry.preferenceUiActionMetricsLogger =
            SettingsMetricsLogger(context, metricsFeatureProvider)
    }

    override fun finished(description: Description) {
        PreferenceScreenRegistry.preferenceUiActionMetricsLogger = null
    }
}
